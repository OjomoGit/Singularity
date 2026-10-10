package singularity.ui.widgets;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import singularity.ui.animations.AnimationUtils;

import java.util.ArrayList;
import java.util.List;

public class WidgetManager extends VBox {

    private final StackPane mainStack;
    private final VBox widgetDisplayContainer;
    private final VBox widgetMenuContainer;

    private final ClockWidget clockWidget;
    private final SystemStatsWidget statsWidget;
    private final TerminalWidget terminalWidget;

    private final List<WidgetItem> widgetItems = new ArrayList<>();
    private boolean isManagerView = false;
    private final Label headerTitle;
    private final FontIcon headerIcon;

    private static class WidgetItem {
        String name;
        Node widgetNode;
        boolean enabled;
        CheckBox checkBox;

        WidgetItem(String name, Node widgetNode, boolean enabled) {
            this.name = name;
            this.widgetNode = widgetNode;
            this.enabled = enabled;
        }
    }

    public WidgetManager() {
        setSpacing(12);

        // Interactive Header
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setSpacing(8);
        headerBox.setStyle("-fx-cursor: hand; -fx-padding: 4 6; -fx-background-radius: 8;");

        headerTitle = new Label("Widgets & System");
        headerTitle.getStyleClass().add("sub-header-title");
        headerTitle.setStyle("-fx-font-size: 18px; -fx-text-fill: white; -fx-padding: 0;");

        headerIcon = new FontIcon("fth-sliders");
        headerIcon.setIconSize(16);
        headerIcon.setIconColor(Color.web("#a1a1aa"));

        HBox.setHgrow(headerTitle, Priority.ALWAYS);
        headerBox.getChildren().addAll(headerTitle, headerIcon);

        AnimationUtils.applySmoothHoverAndClick(headerBox);
        headerBox.setOnMouseClicked(e -> toggleView());

        // Create Widget Instances
        clockWidget = new ClockWidget();
        statsWidget = new SystemStatsWidget();
        terminalWidget = new TerminalWidget();

        widgetItems.add(new WidgetItem("Clock Widget", clockWidget, true));
        widgetItems.add(new WidgetItem("System Stats", statsWidget, true));
        widgetItems.add(new WidgetItem("Mini Terminal", terminalWidget, true));

        // Stack Container for Display vs Customization Menu
        mainStack = new StackPane();

        widgetDisplayContainer = new VBox(15);
        widgetMenuContainer = new VBox(12);
        widgetMenuContainer.setVisible(false);
        widgetMenuContainer.setManaged(false);
        widgetMenuContainer.getStyleClass().add("glass-pane");
        widgetMenuContainer.setStyle("-fx-padding: 15; -fx-background-radius: 12;");

        mainStack.getChildren().addAll(widgetDisplayContainer, widgetMenuContainer);

        getChildren().addAll(headerBox, mainStack);

        buildManagerMenu();
        refreshDisplayWidgets();
    }

    public void setAccentColor(Color color) {
        if (clockWidget != null) {
            clockWidget.setAccentColor(color);
        }
    }

    public void toggleView() {
        isManagerView = !isManagerView;

        Node currentView = isManagerView ? widgetDisplayContainer : widgetMenuContainer;
        Node nextView = isManagerView ? widgetMenuContainer : widgetDisplayContainer;

        headerTitle.setText(isManagerView ? "Widget Settings" : "Widgets & System");
        headerIcon.setIconLiteral(isManagerView ? "fth-check" : "fth-sliders");

        // Fluid slide & scale transition between views
        FadeTransition fadeOut = new FadeTransition(Duration.millis(180), currentView);
        fadeOut.setToValue(0);
        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(180), currentView);
        scaleOut.setToX(0.95); scaleOut.setToY(0.95);

