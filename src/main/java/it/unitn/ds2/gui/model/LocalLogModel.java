package it.unitn.ds2.gui.model;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.fields.LogEntry;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class LocalLogModel extends AbstractModel {
    private final ObservableList<LogEntry> logEntries;

    public LocalLogModel(ActorRef<Raft> server) {
        super(server);
        logEntries = FXCollections.observableArrayList();
    }

    public SimpleListProperty<LogEntry> logEntriesProperty() {
        return new SimpleListProperty<>(logEntries);
    }

    public void addLogEntry(int index, LogEntry logEntry) {
        logEntries.add(index - 1, logEntry);
    }

    public void removeLogEntry(int index) {
        logEntries.remove(index - 1);
    }
}
