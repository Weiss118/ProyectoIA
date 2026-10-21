package uabc.fernandez.proyectoia;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.util.List;

/**
 * Clase que cada compañero usará en su proyecto para desplegar
 * visualmente el recorrido del 8-Puzzle en su propia pantalla.
 */
public class VentanaJugador {

    public static void mostrar(String nombreAgente, List<String> historialIteraciones) {
        // Platform.runLater asegura que se ejecute en el hilo gráfico de JavaFX
        Platform.runLater(() -> {
            Stage stage = new Stage();
            stage.setTitle("Agente Jugador - " + nombreAgente);

            final int[] iteracionActual = {0};

            VBox root = new VBox(20);
            root.setAlignment(Pos.CENTER);
            root.setStyle("-fx-background-color: #FAFAFA; -fx-padding: 25;");

            Label lblNombre = new Label(nombreAgente);
            lblNombre.setFont(Font.font("Arial", FontWeight.BOLD, 22));
            lblNombre.setTextFill(Color.web("#1A237E"));

            Label lblIteracion = new Label("Iteración: 1 de " + historialIteraciones.size());
            lblIteracion.setFont(Font.font("Arial", FontWeight.BOLD, 18));
            lblIteracion.setTextFill(Color.web("#424242"));

            GridPane grid = crearGridTablero(historialIteraciones.get(0), 110);

            // Botones Flotantes integrados para la navegación
            BotonFlotante btnRegresar = new BotonFlotante("◄ Regresar", "#3F51B5", "click.wav");
            BotonFlotante btnSiguiente = new BotonFlotante("Siguiente ►", "#3F51B5", "click.wav");

            btnRegresar.setMinWidth(200);
            btnRegresar.setMinHeight(55);
            btnSiguiente.setMinWidth(200);
            btnSiguiente.setMinHeight(55);

            btnRegresar.setOnAction(e -> {
                if (iteracionActual[0] > 0) {
                    iteracionActual[0]--;
                    lblIteracion.setText("Iteración: " + (iteracionActual[0] + 1) + " de " + historialIteraciones.size());
                    actualizarGridTablero(grid, historialIteraciones.get(iteracionActual[0]), 110);
                }
            });

            btnSiguiente.setOnAction(e -> {
                if (iteracionActual[0] < historialIteraciones.size() - 1) {
                    iteracionActual[0]++;
                    lblIteracion.setText("Iteración: " + (iteracionActual[0] + 1) + " de " + historialIteraciones.size());
                    actualizarGridTablero(grid, historialIteraciones.get(iteracionActual[0]), 110);
                }
            });

            HBox panelNavegacion = new HBox(20, btnRegresar, btnSiguiente);
            panelNavegacion.setAlignment(Pos.CENTER);

            root.getChildren().addAll(lblNombre, lblIteracion, grid, panelNavegacion);

            stage.setScene(new Scene(root, 600, 700));
            stage.show();
        });
    }

    private static GridPane crearGridTablero(String estado, double tileSize) {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(8);
        grid.setVgap(8);
        actualizarGridTablero(grid, estado, tileSize);
        return grid;
    }

    private static void actualizarGridTablero(GridPane grid, String estado, double tileSize) {
        grid.getChildren().clear();
        int index = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                char val = estado.charAt(index++);
                StackPane tile = new StackPane();
                tile.setPrefSize(tileSize, tileSize);

                if (val == '0') {
                    tile.setStyle("-fx-background-color: #E0E0E0; -fx-border-color: #CCCCCC; -fx-border-radius: 5; -fx-background-radius: 5;");
                } else {
                    tile.setStyle("-fx-background-color: #3F51B5; -fx-border-color: #1A237E; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5;");
                    Label lblVal = new Label(String.valueOf(val));
                    lblVal.setFont(Font.font("Arial", FontWeight.BOLD, tileSize * 0.42));
                    lblVal.setTextFill(Color.WHITE);
                    tile.getChildren().add(lblVal);
                }
                grid.add(tile, col, row);
            }
        }
    }
}