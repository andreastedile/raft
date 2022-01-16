package it.unitn.ds2.gui.components;

import akka.actor.typed.ActorSystem;

public class ApplicationContext {
    public final CommandBus commandBus;
    public final EventBus eventBus;

    public ApplicationContext() {
        ActorSystem.create(SimulationController.create(this), "raft-cluster");

        commandBus = new CommandBus();
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
