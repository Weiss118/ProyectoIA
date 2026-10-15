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

    // Nombres e información de cada agente jugador
    private final String[] nombresAgentes = {
            "Agente 1 (Hamming)",
            "Agente 2 (Manhattan)",
            "Agente 3 (Manhattan + Conflicto Lineal)"
    };

    // Componentes interactivos de la pantalla de juego
    private VBox contenedorTableroInicialJuego;
    private VBox contenedorTableroJugador;
    private Label lblNombreJugador;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.primaryStage.setTitle("8-Puzzle");

        // Instancia mock del Agente GUI para la demo local
        this.agente = new AgenteGUI();
        this.agente.setGuiApp(this);

        // Iniciar en Pantalla Menú
        mostrarPantallaMenu();
        primaryStage.show();
    }

    // =========================================================================
    // 1. PANTALLA MENÚ
    // =========================================================================
    public void mostrarPantallaMenu() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #F4F4F9; -fx-padding: 30;");

        Label lblTitulo = new Label("Pantalla Menu");
        lblTitulo.setFont(Font.font("Arial", FontWeight.BOLD, 26));

        // Cuadro Tablero Inicial (3x3)
        VBox boxTablero = new VBox(10);
        boxTablero.setAlignment(Pos.CENTER);
        boxTablero.setStyle("-fx-border-color: #777777; -fx-border-width: 2; -fx-padding: 15; -fx-background-color: #FFFFFF;");
        boxTablero.setMaxSize(260, 260);

        Label lblTableroTitulo = new Label("Tablero inicial");
        lblTableroTitulo.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        GridPane gridTablero = crearGridTablero(agente.getEstadoTableroInicial(), 60);
        boxTablero.getChildren().addAll(lblTableroTitulo, gridTablero);

        // Botones de acción
        Button btnGenerar = new Button("Generar nuevo tablero");
        Button btnComenzar = new Button("Comenzar juego");
        Button btnSalir = new Button("Salir");

        String estiloBoton = "-fx-min-width: 200px; -fx-min-height: 40px; -fx-font-size: 14px;";
        btnGenerar.setStyle(estiloBoton);
        btnComenzar.setStyle(estiloBoton);
        btnSalir.setStyle(estiloBoton);

        // Eventos
        btnGenerar.setOnAction(e -> {
            agente.setEstadoTableroInicial(agente.generarEstadoAleatorio());
            gridTablero.getChildren().clear();
            actualizarGridTablero(gridTablero, agente.getEstadoTableroInicial(), 60);
        });

        btnComenzar.setOnAction(e -> mostrarPantallaJuego());
        btnSalir.setOnAction(e -> primaryStage.close());

        root.getChildren().addAll(lblTitulo, boxTablero, btnGenerar, btnComenzar, btnSalir);

        Scene scene = new Scene(root, 900, 700);
        primaryStage.setScene(scene);
    }

    // =========================================================================
    // 2. PANTALLA JUEGO
    // =========================================================================
    public void mostrarPantallaJuego() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #F4F4F9; -fx-padding: 20;");

        // --- ENCABEZADO ---
        Label lblTitulo = new Label("Pantalla Juego");
        lblTitulo.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        BorderPane.setAlignment(lblTitulo, Pos.CENTER);
        root.setTop(lblTitulo);

        // --- PANEL IZQUIERDO (Tablero Inicial + Tabla Resumen) ---
        VBox panelIzquierdo = new VBox(20);
        panelIzquierdo.setAlignment(Pos.TOP_CENTER);
        panelIzquierdo.setPrefWidth(320);

        // Box Tablero Inicial
        contenedorTableroInicialJuego = new VBox(10);
        contenedorTableroInicialJuego.setAlignment(Pos.CENTER);
        contenedorTableroInicialJuego.setStyle("-fx-border-color: #777777; -fx-border-width: 2; -fx-padding: 10; -fx-background-color: #FFFFFF;");

        Label lblTabInicial = new Label("Tablero inicial");
        lblTabInicial.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        GridPane gridInicial = crearGridTablero(agente.getEstadoTableroInicial(), 45);
        contenedorTableroInicialJuego.getChildren().addAll(lblTabInicial, gridInicial);

        // Tabla Resumen
        TableView<MetricaAgente> tablaMetrics = new TableView<>();
        tablaMetrics.setPrefHeight(200);

        TableColumn<MetricaAgente, String> colAgente = new TableColumn<>("Agente");
        colAgente.setCellValueFactory(new PropertyValueFactory<>("agente"));

        TableColumn<MetricaAgente, String> colIter = new TableColumn<>("Iteraciones");
        colIter.setCellValueFactory(new PropertyValueFactory<>("iteraciones"));

        TableColumn<MetricaAgente, String> colPos = new TableColumn<>("Posicion");
        colPos.setCellValueFactory(new PropertyValueFactory<>("posicion"));

        tablaMetrics.getColumns().addAll(colAgente, colIter, colPos);
        tablaMetrics.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Datos mock para el avance
        ObservableList<MetricaAgente> datos = FXCollections.observableArrayList(
                new MetricaAgente("Agente 1", "-", "-"),
                new MetricaAgente("Agente 2", "-", "-"),
                new MetricaAgente("Agente 3", "-", "-")
        );
        tablaMetrics.setItems(datos);

        panelIzquierdo.getChildren().addAll(contenedorTableroInicialJuego, tablaMetrics);
        root.setLeft(panelIzquierdo);

        // --- PANEL CENTRAL (Tablero del Jugador + Navegación) ---
        HBox panelCentralNavegacion = new HBox(15);
        panelCentralNavegacion.setAlignment(Pos.CENTER);

        Button btnAnterior = new Button("Jug. anterior");
        Button btnSiguiente = new Button("Sig. jugador");

        lblNombreJugador = new Label(nombresAgentes[agente.getJugadorActual()]);
        lblNombreJugador.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        contenedorTableroJugador = new VBox(10);
        contenedorTableroJugador.setAlignment(Pos.CENTER);
        contenedorTableroJugador.setStyle("-fx-border-color: #333333; -fx-border-width: 2; -fx-padding: 20; -fx-background-color: #FFFFFF;");

        GridPane gridJugador = crearGridTablero(agente.getEstadosJugadores()[agente.getJugadorActual()], 80);
        contenedorTableroJugador.getChildren().addAll(lblNombreJugador, gridJugador);

        // Acciones de flechas/botones laterales
        btnAnterior.setOnAction(e -> {
            int idx = (agente.getJugadorActual() - 1 + 3) % 3;
            agente.setJugadorActual(idx);
            actualizarTableroJugadorVisual();
        });

        btnSiguiente.setOnAction(e -> {
            int idx = (agente.getJugadorActual() + 1) % 3;
            agente.setJugadorActual(idx);
            actualizarTableroJugadorVisual();
        });

        panelCentralNavegacion.getChildren().addAll(btnAnterior, contenedorTableroJugador, btnSiguiente);
        root.setCenter(panelCentralNavegacion);

        // --- PANEL INFERIOR (Botones de Iteración y Control) ---
        HBox panelInferior = new HBox(15);
        panelInferior.setAlignment(Pos.CENTER);
        panelInferior.setStyle("-fx-padding: 20 0 0 0;");

        Button btnSigIter = new Button("Siguiente iteracion");
        Button btnRegIter = new Button("Regresar iteracion");
        Button btnComenzarNuevo = new Button("Comenzar juego nuevo");
        Button btnSalirJuego = new Button("Salir");

        btnComenzarNuevo.setOnAction(e -> {
            // Genera nuevo tablero y regresa al menú
            agente.setEstadoTableroInicial(agente.generarEstadoAleatorio());
            mostrarPantallaMenu();
        });

        btnSalirJuego.setOnAction(e -> primaryStage.close());

        panelInferior.getChildren().addAll(btnSigIter, btnRegIter, btnComenzarNuevo, btnSalirJuego);
        root.setBottom(panelInferior);

        Scene scene = new Scene(root, 1100, 720);
        primaryStage.setScene(scene);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES Y RENDERIZADO DEL TABLERO
    // =========================================================================
    private void actualizarTableroJugadorVisual() {
        lblNombreJugador.setText(nombresAgentes[agente.getJugadorActual()]);
        contenedorTableroJugador.getChildren().clear();
        GridPane grid = crearGridTablero(agente.getEstadosJugadores()[agente.getJugadorActual()], 80);
        contenedorTableroJugador.getChildren().addAll(lblNombreJugador, grid);
    }

    private GridPane crearGridTablero(String estado, double tileSize) {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(5);
        grid.setVgap(5);
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
                    tile.setStyle("-fx-background-color: #E0E0E0; -fx-border-color: #CCCCCC;");
                } else {
                    tile.setStyle("-fx-background-color: #3F51B5; -fx-border-color: #1A237E; -fx-border-radius: 3; -fx-background-radius: 3;");
                    Label lblVal = new Label(String.valueOf(val));
                    lblVal.setFont(Font.font("Arial", FontWeight.BOLD, tileSize * 0.4));
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
