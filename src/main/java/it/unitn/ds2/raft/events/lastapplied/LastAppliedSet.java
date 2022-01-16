package it.unitn.ds2.raft.events.lastapplied;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.events.RaftEvent;

public class LastAppliedSet extends RaftEvent {
    public final int lastApplied;

    public LastAppliedSet(ActorRef<Raft> publisher, long timestamp, int lastApplied) {
        super(publisher, timestamp);
        this.lastApplied = lastApplied;
    }
}
