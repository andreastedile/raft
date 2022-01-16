package it.unitn.ds2.gui.commands;

import it.unitn.ds2.raft.simulation.Command;

public class SendCommand implements GUICommand {
    public final Command command;

    public SendCommand(Command command) {
        this.command = command;
    }
}
