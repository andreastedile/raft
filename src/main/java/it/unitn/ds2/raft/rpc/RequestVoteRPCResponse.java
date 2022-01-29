package it.unitn.ds2.raft.rpc;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;

public final class RequestVoteRPCResponse extends RPCResponse<VoteRequest, Vote> {
    /**
     * Constructs a request vote RPC response.
     *
     * @param sender   for the receiver to reply to.
     * @param seqNum   for sender to check if reply was reordered.
     * @param response vote to be processed.
     */
    public RequestVoteRPCResponse(ActorRef<Raft> sender, int seqNum, VoteRequest request, Vote response) {
        super(sender, seqNum, request, response);
    }
}
