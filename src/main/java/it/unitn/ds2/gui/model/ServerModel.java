package it.unitn.ds2.gui.model;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.events.StateChange;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

public class ServerModel extends AbstractModel {
    private final SimpleObjectProperty<StateChange.State> state;
    private final SimpleIntegerProperty currentTerm;
    private final SimpleObjectProperty<ActorRef<Raft>> votedFor;
    private final ObservableSet<ActorRef<Raft>> suspicions;

    public ServerModel(ActorRef<Raft> server) {
        super(server);
        state = new SimpleObjectProperty<>();
        currentTerm = new SimpleIntegerProperty();
        votedFor = new SimpleObjectProperty<>();
        //noinspection unchecked
        suspicions = FXCollections.observableSet();
    }

    public void setState(StateChange.State state) {
        this.state.set(state);
    }

    public SimpleObjectProperty<StateChange.State> stateProperty() {
        return state;
    }

    public void setCurrentTerm(int currentTerm) {
        this.currentTerm.set(currentTerm);
    }

    public SimpleIntegerProperty currentTermProperty() {
        return currentTerm;
    }

    public void setVotedFor(ActorRef<Raft> votedFor) {
        this.votedFor.set(votedFor);
    }

    public SimpleObjectProperty<ActorRef<Raft>> votedForProperty() {
        return votedFor;
    }

    public SimpleSetProperty<ActorRef<Raft>> suspicionsProperty() {
        return new SimpleSetProperty<>(suspicions);
    }

    public void addSuspected(ActorRef<Raft> server) {
        suspicions.add(server);
    }

    public void removeSuspected(ActorRef<Raft> server) {
        suspicions.remove(server);
    }

    public void clearSuspicionList() {
        suspicions.clear();
    }
}
