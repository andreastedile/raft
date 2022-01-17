package it.unitn.ds2.raft.fields;

import akka.actor.typed.eventstream.EventStream;
import akka.actor.typed.javadsl.ActorContext;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.events.lastapplied.LastAppliedIncrement;
import it.unitn.ds2.raft.events.lastapplied.LastAppliedSet;

/**
 * index of highest log entry applied to servers.state machine
 * (initialized to 0, increases monotonically)
 */
public class LastApplied implements ContextAware {
    private ActorContext<Raft> ctx;
    private int lastApplied;

    public LastApplied() {
    }

    public LastApplied(int lastApplied) {
        this.lastApplied = lastApplied;
    }

    public int get() {
        return lastApplied;
    }

    public void set(int lastApplied) {
        this.lastApplied = lastApplied;
        if (ctx != null) {
            ctx.getLog().debug("lastApplied ← " + lastApplied);
            var event = new LastAppliedSet(ctx.getSelf(), ctx.getSystem().uptime(), lastApplied);
            var publish = new EventStream.Publish<>(event);
            ctx.getSystem().eventStream().tell(publish);
        }
    }

    public void increment() {
        lastApplied++;
        if (ctx != null) {
            ctx.getLog().debug("lastApplied ← " + lastApplied);
            var event = new LastAppliedIncrement(ctx.getSelf(), ctx.getSystem().uptime(), lastApplied);
            var publish = new EventStream.Publish<>(event);
            ctx.getSystem().eventStream().tell(publish);
        }
    }

    @Override
    public String toString() {
        return Integer.toString(lastApplied);
    }

    @Override
    public void setCtx(ActorContext<Raft> ctx) {
        this.ctx = ctx;
    }
}
