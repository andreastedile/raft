package it.unitn.ds2.gui.view.server;

import it.unitn.ds2.gui.commands.CrashServer;
import it.unitn.ds2.gui.commands.RestartServer;
import it.unitn.ds2.gui.commands.StartSimulation;
import it.unitn.ds2.gui.commands.StopSimulation;
import it.unitn.ds2.gui.components.ApplicationContext;
import it.unitn.ds2.gui.model.ServerModel;
import it.unitn.ds2.raft.events.StateChange;
import it.unitn.ds2.raft.properties.SimulationProperties;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;

import java.time.Duration;
import java.util.Random;


public class StateChangeTableCell<T> extends TableCell<T, ServerModel> {
    private final Button button;
    private final ApplicationContext applicationContext;

    public StateChangeTableCell(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        button = new Button("Waiting");
        button.setDisable(true);
    }

    @Override
    protected void updateItem(ServerModel item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setGraphic(null);
        } else {
            setGraphic(button);

            applicationContext.eventBus.listenFor(StateChange.class, stateChangeEvent -> {
                if (item.getServer().equals(stateChangeEvent.publisher)) {
                    switch (stateChangeEvent.state) {
                        case OFFLINE -> {
                            button.setText("Resume");
                            button.setDisable(false);
                            button.setOnAction(event -> {
                                var command = new RestartServer(item.getServer());
                                applicationContext.commandBus.emit(command);
                            });
                        }
                        case FOLLOWER, LEADER, CANDIDATE -> {
                            button.setText("Crash");
                            button.setDisable(false);
                            button.setOnAction(event -> {
                                SimulationProperties properties = SimulationProperties.getInstance();
                                Duration duration = Duration.ofSeconds(
                                        new Random().nextLong(properties.maxCrashDuration));
                                var command = new CrashServer(item.getServer(), duration);
                                applicationContext.commandBus.emit(command);
                                button.setText("Crashed (" + duration.getSeconds() + "s)");
                                button.setDisable(true);
                            });
                        }
                    }
                }
            });
        }
    }
}