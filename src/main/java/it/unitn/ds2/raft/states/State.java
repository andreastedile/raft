package it.unitn.ds2.raft.states;

import it.unitn.ds2.raft.fields.*;

public abstract class State {
    // Persistent servers.state on all servers:
    public final CurrentTerm currentTerm;
    public final VotedFor votedFor;
    public final Log log;

    // Volatile servers.state on all servers:
    public final CommitIndex commitIndex;
    public final LastApplied lastApplied;

    protected State() {
        currentTerm = new CurrentTerm();
        votedFor = new VotedFor();
        log = new Log();

        commitIndex = new CommitIndex();
        lastApplied = new LastApplied();
    }

    public State(CurrentTerm currentTerm, VotedFor votedFor, Log log, CommitIndex commitIndex, LastApplied lastApplied) {
        this.currentTerm = currentTerm;
        this.votedFor = votedFor;
        this.log = log;
        this.commitIndex = commitIndex;
        this.lastApplied = lastApplied;
    }
}
