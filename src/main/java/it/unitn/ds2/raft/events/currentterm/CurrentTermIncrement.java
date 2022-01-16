package it.unitn.ds2.raft.events.currentterm;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.events.RaftEvent;

public class CurrentTermIncrement extends RaftEvent {
    public final int currentTerm;

    public CurrentTermIncrement(ActorRef<Raft> publisher, long timestamp, int currentTerm) {
        super(publisher, timestamp);
        this.currentTerm = currentTerm;
    }
}
