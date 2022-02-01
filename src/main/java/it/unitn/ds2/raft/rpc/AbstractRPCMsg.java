package it.unitn.ds2.raft.rpc;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;

public abstract class AbstractRPCMsg implements Raft {
    public final ActorRef<Raft> sender;

    /**
     * Base class for RPC messages.
     *
     * @param sender for receiver to reply to.
     */
    public AbstractRPCMsg(ActorRef<Raft> sender) {
        this.sender = sender;
    }
}
