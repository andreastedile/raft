package it.unitn.ds2.raft.rpc;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;

public abstract class RPCRequest<Req extends RaftRequest, Res extends RaftResponse> extends AbstractRPCMsg {
    public final ActorRef<Res> replyTo;
    public final Req req;

    /**
     * Constructs an RPC request.
     *
     * @param sender  of the message.
     * @param seqNum  for sender to check if reply was reordered.
     * @param replyTo for receiver to reply to.
     * @param req     request to be processed.
     */
    public RPCRequest(ActorRef<Raft> sender, int seqNum, ActorRef<Res> replyTo, Req req) {
        super(sender, seqNum);
        this.replyTo = replyTo;
        this.req = req;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                "{sender=" + sender.path().name() +
                ", seqNum=" + seqNum +
                ", request=" + req +
                "}";
    }
}
