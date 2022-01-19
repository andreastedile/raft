package it.unitn.ds2.gui.view.toolbar;

import akka.actor.typed.eventstream.EventStream;
import it.unitn.ds2.gui.commands.AddServer;
import it.unitn.ds2.gui.commands.StartSimulation;
import it.unitn.ds2.gui.commands.StopSimulation;
import it.unitn.ds2.gui.components.ApplicationContext;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.input.KeyEvent;

public class SimulationControlToolbar extends AbstractToolbar {
    private final Button add, start, stop;

    public SimulationControlToolbar(ApplicationContext applicationContext) {
        super(applicationContext);

        add = new Button("Add");
        add.setOnAction(this::handleAdd);

        start = new Button("Start");
        start.setOnAction(this::handleStart);
        start.setDisable(true);

        stop = new Button("Stop");
        stop.setOnAction(this::handleStop);
        stop.setDisable(true);

        getItems().addAll(add, start, stop);

        addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            switch (e.getCode()) {
                case A -> add.fire();
                case G -> start.fire();
                case S -> stop.fire();
            }
        });
    }

    private void handleAdd(ActionEvent event) {
        var command = new AddServer();
        applicationContext.commandBus.emit(command);
        start.setDisable(false);
    }

    private void handleStart(ActionEvent event) {
        var command = new StartSimulation();
        applicationContext.commandBus.emit(command);
        add.setDisable(true);
        start.setDisable(true);
        stop.setDisable(false);
    }

    private void handleStop(ActionEvent event) {
        var command = new StopSimulation();
        applicationContext.commandBus.emit(command);
        add.setDisable(true);
        stop.setDisable(true);
        start.setDisable(false);
    }
}
