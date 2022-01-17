package it.unitn.ds2.raft.fields;

import akka.actor.typed.eventstream.EventStream;
import akka.actor.typed.javadsl.ActorContext;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.events.currentterm.CurrentTermIncrement;
import it.unitn.ds2.raft.events.currentterm.CurrentTermSet;

/**
 * latest term server has seen
 * (initialized to 0 on first boot, increases monotonically)
 */
public class CurrentTerm implements ContextAware {
    private ActorContext<Raft> ctx;
    private int currentTerm;

    public CurrentTerm() {
    }

    public CurrentTerm(int currentTerm) {
        this.currentTerm = currentTerm;
    }

    public int get() {
        return currentTerm;
    }

    public void set(int currentTerm) {
        this.currentTerm = currentTerm;
        if (ctx != null) {
            ctx.getLog().debug("currentTerm ← " + currentTerm);
            var event = new CurrentTermSet(ctx.getSelf(), ctx.getSystem().uptime(), currentTerm);
            var publish = new EventStream.Publish<>(event);
            ctx.getSystem().eventStream().tell(publish);
        }
    }

    public void increment() {
        currentTerm++;
        if (ctx != null) {
            ctx.getLog().debug("currentTerm ← " + currentTerm);
            var event = new CurrentTermIncrement(ctx.getSelf(), ctx.getSystem().uptime(), currentTerm);
            var publish = new EventStream.Publish<>(event);
            ctx.getSystem().eventStream().tell(publish);
        }
    }

    @Override
    public String toString() {
        return Integer.toString(currentTerm);
    }

    @Override
    public void setCtx(ActorContext<Raft> ctx) {
        this.ctx = ctx;
    }
}
