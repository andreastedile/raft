package it.unitn.ds2.gui.view.toolbar;

import it.unitn.ds2.gui.components.ApplicationContext;
import javafx.scene.control.ToolBar;

public abstract class AbstractToolbar extends ToolBar {
    protected final ApplicationContext applicationContext;

    protected AbstractToolbar(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}
