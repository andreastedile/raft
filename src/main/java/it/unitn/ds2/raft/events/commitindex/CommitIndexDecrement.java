package it.unitn.ds2.raft.events.commitindex;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.events.RaftEvent;

public class CommitIndexDecrement extends RaftEvent {
    public final int commitIndex;


    public CommitIndexDecrement(ActorRef<Raft> publisher, long timestamp, int commitIndex) {
        super(publisher, timestamp);
        this.commitIndex = commitIndex;
    }
}
