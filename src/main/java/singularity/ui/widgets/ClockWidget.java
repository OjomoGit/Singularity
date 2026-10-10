package singularity.ui.widgets;

import javafx.animation.AnimationTimer;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;
import singularity.ui.animations.AnimationUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ClockWidget extends VBox {

    private boolean isAnalog = false;

    private final Label digitalTimeLabel;
    private final Label digitalDateLabel;
    private final StackPane contentContainer;
    private final Canvas analogCanvas;
    private final Button toggleModeBtn;
    private final FontIcon toggleIcon;

    private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy");

    private AnimationTimer timer;
    private Color accentColor = Color.web("#d946ef");

    public ClockWidget() {
        getStyleClass().add("glass-pane");
        getStyleClass().add("widget-card");
        setSpacing(10);
        setStyle("-fx-padding: 12; -fx-background-radius: 12;");

        // Header with title and toggle mode button
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(8);

        Label title = new Label("Clock");
        title.setStyle("-fx-text-fill: -fx-text-muted; -fx-font-size: 13px; -fx-font-weight: bold;");

        toggleIcon = new FontIcon("fth-clock");
        toggleIcon.setIconSize(14);
        toggleIcon.setIconColor(Color.web("#a1a1aa"));

        toggleModeBtn = new Button();
        toggleModeBtn.setGraphic(toggleIcon);
        toggleModeBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2 6;");
        AnimationUtils.applySmoothHoverAndClick(toggleModeBtn);

        toggleModeBtn.setOnAction(e -> toggleMode());

        HBox.setHgrow(title, javafx.scene.layout.Priority.ALWAYS);
        header.getChildren().addAll(title, toggleModeBtn);

        // Content Area
        contentContainer = new StackPane();
        contentContainer.setAlignment(Pos.CENTER);

        // Digital View
        VBox digitalBox = new VBox(4);
        digitalBox.setAlignment(Pos.CENTER);
        digitalTimeLabel = new Label();
        digitalTimeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");

        digitalDateLabel = new Label();
        digitalDateLabel.setStyle("-fx-text-fill: -fx-text-muted; -fx-font-size: 11px;");

        digitalBox.getChildren().addAll(digitalTimeLabel, digitalDateLabel);

        // Analog View
        analogCanvas = new Canvas(110, 110);
        analogCanvas.setVisible(false);

        contentContainer.getChildren().addAll(digitalBox, analogCanvas);
        getChildren().addAll(header, contentContainer);

        startClock();
    }

    public void setAccentColor(Color color) {
        if (color != null) {
            this.accentColor = color;
        }
    }

    private void toggleMode() {
        isAnalog = !isAnalog;
        if (isAnalog) {
            contentContainer.getChildren().get(0).setVisible(false);
            analogCanvas.setVisible(true);
            toggleIcon.setIconLiteral("fth-type");
        } else {
            contentContainer.getChildren().get(0).setVisible(true);
            analogCanvas.setVisible(false);
            toggleIcon.setIconLiteral("fth-clock");
        }
    }

    private void startClock() {
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                LocalDateTime nowTime = LocalDateTime.now();
                digitalTimeLabel.setText(nowTime.format(timeFormat));
                digitalDateLabel.setText(nowTime.format(dateFormat));

                if (isAnalog) {
                    drawAnalogClock(nowTime);
                }
            }
        };
        timer.start();
    }

    private void drawAnalogClock(LocalDateTime now) {
        GraphicsContext gc = analogCanvas.getGraphicsContext2D();
        double w = analogCanvas.getWidth();
        double h = analogCanvas.getHeight();
        double cx = w / 2.0;
        double cy = h / 2.0;
        double r = Math.min(w, h) / 2.0 - 5;

        gc.clearRect(0, 0, w, h);

        // Outer face
        gc.setStroke(Color.web("rgba(255, 255, 255, 0.15)"));
        gc.setLineWidth(2);
        gc.strokeOval(cx - r, cy - r, r * 2, r * 2);

        // Tick marks
        gc.setStroke(Color.web("rgba(255, 255, 255, 0.4)"));
        gc.setLineWidth(1.5);
        for (int i = 0; i < 12; i++) {
            double angle = Math.toRadians(i * 30);
            double x1 = cx + (r - 6) * Math.sin(angle);
            double y1 = cy - (r - 6) * Math.cos(angle);
            double x2 = cx + r * Math.sin(angle);
            double y2 = cy - r * Math.cos(angle);
            gc.strokeLine(x1, y1, x2, y2);
        }

        double sec = now.getSecond() + now.getNano() / 1_000_000_000.0;
        double min = now.getMinute() + sec / 60.0;
        double hour = (now.getHour() % 12) + min / 60.0;

        // Hour hand
        double hAngle = Math.toRadians(hour * 30);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(3.5);
        gc.strokeLine(cx, cy, cx + (r * 0.5) * Math.sin(hAngle), cy - (r * 0.5) * Math.cos(hAngle));

        // Minute hand
        double mAngle = Math.toRadians(min * 6);
        gc.setStroke(Color.web("#f4f4f5"));
        gc.setLineWidth(2.5);
        gc.strokeLine(cx, cy, cx + (r * 0.72) * Math.sin(mAngle), cy - (r * 0.72) * Math.cos(mAngle));

        // Second hand (Accent Color)
        double sAngle = Math.toRadians(sec * 6);
        gc.setStroke(accentColor);
        gc.setLineWidth(1.5);
        gc.strokeLine(cx, cy, cx + (r * 0.85) * Math.sin(sAngle), cy - (r * 0.85) * Math.cos(sAngle));

        // Center pivot dot
        gc.setFill(accentColor);
        gc.fillOval(cx - 3, cy - 3, 6, 6);
    }
}
