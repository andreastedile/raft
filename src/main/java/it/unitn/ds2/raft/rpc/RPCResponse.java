package it.unitn.ds2.raft.rpc;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;

public abstract class RPCResponse<Req extends RaftRequest, Res extends RaftResponse> extends AbstractRPCMsg {
    public final Req req;
    public final Res res;

    /**
     * Constructs an RPC response.
     *
     * @param sender for the receiver to reply to.
     * @param seqNum for sender to check if reply was reordered.
     * @param req    for sender to check what request this response was generated for.
     * @param res    response to be processed.
     */
    public RPCResponse(ActorRef<Raft> sender, int seqNum, Req req, Res res) {
        super(sender, seqNum);
        this.req = req;
        this.res = res;
    }

    @Override
    public final String toString() {
        return getClass().getSimpleName() +
                "{sender=" + sender.path().name() +
                ", seqNum=" + seqNum +
                ", response=" + res +
                "}";
    }
}
