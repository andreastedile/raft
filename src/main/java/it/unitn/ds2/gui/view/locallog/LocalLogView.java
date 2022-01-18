package it.unitn.ds2.gui.view.locallog;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.gui.components.ApplicationContext;
import it.unitn.ds2.gui.model.LocalLogModel;
import it.unitn.ds2.gui.view.AbstractTableView;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.events.Spawn;
import it.unitn.ds2.raft.events.commitindex.CommitIndexDecrement;
import it.unitn.ds2.raft.events.commitindex.CommitIndexIncrement;
import it.unitn.ds2.raft.events.commitindex.CommitIndexSet;
import it.unitn.ds2.raft.events.lastapplied.LastAppliedIncrement;
import it.unitn.ds2.raft.events.lastapplied.LastAppliedSet;
import it.unitn.ds2.raft.events.log.LogAppend;
import it.unitn.ds2.raft.events.log.LogRemove;
import it.unitn.ds2.raft.fields.LogEntry;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;

public class LocalLogView extends AbstractTableView<LocalLogModel> {
    public LocalLogView(ApplicationContext applicationContext) {
        super(applicationContext);

        TableColumn<LocalLogModel, ObservableList<LogEntry>> localLog = new TableColumn<>("Local log");
        localLog.setCellValueFactory(param -> param.getValue().logEntriesProperty());
        localLog.setCellFactory(param -> new LocalLogTableCell());
        getColumns().add(localLog);

        TableColumn<LocalLogModel, Number> commitIndex = new TableColumn<>("Commit index");
        commitIndex.setCellValueFactory(param -> param.getValue().commitIndexProperty());
        getColumns().add(commitIndex);

        TableColumn<LocalLogModel, Number> lastApplied = new TableColumn<>("Last applied");
        lastApplied.setCellValueFactory(param -> param.getValue().lastAppliedProperty());
        getColumns().add(lastApplied);

        applicationContext.eventBus.listenFor(Spawn.class, this::onSpawn);
        applicationContext.eventBus.listenFor(LogAppend.class, this::onLogAppendEvent);
        applicationContext.eventBus.listenFor(LogRemove.class, this::onLogRemoveEvent);
        applicationContext.eventBus.listenFor(CommitIndexIncrement.class, this::onCommitIndexIncrement);
        applicationContext.eventBus.listenFor(CommitIndexDecrement.class, this::onCommitIndexDecrement);
        applicationContext.eventBus.listenFor(CommitIndexSet.class, this::onCommitIndexSet);
        applicationContext.eventBus.listenFor(LastAppliedIncrement.class, this::onLastAppliedIncrement);
        applicationContext.eventBus.listenFor(LastAppliedSet.class, this::onLastAppliedSet);
    }

    @Override
    protected LocalLogModel createModel(ActorRef<Raft> server) {
        return new LocalLogModel(server);
    }

    private void onSpawn(Spawn event) {
        createModel(event.publisher);
    }

    private void onLogAppendEvent(LogAppend event) {
        getModel(event.publisher).addLogEntry(event.index, event.logEntry);
    }

    private void onLogRemoveEvent(LogRemove event) {
        getModel(event.publisher).removeLogEntry(event.index);
    }

    private void onCommitIndexIncrement(CommitIndexIncrement event) {
        getModel(event.publisher).commitIndexProperty().set(event.commitIndex);
    }

    private void onCommitIndexDecrement(CommitIndexDecrement event) {
        getModel(event.publisher).commitIndexProperty().set(event.commitIndex);
    }

    private void onCommitIndexSet(CommitIndexSet event) {
        getModel(event.publisher).commitIndexProperty().set(event.commitIndex);
    }

    private void onLastAppliedIncrement(LastAppliedIncrement event) {
        getModel(event.publisher).lastAppliedProperty().set(event.lastApplied);
    }

    private void onLastAppliedSet(LastAppliedSet event) {
        getModel(event.publisher).lastAppliedProperty().set(event.lastApplied);
    }
}
