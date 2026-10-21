package uabc.fernandez.proyectoia;

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AgenteGUI extends Agente {

    private GUIApp guiApp;
    private String estadoTableroInicial;
    private String[] estadosJugadores;
    private int jugadorActual = 0;

    private final String NOMBRE_GENERADOR = "generador";
    private final String[] NOMBRES_JUGADORES = {"jugador1", "jugador2", "jugador3"};

    public AgenteGUI() {
        this.estadoTableroInicial = generarEstadoAleatorio();
        this.estadosJugadores = new String[]{
                generarEstadoAleatorio(),
                generarEstadoAleatorio(),
                generarEstadoAleatorio()
        };
    }

    @Override
    protected void setup() {
        super.setup();

        // Obtener la referencia de GUIApp pasada como argumento desde JADE
        Object[] args = getArguments();
        if (args != null && args.length > 0 && args[0] instanceof GUIApp) {
            this.guiApp = (GUIApp) args[0];
            this.guiApp.setAgente(this); // Vincula este agente de JADE con la UI de JavaFX
            System.out.println("[AgenteGUI] Vinculado exitosamente con la interfaz JavaFX.");
        }

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                percepcion();
                razonamiento();
                actuar();
                block(500);
            }
        });
    }

    public void setGuiApp(GUIApp app) {
        this.guiApp = app;
    }

    // =========================================================================
    // 1. ÁREA DE PERCEPCIÓN: Recepción de respuestas de Pasos y Jugadores
    // =========================================================================
    @Override
    public void percepcion() {
        MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
        ACLMessage msg = receive(mt);

        if (msg != null) {
            String contenido = msg.getContent();
            String emisor = msg.getSender().getLocalName();

            System.out.println("[AgenteGUI] Mensaje INFORM recibido de [" + emisor + "]: " + contenido);

            if (contenido != null) {
                // Caso A: El AgenteGenerador (Pasos) envía el tablero inicial (cadena de 9 dígitos)
                if (contenido.length() == 9 && contenido.matches("[0-8]+")) {
                    this.estadoTableroInicial = contenido;
                    if (guiApp != null) {
                        Platform.runLater(() -> guiApp.actualizarTableroInicialEnUI(contenido));
                    }
                }
                // Caso B: Un AgenteJugador envía sus métricas ("secuencia;longitud;nodos;tiempoMs")
                else if (contenido.contains(";")) {
                    procesarResultadoJugador(emisor, contenido);
                }
            }
        }
    }

    // =========================================================================
    // 2. PETICIONES A OTROS AGENTES
    // =========================================================================
    public void pedirTableroAGenerador() {
        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
        request.addReceiver(new AID(NOMBRE_GENERADOR, AID.ISLOCALNAME));
        request.setContent("GENERAR_RETO");
        send(request);
        System.out.println("[AgenteGUI] REQUEST enviado a [" + NOMBRE_GENERADOR + "]: GENERAR_RETO");
    }

    public void iniciarJuegoConJugadores() {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        for (String nombreJugador : NOMBRES_JUGADORES) {
            msg.addReceiver(new AID(nombreJugador, AID.ISLOCALNAME));
        }
        msg.setContent(estadoTableroInicial);
        send(msg);
        System.out.println("[AgenteGUI] RESOLVER enviado a jugadores con tablero: " + estadoTableroInicial);
    }

    // =========================================================================
    // 3. PROCESAMIENTO DE RESULTADOS Y MÉTRICAS
    // =========================================================================
    private void procesarResultadoJugador(String emisor, String contenido) {
        try {
            String[] partes = contenido.split(";");
            if (partes.length >= 4) {
                int longitud = Integer.parseInt(partes[1]);
                int nodos = Integer.parseInt(partes[2]);
                long tiempoMs = Long.parseLong(partes[3]);

                int indexAgente = obtenerIndiceAgente(emisor);

                if (indexAgente != -1 && guiApp != null) {
                    Platform.runLater(() -> {
                        guiApp.actualizarResultadoJugador(indexAgente, longitud, nodos, tiempoMs);
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("[AgenteGUI] Error procesando métricas del jugador [" + emisor + "]: " + e.getMessage());
        }
    }

    private int obtenerIndiceAgente(String emisor) {
        if (emisor.equalsIgnoreCase("jugador1") || emisor.equalsIgnoreCase("agente1")) return 0;
        if (emisor.equalsIgnoreCase("jugador2") || emisor.equalsIgnoreCase("agente2")) return 1;
        if (emisor.equalsIgnoreCase("jugador3") || emisor.equalsIgnoreCase("agente3")) return 2;
        return -1;
    }

    @Override public void razonamiento() { }
    @Override public void actuar() { }

    public String generarEstadoAleatorio() {
        List<Character> fichas = new ArrayList<>();
        for (char c = '0'; c <= '8'; c++) fichas.add(c);
        Collections.shuffle(fichas);
        StringBuilder sb = new StringBuilder();
        for (char c : fichas) sb.append(c);
        return sb.toString();
    }

    public String getEstadoTableroInicial() {
        if (estadoTableroInicial == null) estadoTableroInicial = generarEstadoAleatorio();
        return estadoTableroInicial;
    }

    public void setEstadoTableroInicial(String nuevo) { this.estadoTableroInicial = nuevo; }
    public String[] getEstadosJugadores() { return estadosJugadores; }
    public int getJugadorActual() { return jugadorActual; }
    public void setJugadorActual(int idx) { this.jugadorActual = idx; }
}