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

    // Nombre de los agentes en la red JADE
    private final String NOMBRE_GENERADOR = "agenteGenerador";
    private final String[] NOMBRES_JUGADORES = {"agente1", "agente2", "agente3"};

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

        // Comportamiento cíclico de JADE para escuchar percepciones
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                percepcion();
                razonamiento();
                actuar();
                block(500); // Revisa mensajes cada 500ms
            }
        });
    }

    public void setGuiApp(GUIApp app) {
        this.guiApp = app;
    }

    // =========================================================================
    // 1. ÁREA DE PERCEPCIÓN: Recepción de mensajes FIPA-ACL
    // =========================================================================
    @Override
    public void percepcion() {
        MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
        ACLMessage msg = receive(mt);

        if (msg != null) {
            String contenido = msg.getContent();
            String emisor = msg.getSender().getLocalName();

            System.out.println("Mensaje recibido de [" + emisor + "]: " + contenido);

            if (contenido != null) {
                // Caso A: El AgenteGenerador envía el tablero inicial (cadena de 9 dígitos)
                if (contenido.length() == 9 && contenido.matches("[0-8]+")) {
                    this.estadoTableroInicial = contenido;
                    if (guiApp != null) {
                        Platform.runLater(() -> guiApp.actualizarTableroInicialEnUI(contenido));
                    }
                }
                // Caso B: Un AgenteJugador envía sus métricas con el formato "secuencia;longitud;nodos;tiempoMs"
                else if (contenido.contains(";")) {
                    procesarResultadoJugador(emisor, contenido);
                }
            }
        }
    }

    // =========================================================================
    // 2. PETICIONES A OTROS AGENTES (FIPA-ACL)
    // =========================================================================

    /**
     * Envía un REQUEST a Pasos (AgenteGenerador) para pedir un nuevo tablero inicial.
     */
    public void pedirTableroAGenerador() {
        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
        request.addReceiver(new AID(NOMBRE_GENERADOR, AID.ISLOCALNAME));
        request.setContent("GENERAR_TABLERO");
        send(request);
        System.out.println(" Petición enviada a " + NOMBRE_GENERADOR + ": GENERAR_TABLERO");
    }

    /**
     * Envía el tablero aceptado a los agentes jugadores para que inicien la resolución.
     */
    public void iniciarJuegoConJugadores() {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        for (String nombreJugador : NOMBRES_JUGADORES) {
            msg.addReceiver(new AID(nombreJugador, AID.ISLOCALNAME));
        }
        msg.setContent("RESOLVER:" + estadoTableroInicial);
        send(msg);
        System.out.println(" Tablero " + estadoTableroInicial + " enviado a los agentes jugadores.");
    }

    // =========================================================================
    // 3. PROCESAMIENTO DE RESULTADOS Y METRICAS
    // =========================================================================
    private void procesarResultadoJugador(String emisor, String contenido) {
        try {
            String[] partes = contenido.split(";");
            if (partes.length >= 4) {
                String secuencia = partes[0];
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
            System.err.println("Error procesando métricas del jugador " + emisor + ": " + e.getMessage());
        }
    }

    private int obtenerIndiceAgente(String emisor) {
        if (emisor.equalsIgnoreCase("agente1")) return 0;
        if (emisor.equalsIgnoreCase("agente2")) return 1;
        if (emisor.equalsIgnoreCase("agente3")) return 2;
        return -1;
    }

    @Override
    public void razonamiento() { }

    @Override
    public void actuar() { }

    // Generador auxiliar de cadenas de 9 dígitos (0 al 8)
    public String generarEstadoAleatorio() {
        List<Character> fichas = new ArrayList<>();
        for (char c = '0'; c <= '8'; c++) {
            fichas.add(c);
        }
        Collections.shuffle(fichas);
        StringBuilder sb = new StringBuilder();
        for (char c : fichas) {
            sb.append(c);
        }
        return sb.toString();
    }

    // Getters y Setters
    public String getEstadoTableroInicial() {
        if (estadoTableroInicial == null) {
            estadoTableroInicial = generarEstadoAleatorio();
        }
        return estadoTableroInicial;
    }

    public void setEstadoTableroInicial(String nuevo) {
        this.estadoTableroInicial = nuevo;
    }

    public String[] getEstadosJugadores() {
        return estadosJugadores;
    }

    public int getJugadorActual() {
        return jugadorActual;
    }

    public void setJugadorActual(int idx) {
        this.jugadorActual = idx;
    }
}