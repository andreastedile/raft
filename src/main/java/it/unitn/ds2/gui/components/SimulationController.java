package it.unitn.ds2.gui.components;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import it.unitn.ds2.gui.commands.*;
import it.unitn.ds2.raft.Raft;
import it.unitn.ds2.raft.events.RaftEvent;
import it.unitn.ds2.raft.events.Spawn;
import it.unitn.ds2.raft.simulation.Crash;
import it.unitn.ds2.raft.simulation.Join;
import it.unitn.ds2.raft.simulation.Start;
import it.unitn.ds2.raft.simulation.Stop;
import it.unitn.ds2.raft.states.follower.Follower;

import java.util.ArrayList;
import java.util.List;

public class SimulationController extends AbstractBehavior<Raft> {
    private final ApplicationContext applicationContext;
    private final List<ActorRef<Raft>> servers;

    public SimulationController(ActorContext<Raft> actorContext, ApplicationContext applicationContext) {
        super(actorContext);
        actorContext.setLoggerName(actorContext.getSelf().path().name());

        this.applicationContext = applicationContext;
        servers = new ArrayList<>();

        applicationContext.commandBus.listenFor(AddServer.class, this::onAddServer);
        applicationContext.commandBus.listenFor(StartSimulation.class, this::onStartSimulation);
    }

    public static Behavior<Raft> create(ApplicationContext applicationContext) {
        return Behaviors.setup(actorContext -> new SimulationController(actorContext, applicationContext));
    }

    @Override
    public Receive<Raft> createReceive() {
        return newReceiveBuilder()
                // it.unitn.ds2.raft.events
                .onMessage(RaftEvent.class, this::onEvent)
                // it.unitn.ds2.gui.commands
                .onMessage(AddServer.class, this::onAddServer)
                .onMessage(CrashServer.class, this::onCrashServer)
                .onMessage(StartSimulation.class, this::onStartSimulation)
                .onMessage(StopSimulation.class, this::onStopSimulation)
                .onMessage(SendCommand.class, this::onSendStateMachineCommand)
                .onMessage(RestartServer.class, this::onRestartServerCommand)
                .build();
    }

    private Behavior<Raft> onEvent(RaftEvent event) {
        applicationContext.eventBus.emit(event);
        return this;
    }

    private Behavior<Raft> onAddServer(AddServer command) {
        getContext().getLog().info("Add server command");
        var server = getContext().spawn(Follower.create(), "server" + (servers.size() + 1));
        servers.add(server);

        var event = new Spawn(server, getContext().getSystem().uptime());
        applicationContext.eventBus.emit(event);

        var join = new Join(server);
        servers.forEach(other -> other.tell(join));
        servers.stream().map(Join::new).forEach(server::tell);
        return this;
    }

    private Behavior<Raft> onCrashServer(CrashServer command) {
        getContext().getLog().info("Crash server command");
        command.server.tell(new Crash());
        return this;
    }

    private Behavior<Raft> onStartSimulation(StartSimulation command) {
        getContext().getLog().info("Start it.unitn.ds2.raft.simulation command");
        var start = new Start();
        servers.forEach(server -> server.tell(start));
        return this;
    }

    private Behavior<Raft> onStopSimulation(StopSimulation command) {
        getContext().getLog().info("Stop it.unitn.ds2.raft.simulation command");
        var stop = new Stop();
        servers.forEach(server -> server.tell(stop));
        return this;
    }

    private Behavior<Raft> onSendStateMachineCommand(SendCommand command) {
        getContext().getLog().info("Send state machine command command");
        servers.forEach(server -> server.tell(command.command));
        return this;
    }

    private Behavior<Raft> onRestartServerCommand(RestartServer command) {
        getContext().getLog().info("Restart server command command");
        var start = new Start();
        command.server.tell(start);
        return this;
    }
}
