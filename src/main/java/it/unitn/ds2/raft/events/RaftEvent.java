package it.unitn.ds2.raft.events;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;

import java.io.Serializable;

public abstract class RaftEvent implements Raft, Serializable {
    public final ActorRef<Raft> publisher;
    public final long timestamp;

    public RaftEvent(ActorRef<Raft> publisher, long timestamp) {
        this.publisher = publisher;
        this.timestamp = timestamp;
    }
}
