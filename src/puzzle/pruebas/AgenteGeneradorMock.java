package puzzle.pruebas;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import puzzle.busqueda.BusquedaConGenerador;

/**
 * SIMULA al Agente Generador (solo para probar al jugador).
 * Recibe REQUEST con un estado y responde INFORM con sus hijos: "U:estado,D:estado,..."
 * Opciones (-D en la JVM):  -DretardoMs=200 simula latencia de red;  -DsinMovimiento=true envía solo estados;
 * -DsinConversacion=true responde con un mensaje nuevo (sin conversation-id), como lo haría un generador descuidado.
 */
public class AgenteGeneradorMock extends Agent {

    private static final long RETARDO_MS = Long.getLong("retardoMs", 0L);
    private static final boolean CON_MOVIMIENTO = !Boolean.getBoolean("sinMovimiento");
    private static final boolean SIN_CONVERSACION = Boolean.getBoolean("sinConversacion");

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " (MOCK del generador) listo");
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
                if (msg == null) { block(); return; }

                String estado = msg.getContent() == null ? "" : msg.getContent().trim();
                if (RETARDO_MS > 0) {
                    try { Thread.sleep(RETARDO_MS); } catch (InterruptedException e) { /* nada */ }
                }

                ACLMessage respuesta = msg.createReply();
                if (SIN_CONVERSACION) {                       // mensaje nuevo: sin conversation-id ni in-reply-to
                    respuesta = new ACLMessage(ACLMessage.INFORM);
                    respuesta.addReceiver(msg.getSender());
                }
                if (!BusquedaConGenerador.esValido(estado)) {
                    respuesta.setPerformative(ACLMessage.FAILURE);
                    respuesta.setContent("estado-invalido");
                } else {
                    respuesta.setPerformative(ACLMessage.INFORM);
                    respuesta.setContent(BusquedaConGenerador.formatearHijos(TableroPrueba.hijosDe(estado), CON_MOVIMIENTO));
                }
                myAgent.send(respuesta);
                System.out.println(getLocalName() + " <- hijos de " + estado + " -> " + respuesta.getContent());
            }
        });
    }
}
