package it.unitn.ds2.gui.view.server;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.gui.model.ServerModel;
import it.unitn.ds2.raft.Raft;
import javafx.collections.ObservableList;
import javafx.scene.control.TableCell;

import java.util.stream.Collectors;

public class SuspicionsTableCell extends TableCell<ServerModel, ObservableList<ActorRef<Raft>>> {
    @Override
    protected void updateItem(ObservableList<ActorRef<Raft>> item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
        } else {
            setText(item.stream()
                    .map(server -> server.path().name())
                    .collect(Collectors.joining(", ")));
        }
    }
}
