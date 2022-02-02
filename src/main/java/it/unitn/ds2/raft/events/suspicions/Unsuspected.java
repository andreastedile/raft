package it.unitn.ds2.raft.events.suspicions;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.events.RaftEvent;

public class Unsuspected extends RaftEvent {
    public final ActorRef<Raft> unsuspected;

    public Unsuspected(ActorRef<Raft> publisher, long timestamp, ActorRef<Raft> unsuspected) {
        super(publisher, timestamp);
        this.unsuspected = unsuspected;
    }
}
