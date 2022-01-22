package it.unitn.ds2.raft.simulation;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.fields.Servers;

import java.util.List;

public final class ClientStart implements Raft {
    public final Servers servers;

    public ClientStart(List<ActorRef<Raft>> server) {
        this.servers = new Servers(server);
    }

}
