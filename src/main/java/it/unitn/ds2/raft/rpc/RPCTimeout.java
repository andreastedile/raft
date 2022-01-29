package it.unitn.ds2.raft.rpc;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;

public final class RPCTimeout implements Raft {
    public final ActorRef<Raft> server;

    public RPCTimeout(ActorRef<Raft> server) {
        this.server = server;
    }

    @Override
    public String toString() {
        return "RPCTimeout{" +
                "server=" + server.path().name() +
                '}';
    }
}
