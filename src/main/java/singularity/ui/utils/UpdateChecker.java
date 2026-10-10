package singularity.ui.utils;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class UpdateChecker {

    // DEINE AKTUELLE VERSION (Erhöhe diesen Wert bei jedem neuen Release)
    public static final String CURRENT_VERSION = "v1.0.0";

    // Deines GitHub Repository
    private static final String GITHUB_API_URL = "https://api.github.com/repos/OjomoGit/Singularity/releases/latest";

    public static void checkForUpdatesAsync() {
        new Thread(() -> {
            try {
                URL url = URI.create(GITHUB_API_URL).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                conn.setRequestProperty("User-Agent", "Singularity-App");

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    String json = response.toString();

                    // Liest das Versionstag (z.B. "v1.0.1") aus dem GitHub-JSON
                    String latestVersion = extractJsonValue(json, "tag_name");
                    // Liest die Download-URL der .jar-Datei aus
                    String downloadUrl = extractJsonValue(json, "browser_download_url");

                    if (latestVersion != null && !latestVersion.equalsIgnoreCase(CURRENT_VERSION)) {
                        System.out.println("Neues Update gefunden: " + latestVersion);

                        // Zeigt den Dialog auf dem Haupt-UI-Thread an
                        Platform.runLater(() -> showUpdateDialog(latestVersion, downloadUrl));
                    } else {
                        System.out.println("App ist auf dem neuesten Stand (" + CURRENT_VERSION + ").");
                    }
                }
            } catch (Exception e) {
                System.out.println("Update-Prüfung fehlgeschlagen: " + e.getMessage());
            }
        }).start();
    }

    private static void showUpdateDialog(String newVersion, String downloadUrl) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Update Verfügbar");
        alert.setHeaderText("Eine neue Version von Singularity ist da! (" + newVersion + ")");
        alert.setContentText("Möchtest du das Update jetzt automatisch herunterladen und installieren?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                downloadAndApplyUpdate(downloadUrl);
            }
        });
    }

    private static void downloadAndApplyUpdate(String downloadUrl) {
        new Thread(() -> {
            try {
                File currentJar = new File(UpdateChecker.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                File newJar = new File(currentJar.getParent(), "Singularity-update.jar");

                System.out.println("Lade Update herunter...");
                URL url = URI.create(downloadUrl).toURL();
                try (InputStream in = url.openStream();
                     FileOutputStream out = new FileOutputStream(newJar)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }

                System.out.println("Update heruntergeladen. Starte Neustart-Skript...");

                // Erstellt ein kleines Skript, das die alte JAR ersetzt und die neue startet
                applyUpdateAndRestart(currentJar, newJar);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void applyUpdateAndRestart(File currentJar, File newJar) {
        try {
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("win")) {
                // Windows Batch Skript: Wartet 2 Sek., überschreibt die JAR und startet die App neu
                File script = new File(currentJar.getParent(), "update.bat");
                try (PrintWriter writer = new PrintWriter(script)) {
                    writer.println("@echo off");
                    writer.println("timeout /t 2 /nobreak > nul");
                    writer.println("move /y \"" + newJar.getAbsolutePath() + "\" \"" + currentJar.getAbsolutePath() + "\"");
                    writer.println("start java -jar \"" + currentJar.getAbsolutePath() + "\"");
                    writer.println("del \"%~f0\"");
                }
                Runtime.getRuntime().exec("cmd /c start " + script.getAbsolutePath());
            } else {
                // Linux / macOS Shell Skript
                File script = new File(currentJar.getParent(), "update.sh");
                try (PrintWriter writer = new PrintWriter(script)) {
                    writer.println("#!/bin/sh");
                    writer.println("sleep 2");
                    writer.println("mv -f \"" + newJar.getAbsolutePath() + "\" \"" + currentJar.getAbsolutePath() + "\"");
                    writer.println("java -jar \"" + currentJar.getAbsolutePath() + "\" &");
                    writer.println("rm -- \"$0\"");
                }
                script.setExecutable(true);
                Runtime.getRuntime().exec(new String[]{"/bin/sh", script.getAbsolutePath()});
            }

            // Beendet die laufende Instanz sauber
            Platform.exit();
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Hilfsmethode, um einfache JSON-Werte ohne externe Libs auszulesen
    private static String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int start = json.indexOf(searchKey);
        if (start == -1) return null;
        start += searchKey.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }
}