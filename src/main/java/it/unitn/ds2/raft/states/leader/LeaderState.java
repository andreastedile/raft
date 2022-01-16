package it.unitn.ds2.raft.states.leader;

import it.unitn.ds2.raft.fields.*;
import it.unitn.ds2.raft.states.State;
import it.unitn.ds2.raft.states.candidate.CandidateState;

public final class LeaderState extends State {
    // Volatile state on leaders:
    public final NextIndex nextIndex;
    public final MatchIndex matchIndex;

    private LeaderState(Servers servers, CurrentTerm currentTerm, VotedFor votedFor, Log log, CommitIndex commitIndex, LastApplied lastApplied) {
        super(currentTerm, votedFor, log, commitIndex, lastApplied);
        nextIndex = new NextIndex(servers.getAll(), log.lastLogIndex());
        matchIndex = new MatchIndex(servers.getAll());
    }

    public static LeaderState fromState(Servers servers, CandidateState state) {
        return new LeaderState(servers, state.currentTerm, state.votedFor, state.log, state.commitIndex, state.lastApplied);
    }
}
