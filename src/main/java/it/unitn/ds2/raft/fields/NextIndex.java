package it.unitn.ds2.raft.fields;

import akka.actor.typed.ActorRef;
import akka.actor.typed.eventstream.EventStream;
import akka.actor.typed.javadsl.ActorContext;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.events.nextindex.NextIndexDecrement;
import it.unitn.ds2.raft.events.nextindex.NextIndexIncrement;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * for each server, index of the next log entry to send to that server
 * (initialized to leader last log index + 1)
 */
public class NextIndex implements ContextAware {
    private ActorContext<Raft> ctx;
    private final Map<ActorRef<Raft>, Integer> nextIndex;

    public NextIndex(List<ActorRef<Raft>> servers, int lastLogIndex) {
        nextIndex = new HashMap<>(servers.size());
        servers.forEach(server -> nextIndex.put(server, lastLogIndex + 1));
    }

    public int get(ActorRef<Raft> server) {
        return nextIndex.get(server);
    }

    public void increment(ActorRef<Raft> server) {
        nextIndex.put(server, nextIndex.get(server) + 1);
        if (ctx != null) {
            ctx.getLog().debug("nextIndex[" + server.path().name() + "] ← " + nextIndex.get(server));
            var event = new NextIndexIncrement(ctx.getSelf(), ctx.getSystem().uptime(), server, nextIndex.get(server));
            var publish = new EventStream.Publish<>(event);
            ctx.getSystem().eventStream().tell(publish);
        }
    }

    public void decrement(ActorRef<Raft> server) {
        nextIndex.put(server, nextIndex.get(server) - 1);
        if (ctx != null) {
            ctx.getLog().debug("nextIndex[" + server.path().name() + "] ← " + nextIndex.get(server));
            var event = new NextIndexDecrement(ctx.getSelf(), ctx.getSystem().uptime(), server, nextIndex.get(server));
            var publish = new EventStream.Publish<>(event);
            ctx.getSystem().eventStream().tell(publish);
        }
    }

    public Collection<Integer> values() {
        return nextIndex.values();
    }

    @Override
    public String toString() {
        return nextIndex.entrySet().stream()
                .map(entry -> "(" + entry.getKey() + ", " + entry.getValue() + ")")
                .collect(Collectors.joining(", "));
    }

    @Override
    public void setCtx(ActorContext<Raft> ctx) {
        this.ctx = ctx;
    }
}
