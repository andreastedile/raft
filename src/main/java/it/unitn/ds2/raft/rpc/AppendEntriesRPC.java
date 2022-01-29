package it.unitn.ds2.raft.rpc;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;

public final class AppendEntriesRPC extends RPCRequest<AppendEntries, AppendEntriesResult> {
    /**
     * Constructs an append entries RPC.
     *
     * @param sender  of the message.
     * @param seqNum  for sender to check if reply was reordered.
     * @param replyTo for receiver to reply to.
     * @param request append entries to be processed.
     */
    public AppendEntriesRPC(ActorRef<Raft> sender, int seqNum, ActorRef<AppendEntriesResult> replyTo, AppendEntries request) {
        super(sender, seqNum, replyTo, request);
    }
}
