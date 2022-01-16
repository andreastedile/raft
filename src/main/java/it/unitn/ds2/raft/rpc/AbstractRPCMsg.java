package it.unitn.ds2.raft.rpc;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;

public abstract class AbstractRPCMsg implements RPC {
    public final ActorRef<Raft> sender;
    public final int seqNum;
    public final int term;

    /**
     * Base class for RPC messages of page 4.
     *
     * @param sender for the receiver to reply to.
     * @param seqNum for sender to check if reply was reordered.
     * @param term   sender's term.
     */
    public AbstractRPCMsg(ActorRef<Raft> sender, int seqNum, int term) {
        this.sender = sender;
        this.seqNum = seqNum;
        this.term = term;
    }
}
