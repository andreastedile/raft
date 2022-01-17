package it.unitn.ds2.raft.fields;

import akka.actor.typed.ActorRef;
import akka.actor.typed.eventstream.EventStream;
import akka.actor.typed.javadsl.ActorContext;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.events.votes.VotesAddVote;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Votes implements ContextAware {
    private ActorContext<Raft> ctx;
    private final Map<ActorRef<Raft>, Boolean> votes;

    public Votes(List<ActorRef<Raft>> servers) {
        votes = new HashMap<>(servers.size());
    }

    public void addVote(ActorRef<Raft> server, boolean voteGranted) {
        votes.put(server, voteGranted);
        if (ctx != null) {
            if (voteGranted) {
                ctx.getLog().debug("votes[" + server.path().name() + "] ← granted");
            } else {
                ctx.getLog().debug("votes[" + server.path().name() + "] ← denied");
            }
            var event = new VotesAddVote(ctx.getSelf(), ctx.getSystem().uptime(), server, voteGranted);
            var publish = new EventStream.Publish<>(event);
            ctx.getSystem().eventStream().tell(publish);
        }
    }

    public void clear() {
        votes.clear();
    }

    public int nGranted() {
        return (int) votes.values().stream()
                .filter(Boolean::booleanValue)
                .count();
    }

    public int nDenied() {
        return votes.size() - nGranted();
    }

    @Override
    public String toString() {
        return votes.entrySet().stream()
                .map(entry -> "(" + entry.getKey() + ", " + entry.getValue() + ",")
                .collect(Collectors.joining(", "));
    }

    @Override
    public void setCtx(ActorContext<Raft> ctx) {
        this.ctx = ctx;
    }
}
