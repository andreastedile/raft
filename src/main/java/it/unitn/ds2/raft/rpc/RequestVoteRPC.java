package it.unitn.ds2.raft.rpc;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;

public final class RequestVoteRPC extends RPCRequest<VoteRequest, Vote> {
    /**
     * Constructs a request vote RPC.
     *
     * @param sender  for the receiver to reply to.
     * @param seqNum  for sender to check if reply was reordered.
     * @param replyTo for receiver to reply to.
     * @param request vote request to be processed.
     */
    public RequestVoteRPC(ActorRef<Raft> sender, int seqNum, ActorRef<Vote> replyTo, VoteRequest request) {
        super(sender, seqNum, replyTo, request);
    }
}
