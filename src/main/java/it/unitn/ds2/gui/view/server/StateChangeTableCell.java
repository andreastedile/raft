package it.unitn.ds2.gui.view.server;

import it.unitn.ds2.gui.commands.CrashServer;
import it.unitn.ds2.gui.components.ApplicationContext;
import it.unitn.ds2.gui.model.ServerModel;
import it.unitn.ds2.raft.events.StateChange;
import it.unitn.ds2.raft.properties.SimulationProperties;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;


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
                            button.setText("Offline");
                            button.setDisable(true);
                            //button.setOnAction(event -> {
                            //    var command = new RestartServer(item.getServer());
                            //    applicationContext.commandBus.emit(command);
                            //});
                        }
                        case FOLLOWER, LEADER, CANDIDATE -> {
                            button.setText("Crash");
                            button.setDisable(false);
                            button.setOnAction(event -> {
                                var duration = randomCrashDuration();
                                var command = new CrashServer(item.getServer(), duration);
                                applicationContext.commandBus.emit(command);
                                button.setText("Crashed (" + duration.toMillis() + "s)");
                                button.setDisable(true);
                            });
                        }
                    }
                }
            });
        }
    }

    private static Duration randomCrashDuration() {
        var properties = SimulationProperties.getInstance();
        long durationMs = ThreadLocalRandom.current().nextLong(properties.maxCrashDurationMs);
        return Duration.ofSeconds(durationMs);
    }
}