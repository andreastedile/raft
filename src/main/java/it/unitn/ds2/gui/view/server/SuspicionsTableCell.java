package it.unitn.ds2.gui.view.server;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.gui.model.ServerModel;
import it.unitn.ds2.raft.Raft;
import javafx.collections.ObservableSet;
import javafx.scene.control.TableCell;

import java.util.stream.Collectors;

public class SuspicionsTableCell extends TableCell<ServerModel, ObservableSet<ActorRef<Raft>>> {
    @Override
    protected void updateItem(ObservableSet<ActorRef<Raft>> item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
        } else {
            setText(item.stream()
                    .map(server -> server.path().name())
                    .sorted()
                    .collect(Collectors.joining(", ")));
        }
    }
}
