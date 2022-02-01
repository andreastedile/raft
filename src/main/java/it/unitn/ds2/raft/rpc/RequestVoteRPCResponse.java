package it.unitn.ds2.raft.rpc;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;

public final class RequestVoteRPCResponse extends RPCResponse<VoteRequest, Vote> {
    /**
     * Constructs a request vote RPC response.
     *
     * @param sender   for the receiver to reply to.
     * @param response vote to be processed.
     */
    public RequestVoteRPCResponse(ActorRef<Raft> sender, VoteRequest request, Vote response) {
        super(sender, request, response);
    }
}
