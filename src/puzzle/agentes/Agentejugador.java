package puzzle.agentes;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import puzzle.busqueda.BusquedaConGenerador;
import puzzle.busqueda.BusquedaConGenerador.ResultadoBusqueda;
import puzzle.busqueda.Heuristica;

/**
 * Clase padre de los 3 Agentes Jugadores. Ciclo de vida: PERCIBIR -> RAZONAR -> ACTUAR.
 *
 * Flujo de un reto:
 *   GUI --REQUEST(estado inicial)--> Jugador
 *   repetir: Jugador --REQUEST(estado del nodo elegido)--> Generador
 *            Jugador <--INFORM("U:estado,D:estado,...")--- Generador   (los hijos)
 *            (el jugador descarta los ya visitados, calcula h y elige el siguiente nodo)
 *   al llegar a la meta: Jugador --INFORM(secuencia;longitud;nodos;tiempoMs)--> GUI
 *   si falla:            Jugador --FAILURE(motivo)--> GUI
 *
 * Cada jugador hijo solo define su heurística.
 */
public abstract class Agentejugador extends Agent {

    // ---- Contrato con los otros agentes ----
    public static final String GENERADOR = System.getProperty("generador", "generador"); // nombre del agente generador
    public static final long TIMEOUT_GENERADOR_MS = Long.getLong("timeoutGeneradorMs", 10_000L);

    /** true = imprime en consola cada vez que pide hijos al generador. */
    protected boolean traza = true;

    /** Cada hijo devuelve su propia heurística. */
    protected abstract Heuristica crearHeuristica();

    /**
     * Gancho opcional: se llama DESPUÉS de haberle contestado a quien mandó el reto.
     * Por defecto no hace nada, así que los jugadores que no lo sobreescriban se comportan igual que antes.
     * Lo usa AgenteJugador1 para abrir su ventana paso a paso sin retrasar el INFORM.
     * Ojo: corre en el hilo del agente, así que nada de Swing directo (usar SwingUtilities.invokeLater).
     */
    protected void alTerminar(ResultadoBusqueda resultado) { }

    private Heuristica heuristica;
    private BusquedaConGenerador busqueda;  // la búsqueda en curso
    private ACLMessage solicitud;           // el reto que atiendo (null = estoy libre)
    private String conversacion;            // id para reconocer las respuestas del generador
    private long vencimiento;               // hora límite para que responda el generador
    private int numeroBusqueda = 0;

    @Override
    protected void setup() {
        heuristica = crearHeuristica();
        System.out.println(getLocalName() + " listo (" + heuristica.nombre() + "), generador esperado: '" + GENERADOR + "'");

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage percepcion = percibir();                       // 1. PERCIBIR
                if (percepcion == null && !tiempoAgotado()) {
                    if (solicitud == null) block();                       // libre: espero un reto
                    else block(Math.max(1, vencimiento - System.currentTimeMillis())); // espero al generador
                    return;
                }
                String siguiente = razonar(percepcion);                   // 2. RAZONAR
                actuar(siguiente);                                        // 3. ACTUAR
            }
        });
    }

    // PERCIBIR: libre -> busca un reto (REQUEST de la GUI); ocupado -> busca la respuesta del generador.
    protected ACLMessage percibir() {
        if (solicitud == null) {
            return receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
        }
        // La respuesta se reconoce por el conversation-id, o por venir del generador
        // (por si el generador no copia el conversation-id al responder).
        return receive(new MessageTemplate(m ->
                conversacion.equals(m.getConversationId())
                || (m.getSender() != null && GENERADOR.equals(m.getSender().getLocalName())
                    && m.getPerformative() != ACLMessage.REQUEST)));
    }

    // RAZONAR: actualiza la búsqueda con lo percibido.
    // Devuelve el estado cuyos hijos hay que pedir, o null si la búsqueda terminó (éxito o falla).
    protected String razonar(ACLMessage percepcion) {
        if (solicitud == null) {                                  // llegó un reto nuevo
            solicitud = percepcion;
            conversacion = getLocalName() + "-" + (++numeroBusqueda);
            busqueda = new BusquedaConGenerador(heuristica);
            String estado = percepcion.getContent() == null ? "" : percepcion.getContent().trim();
            System.out.println(getLocalName() + " percibió reto: " + estado);
            return busqueda.iniciar(estado);
        }
        if (percepcion == null) {                                 // el generador no respondió a tiempo
            busqueda.abortar(ResultadoBusqueda.FALLA_GENERADOR_SIN_RESPUESTA);
            return null;
        }
        if (percepcion.getPerformative() == ACLMessage.INFORM) {  // llegaron los hijos
            return busqueda.recibirHijos(BusquedaConGenerador.parsearHijos(percepcion.getContent()));
        }
        busqueda.abortar(ResultadoBusqueda.FALLA_GENERADOR_ERROR); // FAILURE u otra cosa
        return null;
    }

    // ACTUAR: pide hijos al generador, o responde a la GUI si ya terminó.
    protected void actuar(String siguiente) {
        if (siguiente != null) {
            ACLMessage pedido = new ACLMessage(ACLMessage.REQUEST);
            pedido.addReceiver(new AID(GENERADOR, AID.ISLOCALNAME));
            pedido.setConversationId(conversacion);
            pedido.setContent(siguiente);
            send(pedido);
            vencimiento = System.currentTimeMillis() + TIMEOUT_GENERADOR_MS;
            if (traza) {
                System.out.println(getLocalName() + " -> pide hijos de " + siguiente
                        + " (h=" + busqueda.getHActual() + ", expandidos=" + busqueda.getExpandidos()
                        + ", frontera=" + busqueda.getTamFrontera() + ")");
            }
            return;
        }

        ResultadoBusqueda r = busqueda.getResultado();
        ACLMessage respuesta = solicitud.createReply();
        if (r.exito) {
            respuesta.setPerformative(ACLMessage.INFORM);
            respuesta.setContent(r.aInform());                    // secuencia;longitud;nodos;tiempoMs
        } else {
            respuesta.setPerformative(ACLMessage.FAILURE);
            respuesta.setContent(r.motivoFalla);
        }
        send(respuesta);
        System.out.println(getLocalName() + " responde " + ACLMessage.getPerformative(respuesta.getPerformative())
                + ": " + respuesta.getContent());

        while (receive(MessageTemplate.MatchConversationId(conversacion)) != null) { /* limpia respuestas tardías */ }
        solicitud = null;                                         // libre para otro reto
        busqueda = null;

        // Ya contestamos y estamos libres: ahora lo opcional. Si el gancho falla, el agente sigue vivo.
        try {
            alTerminar(r);
        } catch (RuntimeException e) {
            System.out.println(getLocalName() + " aviso: alTerminar() falló (" + e + "), la búsqueda sí terminó bien");
        }
    }

    private boolean tiempoAgotado() {
        return solicitud != null && System.currentTimeMillis() >= vencimiento;
    }
}
