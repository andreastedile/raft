package it.unitn.ds2.gui.view.server;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.gui.components.ApplicationContext;
import it.unitn.ds2.gui.model.ServerModel;
import it.unitn.ds2.gui.view.AbstractTableView;
import it.unitn.ds2.gui.view.ActorRefTableCell;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.events.StateChange;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.TableColumn;

public class ServerView extends AbstractTableView<ServerModel> {
    public ServerView(ApplicationContext applicationContext) {
        super(applicationContext);

        TableColumn<ServerModel, StateChange.State> status = new TableColumn<>("Status");
        status.setCellValueFactory(param -> param.getValue().stateProperty());
        getColumns().add(status);

        TableColumn<ServerModel, Number> currentTerm = new TableColumn<>("Current term");
        currentTerm.setCellValueFactory(param -> param.getValue().currentTermProperty());
        getColumns().add(currentTerm);

        TableColumn<ServerModel, ActorRef<Raft>> votedFor = new TableColumn<>("Voted for");
        votedFor.setCellFactory(param -> new ActorRefTableCell<>());
        votedFor.setCellValueFactory(param -> param.getValue().votedForProperty());
        getColumns().add(votedFor);

        // The code below implementing the button is taken from https://stackoverflow.com/a/32284751
        TableColumn<ServerModel, ServerModel> changeState = new TableColumn<>("Change state");
        changeState.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        getColumns().add(changeState);
    }

    @Override
    protected ServerModel createModel(ActorRef<Raft> server) {
        return new ServerModel(server);
    }
}
