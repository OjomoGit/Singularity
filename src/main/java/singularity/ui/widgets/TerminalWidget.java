package singularity.ui.widgets;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import singularity.ui.controllers.MainController;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TerminalWidget extends VBox {

    private final VBox outputBox;
    private final ScrollPane scrollPane;
    private final TextField inputField;

    public TerminalWidget() {
        getStyleClass().add("glass-pane");
        getStyleClass().add("widget-card");
        setSpacing(8);
        setStyle("-fx-padding: 10; -fx-background-radius: 12; -fx-min-height: 180;");

        Label title = new Label("Terminal");
        title.setStyle("-fx-text-fill: -fx-text-muted; -fx-font-size: 13px; -fx-font-weight: bold;");

        // Output Console Area
        outputBox = new VBox(3);
        outputBox.setStyle("-fx-background-color: transparent;");

        scrollPane = new ScrollPane(outputBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(120);
        scrollPane.setMaxHeight(150);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-viewport-background: transparent; -fx-padding: 0;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Input Line
        HBox inputRow = new HBox(6);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        Label prompt = new Label(">");
        prompt.setStyle("-fx-text-fill: -fx-accent; -fx-font-weight: bold; -fx-font-size: 12px;");

        inputField = new TextField();
        inputField.setPromptText("Enter command...");
        inputField.setStyle("-fx-background-color: rgba(0,0,0,0.3); -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 11px;");
        HBox.setHgrow(inputField, Priority.ALWAYS);

        inputField.setOnAction(e -> handleCommandSubmit());

        inputRow.getChildren().addAll(prompt, inputField);

        getChildren().addAll(title, scrollPane, inputRow);

        printLine("Singularity Terminal v1.0",Color.WHITE );
        printLine("Type 'help' for Commands.", Color.web("#a1a1aa"));
    }

    private void printLine(String text, Color color) {
        Label line = new Label(text);
        line.setWrapText(true);
        line.setStyle(String.format("-fx-text-fill: #%02x%02x%02x; -fx-font-family: 'Minecraft'; -fx-font-size: 11px;",
                (int)(color.getRed() * 255), (int)(color.getGreen() * 255), (int)(color.getBlue() * 255)));
        outputBox.getChildren().add(line);

        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    private void handleCommandSubmit() {
        String cmd = inputField.getText().trim();
        inputField.clear();

        if (cmd.isEmpty()) return;

        printLine("> " + cmd, Color.web("#f4f4f5"));

        String lower = cmd.toLowerCase();
        if (lower.equals("clear") || lower.equals("cls")) {
            outputBox.getChildren().clear();
        } else if (lower.equals("help")) {
            printLine("Built-in commands:", Color.web("#d946ef"));
            printLine("  help     - Show command list", Color.web("#a1a1aa"));
            printLine("  clear    - Clear console", Color.web("#a1a1aa"));
            printLine("  time     - Show system time", Color.web("#a1a1aa"));
            printLine("  sysinfo  - Show system specs", Color.web("#a1a1aa"));
            printLine("  echo     - Print text", Color.web("#a1a1aa"));
            printLine("  <cmd>    - Execute OS shell command", Color.web("#a1a1aa"));
        } else if (lower.equals("time") || lower.equals("date")) {
            printLine(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), Color.web("#38bdf8"));
        } else if (lower.equals("sysinfo") || lower.equals("stats")) {
            printLine("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"), Color.web("#38bdf8"));
            printLine("Java: " + System.getProperty("java.version"), Color.web("#38bdf8"));
            printLine("Cores: " + Runtime.getRuntime().availableProcessors(), Color.web("#38bdf8"));
            printLine("Max Memory: " + (Runtime.getRuntime().maxMemory() / 1048576) + " MB", Color.web("#38bdf8"));
        } else if (lower.startsWith("echo ")) {
            printLine(cmd.substring(5), Color.web("#f4f4f5"));
        } else {
            executeShellCommandAsync(cmd);
        }
    }

    private void executeShellCommandAsync(String cmd) {
        new Thread(() -> {
            try {
                boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
                ProcessBuilder pb = isWindows ?
                        new ProcessBuilder("cmd.exe", "/c", cmd) :
                        new ProcessBuilder("/bin/sh", "-c", cmd);

                pb.redirectErrorStream(true);
                Process proc = pb.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    int lineCount = 0;
                    while ((line = reader.readLine()) != null && lineCount < 40) {
                        final String outputStr = line;
                        Platform.runLater(() -> printLine(outputStr, Color.web("#a1a1aa")));
                        lineCount++;
                    }
                }
                proc.waitFor();
            } catch (Exception ex) {
                Platform.runLater(() -> printLine("Error: " + ex.getMessage(), Color.web("#ef4444")));
            }
        }).start();
    }
}
