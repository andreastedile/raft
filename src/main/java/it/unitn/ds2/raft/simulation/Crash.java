package it.unitn.ds2.raft.simulation;

import it.unitn.ds2.raft.Raft;

import java.time.Duration;

public final class Crash implements Raft {
    public final Duration duration;

    /**
     * @param duration Duration for which the server receiving the message should stay crashed
     *                 and after which it should resume.
     *                 If null, the server will stay crashed until it will receive a {@link Start} message.
     */
    public Crash(Duration duration) {
        this.duration = duration;
    }
}