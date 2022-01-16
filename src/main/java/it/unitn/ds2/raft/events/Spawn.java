package it.unitn.ds2.raft.events;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;

public class Spawn extends RaftEvent {
    public Spawn(ActorRef<Raft> publisher, long timestamp) {
        super(publisher, timestamp);
    }
}
