package it.unitn.ds2.raft.rpc;

import akka.actor.typed.ActorRef;
import it.unitn.ds2.raft.simulation.Command;

public class EntryConfirmedTimeout implements RPC {
    public final Command lostCommand;

    @Override
    public ActorRef<RPC> sender() {
        return null;
    }

    @Override
    public int seqNum() {
        return 1;
    }

    public EntryConfirmedTimeout(Command cmd) {
        this.lostCommand = cmd;
    }
}
