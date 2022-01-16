package it.unitn.ds2.raft.fields;

import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.ActorContext;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.rpc.AbstractRPCMsg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SeqNum implements ContextAware {
    private ActorContext<Raft> ctx;
    private final Map<ActorRef<Raft>, Integer> seqNums;
    private final Map<ActorRef<Raft>, AbstractRPCMsg> lastSent;

    public SeqNum(List<ActorRef<Raft>> servers) {
        seqNums = new HashMap<>(servers.size());
        lastSent = new HashMap<>(servers.size());
    }

    public int computeNext(ActorRef<Raft> server) {
        int next = seqNums.merge(server, 1, Integer::sum);
        if (ctx != null) {
            ctx.getLog().debug("seqNum[" + server.path().name() + "] ← " + next);
        }
        return next;
    }

    public void setLastSent(ActorRef<Raft> server, AbstractRPCMsg msg) {
        lastSent.put(server, msg);
        if (ctx != null) {
            ctx.getLog().debug("lastSent[" + server.path().name() + "] ← " + msg);
        }
    }

    public AbstractRPCMsg getMsg(ActorRef<Raft> server) {
        return lastSent.get(server);
    }

    public int expectedSeqNum(ActorRef<Raft> server) {
        return seqNums.get(server);
    }

    @Override
    public String toString() {
        return seqNums.entrySet().stream()
                .map(entry -> "(" + entry.getKey() + ", " + entry.getValue() + ")")
                .collect(Collectors.joining(", "));
    }

    @Override
    public void setCtx(ActorContext<Raft> ctx) {
        this.ctx = ctx;
    }
}
