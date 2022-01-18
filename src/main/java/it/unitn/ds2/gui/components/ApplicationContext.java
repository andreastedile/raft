package it.unitn.ds2.gui.components;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import it.unitn.ds2.raft.Raft;

public class ApplicationContext {
    public final CommandBus commandBus;
    public final EventBus eventBus;

    public ApplicationContext() {
        Behavior<Raft> raftBehavior = SimulationController.create(this);
        ActorSystem<Raft> raftActorSystem = ActorSystem.create(raftBehavior, "raft-cluster");

        commandBus = new CommandBus(raftActorSystem);
        eventBus = new EventBus();

    }

    /**
     * Terminates all the it.unitn.ds2.gui.components of the application.
     */
    public void terminate() {
        commandBus.terminate();
        eventBus.terminate();
    }
}
