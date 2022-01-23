package it.unitn.ds2.raft.rpc;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.fields.Servers;

import java.util.List;

public final class ClientStart implements Raft {
    public final Servers servers;
    public final boolean useAutoMode;

    public ClientStart(List<ActorRef<Raft>> server) {
        this.servers = new Servers(server);
        this.useAutoMode = false;
    }

    public ClientStart(List<ActorRef<Raft>> server, boolean desiredAutoMode) {
        this.servers = new Servers(server);
        this.useAutoMode = desiredAutoMode;
    }

}
