package it.unitn.ds2.raft.fields;

import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.ActorContext;
import it.unitn.ds2.raft.Raft;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Other known servers in the Raft cluster.
 */
public class Servers implements ContextAware {
    private ActorContext<Raft> ctx;
    private final List<ActorRef<Raft>> servers;

    public Servers() {
        servers = new ArrayList<>();
    }

    public Servers(List<ActorRef<Raft>> servers) {
        this.servers = new ArrayList<>(servers);
    }

    public void add(ActorRef<Raft> server) {
        servers.add(server);
        if (ctx != null) {
            ctx.getLog().debug(server.path().name() + " joined the Raft cluster");
        }
    }

    public List<ActorRef<Raft>> getAll() {
        return new ArrayList<>(servers);
    }

    public int size() {
        return servers.size();
    }

    @Override
    public String toString() {
        return servers.stream()
                .map(server -> server.path().name())
                .collect(Collectors.joining(", "));
    }

    @Override
    public void setCtx(ActorContext<Raft> ctx) {
        this.ctx = ctx;
    }
}
