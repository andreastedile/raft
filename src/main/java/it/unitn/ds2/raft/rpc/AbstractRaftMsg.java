package it.unitn.ds2.raft.rpc;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;

public abstract class AbstractRaftMsg {
    public final ActorRef<Raft> sender;
    public final int term;

    /**
     * Base class for the messages described at page 4.
     *
     * @param sender of the message.
     * @param term   sender's term.
     */
    public AbstractRaftMsg(ActorRef<Raft> sender, int term) {
        this.sender = sender;
        this.term = term;
    }
}
