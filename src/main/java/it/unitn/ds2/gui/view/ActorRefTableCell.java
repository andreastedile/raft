package it.unitn.ds2.gui.view;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;
import javafx.scene.control.TableCell;

public class ActorRefTableCell<T> extends TableCell<T, ActorRef<Raft>> {
    @Override
    protected void updateItem(ActorRef<Raft> item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            setText(item.path().name());
        }
    }
}