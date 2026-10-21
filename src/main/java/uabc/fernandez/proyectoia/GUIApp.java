package uabc.fernandez.proyectoia;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class GUIApp extends Application {

    private Stage primaryStage;
    private AgenteGUI agente;

    private final List<Stage> ventanasJugadores = new ArrayList<>();

    private final String[] nombresAgentes = {
            "Agente 1 (Hamming)",
            "Agente 2 (Manhattan)",
            "Agente 3 (Manhattan + Conflicto Lineal)"
    };

    private Label lblGanador;
    private TableView<MetricaAgente> tablaMetrics;
    private ObservableList<MetricaAgente> datosMetricas;

    private GridPane gridTableroMenu;

    // Ruta por defecto para los efectos de sonido de BotonFlotante
    private final String RUTA_SONIDO = "click.wav";

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.primaryStage.setTitle("8-Puzzle - Dashboard Principal (Agente GUI)");

        this.agente = new AgenteGUI();
        this.agente.setGuiApp(this);

        mostrarPantallaMenu();
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    // =========================================================================
    // 1. PANTALLA MENÚ (Con BotonFlotante)
    // =========================================================================
    public void mostrarPantallaMenu() {
        cerrarVentanasJugadores();

        VBox root = new VBox(25);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #F4F4F9; -fx-padding: 40;");

        Label lblTitulo = new Label("Pantalla Menu - Selección de Tablero");
        lblTitulo.setFont(Font.font("Arial", FontWeight.BOLD, 32));

        VBox boxTablero = new VBox(15);
        boxTablero.setAlignment(Pos.CENTER);
        boxTablero.setStyle("-fx-border-color: #777777; -fx-border-width: 2; -fx-padding: 20; -fx-background-color: #FFFFFF; -fx-border-radius: 8; -fx-background-radius: 8;");
        boxTablero.setMaxSize(360, 380);

        Label lblTableroTitulo = new Label("Tablero Inicial Seleccionado");
        lblTableroTitulo.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        gridTableroMenu = crearGridTablero(agente.getEstadoTableroInicial(), 90);
        boxTablero.getChildren().addAll(lblTableroTitulo, gridTableroMenu);

        // --- Botones Flotantes del Menú ---
        BotonFlotante btnGenerar = new BotonFlotante("Generar nuevo tablero", "#3F51B5", RUTA_SONIDO);
        BotonFlotante btnComenzar = new BotonFlotante("Comenzar juego", "#2E7D32", RUTA_SONIDO);
        BotonFlotante btnSalir = new BotonFlotante("Salir", "#C62828", RUTA_SONIDO);

        btnGenerar.setMinWidth(380);
        btnComenzar.setMinWidth(380);
        btnSalir.setMinWidth(380);

        // Evento Generar: Pide el tablero al AgenteGenerador (Pasos)
        btnGenerar.setOnAction(e -> {
            try {
                agente.pedirTableroAGenerador();
            } catch (Exception ex) {
                // Fallback local si el contenedor de JADE no está en red aún
                agente.setEstadoTableroInicial(agente.generarEstadoAleatorio());
                actualizarTableroInicialEnUI(agente.getEstadoTableroInicial());
            }
        });

        // Evento Comenzar: Inicia la resolución y despliega los paneles
        btnComenzar.setOnAction(e -> {
            agente.iniciarJuegoConJugadores();
            mostrarDashboardResultados();
            desplegarVentanasJugadores();
        });

        btnSalir.setOnAction(e -> primaryStage.close());

        root.getChildren().addAll(lblTitulo, boxTablero, btnGenerar, btnComenzar, btnSalir);
        primaryStage.setScene(new Scene(root, 1920, 1080));
    }

    /**
     * Actualiza el tablero en la pantalla Menú cuando llega la respuesta de JADE.
     */
    public void actualizarTableroInicialEnUI(String nuevoTablero) {
        agente.setEstadoTableroInicial(nuevoTablero);
        if (gridTableroMenu != null) {
            gridTableroMenu.getChildren().clear();
            actualizarGridTablero(gridTableroMenu, nuevoTablero, 90);
        }
    }

    // =========================================================================
    // 2. DASHBOARD PRINCIPAL
    // =========================================================================
    public void mostrarDashboardResultados() {
        VBox root = new VBox(35);
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: #F4F4F9; -fx-padding: 50;");

        Label lblTitulo = new Label("Dashboard de Monitoreo Multiagente");
        lblTitulo.setFont(Font.font("Arial", FontWeight.BOLD, 36));

        // Banner del Agente Ganador
        lblGanador = new Label("🏆 ¡GANADOR: Agente 2 (Manhattan) con 8 iteraciones!");
        lblGanador.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        lblGanador.setTextFill(Color.web("#2E7D32"));
        lblGanador.setStyle("-fx-background-color: #E8F5E9; -fx-border-color: #2E7D32; -fx-border-width: 2; -fx-padding: 15 30 15 30; -fx-border-radius: 8; -fx-background-radius: 8;");

        // Tabla Resumen
        tablaMetrics = new TableView<>();
        tablaMetrics.setStyle("-fx-font-size: 18px;");
        tablaMetrics.setMaxWidth(950);

        double alturaFila = 50.0;
        double alturaEncabezado = 45.0;
        tablaMetrics.setFixedCellSize(alturaFila);
        tablaMetrics.setPrefHeight((alturaFila * 3) + alturaEncabezado + 4);

        TableColumn<MetricaAgente, String> colAgente = new TableColumn<>("Agente / Algoritmo");
        colAgente.setCellValueFactory(new PropertyValueFactory<>("agente"));

        TableColumn<MetricaAgente, String> colIter = new TableColumn<>("Iteraciones / Pasos");
        colIter.setCellValueFactory(new PropertyValueFactory<>("iteraciones"));

        TableColumn<MetricaAgente, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("posicion"));

        tablaMetrics.getColumns().addAll(colAgente, colIter, colEstado);
        tablaMetrics.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        datosMetricas = FXCollections.observableArrayList(
                new MetricaAgente(nombresAgentes[0], "14 pasos", "Resuelto"),
                new MetricaAgente(nombresAgentes[1], "8 pasos", "Resuelto (Ganador)"),
                new MetricaAgente(nombresAgentes[2], "10 pasos", "Resuelto")
        );
        tablaMetrics.setItems(datosMetricas);

        // --- Botones Flotantes del Dashboard ---
        BotonFlotante btnNuevoJuego = new BotonFlotante("Comenzar juego nuevo", "#3F51B5", RUTA_SONIDO);
        BotonFlotante btnSalirDashboard = new BotonFlotante("Salir / Cerrar Agente", "#C62828", RUTA_SONIDO);

        btnNuevoJuego.setMinWidth(360);
        btnSalirDashboard.setMinWidth(360);

        btnNuevoJuego.setOnAction(e -> mostrarPantallaMenu());
        btnSalirDashboard.setOnAction(e -> {
            cerrarVentanasJugadores();
            primaryStage.close();
        });

        HBox panelBotones = new HBox(30);
        panelBotones.setAlignment(Pos.CENTER);
        panelBotones.getChildren().addAll(btnNuevoJuego, btnSalirDashboard);

        root.getChildren().addAll(lblTitulo, lblGanador, tablaMetrics, panelBotones);
        primaryStage.setScene(new Scene(root, 1920, 1080));
    }

    /**
     * Actualiza la tabla de métricas en tiempo real al recibir la respuesta de un agente.
     */
    public void actualizarResultadoJugador(int indexAgente, int pasos, int nodos, long tiempoMs) {
        if (datosMetricas != null && indexAgente >= 0 && indexAgente < datosMetricas.size()) {
            datosMetricas.set(indexAgente, new MetricaAgente(
                    nombresAgentes[indexAgente],
                    pasos + " pasos (" + tiempoMs + " ms)",
                    "Resuelto"
            ));
            evaluarGanador();
        }
    }

    private void evaluarGanador() {
        // Lógica para resaltar el menor número de pasos en el banner verde
        int minPasos = Integer.MAX_VALUE;
        int idxGanador = -1;

        for (int i = 0; i < datosMetricas.size(); i++) {
            String iterText = datosMetricas.get(i).getIteraciones();
            if (iterText.contains("pasos")) {
                try {
                    int val = Integer.parseInt(iterText.split(" ")[0]);
                    if (val < minPasos) {
                        minPasos = val;
                        idxGanador = i;
                    }
                } catch (Exception ignored) {}
            }
        }

        if (idxGanador != -1) {
            lblGanador.setText("🏆 ¡GANADOR: " + nombresAgentes[idxGanador] + " con " + minPasos + " iteraciones!");
        }
    }

    // =========================================================================
    // 3. VENTANAS INDIVIDUALES DE AGENTES
    // =========================================================================
    private void desplegarVentanasJugadores() {
        cerrarVentanasJugadores();

        int[] totalPasosAgentes = {14, 8, 10};

        for (int i = 0; i < 3; i++) {
            final int idxAgente = i;
            Stage playerStage = new Stage();
            playerStage.setTitle("Agente Jugador - " + nombresAgentes[idxAgente]);

            List<String> historialIteraciones = generarSecuenciaEjemplo(
                    agente.getEstadoTableroInicial(),
                    totalPasosAgentes[idxAgente]
            );

            final int[] iteracionActual = {0};

            VBox root = new VBox(20);
            root.setAlignment(Pos.CENTER);
            root.setStyle("-fx-background-color: #FAFAFA; -fx-padding: 25;");

            Label lblNombre = new Label(nombresAgentes[idxAgente]);
            lblNombre.setFont(Font.font("Arial", FontWeight.BOLD, 22));
            lblNombre.setTextFill(Color.web("#1A237E"));

            Label lblIteracion = new Label("Iteración: 1 de " + historialIteraciones.size());
            lblIteracion.setFont(Font.font("Arial", FontWeight.BOLD, 18));
            lblIteracion.setTextFill(Color.web("#424242"));

            GridPane gridJugador = crearGridTablero(historialIteraciones.get(0), 110);

            BotonFlotante btnAnterior = new BotonFlotante("◄ Regresar", "#3F51B5", RUTA_SONIDO);
            BotonFlotante btnSiguiente = new BotonFlotante("Siguiente ►", "#3F51B5", RUTA_SONIDO);

            btnAnterior.setMinWidth(220);
            btnAnterior.setMinHeight(60);
            btnSiguiente.setMinWidth(220);
            btnSiguiente.setMinHeight(60);

            btnAnterior.setOnAction(e -> {
                if (iteracionActual[0] > 0) {
                    iteracionActual[0]--;
                    lblIteracion.setText("Iteración: " + (iteracionActual[0] + 1) + " de " + historialIteraciones.size());
                    gridJugador.getChildren().clear();
                    actualizarGridTablero(gridJugador, historialIteraciones.get(iteracionActual[0]), 110);
                }
            });

            btnSiguiente.setOnAction(e -> {
                if (iteracionActual[0] < historialIteraciones.size() - 1) {
                    iteracionActual[0]++;
                    lblIteracion.setText("Iteración: " + (iteracionActual[0] + 1) + " de " + historialIteraciones.size());
                    gridJugador.getChildren().clear();
                    actualizarGridTablero(gridJugador, historialIteraciones.get(iteracionActual[0]), 110);
                }
            });

            HBox panelNavegacion = new HBox(20);
            panelNavegacion.setAlignment(Pos.CENTER);
            panelNavegacion.getChildren().addAll(btnAnterior, btnSiguiente);

            root.getChildren().addAll(lblNombre, lblIteracion, gridJugador, panelNavegacion);

            Scene scene = new Scene(root, 650, 720);
            playerStage.setScene(scene);

            playerStage.setX(60 + (i * 620));
            playerStage.setY(150);

            playerStage.show();
            ventanasJugadores.add(playerStage);
        }
    }

    private void cerrarVentanasJugadores() {
        for (Stage stage : ventanasJugadores) {
            stage.close();
        }
        ventanasJugadores.clear();
    }

    private List<String> generarSecuenciaEjemplo(String estadoInicial, int totalPasos) {
        List<String> secuencia = new ArrayList<>();
        secuencia.add(estadoInicial);

        String actual = estadoInicial;
        for (int step = 0; step < totalPasos; step++) {
            actual = hacerMovimientoAleatorioSimulado(actual);
            secuencia.add(actual);
        }
        return secuencia;
    }

    private String hacerMovimientoAleatorioSimulado(String estado) {
        char[] array = estado.toCharArray();
        int zeroIdx = estado.indexOf('0');
        int row = zeroIdx / 3;
        int col = zeroIdx % 3;

        List<Integer> vecinos = new ArrayList<>();
        if (row > 0) vecinos.add(zeroIdx - 3);
        if (row < 2) vecinos.add(zeroIdx + 3);
        if (col > 0) vecinos.add(zeroIdx - 1);
        if (col < 2) vecinos.add(zeroIdx + 1);

        int targetIdx = vecinos.get((int) (Math.random() * vecinos.size()));
        array[zeroIdx] = array[targetIdx];
        array[targetIdx] = '0';

        return new String(array);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES
    // =========================================================================
    private GridPane crearGridTablero(String estado, double tileSize) {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(8);
        grid.setVgap(8);
        actualizarGridTablero(grid, estado, tileSize);
        return grid;
    }

    private void actualizarGridTablero(GridPane grid, String estado, double tileSize) {
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

    public static void main(String[] args) {
        launch(args);
    }
}