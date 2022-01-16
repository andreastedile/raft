package it.unitn.ds2.gui.commands;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;

public class RestartServer implements GUICommand {
    public final ActorRef<Raft> server;

    public RestartServer(ActorRef<Raft> server) {
        this.server = server;
    }
}
