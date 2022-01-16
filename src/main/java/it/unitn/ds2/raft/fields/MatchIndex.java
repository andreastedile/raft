package it.unitn.ds2.raft.fields;

import akka.actor.typed.ActorRef;
import akka.actor.typed.eventstream.EventStream;
import akka.actor.typed.javadsl.ActorContext;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.events.matchindex.MatchIndexIncrement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * for each server, index of highest log entry known to be replicated on server
 * (initialized to 0, increases monotonically)
 */
public class MatchIndex implements ContextAware {
    private ActorContext<Raft> ctx;
    private final Map<ActorRef<Raft>, Integer> matchIndex;

    public MatchIndex(List<ActorRef<Raft>> servers) {
        matchIndex = new HashMap<>(servers.size());
        servers.forEach(server -> matchIndex.put(server, 0));
    }

    public int get(ActorRef<Raft> server) {
        return matchIndex.get(server);
    }

    public void increment(ActorRef<Raft> server) {
        matchIndex.put(server, matchIndex.get(server) + 1);
        if (ctx != null) {
            ctx.getLog().debug("matchIndex[" + server.path().name() + "] ← " + matchIndex.get(server));
            var event = new MatchIndexIncrement(ctx.getSelf(), ctx.getSystem().uptime(), server, matchIndex.get(server));
            new EventStream.Publish<>(event);
        }
    }

    @Override
    public String toString() {
        return matchIndex.entrySet().stream()
                .map(entry -> "(" + entry.getKey() + ", " + entry.getValue() + ")")
                .collect(Collectors.joining(", "));
    }

    @Override
    public void setCtx(ActorContext<Raft> ctx) {
        this.ctx = ctx;
    }
}
