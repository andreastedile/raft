package it.unitn.ds2.raft.events.log;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.events.RaftEvent;
import it.unitn.ds2.raft.fields.LogEntry;

public class LogRemove extends RaftEvent {
    public final LogEntry logEntry;
    public final int index;

    public LogRemove(ActorRef<Raft> publisher, long timestamp, LogEntry logEntry, int index) {
        super(publisher, timestamp);
        this.logEntry = logEntry;
        this.index = index;
    }
}
