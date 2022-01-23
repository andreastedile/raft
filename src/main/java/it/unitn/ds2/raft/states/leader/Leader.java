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
import it.unitn.ds2.raft.fields.SeqNum;
import it.unitn.ds2.raft.fields.Servers;
import it.unitn.ds2.raft.rpc.AppendEntries;
import it.unitn.ds2.raft.rpc.AppendEntriesResult;
import it.unitn.ds2.raft.rpc.RPCTimeout;
import it.unitn.ds2.raft.simulation.Command;
import it.unitn.ds2.raft.simulation.Crash;
import it.unitn.ds2.raft.simulation.Stop;
import it.unitn.ds2.raft.states.Server;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

public final class Leader extends Server {
    public static Behavior<Raft> elected(ActorContext<Raft> ctx, Servers servers, SeqNum seqNum, LeaderState state) {
        state.nextIndex.setCtx(ctx);
        state.matchIndex.setCtx(ctx);

        var event = new StateChange(ctx.getSelf(), ctx.getSystem().uptime(), StateChange.State.LEADER);
        var publish = new EventStream.Publish<>(event);
        ctx.getSystem().eventStream().tell(publish);


        return Behaviors.withTimers(timers ->
                Behaviors.withStash(10, (StashBuffer<Raft> stash) -> {
                    servers.getAll().forEach(server -> appendEntriesRPC(ctx, timers, seqNum, state, server, true));

                    return Behaviors.intercept(() -> checkSeqNum(timers, seqNum),
                            Behaviors.intercept(() -> checkTerm(timers, servers, state),
                                    Behaviors.receive(Raft.class)
                                            .onMessage(Command.class, msg -> onCommand(ctx, stash, timers, servers, seqNum, state, msg))
                                            .onMessage(AppendEntriesResult.class, msg -> onAppendEntriesResult(ctx, stash, timers, servers, seqNum, state, msg))
                                            .onMessage(RPCTimeout.class, msg -> onRPCTimeout(ctx, timers, seqNum, state, msg))
                                            .onMessage(Crash.class, msg -> crash(ctx, timers, servers, state, msg))
                                            .onMessage(Stop.class, msg -> stop(ctx, timers, servers, state))
                                            .build()
                            )
                    );
                })
        );
    }

    private static Behavior<Raft> onCommand(ActorContext<Raft> ctx, StashBuffer<Raft> stash, TimerScheduler<Raft> timers, Servers servers, SeqNum seqNum, LeaderState state, Command msg) {
        ctx.getLog().debug("Received command " + msg);
        if (stash.isEmpty()) {
            var entry = new LogEntry(msg.command, state.currentTerm.get());
            state.log.append(entry);
            servers.getAll().forEach(server -> appendEntriesRPC(ctx, timers, seqNum, state, server, false));
        } else {
            ctx.getLog().debug(stash.size() + " it.unitn.ds2.gui.commands still needs processing. Stashing the command");
            stash.stash(msg);
        }
        return Behaviors.same();
    }

    private static void appendEntriesRPC(ActorContext<Raft> ctx, TimerScheduler<Raft> timers, SeqNum seqNum, LeaderState state, ActorRef<Raft> server, boolean isHeartbeat) {
        startRPCTimeout(ctx, timers, server);
        var appendEntries = createAppendEntries(ctx, seqNum, state, server, isHeartbeat);
        seqNum.setLastSent(server, appendEntries); // todo: improvement
        if (isHeartbeat) {
            ctx.getLog().debug("Sending heartbeat " + appendEntries + " to " + server.path().name());
        } else {
            ctx.getLog().debug("Sending appendEntries " + appendEntries + " to " + server.path().name());
        }
        server.tell(appendEntries);
    }

    public static void startRPCTimeout(ActorContext<Raft> ctx, TimerScheduler<Raft> timers, ActorRef<Raft> server) {
        ctx.getLog().debug("RPC timeout for " + server.path().name() + " ← " + properties.rpcTimeoutMs + "ms");
        timers.startSingleTimer(server, new RPCTimeout(server), Duration.ofMillis(properties.rpcTimeoutMs));
    }

    private static Behavior<Raft> onRPCTimeout(ActorContext<Raft> ctx, TimerScheduler<Raft> timers, SeqNum seqNum, LeaderState state, RPCTimeout msg) {
        ctx.getLog().debug("RPC timeout waiting for " + msg.server.path().name());
        appendEntriesRPC(ctx, timers, seqNum, state, msg.server, false);
        return Behaviors.same();
    }

    private static Behavior<Raft> onAppendEntriesResult(ActorContext<Raft> ctx, StashBuffer<Raft> stash, TimerScheduler<Raft> timers,
                                                        Servers servers,
                                                        SeqNum seqNum,
                                                        LeaderState state,
                                                        AppendEntriesResult msg) {
        ctx.getLog().debug("Received " + msg);

        if (msg.seqNum != seqNum.expectedSeqNum(msg.sender)) {
            ctx.getLog().debug("Discarded because sequence numbers don't match " +
                    "(message's seqNum is " + msg.seqNum + ", expected " + seqNum.expectedSeqNum(msg.sender));
            return Behaviors.same();
        }

        if (msg.success) {
            var sent = (AppendEntries) seqNum.getMsg(msg.sender);
            if (!sent.isHeartbeat()) {
                // If successful: update nextIndex and matchIndex for follower
                state.nextIndex.increment(msg.sender);
                state.matchIndex.increment(msg.sender);
            }

            // If there exists an N such that N > commitIndex,
            // a majority of matchIndex[i] ≥ N, and log[N].term == currentTerm:
            // set commitIndex = N
            IntStream.rangeClosed(state.commitIndex.get() + 1, state.log.lastLogIndex())
                    .forEach(N -> {
                        long count = state.nextIndex.values().stream().filter(index -> index >= N).count() + 1;
                        if (count >= majority(servers.size())) {
                            if (state.log.get(N).term == state.currentTerm.get()) {
                                state.commitIndex.increment(); // todo: ?
                                state.lastApplied.increment();
                                stash.unstash(Behaviors.same(), 1, Function.identity());
                            }
                        }
                    });

            appendEntriesRPC(ctx, timers, seqNum, state, msg.sender, true);
        } else {
            state.nextIndex.decrement(msg.sender);

            appendEntriesRPC(ctx, timers, seqNum, state, msg.sender, false);
        }

        return Behaviors.same();
    }

    private static AppendEntries createAppendEntries(ActorContext<Raft> ctx, SeqNum seqNum, LeaderState state, ActorRef<Raft> server, boolean isHeartbeat) {
        int prevLogIndex = state.nextIndex.get(server) - 1; // prevLogIndex – index of log entry immediately preceding new ones.
        int prevLogTerm = prevLogIndex > 0 ? state.log.get(prevLogIndex).term : 0; // term of prevLogIndex entry.
        return new AppendEntries(ctx.getSelf(), // sender – for the receiver to reply to.
                seqNum.computeNext(server), // seqNum – for receiver to check if message was reordered.
                state.currentTerm.get(), // term – leader’s term.
                ctx.getSelf(), // leader id
                prevLogIndex,
                prevLogTerm,
                isHeartbeat ? List.of() : state.log.getFrom(state.nextIndex.get(server)), // entries – log entries to store.
                state.commitIndex.get()); // leaderCommit – leader’s commitIndex.
    }
}
