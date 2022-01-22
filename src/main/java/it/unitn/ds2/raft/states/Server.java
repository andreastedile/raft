package it.unitn.ds2.raft.states;

import akka.actor.typed.Behavior;
import akka.actor.typed.BehaviorInterceptor;
import akka.actor.typed.TypedActorContext;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.TimerScheduler;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.fields.SeqNum;
import it.unitn.ds2.raft.fields.Servers;
import it.unitn.ds2.raft.properties.SimulationProperties;
import it.unitn.ds2.raft.rpc.AbstractRPCMsg;
import it.unitn.ds2.raft.rpc.RPCResponse;
import it.unitn.ds2.raft.rpc.RequestVote;
import it.unitn.ds2.raft.rpc.Vote;
import it.unitn.ds2.raft.simulation.Crash;
import it.unitn.ds2.raft.simulation.Start;
import it.unitn.ds2.raft.states.follower.Follower;
import it.unitn.ds2.raft.states.follower.FollowerState;
import it.unitn.ds2.raft.states.offline.Offline;

public class Server {

    protected final static SimulationProperties properties = SimulationProperties.getInstance();

    protected static Behavior<Raft> requestVoteRPC(ActorContext<Raft> ctx, State state, RequestVote msg) {
        ctx.getLog().debug("Received " + msg);

        // Receiver implementation:
        // 2. If votedFor is null or candidateId,
        // and candidate’s log is at least as up-to-date as receiver’s log, grant vote
        if (msg.term < state.currentTerm.get()) {
            // 1. Reply false if term < currentTerm
            ctx.getLog().debug("Sender is lagging (message's term is " + msg.term + ", currentTerm is " + state.currentTerm.get());
            ctx.getLog().debug("Denying vote to " + msg.candidateId);
            var vote = new Vote(ctx.getSelf(), msg.seqNum, state.currentTerm.get(), false);
            msg.sender.tell(vote);
        } else if (state.votedFor.get() == null || state.votedFor.get().equals(msg.candidateId)) {
            ctx.getLog().debug("Granting vote to " + msg.candidateId);
            state.votedFor.set(msg.sender);
            var vote = new Vote(ctx.getSelf(), msg.seqNum, state.currentTerm.get(), true);
            msg.sender.tell(vote);
        } else {
            ctx.getLog().debug("Denying vote to " + msg.candidateId);
            var vote = new Vote(ctx.getSelf(), msg.seqNum, state.currentTerm.get(), false);
            msg.sender.tell(vote);
        }
        return Behaviors.same();
    }

    protected static BehaviorInterceptor<Raft, Raft> checkTerm(TimerScheduler<Raft> timers, Servers servers, State state) {
        return new BehaviorInterceptor<>(Raft.class) {
            @Override
            public Behavior<Raft> aroundReceive(TypedActorContext<Raft> ctx, Raft msg, ReceiveTarget<Raft> target) {
                if (msg instanceof AbstractRPCMsg rpc) {
                    if (rpc.term > state.currentTerm.get()) {
                        ctx.asJava().getLog().debug("Received " + msg);
                        ctx.asJava().getLog().debug("Lagging (message's term is " + rpc.term + ", currentTerm is " + state.currentTerm);
                        state.currentTerm.set(rpc.term);
                        state.votedFor.set(null);
                        timers.cancelAll();
                        return Follower.waitForAppendEntries(ctx.asJava(), servers, FollowerState.fromAnyState(state));
                    }
                }
                return target.apply(ctx, msg);
            }
        };
    }

    protected static BehaviorInterceptor<Raft, Raft> checkSeqNum(TimerScheduler<Raft> timers, SeqNum seqNum) {
        return new BehaviorInterceptor<>(Raft.class) {
            @Override
            public Behavior<Raft> aroundReceive(TypedActorContext<Raft> ctx, Raft msg, ReceiveTarget<Raft> target) {
                if (msg instanceof RPCResponse rpc) {
                    if (rpc.seqNum() != seqNum.expectedSeqNum(rpc.sender().unsafeUpcast())) {
                        ctx.asJava().getLog().debug("Discarded because sequence numbers don't match " +
                                "(message's seqNum is " + rpc.seqNum() + ", expected " + seqNum.expectedSeqNum(rpc.sender().unsafeUpcast()));
                        return Behaviors.same();
                    }
                }
                return target.apply(ctx, msg);
            }
        };
    }

    protected static Behavior<Raft> stop(ActorContext<Raft> ctx, TimerScheduler<Raft> timers,
                                         Servers servers, State state) {
        timers.cancelAll();

        ctx.getLog().info("Received stop command. State is:\n" + state);

        return Offline.waiting(ctx, servers, state);
    }

    protected static Behavior<Raft> crash(ActorContext<Raft> ctx, TimerScheduler<Raft> timers,
                                          Servers servers, State state, Crash msg) {
        timers.cancelAll();

        if (msg.duration == null) {
            ctx.getLog().info("Received crash command. State is:\n" + state);
        } else {
            ctx.getLog().info("Received crash command, crashing for " + msg.duration.toMillis() + "ms. State is:\n" + state);
        }

        // Schedule a message to self for recovery
        if (msg.duration != null) {
            timers.startSingleTimer("restart", new Start(), msg.duration);
        }

        return Offline.waiting(ctx, servers, state);
    }

    protected static int majority(int groupSize) {
        // Group size | Majority | Failures tolerated
        //      1     |     1    |     0
        //      2     |     2    |     0
        //      3     |     2    |     1
        //      4     |     3    |     1
        //      5     |     3    |     2
        //      6     |     4    |     2
        //      7     |     4    |     3
        if (groupSize % 2 == 0) {
            return groupSize / 2 + 1;
        }
        return (int) Math.ceil(groupSize / 2.);
    }

}