        ParallelTransition animOut = new ParallelTransition(fadeOut, scaleOut);
        animOut.setOnFinished(e -> {
            currentView.setVisible(false);
            currentView.setManaged(false);

            nextView.setVisible(true);
            nextView.setManaged(true);
            nextView.setOpacity(0);
            nextView.setScaleX(0.95);
            nextView.setScaleY(0.95);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(220), nextView);
            fadeIn.setToValue(1.0);
            ScaleTransition scaleIn = new ScaleTransition(Duration.millis(220), nextView);
            scaleIn.setToX(1.0); scaleIn.setToY(1.0);
            scaleIn.setInterpolator(Interpolator.EASE_OUT);

            new ParallelTransition(fadeIn, scaleIn).play();
        });

        animOut.play();
    }

    private void buildManagerMenu() {
        widgetMenuContainer.getChildren().clear();

        Label subtitle = new Label("Customize Active Widgets");
        subtitle.setStyle("-fx-text-fill: -fx-text-muted; -fx-font-size: 12px; -fx-font-weight: bold;");
        widgetMenuContainer.getChildren().add(subtitle);

        for (int i = 0; i < widgetItems.size(); i++) {
            final int index = i;
            WidgetItem item = widgetItems.get(i);

            HBox itemRow = new HBox(8);
            itemRow.setAlignment(Pos.CENTER_LEFT);
            itemRow.setStyle("-fx-background-color: rgba(0,0,0,0.25); -fx-padding: 8 10; -fx-background-radius: 8;");

            CheckBox cb = new CheckBox(item.name);
            cb.setSelected(item.enabled);
            cb.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
            cb.setOnAction(e -> {
                item.enabled = cb.isSelected();
                refreshDisplayWidgets();
            });
            item.checkBox = cb;

            HBox.setHgrow(cb, Priority.ALWAYS);

            // Move Up button
            Button moveUpBtn = new Button();
            FontIcon upIcon = new FontIcon("fth-chevron-up");
            upIcon.setIconSize(12); upIcon.setIconColor(Color.WHITE);
            moveUpBtn.setGraphic(upIcon);
            moveUpBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2 4;");
            moveUpBtn.setDisable(index == 0);
            moveUpBtn.setOnAction(e -> moveItem(index, index - 1));

            // Move Down button
            Button moveDownBtn = new Button();
            FontIcon downIcon = new FontIcon("fth-chevron-down");
            downIcon.setIconSize(12); downIcon.setIconColor(Color.WHITE);
            moveDownBtn.setGraphic(downIcon);
            moveDownBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2 4;");
            moveDownBtn.setDisable(index == widgetItems.size() - 1);
            moveDownBtn.setOnAction(e -> moveItem(index, index + 1));

            itemRow.getChildren().addAll(cb, moveUpBtn, moveDownBtn);
            widgetMenuContainer.getChildren().add(itemRow);
        }

        // Back / Done Button
        Button doneBtn = new Button("Done");
        doneBtn.setMaxWidth(Double.MAX_VALUE);
        doneBtn.setStyle("-fx-background-color: linear-gradient(to right, -fx-accent-dark 0%, -fx-accent 100%); -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8; -fx-background-radius: 8; -fx-cursor: hand;");
        AnimationUtils.applySmoothHoverAndClick(doneBtn);
        doneBtn.setOnAction(e -> toggleView());

        widgetMenuContainer.getChildren().add(doneBtn);
    }

    private void moveItem(int fromIndex, int toIndex) {
        if (toIndex < 0 || toIndex >= widgetItems.size()) return;
        WidgetItem item = widgetItems.remove(fromIndex);
        widgetItems.add(toIndex, item);
        buildManagerMenu();
        refreshDisplayWidgets();
    }

    private void refreshDisplayWidgets() {
        widgetDisplayContainer.getChildren().clear();
        for (WidgetItem item : widgetItems) {
            if (item.enabled) {
                widgetDisplayContainer.getChildren().add(item.widgetNode);
            }
        }
    }
}
