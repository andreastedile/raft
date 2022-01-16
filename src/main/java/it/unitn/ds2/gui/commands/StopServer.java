package it.unitn.ds2.gui.commands;

import akka.actor.ActorRef;

public class StopServer implements GUICommand {
    public final ActorRef server;

    public StopServer(ActorRef server) {
        this.server = server;
    }
}
