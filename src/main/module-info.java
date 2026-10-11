module singularity {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires javafx.web;
    requires jdk.jsobject;
    requires java.desktop;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.feather;

    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires java.management;

    opens singularity.app to javafx.graphics;
    opens singularity.ui.controllers to javafx.fxml;
    opens singularity.ui.widgets to javafx.fxml;
    opens singularity.modules.poker to javafx.fxml;

    exports singularity.app;
    exports singularity.core;
    exports singularity.ui.animations;
    exports singularity.ui.widgets;
    exports singularity.modules.poker;
}