package it.unitn.ds2.raft;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import it.unitn.ds2.raft.fields.Servers;
import it.unitn.ds2.raft.simulation.ClientStart;
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
        return Behaviors.receive(Raft.class)
                .onMessage(Command.class, cmd -> onCommandReceived(ctx, cmd, msg.servers))
                .build();
    }

    private static Behavior<Raft> onCommandReceived(ActorContext<Raft> ctx, Command msg, Servers servers) {
        ctx.getLog().info("Send state machine command to servers");
        servers.getAll().forEach(server -> server.tell(msg));

        return Behaviors.same();
    }
}
