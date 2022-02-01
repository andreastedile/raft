package it.unitn.ds2.raft.states.follower;

import akka.actor.typed.Behavior;
import akka.actor.typed.eventstream.EventStream;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.TimerScheduler;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.events.StateChange;
import it.unitn.ds2.raft.fields.Servers;
import it.unitn.ds2.raft.rpc.AppendEntriesRPC;
import it.unitn.ds2.raft.rpc.AppendEntriesResult;
import it.unitn.ds2.raft.rpc.ElectionTimeout;
import it.unitn.ds2.raft.rpc.RequestVoteRPC;
import it.unitn.ds2.raft.simulation.Crash;
import it.unitn.ds2.raft.simulation.Stop;
import it.unitn.ds2.raft.states.Server;
import it.unitn.ds2.raft.states.candidate.Candidate;
import it.unitn.ds2.raft.states.candidate.CandidateState;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public final class Follower extends Server {
    private static long randomElectionTimeout() {
        return ThreadLocalRandom.current().nextLong(properties.minElectionTimeoutMs, properties.maxElectionTimeoutMs);
    }

    public static Behavior<Raft> waitForAppendEntries(ActorContext<Raft> ctx, Servers servers, FollowerState state) {
        ctx.getLog().debug("Waiting for append entries");

        var event = new StateChange(ctx.getSelf(), ctx.getSystem().uptime(), StateChange.State.FOLLOWER);
        var publish = new EventStream.Publish<>(event);
        ctx.getSystem().eventStream().tell(publish);

        return Behaviors.withTimers(timers -> {
            startElectionTimer(ctx, timers);

            return Behaviors.receive(Raft.class)
                    .onMessage(AppendEntriesRPC.class, msg -> onAppendEntriesRPC(ctx, timers, servers, state, msg))
                    .onMessage(ElectionTimeout.class, msg -> onElectionTimeout(ctx, servers, state))
                    .onMessage(RequestVoteRPC.class, msg -> onRequestVoteRPC(ctx, timers, servers, state, msg))
                    .onMessage(Crash.class, msg -> crash(ctx, timers, servers, state, msg))
                    .onMessage(Stop.class, msg -> stop(ctx, timers, servers, state))
                    .onAnyMessage(msg -> Behaviors.ignore())
                    .build();
        });
    }

    private static void startElectionTimer(ActorContext<Raft> ctx, TimerScheduler<Raft> timers) {
        long timeout = randomElectionTimeout();
        timers.startSingleTimer("election timeout", new ElectionTimeout(), Duration.ofMillis(timeout));
    }

    private static Behavior<Raft> onElectionTimeout(ActorContext<Raft> ctx, Servers servers, FollowerState state) {
        ctx.getLog().debug("Election timeout!");
        return Candidate.beginElection(ctx, servers, CandidateState.fromState(state));
    }

    private static Behavior<Raft> onAppendEntriesRPC(ActorContext<Raft> ctx, TimerScheduler<Raft> timers, Servers servers, FollowerState state, AppendEntriesRPC msg) {
        ctx.getLog().debug("Received " + msg);

        if (msg.req.term > state.currentTerm.get()) {
            ctx.getLog().debug("Leader's term is " + msg.req.term + ", currentTerm is " + state.currentTerm);
            timers.cancel("election timeout");
            state.currentTerm.set(msg.req.term);
            state.votedFor.set(null);
            ctx.getSelf().tell(msg);
            return Behaviors.same();
        }

        startElectionTimer(ctx, timers);

        // 1. Reply false if term < currentTerm
        if (msg.req.term < state.currentTerm.get()) {
            ctx.getLog().debug("Leader's term is " + msg.req.term + ", currentTerm is " + state.currentTerm.get() + ": ignoring");
            var result = new AppendEntriesResult(ctx.getSelf(), state.currentTerm.get(), false);
            msg.replyTo.tell(result);
            return Behaviors.same();
        }
        // 2. Reply false if log doesn't contain an entry at prevLogIndex whose term matches prevLogTerm
        if (msg.req.prevLogIndex > 0 && state.log.lastLogIndex() == 0) {
            ctx.getLog().debug("Leader's prevLogIndex is " + msg.req.prevLogIndex + ", lastLogIndex is 0: ignoring");
            var result = new AppendEntriesResult(ctx.getSelf(), state.currentTerm.get(), false);
            msg.replyTo.tell(result);
            return Behaviors.same();
        }
        if (msg.req.prevLogIndex > 0 && state.log.get(msg.req.prevLogIndex).term != msg.req.prevLogTerm) {
            ctx.getLog().debug("Leader's prevLogTerm is " + msg.req.prevLogTerm + ", but log has " + state.log.get(msg.req.prevLogIndex).term + ": ignoring");
            var result = new AppendEntriesResult(ctx.getSelf(), state.currentTerm.get(), false);
            msg.replyTo.tell(result);
            return Behaviors.same();
        }
        // 3. If an existing entry conflicts with a new one (same index but different terms),
        // delete the existing entry and all that follow it
        // 4. Append any new entries not already in the log
        state.log.storeEntries(msg.req.prevLogIndex + 1, msg.req.entries);

        // 5. If leaderCommit > commitIndex, set
        // commitIndex = min(leaderCommit, index of last new entry)
        if (msg.req.leaderCommit > state.commitIndex.get()) {
            state.commitIndex.set(Math.min(msg.req.leaderCommit, state.log.lastLogIndex()));
            // All Servers:
            // If commitIndex > lastApplied: increment lastApplied, apply
            // log[lastApplied] to servers.state machine
            if (state.commitIndex.get() > state.lastApplied.get()) {
                state.lastApplied.set(state.commitIndex.get());
            }
        }

        var result = new AppendEntriesResult(ctx.getSelf(), state.currentTerm.get(), true);
        msg.replyTo.tell(result);

        return Behaviors.same();
    }

    private static Behavior<Raft> onRequestVoteRPC(ActorContext<Raft> ctx, TimerScheduler<Raft> timers,
                                                   Servers servers, FollowerState state, RequestVoteRPC msg) {
        ctx.getLog().debug("Received " + msg);

        if (msg.req.term > state.currentTerm.get()) {
            ctx.getLog().debug("Leader's term is " + msg.req.term + ", currentTerm is " + state.currentTerm);
            timers.cancel("election timeout");
            state.currentTerm.set(msg.req.term);
            state.votedFor.set(null);
            ctx.getSelf().tell(msg);
            return Behaviors.same();
        }

        return onRequestVoteRPC(ctx, state, msg);
    }

    private static Behavior<Raft> crash(ActorContext<Raft> ctx, TimerScheduler<Raft> timers,
                                        Servers servers, FollowerState state, Crash msg) {
        timers.cancel("election timeout");
        return crash(ctx, servers, state, msg);
    }

    private static Behavior<Raft> stop(ActorContext<Raft> ctx, TimerScheduler<Raft> timers,
                                       Servers servers, FollowerState state) {
        timers.cancel("election timeout");
        return stop(ctx, servers, state);
    }
}
