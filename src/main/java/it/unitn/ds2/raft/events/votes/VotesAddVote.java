package it.unitn.ds2.raft.events.votes;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.events.RaftEvent;

public class VotesAddVote extends RaftEvent {
    public final ActorRef<Raft> voter;
    public final boolean voteGranted;

    public VotesAddVote(ActorRef<Raft> publisher, long timestamp, ActorRef<Raft> voter, boolean voteGranted) {
        super(publisher, timestamp);
        this.voter = voter;
        this.voteGranted = voteGranted;
    }
}
