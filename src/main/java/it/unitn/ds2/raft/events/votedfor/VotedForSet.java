package it.unitn.ds2.raft.events.votedfor;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.events.RaftEvent;

public class VotedForSet extends RaftEvent {
    public final ActorRef<Raft> votedFor;

    public VotedForSet(ActorRef<Raft> publisher, long timestamp, ActorRef<Raft> votedFor) {
        super(publisher, timestamp);
        this.votedFor = votedFor;
    }
}
