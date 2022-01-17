package it.unitn.ds2.raft.fields;

import akka.actor.typed.eventstream.EventStream;
import akka.actor.typed.javadsl.ActorContext;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.events.commitindex.CommitIndexDecrement;
import it.unitn.ds2.raft.events.commitindex.CommitIndexIncrement;
import it.unitn.ds2.raft.events.commitindex.CommitIndexSet;

/**
 * index of highest log entry known to be committed
 * (initialized to 0, increases monotonically)
 */
public class CommitIndex implements ContextAware {
    private ActorContext<Raft> ctx;
    private int commitIndex;

    public CommitIndex() {
    }

    public CommitIndex(int commitIndex) {
        this.commitIndex = commitIndex;
    }

    public int get() {
        return commitIndex;
    }

    public void set(int commitIndex) {
        this.commitIndex = commitIndex;
        if (ctx != null) {
            ctx.getLog().debug("commitIndex ← " + commitIndex);
            var event = new CommitIndexSet(ctx.getSelf(), ctx.getSystem().uptime(), commitIndex);
            var publish = new EventStream.Publish<>(event);
            ctx.getSystem().eventStream().tell(publish);
        }
    }

    public void increment() {
        commitIndex++;
        if (ctx != null) {
            ctx.getLog().debug("commitIndex ← " + commitIndex);
            var event = new CommitIndexIncrement(ctx.getSelf(), ctx.getSystem().uptime(), commitIndex);
            var publish = new EventStream.Publish<>(event);
            ctx.getSystem().eventStream().tell(publish);
        }
    }

    public void decrement() {
        commitIndex--;
        if (ctx != null) {
            ctx.getLog().debug("commitIndex ← " + commitIndex);
            var event = new CommitIndexDecrement(ctx.getSelf(), ctx.getSystem().uptime(), commitIndex);
            var publish = new EventStream.Publish<>(event);
            ctx.getSystem().eventStream().tell(publish);
        }
    }

    @Override
    public String toString() {
        return Integer.toString(commitIndex);
    }

    public void setCtx(ActorContext<Raft> ctx) {
        this.ctx = ctx;
    }
}
