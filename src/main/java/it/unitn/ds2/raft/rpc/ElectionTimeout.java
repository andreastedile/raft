package it.unitn.ds2.raft.rpc;

import akka.actor.typed.ActorRef;

public class ElectionTimeout implements RPC {
    @Override
    public ActorRef<RPC> sender() {
        return null;
    }

    @Override
    public int seqNum() {
        return 0;
    }
}
