package it.unitn.ds2.raft.states.leader;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.eventstream.EventStream;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.StashBuffer;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.events.StateChange;
import it.unitn.ds2.raft.fields.LogEntry;
import it.unitn.ds2.raft.fields.SeqNum;
import it.unitn.ds2.raft.fields.Servers;
import it.unitn.ds2.raft.rpc.*;
import it.unitn.ds2.raft.simulation.Command;
import it.unitn.ds2.raft.simulation.Crash;
import it.unitn.ds2.raft.simulation.Stop;
import it.unitn.ds2.raft.states.Server;
import it.unitn.ds2.raft.states.follower.Follower;
import it.unitn.ds2.raft.states.follower.FollowerState;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

public final class Leader extends Server {
    public static Behavior<Raft> elected(ActorContext<Raft> ctx, Servers servers, SeqNum seqNum, LeaderState state) {
        state.nextIndex.setCtx(ctx);
        state.matchIndex.setCtx(ctx);

        var event = new StateChange(ctx.getSelf(), ctx.getSystem().uptime(), StateChange.State.LEADER);
        var publish = new EventStream.Publish<>(event);
        ctx.getSystem().eventStream().tell(publish);

        return Behaviors.withStash(10, (StashBuffer<Raft> stash) -> {
            servers.getAll().forEach(server -> appendEntriesRPC(ctx, seqNum, state, server, true));

            return Behaviors.receive(Raft.class)
                    .onMessage(Command.class, msg -> onCommand(ctx, stash, servers, seqNum, state, msg))
                    .onMessage(AppendEntriesRPCResponse.class, msg -> onAppendEntriesResult(ctx, stash, servers, seqNum, state, msg))
                    .onMessage(RPCTimeout.class, msg -> onRPCTimeout(ctx, seqNum, state, msg))
                    .onMessage(Crash.class, msg -> crash(ctx, servers, state, msg))
                    .onMessage(Stop.class, msg -> stop(ctx, servers, state))
                    .onMessage(Vote.class, msg -> Behaviors.ignore())
                    .build();
        });
    }

    private static Behavior<Raft> onCommand(ActorContext<Raft> ctx, StashBuffer<Raft> stash, Servers servers, SeqNum seqNum, LeaderState state, Command msg) {
        ctx.getLog().debug("Received command " + msg);
        if (stash.isEmpty()) {
            var entry = new LogEntry(msg.command, state.currentTerm.get());
            state.log.append(entry);
            servers.getAll().forEach(server -> appendEntriesRPC(ctx, seqNum, state, server, false));
        } else {
            ctx.getLog().debug(stash.size() + " it.unitn.ds2.gui.commands still needs processing. Stashing the command");
            stash.stash(msg);
        }
        return Behaviors.same();
    }

    private static void appendEntriesRPC(ActorContext<Raft> ctx, SeqNum seqNum, LeaderState state, ActorRef<Raft> recipient, boolean isHeartbeat) {
        var appendEntries = createAppendEntries(ctx, state, recipient, isHeartbeat);
        if (isHeartbeat) {
            ctx.getLog().debug("Sending heartbeat " + appendEntries + " to " + recipient.path().name());
        } else {
            ctx.getLog().debug("Sending appendEntries " + appendEntries + " to " + recipient.path().name());
        }
        ctx.ask(AppendEntriesResult.class, // resClass
                recipient, // target
                Duration.ofMillis(properties.rpcTimeoutMs), // responseTimeout
                (ActorRef<AppendEntriesResult> replyTo) -> new AppendEntriesRPC(ctx.getSelf(), seqNum.computeNext(recipient), replyTo, appendEntries), // createRequest
                (response, throwable) -> { // applyToResponse
                    if (response != null) {
                        return new AppendEntriesRPCResponse(recipient, seqNum.expectedSeqNum(recipient), appendEntries, response);
                    }
                    return new RPCTimeout(recipient);
                }
        );
    }

    private static Behavior<Raft> onRPCTimeout(ActorContext<Raft> ctx, SeqNum seqNum, LeaderState state, RPCTimeout msg) {
        ctx.getLog().debug("RPC timeout waiting for " + msg.server.path().name());
        appendEntriesRPC(ctx, seqNum, state, msg.server, false);
        return Behaviors.same();
    }

    private static Behavior<Raft> onAppendEntriesResult(ActorContext<Raft> ctx, StashBuffer<Raft> stash,
                                                        Servers servers,
                                                        SeqNum seqNum,
                                                        LeaderState state,
                                                        AppendEntriesRPCResponse msg) {
        if (msg.seqNum < seqNum.expectedSeqNum(msg.sender)) {
            ctx.getLog().debug("Discarded " + msg + " because sequence numbers don't match " +
                    "(message's seqNum is " + msg.seqNum + ", expected " + seqNum.expectedSeqNum(msg.sender));
            return Behaviors.same();
        }

        if (msg.res.term > state.currentTerm.get()) {
            ctx.getLog().debug("Received " + msg);
            ctx.getLog().debug("Lagging (message's term is " + msg.res.term + ", currentTerm is " + state.currentTerm);
            state.currentTerm.set(msg.res.term);
            state.votedFor.set(null);
            return Follower.waitForAppendEntries(ctx, servers, FollowerState.fromAnyState(state));
        }

        if (msg.res.success) {
            if (!msg.req.isHeartbeat()) {
                // If successful: update nextIndex and matchIndex for follower
                state.nextIndex.increment(msg.sender);
                state.matchIndex.increment(msg.sender);
            }

            // If there exists an N such that N > commitIndex,
            // a majority of matchIndex[i] ≥ N, and log[N].term == currentTerm:
            // set commitIndex = N
            IntStream.rangeClosed(state.commitIndex.get() + 1, state.log.lastLogIndex())
                    .forEach(N -> {
                        long count = state.nextIndex.values().stream().filter(index -> index >= N).count() + 1;
                        if (count >= majority(servers.size())) {
                            if (state.log.get(N).term == state.currentTerm.get()) {
                                state.commitIndex.increment(); // todo: ?
                                state.lastApplied.increment();
                                stash.unstash(Behaviors.same(), 1, Function.identity());
                            }
                        }
                    });

            appendEntriesRPC(ctx, seqNum, state, msg.sender, true);
        } else {
            state.nextIndex.decrement(msg.sender);

            appendEntriesRPC(ctx, seqNum, state, msg.sender, false);
        }

        return Behaviors.same();
    }

    private static AppendEntries createAppendEntries(ActorContext<Raft> ctx, LeaderState state, ActorRef<Raft> server, boolean isHeartbeat) {
        int prevLogIndex = state.nextIndex.get(server) - 1; // prevLogIndex – index of log entry immediately preceding new ones.
        int prevLogTerm = prevLogIndex > 0 ? state.log.get(prevLogIndex).term : 0; // term of prevLogIndex entry.
        return new AppendEntries(ctx.getSelf(), // sender – for the receiver to reply to.
                state.currentTerm.get(), // term – leader’s term.
                ctx.getSelf(), // leader id
                prevLogIndex,
                prevLogTerm,
                isHeartbeat ? List.of() : state.log.getFrom(state.nextIndex.get(server)), // entries – log entries to store.
                state.commitIndex.get()); // leaderCommit – leader’s commitIndex.
    }
}
