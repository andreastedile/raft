package it.unitn.ds2.raft.rpc;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;

public final class Vote extends AbstractRaftMsg implements RaftResponse {
    public final boolean voteGranted;

    /**
     * @param sender      of the message.
     * @param term        currentTerm, for candidate to update itself.
     * @param voteGranted true means candidate received vote.
     */
    public Vote(ActorRef<Raft> sender, int term, boolean voteGranted) {
        super(sender, term);
        this.voteGranted = voteGranted;
    }

    @Override
    public String toString() {
        return "Vote{" +
                "term=" + term +
                ", voteGranted=" + voteGranted +
                '}';
    }
}
