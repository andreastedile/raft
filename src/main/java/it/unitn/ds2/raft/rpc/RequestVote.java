package it.unitn.ds2.raft.rpc;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;

public class RequestVote extends AbstractRPCMsg {
    public final ActorRef<Raft> candidateId;
    public final int lastLogIndex;
    public final int lastLogTerm;

    /**
     * Invoked by candidates to gather votes.
     *
     * @param sender       for the receiver to reply to.
     * @param seqNum       for sender to check if reply was reordered.
     * @param term         candidate’s term.
     * @param candidateId  candidate requesting vote.
     * @param lastLogIndex index of candidate’s last log entry.
     * @param lastLogTerm  term of candidate’s last log entry.
     */
    public RequestVote(ActorRef<Raft> sender, int seqNum, int term, ActorRef<Raft> candidateId, int lastLogIndex, int lastLogTerm) {
        super(sender, seqNum, term);
        this.candidateId = candidateId;
        this.lastLogIndex = lastLogIndex;
        this.lastLogTerm = lastLogTerm;
    }

    @Override
    public String toString() {
        return "VoteRequest{" +
                "sender=" + sender.path().name() +
                ", term=" + term +
                ", candidateId=" + candidateId.path().name() +
                ", lastLogIndex=" + lastLogIndex +
                ", lastLogTerm=" + lastLogTerm +
                '}';
    }

    @Override
    public ActorRef<RPC> sender() {
        return sender.narrow();
    }

    @Override
    public int seqNum() {
        return seqNum;
    }
}
