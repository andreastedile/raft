package it.unitn.ds2.gui.view.locallog;

import it.unitn.ds2.gui.model.LocalLogModel;
import it.unitn.ds2.raft.fields.LogEntry;
import javafx.collections.ObservableList;
import javafx.scene.control.TableCell;

import java.util.stream.Collectors;

public class LocalLogTableCell extends TableCell<LocalLogModel, ObservableList<LogEntry>> {
    @Override
    protected void updateItem(ObservableList<LogEntry> item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
        } else {
            setText(item.stream()
                    .map(logEntry -> String.format("[%d, %s]", logEntry.term, logEntry.command))
                    .collect(Collectors.joining(", ")));
        }
    }
}
