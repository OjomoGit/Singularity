module singularity {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;

    requires com.sun.jna;
    requires com.sun.jna.platform;

    requires org.kordamp.ikonli:ikonli-feather-pack:12.3.1;

    requires javafx.scene.media;
    requires java.management;

    opens singularity.app to javafx.graphics;
    opens singularity.ui.controllers to javafx.fxml;
    opens singularity.ui.widgets to javafx.fxml;

    exports singularity.app;
    exports singularity.core;
    exports singularity.ui.animations;
    exports singularity.ui.widgets;
}
