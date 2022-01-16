package it.unitn.ds2.raft.fields;

import akka.actor.typed.javadsl.ActorContext;
import it.unitn.ds2.raft.Raft;

public interface ContextAware {
    void setCtx(ActorContext<Raft> ctx);
}
