package it.unitn.ds2.raft.states.candidate;

import it.unitn.ds2.raft.fields.*;
import it.unitn.ds2.raft.states.State;
import it.unitn.ds2.raft.states.follower.FollowerState;

public final class CandidateState extends State {
    private CandidateState(CurrentTerm currentTerm, VotedFor votedFor, Log log, CommitIndex commitIndex, LastApplied lastApplied) {
        super(currentTerm, votedFor, log, commitIndex, lastApplied);
    }

    public static CandidateState fromState(FollowerState state) {
        return new CandidateState(state.currentTerm, state.votedFor, state.log, state.commitIndex, state.lastApplied);
    }
}
