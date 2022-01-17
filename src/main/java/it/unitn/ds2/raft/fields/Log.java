package it.unitn.ds2.raft.fields;

import akka.actor.typed.eventstream.EventStream;
import akka.actor.typed.javadsl.ActorContext;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.events.log.LogAppend;
import it.unitn.ds2.raft.events.log.LogRemove;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * log entries; each entry contains command for servers.state machine, and term when entry was received by leader
 * (first index is 1)
 */
public class Log implements ContextAware {
    private ActorContext<Raft> ctx;
    private final List<LogEntry> log;

    public Log() {
        log = new ArrayList<>();
    }

    public Log(List<LogEntry> log) {
        this.log = new ArrayList<>(log);
    }

    public LogEntry get(int index) {
        return log.get(index - 1);
    }

    public List<LogEntry> getFrom(int index) {
        return log.subList(index - 1, log.size() - 1);
    }

    public void append(LogEntry entry) {
        log.add(entry);
        if (ctx != null) {
            ctx.getLog().debug("log[" + lastLogIndex() + "] ← " + entry);
            var event = new LogAppend(ctx.getSelf(), ctx.getSystem().uptime(), entry, lastLogIndex());
            var publish = new EventStream.Publish<>(event);
            ctx.getSystem().eventStream().tell(publish);
        }
    }

    public void storeEntries(int indexFrom, List<LogEntry> entries) {
        if (log.isEmpty() && indexFrom > 1) {
            throw new IllegalArgumentException("Cannot append log entries to an empty log from an index greater than 1");
        }
        if (indexFrom < 1) {
            throw new IllegalArgumentException("indexFrom is smaller than 1");
        }
        if (log.size() > 0 && indexFrom > lastLogIndex() + 1) {
            throw new IllegalArgumentException("Cannot append log entries to a log from an index greater than the index of the last entry in the log");
        }

        int i = indexFrom - 1; // points to the entry in the local log corresponding to indexFrom (if any)
        int j = 0; // points to the first entry in the message (if any)

        // advance the pointers until either the entries in the log or in the message are scanned completely,
        // or two conflicting entries are found
        while (i < log.size() && j < entries.size() &&
                log.get(i).equals(entries.get(j))) {
            i++;
            j++;
        }

        if (j < entries.size()) {
            // there are entries to be copied

            if (i < log.size()) {
                // the two entries conflicted

                while (i <= log.size() - 1) {
                    var removed = log.remove(this.log.size() - 1);
                    if (ctx != null) {
                        ctx.getLog().debug("lastLogIndex ← " + lastLogIndex());
                        var event = new LogRemove(ctx.getSelf(), ctx.getSystem().uptime(), removed, lastLogIndex() + 1);
                        var publish = new EventStream.Publish<>(event);
                        ctx.getSystem().eventStream().tell(publish);
                    }
                }
            }

            while (j < entries.size()) {
                log.add(i, entries.get(j));
                if (ctx != null) {
                    ctx.getLog().debug("log[" + lastLogIndex() + "] ← " + log.get(i));
                    var event = new LogAppend(ctx.getSelf(), ctx.getSystem().uptime(), log.get(i), lastLogIndex());
                    var publish = new EventStream.Publish<>(event);
                    ctx.getSystem().eventStream().tell(publish);
                }

                i++;
                j++;
            }
        }
    }

    public int lastLogIndex() {
        return log.size();
    }

    public int lastLogTerm() {
        return log.isEmpty() ? 0 : log.get(log.size() - 1).term;
    }

    @Override
    public void setCtx(ActorContext<Raft> ctx) {
        this.ctx = ctx;
    }

    @Override
    public String toString() {
        return log.stream()
                .map(LogEntry::toString)
                .collect(Collectors.joining(", "));
    }
}
