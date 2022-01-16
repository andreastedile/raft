package it.unitn.ds2.raft.rpc;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;

public class AppendEntriesResult extends AbstractRPCMsg implements RPCResponse {
    public final boolean success;

    /**
     * @param sender  for the receiver to reply to.
     * @param seqNum  for sender to check if reply was reordered.
     * @param term    currentTerm, for leader to update itself.
     * @param success true if follower contained entry matching prevLogIndex and prevLogTerm.
     */
    public AppendEntriesResult(ActorRef<Raft> sender, int seqNum, int term, boolean success) {
        super(sender, seqNum, term);
        this.success = success;
    }

    @Override
    public String toString() {
        return "AppendEntriesResult{" +
                "sender=" + sender +
                ", term=" + term +
                ", success=" + success +
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
