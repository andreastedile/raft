package it.unitn.ds2.raft.states.candidate;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.TimerScheduler;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.fields.SeqNum;
import it.unitn.ds2.raft.fields.Servers;
import it.unitn.ds2.raft.fields.Votes;
import it.unitn.ds2.raft.rpc.*;
import it.unitn.ds2.raft.states.Server;
import it.unitn.ds2.raft.states.follower.Follower;
import it.unitn.ds2.raft.states.follower.FollowerState;
import it.unitn.ds2.raft.states.leader.Leader;
import it.unitn.ds2.raft.states.leader.LeaderState;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public final class Candidate extends Server {
    private static long randomElectionTimeout() {
        return ThreadLocalRandom.current().nextLong(properties.minElectionTimeoutMs, properties.maxElectionTimeoutMs);
    }

    public static Behavior<Raft> beginElection(ActorContext<Raft> ctx, Servers servers, CandidateState state) {
        Votes votes = new Votes(servers.getAll());
        votes.setCtx(ctx);
        SeqNum seqNum = new SeqNum(servers.getAll());
        seqNum.setCtx(ctx);

        ctx.getLog().debug("Begin election");
        state.currentTerm.increment();
        state.votedFor.set(ctx.getSelf());
        votes.clear();
        votes.addVote(ctx.getSelf(), true);

        if (votes.nGranted() == majority(servers.size() + 1)) {
            ctx.getLog().debug("Election won!");
            return Leader.elected(ctx, servers, seqNum, LeaderState.fromState(servers, state));
        }

        return Behaviors.withTimers(timers -> {
            startElectionTimer(ctx, timers);
            servers.getAll().forEach(server -> sendRequestVote(ctx, timers, seqNum, server, state));

            return Behaviors.intercept(() -> checkSeqNum(timers, seqNum),
                    Behaviors.intercept(() -> checkTerm(timers, servers, state),
                            Behaviors.receive(Raft.class)
                                    .onMessage(Vote.class, msg -> onVote(ctx, timers, servers, seqNum, votes, state, msg))
                                    .onMessage(RPCTimeout.class, msg -> onRPCTimeout(ctx, timers, seqNum, state, msg))
                                    .onMessage(ElectionTimeout.class, msg -> onElectionTimeout(ctx, timers, servers, state))
                                    .onMessage(AppendEntries.class, msg -> onAppendEntries(ctx, timers, servers, state, msg))
                                    .build())
            );
        });
    }

    private static void startElectionTimer(ActorContext<Raft> ctx, TimerScheduler<Raft> timers) {
        long timeout = randomElectionTimeout();
        timers.startSingleTimer("election timeout", new ElectionTimeout(), Duration.ofMillis(timeout));
        ctx.getLog().debug("Election timeout ← " + timeout + "ms");
    }

    public static void sendRequestVote(ActorContext<Raft> ctx, TimerScheduler<Raft> timers, SeqNum seqNum, ActorRef<Raft> server, CandidateState state) {
        startRPCTimeout(ctx, timers, server);

        var request = new RequestVote(ctx.getSelf(), seqNum.computeNext(server), state.currentTerm.get(), ctx.getSelf(), state.log.lastLogIndex(), state.log.lastLogTerm());
        ctx.getLog().debug("Requesting vote " + request + server.path().name());
        server.tell(request);
    }

    public static void startRPCTimeout(ActorContext<Raft> ctx, TimerScheduler<Raft> timers, ActorRef<Raft> server) {
        ctx.getLog().debug("RPC timeout for " + server.path().name() + " ← " + properties.rpcTimeoutMs + "ms");
        timers.startSingleTimer(server, new RPCTimeout(server), Duration.ofMillis(properties.rpcTimeoutMs));
    }

    private static Behavior<Raft> onRPCTimeout(ActorContext<Raft> ctx, TimerScheduler<Raft> timers, SeqNum seqNum, CandidateState state, RPCTimeout msg) {
        ctx.getLog().debug("RPC timeout waiting for " + msg.server.path().name());
        sendRequestVote(ctx, timers, seqNum, msg.server, state);
        return Behaviors.same();
    }

    private static Behavior<Raft> onElectionTimeout(ActorContext<Raft> ctx, TimerScheduler<Raft> timers, Servers servers, CandidateState state) {
        ctx.getLog().debug("Election timeout!");
        servers.getAll().forEach(timers::cancel);
        return beginElection(ctx, servers, state);
    }

    private static Behavior<Raft> onVote(ActorContext<Raft> ctx, TimerScheduler<Raft> timers,
                                         Servers servers, SeqNum seqNum, Votes votes,
                                         CandidateState state, Vote msg) {
        ctx.getLog().debug("Received " + msg);

        votes.addVote(msg.sender, msg.voteGranted);
        timers.cancel(msg.sender);

        if (votes.nGranted() == majority(servers.size() + 1)) {
            ctx.getLog().debug("Election won!");
            servers.getAll().forEach(timers::cancel);
            return Leader.elected(ctx, servers, seqNum, LeaderState.fromState(servers, state));
        } else if (votes.nDenied() == majority(servers.size() + 1)) {
            ctx.getLog().debug("Election lost");
            servers.getAll().forEach(timers::cancel);
            return Follower.waitForAppendEntries(ctx, servers, FollowerState.fromAnyState(state));
        }

        // else: more votes required to take a decision
        return Behaviors.same();
    }

    private static Behavior<Raft> onAppendEntries(ActorContext<Raft> ctx, TimerScheduler<Raft> timers, Servers servers, CandidateState state, AppendEntries msg) {
        ctx.getLog().debug("Received " + msg);

        if (msg.term < state.currentTerm.get()) {
            ctx.getLog().debug("Sender is lagging (message's term is " + msg.term + ", currentTerm is " + state.currentTerm.get() + ")");
            return Behaviors.same();
        }
        // If AppendEntries RPC received from new leader: convert to follower
        timers.cancel("election timeout");
        servers.getAll().forEach(timers::cancel);
        return Follower.waitForAppendEntries(ctx, servers, FollowerState.fromAnyState(state));
    }
}
