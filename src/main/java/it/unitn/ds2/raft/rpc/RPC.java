package it.unitn.ds2.raft.rpc;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;

public interface RPC extends Raft {
    ActorRef<RPC> sender();

    int seqNum();
}
