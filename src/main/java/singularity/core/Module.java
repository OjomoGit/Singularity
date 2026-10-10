package singularity.core;

import javafx.scene.Node;

public interface Module {
    String getModuleName();
    Node getModuleView();
    void initializeModule();
}