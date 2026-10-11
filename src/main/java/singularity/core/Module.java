package singularity.core;

import javafx.scene.Node;

public interface Module {
    String getModuleId();
    String getModuleName();
    String getIconLiteral();

    Node getView();
    void setView(Node view);

    default void onEnable() {}
    default void onDisable() {}
    default void setCloseHandler(Runnable closeHandler) {}
}