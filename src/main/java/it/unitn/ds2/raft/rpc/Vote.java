package it.unitn.ds2.raft.rpc;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;

public class Vote extends AbstractRPCMsg implements RPCResponse {
    public final boolean voteGranted;

    /**
     * @param sender      for the receiver to reply to.
     * @param seqNum      for sender to check if reply was reordered.
     * @param term        currentTerm, for candidate to update itself.
     * @param voteGranted true means candidate received vote.
     */
    public Vote(ActorRef<Raft> sender, int seqNum, int term, boolean voteGranted) {
        super(sender, seqNum, term);
        this.voteGranted = voteGranted;
    }

    @Override
    public String toString() {
        return "Vote{" +
                "sender=" + sender.path().name() +
                ", seqNum=" + seqNum +
                ", term=" + term +
                ", voteGranted=" + voteGranted +
                '}';
    }

    @Override
    public ActorRef<RPC> sender() {
        return sender.narrow();
    }

    @Override
    public int seqNum() {
        return seqNum;
    }
}
