package it.unitn.ds2.raft.states.follower;

import akka.actor.typed.Behavior;
import akka.actor.typed.eventstream.EventStream;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.TimerScheduler;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.events.Spawn;
import it.unitn.ds2.raft.fields.Servers;
import it.unitn.ds2.raft.rpc.AppendEntries;
import it.unitn.ds2.raft.rpc.AppendEntriesResult;
import it.unitn.ds2.raft.rpc.ElectionTimeout;
import it.unitn.ds2.raft.rpc.RequestVote;
import it.unitn.ds2.raft.simulation.Join;
import it.unitn.ds2.raft.simulation.Start;
import it.unitn.ds2.raft.states.Server;
import it.unitn.ds2.raft.states.candidate.Candidate;
import it.unitn.ds2.raft.states.candidate.CandidateState;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public final class Follower extends Server {
    private static long randomElectionTimeout() {
        return ThreadLocalRandom.current().nextLong(properties.minElectionTimeoutMs, properties.maxElectionTimeoutMs);
    }

    public static Behavior<Raft> create() {
        var servers = new Servers();

        return Behaviors.setup(ctx -> {
                    ctx.setLoggerName(ctx.getSystem().name());

                    var spawn = new Spawn(ctx.getSelf(), ctx.getSystem().uptime());
                    var publish = new EventStream.Publish<>(spawn);
                    ctx.getSystem().eventStream().tell(publish);

                    return Behaviors.receive(Raft.class)
                            .onMessage(Join.class, msg -> onJoin(ctx, servers, msg))
                            .onMessage(Start.class, msg -> onStart(ctx, servers))
                            .build();
                }
        );
    }

    public static Behavior<Raft> onJoin(ActorContext<Raft> ctx, Servers servers, Join msg) {
        ctx.getLog().info(msg.server + " joined the Raft cluster");
        servers.add(msg.server);
        return Behaviors.same();
    }

    private static Behavior<Raft> onStart(ActorContext<Raft> ctx, Servers servers) {
        ctx.getLog().info("Starting");

        var state = new FollowerState();
        state.currentTerm.setCtx(ctx);
        state.votedFor.setCtx(ctx);
        state.log.setCtx(ctx);
        state.commitIndex.setCtx(ctx);
        state.lastApplied.setCtx(ctx);

        return waitForAppendEntries(ctx, servers, state);
    }

    public static Behavior<Raft> waitForAppendEntries(ActorContext<Raft> ctx, Servers servers, FollowerState state) {
        return Behaviors.withTimers(timers -> {
            startElectionTimer(ctx, timers);

            return Behaviors.intercept(() -> checkTerm(timers, servers, state),
                    Behaviors.receive(Raft.class)
                            .onMessage(AppendEntries.class, msg -> onAppendEntries(ctx, timers, state, msg))
                            .onMessage(ElectionTimeout.class, msg -> onElectionTimeout(ctx, servers, state))
                            .onMessage(RequestVote.class, msg -> requestVoteRPC(ctx, state, msg))
                            .build());
        });
    }

    private static void startElectionTimer(ActorContext<Raft> ctx, TimerScheduler<Raft> timers) {
        long timeout = randomElectionTimeout();
        timers.startSingleTimer("election timeout", new ElectionTimeout(), Duration.ofMillis(timeout));
        ctx.getLog().debug("Election timeout ← " + timeout + "ms");
    }

    private static Behavior<Raft> onElectionTimeout(ActorContext<Raft> ctx, Servers servers, FollowerState state) {
        ctx.getLog().debug("Election timeout!");
        return Candidate.beginElection(ctx, servers, CandidateState.fromState(state));
    }

    private static Behavior<Raft> onAppendEntries(ActorContext<Raft> ctx, TimerScheduler<Raft> timers, FollowerState state, AppendEntries msg) {
        ctx.getLog().debug("Received " + msg);

        startElectionTimer(ctx, timers);

        // 1. Reply false if term < currentTerm
        if (msg.term < state.currentTerm.get()) {
            ctx.getLog().debug("Sender is lagging (message's term is " + msg.term + ", currentTerm is " + state.currentTerm.get() + ")");
            var result = new AppendEntriesResult(ctx.getSelf(), msg.seqNum, state.currentTerm.get(), false);
            msg.sender.tell(result);
            return Behaviors.same();
        }
        // 2. Reply false if log doesn’t contain an entry at prevLogIndex whose term matches prevLogTerm
        if (msg.prevLogIndex > 0 && state.log.lastLogIndex() == 0) {
            ctx.getLog().debug("Lagging (message's prevLogIndex is " + msg.prevLogIndex + ", lastLogIndex is 0)");
            var result = new AppendEntriesResult(ctx.getSelf(), msg.seqNum, state.currentTerm.get(), false);
            msg.sender.tell(result);
            return Behaviors.same();
        }
        if (msg.prevLogIndex > 0 && state.log.get(msg.prevLogIndex).term != msg.prevLogTerm) {
            ctx.getLog().debug("Lagging (message's prevLogTerm is " + msg.prevLogTerm + ", but log has " + state.log.get(msg.prevLogIndex).term);
            var result = new AppendEntriesResult(ctx.getSelf(), msg.seqNum, state.currentTerm.get(), false);
            msg.sender.tell(result);
            return Behaviors.same();
        }
        // 3. If an existing entry conflicts with a new one (same index but different terms),
        // delete the existing entry and all that follow it
        // 4. Append any new entries not already in the log
        state.log.storeEntries(msg.prevLogIndex, msg.entries);

        // 5. If leaderCommit > commitIndex, set
        // commitIndex = min(leaderCommit, index of last new entry)
        if (msg.leaderCommit > state.commitIndex.get()) {
            state.commitIndex.set(Math.min(msg.leaderCommit, state.log.lastLogIndex()));
            // All Servers:
            // If commitIndex > lastApplied: increment lastApplied, apply
            // log[lastApplied] to servers.state machine
            if (state.commitIndex.get() > state.lastApplied.get()) {
                state.lastApplied.set(state.commitIndex.get());
            }
        }

        var result = new AppendEntriesResult(ctx.getSelf(), msg.seqNum, state.currentTerm.get(), true);
        msg.sender.tell(result);

        return Behaviors.same();
    }
}
