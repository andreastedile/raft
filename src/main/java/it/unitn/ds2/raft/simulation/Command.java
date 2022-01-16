package it.unitn.ds2.raft.simulation;

import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.statemachinecommands.StateMachineCommand;

public final class Command implements Raft {
    public final int seqNum;
    public final StateMachineCommand command;

    public Command(int seqNum, StateMachineCommand command) {
        this.seqNum = seqNum;
        this.command = command;
    }

    @Override
    public final String toString() {
        return "(" + seqNum + ", " + command + ")";
    }
}
