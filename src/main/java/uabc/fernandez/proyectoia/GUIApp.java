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

public class GUIApp extends Application {

    private Stage primaryStage;
    private AgenteGUI agente;
    private jade.wrapper.AgentContainer mainContainer;

    private final String[] nombresAgentes = {
            "Agente 1 (Hamming)",
            "Agente 2 (Manhattan)",
            "Agente 3 (Manhattan + Conflicto Lineal)"
    };

    private Label lblGanador;
    private TableView<MetricaAgente> tablaMetrics;
    private ObservableList<MetricaAgente> datosMetricas;

    private GridPane gridTableroMenu;
    private final String RUTA_SONIDO = "click.wav";

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.primaryStage.setTitle("8-Puzzle - Dashboard Principal (Agente GUI)");

        iniciarJADE();

        mostrarPantallaMenu();
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    private void iniciarJADE() {
        try {
            jade.core.Runtime rt = jade.core.Runtime.instance();
            jade.core.Profile profile = new jade.core.ProfileImpl();

            profile.setParameter(jade.core.Profile.MAIN_HOST, "10.21.59.160");
            profile.setParameter(jade.core.Profile.LOCAL_HOST, "10.21.59.160");
            profile.setParameter(jade.core.Profile.MAIN_PORT, "1099");
            profile.setParameter(jade.core.Profile.GUI, "true");

            mainContainer = rt.createMainContainer(profile);

            if (mainContainer != null) {
                Object[] args = new Object[]{ this };

                jade.wrapper.AgentController agentController = mainContainer.createNewAgent(
                        "agenteGUI",
                        "uabc.fernandez.proyectoia.AgenteGUI",
                        args
                );
                agentController.start();
                System.out.println("Servidor JADE iniciado y AgenteGUI registrado.");
            }
        } catch (Exception e) {
            System.err.println("Error iniciando JADE: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void stop() throws Exception {
        if (mainContainer != null) {
            mainContainer.kill();
        }
        super.stop();
    }

    // =========================================================================
    // 1. PANTALLA MENÚ
    // =========================================================================
    public void mostrarPantallaMenu() {
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

        BotonFlotante btnGenerar = new BotonFlotante("Generar nuevo tablero", "#3F51B5", RUTA_SONIDO);
        BotonFlotante btnComenzar = new BotonFlotante("Comenzar juego", "#2E7D32", RUTA_SONIDO);
        BotonFlotante btnSalir = new BotonFlotante("Salir", "#C62828", RUTA_SONIDO);

        btnGenerar.setMinWidth(380);
        btnComenzar.setMinWidth(380);
        btnSalir.setMinWidth(380);

        btnGenerar.setOnAction(e -> {
            try {
                agente.pedirTableroAGenerador();
            } catch (Exception ex) {
                agente.setEstadoTableroInicial(agente.generarEstadoAleatorio());
                actualizarTableroInicialEnUI(agente.getEstadoTableroInicial());
            }
        });

        // Al comenzar, le enviamos la orden por JADE a tus compañeros y pasamos a tu Dashboard
        btnComenzar.setOnAction(e -> {
            agente.iniciarJuegoConJugadores(); // Envía "RESOLVER:tablero" por JADE
            mostrarDashboardResultados();      // Muestra tu tabla de monitoreo
        });

        btnSalir.setOnAction(e -> primaryStage.close());

        root.getChildren().addAll(lblTitulo, boxTablero, btnGenerar, btnComenzar, btnSalir);
        primaryStage.setScene(new Scene(root, 1920, 1080));
    }

    public void actualizarTableroInicialEnUI(String nuevoTablero) {
        agente.setEstadoTableroInicial(nuevoTablero);
        if (gridTableroMenu != null) {
            gridTableroMenu.getChildren().clear();
            actualizarGridTablero(gridTableroMenu, nuevoTablero, 90);
        }
    }

    // =========================================================================
    // 2. DASHBOARD PRINCIPAL (Monitoreo para el Host)
    // =========================================================================
    public void mostrarDashboardResultados() {
        VBox root = new VBox(35);
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: #F4F4F9; -fx-padding: 50;");

        Label lblTitulo = new Label("Dashboard de Monitoreo Multiagente");
        lblTitulo.setFont(Font.font("Arial", FontWeight.BOLD, 36));

        lblGanador = new Label("⏳ Esperando que los agentes resuelvan el tablero...");
        lblGanador.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        lblGanador.setTextFill(Color.web("#1565C0"));
        lblGanador.setStyle("-fx-background-color: #E3F2FD; -fx-border-color: #1565C0; -fx-border-width: 2; -fx-padding: 15 30 15 30; -fx-border-radius: 8; -fx-background-radius: 8;");

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

        // Inicia en estado "Resolviendo..."
        datosMetricas = FXCollections.observableArrayList(
                new MetricaAgente(nombresAgentes[0], "Procesando...", "En espera"),
                new MetricaAgente(nombresAgentes[1], "Procesando...", "En espera"),
                new MetricaAgente(nombresAgentes[2], "Procesando...", "En espera")
        );
        tablaMetrics.setItems(datosMetricas);

        BotonFlotante btnNuevoJuego = new BotonFlotante("Comenzar juego nuevo", "#3F51B5", RUTA_SONIDO);
        BotonFlotante btnSalirDashboard = new BotonFlotante("Salir / Cerrar Agente", "#C62828", RUTA_SONIDO);

        btnNuevoJuego.setMinWidth(360);
        btnSalirDashboard.setMinWidth(360);

        btnNuevoJuego.setOnAction(e -> mostrarPantallaMenu());
        btnSalirDashboard.setOnAction(e -> primaryStage.close());

        HBox panelBotones = new HBox(30);
        panelBotones.setAlignment(Pos.CENTER);
        panelBotones.getChildren().addAll(btnNuevoJuego, btnSalirDashboard);

        root.getChildren().addAll(lblTitulo, lblGanador, tablaMetrics, panelBotones);
        primaryStage.setScene(new Scene(root, 1920, 1080));
    }

    public void actualizarResultadoJugador(int indexAgente, int pasos, int nodos, long tiempoMs) {
        if (datosMetricas != null && indexAgente >= 0 && indexAgente < datosMetricas.size()) {
            datosMetricas.set(indexAgente, new MetricaAgente(
                    nombresAgentes[indexAgente],
                    pasos + " pasos (" + tiempoMs + " ms, " + nodos + " nodos)",
                    "Resuelto"
            ));
            evaluarGanador();
        }
    }

    private void evaluarGanador() {
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
            lblGanador.setTextFill(Color.web("#2E7D32"));
            lblGanador.setStyle("-fx-background-color: #E8F5E9; -fx-border-color: #2E7D32; -fx-border-width: 2; -fx-padding: 15 30 15 30; -fx-border-radius: 8; -fx-background-radius: 8;");
        }
    }

    private GridPane crearGridTablero(String estado, double tileSize) {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(8);
        grid.setVgap(8);
        actualizarGridTablero(grid, estado, tileSize);
        return grid;
    }

    public void setAgente(AgenteGUI agente) {
        this.agente = agente;
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