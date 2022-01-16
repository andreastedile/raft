package it.unitn.ds2.raft.states.candidate;

import it.unitn.ds2.raft.fields.*;
import it.unitn.ds2.raft.states.State;
import it.unitn.ds2.raft.states.follower.FollowerState;

public final class CandidateState extends State {
    public final Votes votes;

    private CandidateState(Servers servers, CurrentTerm currentTerm, VotedFor votedFor, Log log, CommitIndex commitIndex, LastApplied lastApplied) {
        super(currentTerm, votedFor, log, commitIndex, lastApplied);
        votes = new Votes(servers.getAll());
    }

    public static CandidateState fromState(Servers servers, FollowerState state) {
        return new CandidateState(servers, state.currentTerm, state.votedFor, state.log, state.commitIndex, state.lastApplied);
    }
}
