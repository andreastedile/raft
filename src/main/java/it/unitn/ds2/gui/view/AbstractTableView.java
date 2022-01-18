package it.unitn.ds2.gui.view;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.gui.components.ApplicationContext;
import it.unitn.ds2.gui.model.AbstractModel;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.events.Spawn;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public abstract class AbstractTableView<T extends AbstractModel> extends TableView<T> {
    private final ObservableList<T> models;

    public AbstractTableView(ApplicationContext applicationContext) {

        models = FXCollections.observableArrayList();
        setItems(models);

        TableColumn<T, ActorRef<Raft>> name = new TableColumn<>("Name");
        name.setCellFactory(param -> new ActorRefTableCell<>());
        name.setCellValueFactory(param -> param.getValue().serverProperty());
        getColumns().add(name);

        applicationContext.eventBus.listenFor(Spawn.class, this::onSpawn);

        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        var label = new Label("There are no servers in the Raft cluster");
        setPlaceholder(label);
    }

    protected final T getModel(ActorRef<Raft> server) {
        return models.stream()
                .filter(t -> t.serverProperty().get().equals(server))
                .findFirst()
                .orElseThrow();
    }

    protected abstract T createModel(ActorRef<Raft> server);

    private void onSpawn(Spawn event) {
        getItems().add(createModel(event.publisher));
    }
}
