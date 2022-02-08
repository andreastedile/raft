package it.unitn.ds2.raft.states.leader;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.eventstream.EventStream;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.StashBuffer;
import akka.actor.typed.javadsl.TimerScheduler;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.events.StateChange;
import it.unitn.ds2.raft.fields.LogEntry;
import it.unitn.ds2.raft.fields.Servers;
import it.unitn.ds2.raft.fields.Suspicions;
import it.unitn.ds2.raft.rpc.*;
import it.unitn.ds2.raft.simulation.Command;
import it.unitn.ds2.raft.simulation.Crash;
import it.unitn.ds2.raft.simulation.Stop;
import it.unitn.ds2.raft.states.Server;
import it.unitn.ds2.raft.states.follower.Follower;
import it.unitn.ds2.raft.states.follower.FollowerState;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public final class Leader extends Server {
    public static Behavior<Raft> elected(ActorContext<Raft> ctx, Servers servers, LeaderState state) {
        var event = new StateChange(ctx.getSelf(), ctx.getSystem().uptime(), StateChange.State.LEADER);
        var publish = new EventStream.Publish<>(event);
        ctx.getSystem().eventStream().tell(publish);

        state.nextIndex.setCtx(ctx);
        state.matchIndex.setCtx(ctx);

        var suspicions = new Suspicions();
        suspicions.setCtx(ctx);

        return Behaviors.withStash(10, (StashBuffer<Raft> stash) ->
                Behaviors.withTimers(timers -> {
                    startPeriodicHeartbeat(timers);

                    return Behaviors.receive(Raft.class)
                            .onMessage(Command.class, msg -> onCommand(ctx, stash, servers, state, msg))
                            .onMessage(PeriodicHeartbeat.class, msg -> onPeriodicHeartbeat(ctx, servers, suspicions, state))
                            .onMessage(AppendEntriesRPCResponse.class, msg -> onAppendEntriesResult(ctx, stash, timers, servers, suspicions, state, msg))
                            .onMessage(RPCTimeout.class, msg -> onRPCTimeout(ctx, suspicions, state, msg))
                            .onMessage(RequestVoteRPC.class, msg -> onRequestVoteRPC(ctx, timers, servers, state, msg))
                            .onMessage(Crash.class, msg -> crash(ctx, timers, servers, state, msg))
                            .onMessage(Stop.class, msg -> stop(ctx, timers, servers, state))
                            .onAnyMessage(msg -> Behaviors.same())
                            .build();
                }));
    }

    private static Behavior<Raft> onCommand(ActorContext<Raft> ctx, StashBuffer<Raft> stash, Servers servers, LeaderState state, Command msg) {
        ctx.getLog().debug("Received command " + msg);
        if (stash.isEmpty()) {
            var entry = new LogEntry(msg.command, state.currentTerm.get());
            state.log.append(entry);
            servers.getAll().forEach(server -> appendEntriesRPC(ctx, state, server, false, properties.rpcTimeoutMs));
        } else {
            ctx.getLog().debug(stash.size() + " commands still needs to be processed. Postponing the command");
            stash.stash(msg);
        }
        return Behaviors.same();
    }

    private static void startPeriodicHeartbeat(TimerScheduler<Raft> timers) {
        timers.startTimerAtFixedRate("periodic heartbeat", new PeriodicHeartbeat(), Duration.ofMillis(properties.heartbeatMs));
    }

    private static void stopPeriodicHeartbeat(TimerScheduler<Raft> timers) {
        timers.cancel("periodic heartbeat");
    }

    private static Behavior<Raft> onPeriodicHeartbeat(ActorContext<Raft> ctx, Servers servers, Suspicions suspicions, LeaderState state) {
        servers.getAll().stream()
                .filter(Predicate.not(suspicions::isSuspected))
                .forEach(server -> appendEntriesRPC(ctx, state, server, true, properties.heartbeatMs));
        return Behaviors.same();
    }

    private static void appendEntriesRPC(ActorContext<Raft> ctx, LeaderState state,
                                         ActorRef<Raft> recipient, boolean isHeartbeat, long timeoutMs) {
        var appendEntries = createAppendEntries(ctx, state, recipient, isHeartbeat);
        if (isHeartbeat) {
            ctx.getLog().debug("Sending " + appendEntries + " to " + recipient.path().name() + " (heartbeat)");
        } else {
            ctx.getLog().debug("Sending " + appendEntries + " to " + recipient.path().name());
        }
        ctx.ask(AppendEntriesResult.class, // resClass
                recipient, // target
                Duration.ofMillis(timeoutMs), // responseTimeout
                (ActorRef<AppendEntriesResult> replyTo) -> new AppendEntriesRPC(ctx.getSelf(), replyTo, appendEntries), // createRequest
                (response, throwable) -> { // applyToResponse
                    if (response != null) {
                        return new AppendEntriesRPCResponse(recipient, appendEntries, response);
                    }
                    return new RPCTimeout(recipient);
                }
        );
    }

    private static Behavior<Raft> onRPCTimeout(ActorContext<Raft> ctx, Suspicions suspicions, LeaderState state, RPCTimeout msg) {
        ctx.getLog().debug("RPC timeout waiting for " + msg.server.path().name());
        suspicions.suspect(msg.server);
        appendEntriesRPC(ctx, state, msg.server, false, properties.rpcRetryMs);
        return Behaviors.same();
    }

    private static Behavior<Raft> onAppendEntriesResult(ActorContext<Raft> ctx, StashBuffer<Raft> stash, TimerScheduler<Raft> timers,
                                                        Servers servers,
                                                        Suspicions suspicions,
                                                        LeaderState state,
                                                        AppendEntriesRPCResponse msg) {
        ctx.getLog().debug("Received " + msg);

        suspicions.unsuspect(msg.sender);

        if (msg.res.term > state.currentTerm.get()) {
            ctx.getLog().debug("Message's term is " + msg.res.term + ", currentTerm is " + state.currentTerm);
            stopPeriodicHeartbeat(timers);
            state.currentTerm.set(msg.res.term);
            state.votedFor.set(null);
            return Follower.waitForAppendEntries(ctx, servers, FollowerState.fromAnyState(state));
        }

        if (msg.res.success) {
            if (!msg.req.isHeartbeat()) {
                // If successful: update nextIndex and matchIndex for follower
                state.nextIndex.increment(msg.sender);
                state.matchIndex.increment(msg.sender);
            }

            // If there exists an N such that N > commitIndex,
            // a majority of matchIndex[i] ≥ N, and log[N].term == currentTerm:
            // set commitIndex = N
            IntStream.rangeClosed(state.commitIndex.get() + 1, state.log.lastLogIndex())
                    .forEach(N -> {
                        long count = state.matchIndex.values().stream().filter(index -> index >= N).count() + 1;
                        if (count >= majority(servers.size())) {
                            if (state.log.get(N).term == state.currentTerm.get()) {
                                state.commitIndex.increment(); // todo: ?
                                state.lastApplied.increment();
                                stash.unstash(Behaviors.same(), 1, Function.identity());
                            }
                        }
                    });
        } else {
            state.nextIndex.decrement(msg.sender);

            // Act quickly to repair the follower's log
            appendEntriesRPC(ctx, state, msg.sender, false, properties.rpcTimeoutMs);
        }

        return Behaviors.same();
    }

    private static AppendEntries createAppendEntries(ActorContext<Raft> ctx, LeaderState state, ActorRef<Raft> server, boolean isHeartbeat) {
        int prevLogIndex = state.nextIndex.get(server) - 1; // prevLogIndex – index of log entry immediately preceding new ones.
        int prevLogTerm = prevLogIndex > 0 ? state.log.get(prevLogIndex).term : 0; // term of prevLogIndex entry.
        return new AppendEntries(ctx.getSelf(), // sender – for the receiver to reply to.
                state.currentTerm.get(), // term – leader’s term.
                ctx.getSelf(), // leader id
                prevLogIndex,
                prevLogTerm,
                isHeartbeat ? List.of() : state.log.getFrom(state.nextIndex.get(server)), // entries – log entries to store.
                state.commitIndex.get()); // leaderCommit – leader’s commitIndex.
    }

    private static Behavior<Raft> onRequestVoteRPC(ActorContext<Raft> ctx, TimerScheduler<Raft> timers,
                                                   Servers servers, LeaderState state, RequestVoteRPC msg) {
        ctx.getLog().debug("Received " + msg);

        if (msg.req.term > state.currentTerm.get()) {
            ctx.getLog().debug("Message's term is " + msg.req.term + ", currentTerm is " + state.currentTerm);
            stopPeriodicHeartbeat(timers);
            state.currentTerm.set(msg.req.term);
            state.votedFor.set(null);
            ctx.getSelf().tell(msg);
            return Follower.waitForAppendEntries(ctx, servers, FollowerState.fromAnyState(state));
        }

        return onRequestVoteRPC(ctx, state, msg);
    }

    private static Behavior<Raft> crash(ActorContext<Raft> ctx, TimerScheduler<Raft> timers,
                                        Servers servers, LeaderState state, Crash msg) {
        stopPeriodicHeartbeat(timers);
        return crash(ctx, servers, state, msg);
    }

    private static Behavior<Raft> stop(ActorContext<Raft> ctx, TimerScheduler<Raft> timers,
                                       Servers servers, LeaderState state) {
        stopPeriodicHeartbeat(timers);
        return stop(ctx, servers, state);
    }
}
