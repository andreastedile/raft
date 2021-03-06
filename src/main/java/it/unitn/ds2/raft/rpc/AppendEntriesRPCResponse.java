package it.unitn.ds2.raft.rpc;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;

public final class AppendEntriesRPCResponse extends RPCResponse<AppendEntries, AppendEntriesResult> {
    /**
     * Constructs an append entries RPC response.
     *
     * @param sender   for the receiver to reply to.
     * @param response append entries response to be processed.
     */
    public AppendEntriesRPCResponse(ActorRef<Raft> sender, AppendEntries request, AppendEntriesResult response) {
        super(sender, request, response);
    }
}
