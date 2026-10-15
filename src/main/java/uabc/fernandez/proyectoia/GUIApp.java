package uabc.fernandez.proyectoia;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

public class GUIApp extends Application {

    private Stage primaryStage;
    private AgenteGUI agente;
    private AgentContainer mainContainer;

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
        this.primaryStage.setTitle("8-Puzzle - Agente GUI (JADE + JavaFX)");

        // Instancia del Agente GUI
        this.agente = new AgenteGUI();
        this.agente.setGuiApp(this);

        // Iniciar entorno JADE si está disponible
        iniciarJADE();

        // Iniciar en Pantalla Menú
        mostrarPantallaMenu();

        // Abrir en pantalla completa / maximizada (1920x1080)
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    private void iniciarJADE() {
        try {
            Runtime rt = Runtime.instance();
            Profile profile = new ProfileImpl();
            profile.setParameter(Profile.MAIN_HOST, "localhost");
            profile.setParameter(Profile.GUI, "true");

            mainContainer = rt.createMainContainer(profile);
            AgentController agentController = mainContainer.createNewAgent("agenteGUI", "uabc.fernandez.proyectoia.AgenteGUI", null);
            agentController.start();
            System.out.println("Entorno JADE iniciado correctamente.");
        } catch (Exception e) {
            System.err.println("Aviso JADE: Ejecutando en modo solo GUI local (" + e.getMessage() + ")");
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

        Label lblTitulo = new Label("Pantalla Menu");
        lblTitulo.setFont(Font.font("Arial", FontWeight.BOLD, 36));

        // Cuadro Tablero Inicial (3x3)
        VBox boxTablero = new VBox(15);
        boxTablero.setAlignment(Pos.CENTER);
        boxTablero.setStyle("-fx-border-color: #777777; -fx-border-width: 2; -fx-padding: 20; -fx-background-color: #FFFFFF; -fx-border-radius: 5; -fx-background-radius: 5;");
        boxTablero.setMaxSize(360, 380);

        Label lblTableroTitulo = new Label("Tablero inicial");
        lblTableroTitulo.setFont(Font.font("Arial", FontWeight.BOLD, 20));

        GridPane gridTablero = crearGridTablero(agente.getEstadoTableroInicial(), 90);
        boxTablero.getChildren().addAll(lblTableroTitulo, gridTablero);

        // Estilo provisional grande para los botones
        String estiloBotonMenu = "-fx-font-size: 18px; -fx-font-weight: bold; -fx-min-width: 280px; -fx-min-height: 55px; -fx-cursor: hand;";

        Button btnGenerar = new Button("Generar nuevo tablero");
        Button btnComenzar = new Button("Comenzar juego");
        Button btnSalir = new Button("Salir");

        btnGenerar.setStyle(estiloBotonMenu);
        btnComenzar.setStyle(estiloBotonMenu);
        btnSalir.setStyle(estiloBotonMenu);

        // Eventos
        btnGenerar.setOnAction(e -> {
            agente.setEstadoTableroInicial(agente.generarEstadoAleatorio());
            gridTablero.getChildren().clear();
            actualizarGridTablero(gridTablero, agente.getEstadoTableroInicial(), 90);
        });

        btnComenzar.setOnAction(e -> mostrarPantallaJuego());
        btnSalir.setOnAction(e -> primaryStage.close());

        root.getChildren().addAll(lblTitulo, boxTablero, btnGenerar, btnComenzar, btnSalir);

        Scene scene = new Scene(root, 1920, 1080);
        primaryStage.setScene(scene);
    }

    // =========================================================================
    // 2. PANTALLA JUEGO
    // =========================================================================
    public void mostrarPantallaJuego() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #F4F4F9; -fx-padding: 25;");

        // --- ENCABEZADO ---
        Label lblTitulo = new Label("Pantalla Juego");
        lblTitulo.setFont(Font.font("Arial", FontWeight.BOLD, 34));
        BorderPane.setAlignment(lblTitulo, Pos.CENTER);
        BorderPane.setMargin(lblTitulo, new Insets(0, 0, 20, 0));
        root.setTop(lblTitulo);

        // --- PANEL IZQUIERDO (Tablero Inicial + Tabla Resumen) ---
        VBox panelIzquierdo = new VBox(25);
        panelIzquierdo.setAlignment(Pos.TOP_CENTER);
        panelIzquierdo.setPrefWidth(500);

        // Box Tablero Inicial
        contenedorTableroInicialJuego = new VBox(12);
        contenedorTableroInicialJuego.setAlignment(Pos.CENTER);
        contenedorTableroInicialJuego.setStyle("-fx-border-color: #777777; -fx-border-width: 2; -fx-padding: 20; -fx-background-color: #FFFFFF; -fx-border-radius: 5; -fx-background-radius: 5;");
        contenedorTableroInicialJuego.setMaxWidth(480);

        Label lblTabInicial = new Label("Tablero inicial");
        lblTabInicial.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        GridPane gridInicial = crearGridTablero(agente.getEstadoTableroInicial(), 80);
        contenedorTableroInicialJuego.getChildren().addAll(lblTabInicial, gridInicial);

        // --- TABLA RESUMEN ---
        TableView<MetricaAgente> tablaMetrics = new TableView<>();
        tablaMetrics.setStyle("-fx-font-size: 16px;");

        // 1. Fijamos la altura de cada fila de datos (en píxeles)
        double alturaFila = 42.0;
        double alturaEncabezado = 38.0;
        tablaMetrics.setFixedCellSize(alturaFila);

        // 2. Calculamos el alto exacto para 3 agentes + 1 encabezado (4 filas visuales en total)
        double altoExacto = (alturaFila * 3) + alturaEncabezado + 4;
        tablaMetrics.setPrefHeight(altoExacto);
        tablaMetrics.setMinHeight(altoExacto);
        tablaMetrics.setMaxHeight(altoExacto);

        // Columnas
        TableColumn<MetricaAgente, String> colAgente = new TableColumn<>("Agente");
        colAgente.setCellValueFactory(new PropertyValueFactory<>("agente"));

        TableColumn<MetricaAgente, String> colIter = new TableColumn<>("Iteraciones");
        colIter.setCellValueFactory(new PropertyValueFactory<>("iteraciones"));

        TableColumn<MetricaAgente, String> colPos = new TableColumn<>("Posicion");
        colPos.setCellValueFactory(new PropertyValueFactory<>("posicion"));

        tablaMetrics.getColumns().addAll(colAgente, colIter, colPos);
        tablaMetrics.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Datos mock
        ObservableList<MetricaAgente> datos = FXCollections.observableArrayList(
                new MetricaAgente("Agente 1", "-", "-"),
                new MetricaAgente("Agente 2", "-", "-"),
                new MetricaAgente("Agente 3", "-", "-")
        );
        tablaMetrics.setItems(datos);

        panelIzquierdo.getChildren().addAll(contenedorTableroInicialJuego, tablaMetrics);
        root.setLeft(panelIzquierdo);

        // --- PANEL CENTRAL (Tablero del Jugador + Navegación) ---
        HBox panelCentralNavegacion = new HBox(25);
        panelCentralNavegacion.setAlignment(Pos.CENTER);

        String estiloBotonNavegacion = "-fx-font-size: 16px; -fx-font-weight: bold; -fx-min-width: 180px; -fx-min-height: 65px; -fx-cursor: hand;";

        Button btnAnterior = new Button("◄ Jug. anterior");
        Button btnSiguiente = new Button("Sig. jugador ►");

        btnAnterior.setStyle(estiloBotonNavegacion);
        btnSiguiente.setStyle(estiloBotonNavegacion);

        lblNombreJugador = new Label(nombresAgentes[agente.getJugadorActual()]);
        lblNombreJugador.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        lblNombreJugador.setTextFill(Color.web("#1A237E"));

        // Contenedor principal con tamaño fijo proporcional (Evita el rectángulo estirado)
        contenedorTableroJugador = new VBox(20);
        contenedorTableroJugador.setAlignment(Pos.CENTER);
        contenedorTableroJugador.setStyle("-fx-border-color: #333333; -fx-border-width: 3; -fx-padding: 25; -fx-background-color: #FFFFFF; -fx-border-radius: 8; -fx-background-radius: 8;");
        contenedorTableroJugador.setPrefSize(550, 600);
        contenedorTableroJugador.setMaxSize(550, 600);

        GridPane gridJugador = crearGridTablero(agente.getEstadosJugadores()[agente.getJugadorActual()], 135);
        contenedorTableroJugador.getChildren().addAll(lblNombreJugador, gridJugador);

        // Acciones de navegación
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

        // --- PANEL INFERIOR (Botones de Iteración y Control Grandes) ---
        HBox panelInferior = new HBox(25);
        panelInferior.setAlignment(Pos.CENTER);
        BorderPane.setMargin(panelInferior, new Insets(20, 0, 10, 0));

        String estiloBotonInferior = "-fx-font-size: 16px; -fx-font-weight: bold; -fx-min-width: 240px; -fx-min-height: 60px; -fx-cursor: hand;";
        String estiloBotonSalir = "-fx-font-size: 16px; -fx-font-weight: bold; -fx-min-width: 180px; -fx-min-height: 60px; -fx-cursor: hand;";

        Button btnSigIter = new Button("Siguiente iteración");
        Button btnRegIter = new Button("Regresar iteración");
        Button btnComenzarNuevo = new Button("Comenzar juego nuevo");
        Button btnSalirJuego = new Button("Salir");

        btnSigIter.setStyle(estiloBotonInferior);
        btnRegIter.setStyle(estiloBotonInferior);
        btnComenzarNuevo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-min-width: 260px; -fx-min-height: 60px; -fx-cursor: hand;");
        btnSalirJuego.setStyle(estiloBotonSalir);

        btnComenzarNuevo.setOnAction(e -> {
            agente.setEstadoTableroInicial(agente.generarEstadoAleatorio());
            mostrarPantallaMenu();
        });

        btnSalirJuego.setOnAction(e -> primaryStage.close());

        panelInferior.getChildren().addAll(btnSigIter, btnRegIter, btnComenzarNuevo, btnSalirJuego);
        root.setBottom(panelInferior);

        Scene scene = new Scene(root, 1920, 1080);
        primaryStage.setScene(scene);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES Y RENDERIZADO DEL TABLERO
    // =========================================================================
    private void actualizarTableroJugadorVisual() {
        lblNombreJugador.setText(nombresAgentes[agente.getJugadorActual()]);
        contenedorTableroJugador.getChildren().clear();
        GridPane grid = crearGridTablero(agente.getEstadosJugadores()[agente.getJugadorActual()], 135);
        contenedorTableroJugador.getChildren().addAll(lblNombreJugador, grid);
    }

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