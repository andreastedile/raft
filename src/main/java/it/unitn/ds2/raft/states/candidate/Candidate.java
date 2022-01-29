package it.unitn.ds2.raft.states.candidate;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.eventstream.EventStream;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.TimerScheduler;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.events.StateChange;
import it.unitn.ds2.raft.fields.SeqNum;
import it.unitn.ds2.raft.fields.Servers;
import it.unitn.ds2.raft.fields.Votes;
import it.unitn.ds2.raft.rpc.*;
import it.unitn.ds2.raft.simulation.Crash;
import it.unitn.ds2.raft.simulation.Stop;
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

        var event = new StateChange(ctx.getSelf(), ctx.getSystem().uptime(), StateChange.State.CANDIDATE);
        var publish = new EventStream.Publish<>(event);
        ctx.getSystem().eventStream().tell(publish);

        ctx.getLog().debug("Begin election");
        state.currentTerm.increment();
        state.votedFor.set(ctx.getSelf());
        votes.addVote(ctx.getSelf(), true);

        if (votes.nGranted() == majority(servers.size() + 1)) {
            ctx.getLog().debug("Election won!");
            return Leader.elected(ctx, servers, seqNum, LeaderState.fromState(servers, state));
        }

        return Behaviors.withTimers(timers -> {
            startElectionTimer(ctx, timers);
            servers.getAll().forEach(server -> sendRequestVote(ctx, seqNum, server, state));

            return Behaviors.receive(Raft.class)
                    .onMessage(RequestVoteRPCResponse.class, msg -> onVote(ctx, timers, servers, seqNum, votes, state, msg))
                    .onMessage(RPCTimeout.class, msg -> onRPCTimeout(ctx, seqNum, state, msg.server))
                    .onMessage(ElectionTimeout.class, msg -> onElectionTimeout(ctx, servers, state))
                    .onMessage(AppendEntriesRPC.class, msg -> onAppendEntries(ctx, timers, servers, state, msg))
                    .onMessage(Crash.class, msg -> crash(ctx, timers, servers, state, msg))
                    .onMessage(Stop.class, msg -> stop(ctx, timers, servers, state))
                    .onAnyMessage(msg -> Behaviors.ignore())
                    .build();
        });
    }

    private static void startElectionTimer(ActorContext<Raft> ctx, TimerScheduler<Raft> timers) {
        long timeout = randomElectionTimeout();
        timers.startSingleTimer("election timeout", new ElectionTimeout(), Duration.ofMillis(timeout));
        ctx.getLog().debug("Election timeout ← " + timeout + "ms");
    }

    public static void sendRequestVote(ActorContext<Raft> ctx, SeqNum seqNum, ActorRef<Raft> recipient, CandidateState state) {
        var request = new VoteRequest(ctx.getSelf(), state.currentTerm.get(), ctx.getSelf(), state.log.lastLogIndex(), state.log.lastLogTerm());
        ctx.getLog().debug("Requesting vote " + request + ", timeout ← " + properties.rpcTimeoutMs + "ms");

        ctx.ask(Vote.class, // resClass
                recipient, // target
                Duration.ofMillis(properties.rpcTimeoutMs), // responseTimeout
                (ActorRef<Vote> replyTo) -> new RequestVoteRPC(ctx.getSelf(), seqNum.computeNext(recipient), replyTo, request), // createRequest
                (response, throwable) -> { // applyToResponse
                    if (response != null) {
                        return new RequestVoteRPCResponse(recipient, seqNum.expectedSeqNum(recipient), request, response);
                    }
                    return new RPCTimeout(recipient);
                }
        );
    }

    private static Behavior<Raft> onRPCTimeout(ActorContext<Raft> ctx, SeqNum seqNum, CandidateState state, ActorRef<Raft> recipient) {
        ctx.getLog().debug("RPC timeout waiting for " + recipient.path().name());
        sendRequestVote(ctx, seqNum, recipient, state);
        return Behaviors.same();
    }

    private static Behavior<Raft> onElectionTimeout(ActorContext<Raft> ctx, Servers servers, CandidateState state) {
        ctx.getLog().debug("Election timeout!");
        return beginElection(ctx, servers, state);
    }

    private static Behavior<Raft> onVote(ActorContext<Raft> ctx, TimerScheduler<Raft> timers,
                                         Servers servers, SeqNum seqNum, Votes votes,
                                         CandidateState state, RequestVoteRPCResponse msg) {
        if (msg.seqNum < seqNum.expectedSeqNum(msg.sender)) {
            ctx.getLog().debug("Discarded " + msg + " because sequence numbers don't match " +
                    "(message's seqNum is " + msg.seqNum + ", expected " + seqNum.expectedSeqNum(msg.sender));
            return Behaviors.same();
        }

        if (msg.res.term > state.currentTerm.get()) {
            ctx.getLog().debug("Received " + msg);
            ctx.getLog().debug("Lagging (message's term is " + msg.res.term + ", currentTerm is " + state.currentTerm);
            state.currentTerm.set(msg.res.term);
            state.votedFor.set(null);
            timers.cancel("election timeout");
            return Follower.waitForAppendEntries(ctx.asJava(), servers, FollowerState.fromAnyState(state));
        }

        votes.addVote(msg.sender, msg.res.voteGranted);

        if (votes.nGranted() == majority(servers.size() + 1)) {
            ctx.getLog().debug("Election won!");
            return Leader.elected(ctx, servers, seqNum, LeaderState.fromState(servers, state));
        } else if (votes.nDenied() == majority(servers.size() + 1)) {
            ctx.getLog().debug("Election lost");
            return Follower.waitForAppendEntries(ctx, servers, FollowerState.fromAnyState(state));
        }

        // else: more votes required to take a decision
        return Behaviors.same();
    }

    private static Behavior<Raft> onAppendEntries(ActorContext<Raft> ctx, TimerScheduler<Raft> timers, Servers servers, CandidateState state, AppendEntriesRPC msg) {
        ctx.getLog().debug("Received " + msg);

        if (msg.req.term > state.currentTerm.get()) {
            ctx.getLog().debug("Received " + msg);
            ctx.getLog().debug("Lagging (message's term is " + msg.req.term + ", currentTerm is " + state.currentTerm);
            state.currentTerm.set(msg.req.term);
            state.votedFor.set(null);
            timers.cancel("election timeout");
            return Follower.waitForAppendEntries(ctx.asJava(), servers, FollowerState.fromAnyState(state));
        }

        if (msg.req.term < state.currentTerm.get()) {
            ctx.getLog().debug("Sender is lagging (message's term is " + msg.req.term + ", currentTerm is " + state.currentTerm.get() + ")");
            return Behaviors.same();
        }
        // If AppendEntries RPC received from new leader: convert to follower
        timers.cancel("election timeout");
        return Follower.waitForAppendEntries(ctx, servers, FollowerState.fromAnyState(state));
    }

    private static Behavior<Raft> crash(ActorContext<Raft> ctx, TimerScheduler<Raft> timers,
                                        Servers servers, CandidateState state, Crash msg) {
        timers.cancel("election timeout");
        return crash(ctx, servers, state, msg);
    }

    private static Behavior<Raft> stop(ActorContext<Raft> ctx, TimerScheduler<Raft> timers,
                                       Servers servers, CandidateState state) {
        timers.cancel("election timeout");
        return stop(ctx, servers, state);
    }
}
