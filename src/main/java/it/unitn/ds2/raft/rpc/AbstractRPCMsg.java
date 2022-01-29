package it.unitn.ds2.raft.rpc;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;

public abstract class AbstractRPCMsg implements Raft {
    public final ActorRef<Raft> sender;
    public final int seqNum;

    /**
     * Base class for RPC messages.
     *
     * @param sender for receiver to reply to.
     * @param seqNum for sender to check if reply was reordered.
     */
    public AbstractRPCMsg(ActorRef<Raft> sender, int seqNum) {
        this.sender = sender;
        this.seqNum = seqNum;
    }
}
