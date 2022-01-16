package it.unitn.ds2.raft.events.nextindex;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.events.RaftEvent;

public class NextIndexIncrement extends RaftEvent {
    public final ActorRef<Raft> server;
    public final int nextIndex;

    public NextIndexIncrement(ActorRef<Raft> publisher, long timestamp, ActorRef<Raft> server, int nextIndex) {
        super(publisher, timestamp);
        this.server = server;
        this.nextIndex = nextIndex;
    }
}
