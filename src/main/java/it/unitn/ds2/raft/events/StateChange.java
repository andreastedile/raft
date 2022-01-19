package it.unitn.ds2.raft.events;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;

public class StateChange extends RaftEvent {
    public enum State {
        OFFLINE,
        CRASHED,
        FOLLOWER,
        CANDIDATE,
        LEADER
    }

    public final State state;

    public StateChange(ActorRef<Raft> publisher, long timestamp, State state) {
        super(publisher, timestamp);
        this.state = state;
    }
}
