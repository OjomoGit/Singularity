package singularity.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import singularity.ui.utils.Windows11Blur;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.Stream;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Automatisch alle Fonts aus dem Ordner in den JavaFX Speicher laden, BEVOR die FXML geladen wird
        loadAllFonts();

        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/singularity/fxml/main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1400, 900);

        scene.getStylesheets().add(Objects.requireNonNull(Main.class.getResource("/singularity/css/styles.css")).toExternalForm());
        scene.setFill(Color.TRANSPARENT);

        stage.initStyle(StageStyle.UNDECORATED);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setResizable(true);

        String windowTitle = "Hub";
        stage.setTitle(windowTitle);
        stage.setScene(scene);

        stage.setOnCloseRequest(event -> {
            javafx.application.Platform.exit();
            System.exit(0);
        });

        try (InputStream iconStream = Main.class.getResourceAsStream("/singularity/images/logo.png")) {
            if (iconStream != null) {
                stage.getIcons().add(new Image(iconStream));
            }
        } catch (Exception e) {
            System.out.println("Konnte Fenster-Icon nicht laden: " + e.getMessage());
        }

        stage.show();

        Windows11Blur.applyAcrylic(windowTitle);
    }

    // Lädt automatisch alle Fonts (.ttf und .otf) aus dem Ordner in den Speicher
    // Lädt Fonts sicher aus der verpackten JAR / EXE
    private void loadAllFonts() {
        String[] fontFiles = {
                "Paceon.ttf",
                "Miracle History.ttf",
                "Minecraft.ttf"
                // Füge hier die genauen Dateinamen aller deiner Schriftdateien im Ordner /singularity/fonts/ ein
        };

        for (String fontFileName : fontFiles) {
            String path = "/singularity/fonts/" + fontFileName;
            try (InputStream is = Main.class.getResourceAsStream(path)) {
                if (is != null) {
                    Font loadedFont = Font.loadFont(is, 14);
                    if (loadedFont != null) {
                        System.out.println("Font geladen: " + fontFileName + " (" + loadedFont.getFamily() + ")");
                    } else {
                        System.err.println("Konnte Font nicht parsen: " + fontFileName);
                    }
                } else {
                    System.err.println("Font-Ressource nicht gefunden: " + path);
                }
            } catch (Exception e) {
                System.err.println("Fehler beim Laden von Font " + fontFileName + ": " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
