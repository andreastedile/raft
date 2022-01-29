package it.unitn.ds2.raft.rpc;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;

public final class AppendEntriesResult extends AbstractRaftMsg implements RaftResponse {
    public final boolean success;

    /**
     * @param sender  of the message.
     * @param term    currentTerm, for leader to update itself.
     * @param success true if follower contained entry matching prevLogIndex and prevLogTerm.
     */
    public AppendEntriesResult(ActorRef<Raft> sender, int term, boolean success) {
        super(sender, term);
        this.success = success;
    }

    @Override
    public String toString() {
        return "AppendEntriesResult{" +
                "term=" + term +
                ", success=" + success +
                '}';
    }
}
