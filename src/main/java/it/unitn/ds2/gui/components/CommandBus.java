package it.unitn.ds2.gui.components;

import akka.actor.typed.ActorSystem;
import it.unitn.ds2.gui.commands.AddServer;
import it.unitn.ds2.gui.commands.GUICommand;
import it.unitn.ds2.raft.Raft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class CommandBus implements Component {
    private final Map<Class<? extends GUICommand>, List<Consumer<GUICommand>>> listeners;
    private final ActorSystem<Raft> actorSystem;

    public CommandBus(ActorSystem<Raft> actorSystem) {
        listeners = new HashMap<>();
        this.actorSystem = actorSystem;
    }

    public <T extends GUICommand> void emit(T event) {
        var listeners = this.listeners.get(event.getClass());
        if (listeners != null) {
            listeners.forEach(listener -> actorSystem.tell(event));
        }
    }

    public <T extends GUICommand> void listenFor(Class<T> eventClass, Consumer<T> consumer) {
        if (!listeners.containsKey(eventClass)) {
            listeners.put(eventClass, new ArrayList<>());
        }
        //noinspection unchecked
        listeners.get(eventClass).add((Consumer<GUICommand>) consumer);
    }

    @Override
    public void terminate() {
        listeners.clear();
    }
}
