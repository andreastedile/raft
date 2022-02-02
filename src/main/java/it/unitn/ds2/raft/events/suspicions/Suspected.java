package it.unitn.ds2.raft.events.suspicions;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.events.RaftEvent;

public class Suspected extends RaftEvent {
    public final ActorRef<Raft> suspected;

    public Suspected(ActorRef<Raft> publisher, long timestamp, ActorRef<Raft> suspected) {
        super(publisher, timestamp);
        this.suspected = suspected;
    }
}
