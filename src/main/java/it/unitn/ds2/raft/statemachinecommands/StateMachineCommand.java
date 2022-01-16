package it.unitn.ds2.raft.statemachinecommands;

public abstract class StateMachineCommand {
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    @Override
    public final boolean equals(Object obj) {
        return getClass().equals(obj.getClass());
    }

    @Override
    public final int hashCode() {
        return getClass().hashCode();
    }
}
