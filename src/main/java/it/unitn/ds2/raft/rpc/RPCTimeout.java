package it.unitn.ds2.raft.rpc;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;

public class RPCTimeout implements RPC {
    public final ActorRef<Raft> server;

    /**
     * @param server servers.it.unitn.ds2.raft.states.Server which did not respond in time to a RPC request.
     */
    public RPCTimeout(ActorRef<Raft> server) {
        this.server = server;
    }

    @Override
    public String toString() {
        return "RPCTimeout{" +
                "server=" + server.path().name() +
                '}';
    }

    @Override
    public ActorRef<RPC> sender() {
        return null;
    }

    @Override
    public int seqNum() {
        return 0;
    }
}
