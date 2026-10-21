package puzzle.pruebas;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import puzzle.busqueda.BusquedaConGenerador;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SIMULA al Agente de Despliegue (GUI) (solo para probar a los jugadores).
 * Envía el reto a uno o varios jugadores y muestra la respuesta, comprobando que la secuencia llegue a la meta.
 * Cuando todos contestan, imprime una tabla comparativa (longitud, nodos, tiempo).
 *
 * El tablero se puede pasar como argumento del agente o con -Destado=182043765
 * A quién retar: -Djugador=jugador1  o varios separados por coma: -Djugador=jugador1,jugador2,jugador3
 */
public class AgenteDespliegueMock extends Agent {

    private String estado;
    private final List<String> jugadores = new ArrayList<>();
    /** respuestas recibidas: jugador -> contenido del INFORM/FAILURE (para la tabla final) */
    private final Map<String, String[]> respuestas = new LinkedHashMap<>();

    @Override
    protected void setup() {
        Object[] args = getArguments();
        estado = (args != null && args.length > 0) ? args[0].toString() : System.getProperty("estado", "182043765");
        for (String nombre : System.getProperty("jugador", "jugador3").split(",")) {
            nombre = nombre.trim();
            if (!nombre.isEmpty()) jugadores.add(nombre);
        }

        addBehaviour(new OneShotBehaviour(this) {
            @Override
            public void action() {
                ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
                for (String jugador : jugadores) req.addReceiver(new AID(jugador, AID.ISLOCALNAME));
                req.setContent(estado);
                System.out.println(getLocalName() + " (MOCK GUI) -> REQUEST a " + jugadores + ": " + estado);
                myAgent.send(req);
            }
        });

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage m = myAgent.receive();
                if (m == null) { block(); return; }
                String de = m.getSender().getLocalName();
                String tipo = ACLMessage.getPerformative(m.getPerformative());
                if (m.getPerformative() != ACLMessage.INFORM) {
                    System.out.println(getLocalName() + " <- " + tipo + " de " + de + ": " + m.getContent());
                    anotar(de, null);
                    return;
                }
                String[] p = m.getContent().split(";", -1);
                System.out.println(getLocalName() + " <- " + tipo + " de " + de + ": movimientos=" + p[0]
                        + " | longitud=" + p[1] + " | nodos=" + p[2] + " | tiempo=" + p[3] + " ms");

                // Comprobación: repetir los movimientos sobre el tablero inicial
                String actual = estado;
                for (char mov : p[0].toCharArray()) actual = (actual == null) ? null : TableroPrueba.mover(actual, mov);
                boolean llega = BusquedaConGenerador.META.equals(actual);
                System.out.println(getLocalName() + "    verificación: "
                        + (llega ? "la secuencia llega a la meta" : "ERROR, no llega a la meta"));
                anotar(de, llega ? p : null);
            }
        });
    }

    /** Guarda la respuesta y, si ya contestaron todos los retados, imprime la comparación. */
    private void anotar(String jugador, String[] campos) {
        respuestas.put(jugador, campos);
        if (!respuestas.keySet().containsAll(jugadores)) return;

        System.out.println("\n=== Comparación (tablero " + estado + ") ===");
        System.out.printf("%-12s %-10s %-9s %-7s %s%n", "jugador", "longitud", "nodos", "ms", "secuencia");
        for (String nombre : jugadores) {
            String[] c = respuestas.get(nombre);
            if (c == null) System.out.printf("%-12s %s%n", nombre, "(sin solución válida)");
            else System.out.printf("%-12s %-10s %-9s %-7s %s%n", nombre, c[1], c[2], c[3], c[0]);
        }
        System.out.println();
    }
}
