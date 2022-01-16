package it.unitn.ds2.gui.model;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;
import javafx.beans.property.SimpleObjectProperty;

public abstract class AbstractModel {
    private final SimpleObjectProperty<ActorRef<Raft>> server;

    public AbstractModel(ActorRef<Raft> server) {
        this.server = new SimpleObjectProperty<>(server);
    }

    public ActorRef<Raft> getServer() {
        return server.get();
    }

    public SimpleObjectProperty<ActorRef<Raft>> serverProperty() {
        return server;
    }
}
