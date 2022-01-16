package it.unitn.ds2.gui.commands;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;

import java.time.Duration;

public class CrashServer implements GUICommand {
    public final ActorRef<Raft> server;
    public final Duration duration;

    public CrashServer(ActorRef<Raft> server, Duration duration) {
        this.server = server;
        this.duration = duration;
    }
}
