package it.unitn.ds2.raft.states.candidate;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.eventstream.EventStream;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.TimerScheduler;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.events.StateChange;
import it.unitn.ds2.raft.fields.Servers;
import it.unitn.ds2.raft.fields.Suspicions;
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
        var event = new StateChange(ctx.getSelf(), ctx.getSystem().uptime(), StateChange.State.CANDIDATE);
        var publish = new EventStream.Publish<>(event);
        ctx.getSystem().eventStream().tell(publish);

        ctx.getLog().debug("Begin election");

        var suspicions = new Suspicions();
        suspicions.setCtx(ctx);

        Votes votes = new Votes(servers.getAll());
        votes.setCtx(ctx);

        state.currentTerm.increment();
        state.votedFor.set(ctx.getSelf());
        votes.addVote(ctx.getSelf(), true);

        if (votes.nGranted() == majority(servers.size() + 1)) {
            ctx.getLog().debug("Election won!");
            return Leader.elected(ctx, servers, LeaderState.fromState(servers, state));
        }

        return Behaviors.withTimers(timers -> {
            startElectionTimer(ctx, timers);
            servers.getAll().forEach(server -> requestVoteRPC(ctx, server, state, properties.rpcTimeoutMs));

            return Behaviors.receive(Raft.class)
                    .onMessage(RequestVoteRPCResponse.class, msg -> onRequestVoteRPCResponse(ctx, timers, servers, suspicions, votes, state, msg))
                    .onMessage(RPCTimeout.class, msg -> onRPCTimeout(ctx, suspicions, state, msg.server))
                    .onMessage(ElectionTimeout.class, msg -> onElectionTimeout(ctx, servers, suspicions, state))
                    .onMessage(AppendEntriesRPC.class, msg -> onAppendEntriesRPC(ctx, timers, servers, state, msg))
                    .onMessage(RequestVoteRPC.class, msg -> onRequestVoteRPC(ctx, timers, servers, state, msg))
                    .onMessage(Crash.class, msg -> crash(ctx, timers, servers, state, msg))
                    .onMessage(Stop.class, msg -> stop(ctx, timers, servers, state))
                    .onAnyMessage(msg -> Behaviors.same())
                    .build();
        });
    }

    private static void startElectionTimer(ActorContext<Raft> ctx, TimerScheduler<Raft> timers) {
        long timeout = randomElectionTimeout();
        timers.startSingleTimer("election timeout", new ElectionTimeout(), Duration.ofMillis(timeout));
        ctx.getLog().debug("Election timeout ← " + timeout + "ms");
    }

    private static void cancelElectionTimer(TimerScheduler<Raft> timers) {
        timers.cancel("election timeout");
    }

    public static void requestVoteRPC(ActorContext<Raft> ctx, ActorRef<Raft> recipient, CandidateState state, long timeoutMs) {
        var request = new VoteRequest(ctx.getSelf(), state.currentTerm.get(), ctx.getSelf(), state.log.lastLogIndex(), state.log.lastLogTerm());
        ctx.getLog().debug("Requesting " + request + " to " + recipient.path().name());

        ctx.ask(Vote.class, // resClass
                recipient, // target
                Duration.ofMillis(timeoutMs), // responseTimeout
                (ActorRef<Vote> replyTo) -> new RequestVoteRPC(ctx.getSelf(), replyTo, request), // createRequest
                (response, throwable) -> { // applyToResponse
                    if (response != null) {
                        return new RequestVoteRPCResponse(recipient, request, response);
                    }
                    return new RPCTimeout(recipient);
                }
        );
    }

    private static Behavior<Raft> onRPCTimeout(ActorContext<Raft> ctx, Suspicions suspicions, CandidateState state, ActorRef<Raft> recipient) {
        ctx.getLog().debug("RPC timeout waiting for " + recipient.path().name());
        suspicions.suspect(recipient);
        requestVoteRPC(ctx, recipient, state, properties.rpcRetryMs);
        return Behaviors.same();
    }

    private static Behavior<Raft> onElectionTimeout(ActorContext<Raft> ctx, Servers servers, Suspicions suspicions, CandidateState state) {
        ctx.getLog().debug("Election timeout!");
        servers.getAll().forEach(suspicions::unsuspect);
        return beginElection(ctx, servers, state);
    }

    private static Behavior<Raft> onRequestVoteRPCResponse(ActorContext<Raft> ctx, TimerScheduler<Raft> timers,
                                                           Servers servers, Suspicions suspicions, Votes votes,
                                                           CandidateState state, RequestVoteRPCResponse msg) {
        suspicions.unsuspect(msg.sender);

        if (msg.res.term > state.currentTerm.get()) {
            ctx.getLog().debug("Message's term is " + msg.res.term + ", currentTerm is " + state.currentTerm);
            cancelElectionTimer(timers);
            state.currentTerm.set(msg.res.term);
            state.votedFor.set(null);
            return Follower.waitForAppendEntries(ctx, servers, FollowerState.fromAnyState(state));
        }

        votes.addVote(msg.sender, msg.res.voteGranted);

        if (votes.nGranted() == majority(servers.size() + 1)) {
            // votes.nGranted() includes the candidate's own vote, so subtract 1
            ctx.getLog().debug("Election won! Collected " + (votes.nGranted() - 1) + " votes, " + (votes.nGranted() - 1) + " granted, " + votes.nDenied() + " denied");
            cancelElectionTimer(timers);
            return Leader.elected(ctx, servers, LeaderState.fromState(servers, state));
        }
        if (votes.nDenied() == majority(servers.size() + 1)) {
            ctx.getLog().debug("Election lost! Collected " + (votes.nDenied()) + " votes, " + (votes.nGranted() - 1) + " granted, " + votes.nDenied() + " denied");
            cancelElectionTimer(timers);
            return Follower.waitForAppendEntries(ctx, servers, FollowerState.fromAnyState(state));
        }

        // else: more votes required to take a decision
        return Behaviors.same();
    }

    private static Behavior<Raft> onAppendEntriesRPC(ActorContext<Raft> ctx, TimerScheduler<Raft> timers, Servers servers, CandidateState state, AppendEntriesRPC msg) {
        ctx.getLog().debug("Received " + msg);

        if (msg.req.term > state.currentTerm.get()) {
            ctx.getLog().debug("Message's term is " + msg.req.term + ", currentTerm is " + state.currentTerm);
            cancelElectionTimer(timers);
            state.currentTerm.set(msg.req.term);
            state.votedFor.set(null);
            ctx.getSelf().tell(msg);
            return Follower.waitForAppendEntries(ctx, servers, FollowerState.fromAnyState(state));
        }

        if (msg.req.term < state.currentTerm.get()) {
            ctx.getLog().debug("Message's term is " + msg.req.term + ", currentTerm is " + state.currentTerm.get() + ": ignoring");
            return Behaviors.same();
        }
        // If AppendEntries RPC received from new leader: convert to follower
        cancelElectionTimer(timers);
        return Follower.waitForAppendEntries(ctx, servers, FollowerState.fromAnyState(state));
    }

    private static Behavior<Raft> onRequestVoteRPC(ActorContext<Raft> ctx, TimerScheduler<Raft> timers,
                                                   Servers servers, CandidateState state, RequestVoteRPC msg) {
        ctx.getLog().debug("Received " + msg);

        if (msg.req.term > state.currentTerm.get()) {
            ctx.getLog().debug("Message's term is " + msg.req.term + ", currentTerm is " + state.currentTerm);
            cancelElectionTimer(timers);
            state.currentTerm.set(msg.req.term);
            state.votedFor.set(null);
            ctx.getSelf().tell(msg);
            return Follower.waitForAppendEntries(ctx, servers, FollowerState.fromAnyState(state));
        }

        return Server.onRequestVoteRPC(ctx, state, msg);
    }

    private static Behavior<Raft> crash(ActorContext<Raft> ctx, TimerScheduler<Raft> timers,
                                        Servers servers, CandidateState state, Crash msg) {
        cancelElectionTimer(timers);
        return crash(ctx, servers, state, msg);
    }

    private static Behavior<Raft> stop(ActorContext<Raft> ctx, TimerScheduler<Raft> timers,
                                       Servers servers, CandidateState state) {
        cancelElectionTimer(timers);
        return stop(ctx, servers, state);
    }
}
