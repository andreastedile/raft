package it.unitn.ds2.raft.states.follower;

import it.unitn.ds2.raft.fields.*;
import it.unitn.ds2.raft.states.State;

public final class FollowerState extends State {
    public FollowerState() {
    }

    private FollowerState(CurrentTerm currentTerm, VotedFor votedFor, Log log, CommitIndex commitIndex, LastApplied lastApplied) {
        super(currentTerm, votedFor, log, commitIndex, lastApplied);
    }

    public static FollowerState fromAnyState(State state) {
        return new FollowerState(state.currentTerm, state.votedFor, state.log, state.commitIndex, state.lastApplied);
    }
}
