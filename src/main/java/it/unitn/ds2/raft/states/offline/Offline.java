package it.unitn.ds2.raft.states.offline;

import akka.actor.typed.Behavior;
import akka.actor.typed.eventstream.EventStream;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.events.Spawn;
import it.unitn.ds2.raft.events.StateChange;
import it.unitn.ds2.raft.fields.Servers;
import it.unitn.ds2.raft.simulation.Join;
import it.unitn.ds2.raft.simulation.Start;
import it.unitn.ds2.raft.states.Server;
import it.unitn.ds2.raft.states.State;
import it.unitn.ds2.raft.states.follower.Follower;
import it.unitn.ds2.raft.states.follower.FollowerState;

public class Offline extends Server {
    public static Behavior<Raft> create() {
        var servers = new Servers();

        return Behaviors.setup(ctx -> {
                    ctx.setLoggerName(ctx.getSelf().path().name());

                    var spawn = new Spawn(ctx.getSelf(), ctx.getSystem().uptime());
                    var publish = new EventStream.Publish<>(spawn);
                    ctx.getSystem().eventStream().tell(publish);

                    return Behaviors.receive(Raft.class)
                            .onMessage(Join.class, msg -> onJoin(ctx, servers, msg))
                            .onMessage(Start.class, msg -> start(ctx, servers))
                            .build();
                }
        );
    }

    private static Behavior<Raft> onJoin(ActorContext<Raft> ctx, Servers servers, Join msg) {
        ctx.getLog().info(msg.server.path().name() + " joined the Raft cluster");
        servers.add(msg.server);
        return Behaviors.same();
    }

    private static Behavior<Raft> start(ActorContext<Raft> ctx, Servers servers) {
        ctx.getLog().info("Starting");

        var state = new FollowerState();

        state.currentTerm.setCtx(ctx);
        state.votedFor.setCtx(ctx);
        state.log.setCtx(ctx);
        state.commitIndex.setCtx(ctx);
        state.lastApplied.setCtx(ctx);

        return Follower.waitForAppendEntries(ctx, servers, state);
    }

    public static Behavior<Raft> waiting(ActorContext<Raft> ctx, Servers servers, State state) {
        var event = new StateChange(ctx.getSelf(), ctx.getSystem().uptime(), StateChange.State.OFFLINE);
        var publish = new EventStream.Publish<>(event);
        ctx.getSystem().eventStream().tell(publish);

        return Behaviors.receive(Raft.class)
                .onMessage(Start.class, unused -> Follower.waitForAppendEntries(ctx, servers, FollowerState.fromAnyState(state)))
                .build();
    }
}
