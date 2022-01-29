package it.unitn.ds2.raft.states;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.fields.Servers;
import it.unitn.ds2.raft.properties.SimulationProperties;
import it.unitn.ds2.raft.rpc.RequestVoteRPC;
import it.unitn.ds2.raft.rpc.Vote;
import it.unitn.ds2.raft.simulation.Crash;
import it.unitn.ds2.raft.states.offline.Offline;

public class Server {

    protected final static SimulationProperties properties = SimulationProperties.getInstance();

    protected static Behavior<Raft> onVote(ActorContext<Raft> ctx, State state, RequestVoteRPC msg) {
        ctx.getLog().debug("Received " + msg);

        // Receiver implementation:
        // 2. If votedFor is null or candidateId,
        // and candidate’s log is at least as up-to-date as receiver’s log, grant vote
        if (msg.req.term < state.currentTerm.get()) {
            // 1. Reply false if term < currentTerm
            ctx.getLog().debug("Sender is lagging (message's term is " + msg.req.term + ", currentTerm is " + state.currentTerm.get());
            ctx.getLog().debug("Denying vote to " + msg.req.candidateId.path().name());
            var vote = new Vote(ctx.getSelf(), state.currentTerm.get(), false);
            msg.replyTo.tell(vote);
        } else if (state.votedFor.get() == null || state.votedFor.get().equals(msg.req.candidateId)) {
            ctx.getLog().debug("Granting vote to " + msg.req.candidateId.path().name());
            state.votedFor.set(msg.sender);
            var vote = new Vote(ctx.getSelf(), state.currentTerm.get(), true);
            msg.replyTo.tell(vote);
        } else {
            ctx.getLog().debug("Denying vote to " + msg.req.candidateId.path().name());
            var vote = new Vote(ctx.getSelf(), state.currentTerm.get(), false);
            msg.replyTo.tell(vote);
        }
        return Behaviors.same();
    }

    protected static Behavior<Raft> stop(ActorContext<Raft> ctx, Servers servers, State state) {
        ctx.getLog().info("Received stop command. State is:\n" + state);

        return Offline.waiting(ctx, servers, state);
    }

    protected static Behavior<Raft> crash(ActorContext<Raft> ctx, Servers servers, State state, Crash msg) {
        if (msg.duration == null) {
            ctx.getLog().info("Received crash command. State is:\n" + state);
        } else {
            ctx.getLog().info("Received crash command, crashing for " + msg.duration.toMillis() + "ms. State is:\n" + state);
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
