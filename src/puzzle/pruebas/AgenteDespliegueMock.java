package puzzle.pruebas;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import puzzle.busqueda.BusquedaConGenerador;

/**
 * SIMULA al Agente de Despliegue (GUI) (solo para probar al jugador).
 * Envía el reto al jugador y muestra la respuesta, comprobando que la secuencia llegue a la meta.
 * El tablero se puede pasar como argumento del agente o con -Destado=182043765
 */
public class AgenteDespliegueMock extends Agent {

    private String estado;
    private final String jugador = System.getProperty("jugador", "jugador3");

    @Override
    protected void setup() {
        Object[] args = getArguments();
        estado = (args != null && args.length > 0) ? args[0].toString() : System.getProperty("estado", "182043765");

        addBehaviour(new OneShotBehaviour(this) {
            @Override
            public void action() {
                ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
                req.addReceiver(new AID(jugador, AID.ISLOCALNAME));
                req.setContent(estado);
                System.out.println(getLocalName() + " (MOCK GUI) -> REQUEST a " + jugador + ": " + estado);
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
                    return;
                }
                String[] p = m.getContent().split(";", -1);
                System.out.println(getLocalName() + " <- " + tipo + " de " + de + ": movimientos=" + p[0]
                        + " | longitud=" + p[1] + " | nodos=" + p[2] + " | tiempo=" + p[3] + " ms");

                // Comprobación: repetir los movimientos sobre el tablero inicial
                String actual = estado;
                for (char mov : p[0].toCharArray()) actual = (actual == null) ? null : TableroPrueba.mover(actual, mov);
                System.out.println(getLocalName() + "    verificación: "
                        + (BusquedaConGenerador.META.equals(actual) ? "la secuencia llega a la meta" : "ERROR, no llega a la meta"));
            }
        });
    }
}
