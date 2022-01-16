package it.unitn.ds2.raft.fields;

import it.unitn.ds2.raft.statemachinecommands.StateMachineCommand;

import java.util.Objects;

public class LogEntry implements Comparable<LogEntry> {
    public final StateMachineCommand command;
    public final int term;

    public LogEntry(StateMachineCommand command, int term) {
        this.command = command;
        this.term = term;
    }

    @Override
    public String toString() {
        return "(" + term + ", " + command + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LogEntry logEntry)) return false;
        return term == logEntry.term && command.equals(logEntry.command);
    }

    @Override
    public int hashCode() {
        return Objects.hash(command, term);
    }

    @Override
    public int compareTo(LogEntry o) {
        return Integer.compare(term, o.term);
    }
}
