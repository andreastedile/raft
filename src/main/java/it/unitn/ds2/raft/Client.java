package it.unitn.ds2.raft;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.TimerScheduler;
import it.unitn.ds2.raft.fields.Servers;
import it.unitn.ds2.raft.rpc.ClientStart;
import it.unitn.ds2.raft.rpc.EntryConfirmedTimeout;
import it.unitn.ds2.raft.simulation.Command;

public class Client {
    public static Behavior<Raft> create() {
        return Behaviors.setup(ctx -> {
            ctx.setLoggerName(ctx.getSelf().path().name());

            return Behaviors.receive(Raft.class)
                    .onMessage(ClientStart.class, msg -> onClientStart(ctx, msg))
                    .build();
        });
    }

    private static Behavior<Raft> onClientStart(ActorContext<Raft> ctx, ClientStart msg) {
        if (msg.useAutoMode) {
            return Behaviors.same();
        } else {
            return Behaviors.withTimers(timers -> Behaviors.receive(Raft.class)
                    .onMessage(Command.class, cmd -> onCommandReceived(ctx, cmd, msg.servers, timers))
                    .onMessage(EntryConfirmedTimeout.class, lostCmd -> resendUnconfirmedMessage(ctx, lostCmd.lostCommand, msg.servers, timers))
                    .build()
            );
        }
    }

    private static Behavior<Raft> resendUnconfirmedMessage(ActorContext<Raft> ctx, Command lostCmd, Servers servers, TimerScheduler<Raft> timers) {
        onCommandReceived(ctx, lostCmd, servers, timers);
        return Behaviors.same();
    }

    private static Behavior<Raft> onCommandReceived(ActorContext<Raft> ctx, Command msg, Servers servers, TimerScheduler<Raft> timers) {
        ctx.getLog().info("Send entry to servers");
        servers.getAll().forEach(server -> server.tell(msg));
        return Behaviors.same();
    }
}
