package it.unitn.ds2.gui.view.server;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.gui.components.ApplicationContext;
import it.unitn.ds2.gui.model.ServerModel;
import it.unitn.ds2.gui.view.AbstractTableView;
import it.unitn.ds2.gui.view.ActorRefTableCell;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.events.StateChange;
import it.unitn.ds2.raft.events.currentterm.CurrentTermIncrement;
import it.unitn.ds2.raft.events.currentterm.CurrentTermSet;
import it.unitn.ds2.raft.events.suspicions.Suspected;
import it.unitn.ds2.raft.events.suspicions.Unsuspected;
import it.unitn.ds2.raft.events.votedfor.VotedForSet;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ObservableSet;
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
        changeState.setCellFactory(param -> new StateChangeTableCell<>(applicationContext));
        changeState.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        getColumns().add(changeState);

        TableColumn<ServerModel, ObservableSet<ActorRef<Raft>>> suspicions = new TableColumn<>("Suspicions");
        suspicions.setCellValueFactory(param -> param.getValue().suspicionsProperty());
        suspicions.setCellFactory(param -> new SuspicionsTableCell());
        getColumns().add(suspicions);

        applicationContext.eventBus.listenFor(StateChange.class, this::onStateChange);
        applicationContext.eventBus.listenFor(CurrentTermIncrement.class, this::onCurrentTermIncrement);
        applicationContext.eventBus.listenFor(CurrentTermSet.class, this::onCurrentTermSet);
        applicationContext.eventBus.listenFor(VotedForSet.class, this::onVotedForSet);
        applicationContext.eventBus.listenFor(Suspected.class, this::onSuspected);
        applicationContext.eventBus.listenFor(Unsuspected.class, this::onUnsuspected);
    }

    @Override
    protected ServerModel createModel(ActorRef<Raft> server) {
        return new ServerModel(server);
    }

    private void onStateChange(StateChange event) {
        var model = getModel(event.publisher);
        model.setState(event.state);
        if (event.state == StateChange.State.OFFLINE) {
            model.clearSuspicionList();
        }
    }

    private void onCurrentTermIncrement(CurrentTermIncrement event) {
        getModel(event.publisher).setCurrentTerm(event.currentTerm);
    }

    private void onCurrentTermSet(CurrentTermSet event) {
        getModel(event.publisher).setCurrentTerm(event.currentTerm);
    }

    private void onVotedForSet(VotedForSet event) {
        getModel(event.publisher).setVotedFor(event.votedFor);
    }

    private void onSuspected(Suspected event) {
        getModel(event.publisher).addSuspected(event.suspected);
    }

    private void onUnsuspected(Unsuspected event) {
        getModel(event.publisher).removeSuspected(event.unsuspected);
    }
}
