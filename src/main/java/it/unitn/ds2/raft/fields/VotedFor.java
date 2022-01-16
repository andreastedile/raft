package it.unitn.ds2.raft.fields;

import akka.actor.typed.ActorRef;
import akka.actor.typed.eventstream.EventStream;
import akka.actor.typed.javadsl.ActorContext;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.events.votedfor.VotedForSet;

/**
 * candidateId that received vote in current term
 * (or null if none)
 */
public class VotedFor implements ContextAware {
    private ActorContext<Raft> ctx;
    private ActorRef<Raft> votedFor;

    public ActorRef<Raft> get() {
        return votedFor;
    }

    public void set(ActorRef<Raft> server) {
        this.votedFor = server;
        if (ctx != null) {
            ctx.getLog().debug("votedFor ← " + server.path().name());
            var event = new VotedForSet(ctx.getSelf(), ctx.getSystem().uptime(), server);
            new EventStream.Publish<>(event);
        }
    }

    @Override
    public String toString() {
        return votedFor.path().name();
    }

    @Override
    public void setCtx(ActorContext<Raft> ctx) {
        this.ctx = ctx;
    }
}
