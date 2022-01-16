package it.unitn.ds2.gui.view;

import it.unitn.ds2.gui.components.ApplicationContext;
import it.unitn.ds2.gui.view.locallog.LocalLogView;
import it.unitn.ds2.gui.view.server.ServerView;
import it.unitn.ds2.gui.view.toolbar.ClientToolbar;
import it.unitn.ds2.gui.view.toolbar.SimulationControlToolbar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;


public class MainView extends BorderPane {

    public MainView(ApplicationContext applicationContext) {
        var simulationControlToolbar = new SimulationControlToolbar(applicationContext);
        setTop(simulationControlToolbar);

        var serverView = new ServerView(applicationContext);
        var localLogView = new LocalLogView(applicationContext);
        var vBox = new VBox(serverView, localLogView);
        setCenter(vBox);

        var clientToolbar = new ClientToolbar(applicationContext);
        setBottom(clientToolbar);
    }
}
