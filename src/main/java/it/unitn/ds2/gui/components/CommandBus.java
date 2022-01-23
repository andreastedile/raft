package it.unitn.ds2.gui.components;

import akka.actor.typed.ActorSystem;
import it.unitn.ds2.gui.commands.GUICommand;
import it.unitn.ds2.raft.Raft;

public class CommandBus implements Component {
    private ActorSystem<Raft> actorSystem;

    public CommandBus(ActorSystem<Raft> actorSystem) {
        this.actorSystem = actorSystem;
    }

    public <T extends GUICommand> void emit(T event) {
        actorSystem.tell(event);
    }

    @Override
    public void terminate() {
        actorSystem = null;
    }
}
