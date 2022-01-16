package it.unitn.ds2.raft.rpc;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.fields.LogEntry;

import java.util.List;
import java.util.stream.Collectors;

public class AppendEntries extends AbstractRPCMsg implements RPCRequest {
    public final ActorRef<Raft> leaderId;
    public final int prevLogIndex;
    public final int prevLogTerm;
    public final List<LogEntry> entries;
    public final int leaderCommit;

    /**
     * Invoked by leader to replicate log entries; also used as heartbeat.
     *
     * @param sender       for the receiver to reply to.
     * @param term         leader’s term.
     * @param seqNum       for sender to check if reply was reordered.
     * @param leaderId     so follower can redirect clients.
     * @param prevLogIndex index of log entry immediately preceding new ones.
     * @param prevLogTerm  term of prevLogIndex entry.
     * @param entries      log entries to store (empty for heartbeat, may send more than one for efficiency).
     * @param leaderCommit leader’s commitIndex.
     */
    public AppendEntries(ActorRef<Raft> sender, int seqNum, int term, ActorRef<Raft> leaderId, int prevLogIndex, int prevLogTerm, List<LogEntry> entries, int leaderCommit) {
        super(sender, seqNum, term);
        this.leaderId = leaderId;
        this.prevLogIndex = prevLogIndex;
        this.prevLogTerm = prevLogTerm;
        this.entries = List.copyOf(entries);
        this.leaderCommit = leaderCommit;
    }

    public boolean isHeartbeat() {
        return entries.isEmpty();
    }

    @Override
    public String toString() {
        return "AppendEntries{" +
                "sender=" + sender.path().name() +
                ", seqNum=" + seqNum +
                ", term=" + term +
                ", leaderId=" + leaderId.path().name() +
                ", prevLogIndex=" + prevLogIndex +
                ", prevLogTerm=" + prevLogTerm +
                ", entries=[" + entries.stream().map(LogEntry::toString).collect(Collectors.joining(", ")) + "]" +
                ", leaderCommit=" + leaderCommit +
                '}';
    }

    @Override
    public ActorRef<RPC> sender() {
        return leaderId.narrow();
    }

    @Override
    public int seqNum() {
        return seqNum;
    }
}
