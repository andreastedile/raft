package it.unitn.ds2.raft.properties;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class SimulationProperties {
    private static SimulationProperties instance;

    public long timeScale;
    public long minElectionTimeoutMs;
    public long maxElectionTimeoutMs;
    public long heartbeatMs;
    public long rpcTimeoutMs;

    private SimulationProperties() {
        try (InputStream file = SimulationProperties.class.getResourceAsStream("/it.unitn.ds2.raft.simulation.it.unitn.ds2.raft.properties")) {
            if (file == null) {
                throw new FileNotFoundException("it.unitn.ds2.raft.simulation.it.unitn.ds2.raft.properties not found");
            }

            var props = new java.util.Properties();
            props.load(file);

            timeScale = Long.parseLong(props.getProperty("timeScale"));
            minElectionTimeoutMs = Long.parseLong(props.getProperty("minElectionTimeoutMs")) * timeScale;
            maxElectionTimeoutMs = Long.parseLong(props.getProperty("maxElectionTimeoutMs")) * timeScale;
            heartbeatMs = Long.parseLong(props.getProperty("heartbeatMs")) * timeScale;
            rpcTimeoutMs = Long.parseLong(props.getProperty("rpcTimeoutMs")) * timeScale;
        } catch (IOException ignored) {
        }
    }

    public static SimulationProperties getInstance() {
        if (instance == null) {
            instance = new SimulationProperties();
        }
        return instance;
    }
}
