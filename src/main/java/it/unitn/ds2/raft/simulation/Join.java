package it.unitn.ds2.raft.simulation;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;

public final class Join implements Raft {
    public final ActorRef<Raft> server;

    public Join(ActorRef<Raft> server) {
        this.server = server;
    }

    @Override
    public String toString() {
        return "Join{" +
                "server=" + server.path().name() +
                '}';
    }
}
