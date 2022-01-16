package it.unitn.ds2.gui.view.toolbar;

import it.unitn.ds2.gui.commands.SendCommand;
import it.unitn.ds2.gui.components.ApplicationContext;
import it.unitn.ds2.raft.simulation.Command;
import it.unitn.ds2.raft.statemachinecommands.Add;
import it.unitn.ds2.raft.statemachinecommands.Move;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;

public class ClientToolbar extends AbstractToolbar {
    public ClientToolbar(ApplicationContext applicationContext) {
        super(applicationContext);

        var addBtn = new Button("Add");
        addBtn.setOnAction(this::onAdd);
        getItems().add(addBtn);

        var moveBtn = new Button("Move");
        moveBtn.setOnAction(this::onMove);
        getItems().add(moveBtn);
    }

    private void onAdd(ActionEvent event) {
        var add = new Add();
        var command = new Command(1, add);
        var sendCommand = new SendCommand(command);
        applicationContext.commandBus.emit(sendCommand);
    }

    private void onMove(ActionEvent event) {
        var move = new Move();
        var command = new Command(1, move);
        var sendCommand = new SendCommand(command);
        applicationContext.commandBus.emit(sendCommand);
    }
}
