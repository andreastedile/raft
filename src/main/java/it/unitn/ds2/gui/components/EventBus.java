package it.unitn.ds2.gui.components;

import it.unitn.ds2.raft.events.RaftEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class EventBus implements Component {
    private final Map<Class<? extends RaftEvent>, List<Consumer<RaftEvent>>> listeners;

    public EventBus() {
        listeners = new HashMap<>();
    }

    public <T extends RaftEvent> void emit(T event) {
        var listeners = this.listeners.get(event.getClass());
        if (listeners != null) {
            listeners.forEach(listener -> listener.accept(event));
        }
    }

    public <T extends RaftEvent> void listenFor(Class<T> eventClass, Consumer<T> consumer) {
        if (!listeners.containsKey(eventClass)) {
            listeners.put(eventClass, new ArrayList<>());
        }
        //noinspection unchecked
        listeners.get(eventClass).add((Consumer<RaftEvent>) consumer);
    }

    @Override
    public void terminate() {
        listeners.clear();
    }
}
