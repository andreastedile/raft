package it.unitn.ds2.raft.rpc;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;

public final class VoteRequest extends AbstractRaftMsg implements RaftRequest {
    public final ActorRef<Raft> candidateId;
    public final int lastLogIndex;
    public final int lastLogTerm;

    /**
     * Invoked by candidates to gather votes.
     *
     * @param sender       of the message.
     * @param term         candidate’s term.
     * @param candidateId  candidate requesting vote.
     * @param lastLogIndex index of candidate’s last log entry.
     * @param lastLogTerm  term of candidate’s last log entry.
     */
    public VoteRequest(ActorRef<Raft> sender, int term, ActorRef<Raft> candidateId, int lastLogIndex, int lastLogTerm) {
        super(sender, term);
        this.candidateId = candidateId;
        this.lastLogIndex = lastLogIndex;
        this.lastLogTerm = lastLogTerm;
    }

    @Override
    public String toString() {
        return "VoteRequest{" +
                "term=" + term +
                ", candidateId=" + candidateId.path().name() +
                ", lastLogIndex=" + lastLogIndex +
                ", lastLogTerm=" + lastLogTerm +
                '}';
    }
}
