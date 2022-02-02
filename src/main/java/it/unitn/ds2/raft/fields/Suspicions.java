package it.unitn.ds2.raft.fields;

import akka.actor.typed.ActorRef;
import akka.actor.typed.eventstream.EventStream;
import akka.actor.typed.javadsl.ActorContext;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.events.suspicions.Suspected;
import it.unitn.ds2.raft.events.suspicions.Unsuspected;

import java.util.HashSet;
import java.util.Set;

public class Suspicions implements ContextAware {
    private ActorContext<Raft> ctx;
    private final Set<ActorRef<Raft>> suspicions;

    public Suspicions() {
        suspicions = new HashSet<>();
    }

    public void suspect(ActorRef<Raft> server) {
        if (suspicions.add(server)) {
            var event = new Suspected(ctx.getSelf(), ctx.getSystem().uptime(), server);
            var publish = new EventStream.Publish<>(event);
            ctx.getSystem().eventStream().tell(publish);
        }
    }

    public void unsuspect(ActorRef<Raft> server) {
        if (suspicions.remove(server)) {
            var event = new Unsuspected(ctx.getSelf(), ctx.getSystem().uptime(), server);
            var publish = new EventStream.Publish<>(event);
            ctx.getSystem().eventStream().tell(publish);
        }
    }

    public boolean isSuspected(ActorRef<Raft> server) {
        return suspicions.contains(server);
    }

    @Override
    public void setCtx(ActorContext<Raft> ctx) {
        this.ctx = ctx;
    }
}
