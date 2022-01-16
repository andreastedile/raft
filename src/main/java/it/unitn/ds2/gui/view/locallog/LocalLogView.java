package it.unitn.ds2.gui.view.locallog;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.gui.components.ApplicationContext;
import it.unitn.ds2.gui.model.LocalLogModel;
import it.unitn.ds2.gui.view.AbstractTableView;
import it.unitn.ds2.raft.Raft;
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

        applicationContext.eventBus.listenFor(LogAppend.class, this::onLogAppendEvent);
        applicationContext.eventBus.listenFor(LogRemove.class, this::onLogRemoveEvent);
    }

    @Override
    protected LocalLogModel createModel(ActorRef<Raft> server) {
        return new LocalLogModel(server);
    }

    private void onLogAppendEvent(LogAppend event) {
        getModel(event.publisher).addLogEntry(event.index, event.logEntry);
    }

    private void onLogRemoveEvent(LogRemove event) {
        getModel(event.publisher).removeLogEntry(event.index);
    }
}
