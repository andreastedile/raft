package it.unitn.ds2.raft.events.matchindex;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.events.RaftEvent;

public class MatchIndexIncrement extends RaftEvent {
    public final ActorRef<Raft> server;
    public final int matchIndex;

    public MatchIndexIncrement(ActorRef<Raft> publisher, long timestamp, ActorRef<Raft> server, int matchIndex) {
        super(publisher, timestamp);
        this.server = server;
        this.matchIndex = matchIndex;
    }
}
