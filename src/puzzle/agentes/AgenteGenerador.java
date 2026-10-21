package puzzle.agentes;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.*;

public class AgenteGenerador extends Agent {

    private HashMap<String, List<String>> grafoEspacioSoluciones;

    @Override
    protected void setup() {
        System.out.println("=======================================================");
        System.out.println("[LOG-GENERADOR] Agente '" + getLocalName() + "' INICIADO.");
        System.out.println("[LOG-GENERADOR] Esperando peticiones en la red...");
        System.out.println("=======================================================");

        grafoEspacioSoluciones = new HashMap<>();

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    // Log general de recepción
                    System.out.println("\n[LOG-GENERADOR] >>> Mensaje entrante de: " + msg.getSender().getLocalName());
                    System.out.println("[LOG-GENERADOR] Performativa: " + ACLMessage.getPerformative(msg.getPerformative()));
                    System.out.println("[LOG-GENERADOR] Contenido: " + msg.getContent());

                    if (msg.getPerformative() == ACLMessage.REQUEST) {
                        String contenido = msg.getContent().trim();

                        // 1. Petición para crear una nueva partida
                        if (contenido.equals("GENERAR_RETO")) {
                            System.out.println("[LOG-GENERADOR] Tarea: Crear nuevo reto y construir grafo.");
                            long t0 = System.currentTimeMillis();

                            String nuevoReto = generarRetoResoluble();
                            System.out.println("[LOG-GENERADOR] Reto validado y generado: " + nuevoReto);

                            construirGrafoCompleto(nuevoReto);
                            long t1 = System.currentTimeMillis();

                            System.out.println("[LOG-GENERADOR] Grafo finalizado en " + (t1 - t0) + " ms.");
                            System.out.println("[LOG-GENERADOR] Nodos únicos en memoria: " + grafoEspacioSoluciones.size());

                            ACLMessage reply = msg.createReply();
                            reply.setPerformative(ACLMessage.INFORM);
                            reply.setContent(nuevoReto);
                            send(reply);
                            System.out.println("[LOG-GENERADOR] <<< Respuesta enviada a " + msg.getSender().getLocalName() + " con el reto.");
                        }
                        // 2. Petición de un Agente Jugador (el estado actual)
                        else if (contenido.length() == 9) {
                            System.out.println("[LOG-GENERADOR] Tarea: Buscar hijos para el nodo [" + contenido + "]");
                            List<String> hijos = grafoEspacioSoluciones.getOrDefault(contenido, new ArrayList<>());

                            ACLMessage reply = msg.createReply();
                            reply.setPerformative(ACLMessage.INFORM);
                            String respuestaHijos = String.join(",", hijos);
                            reply.setContent(respuestaHijos);
                            send(reply);
                            System.out.println("[LOG-GENERADOR] Hijos encontrados (" + hijos.size() + "): " + respuestaHijos);
                            System.out.println("[LOG-GENERADOR] <<< Respuesta enviada a " + msg.getSender().getLocalName());
                        } else {
                            System.out.println("[LOG-GENERADOR] ADVERTENCIA: Contenido no reconocido o longitud inválida.");
                        }
                    } else {
                        System.out.println("[LOG-GENERADOR] IGNORANDO: El mensaje no es un REQUEST.");
                    }
                } else {
                    block();
                }
            }
        });
    }

    private void construirGrafoCompleto(String estadoInicial) {
        grafoEspacioSoluciones.clear();
        Queue<String> frontera = new LinkedList<>();

        frontera.add(estadoInicial);
        grafoEspacioSoluciones.put(estadoInicial, new ArrayList<>());

        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};
        char[] direcciones = {'U', 'D', 'L', 'R'};

        while (!frontera.isEmpty()) {
            String actual = frontera.poll();
            int posVacia = actual.indexOf('0');
            int fila = posVacia / 3;
            int col = posVacia % 3;

            for (int i = 0; i < 4; i++) {
                int nuevaFila = fila + dx[i];
                int nuevaCol = col + dy[i];

                if (nuevaFila >= 0 && nuevaFila < 3 && nuevaCol >= 0 && nuevaCol < 3) {
                    int nuevaPos = nuevaFila * 3 + nuevaCol;
                    String estadoHijo = intercambiarFichas(actual, posVacia, nuevaPos);

                    String hijoFormateado = direcciones[i] + ":" + estadoHijo;
                    grafoEspacioSoluciones.get(actual).add(hijoFormateado);

                    if (!grafoEspacioSoluciones.containsKey(estadoHijo)) {
                        grafoEspacioSoluciones.put(estadoHijo, new ArrayList<>());
                        frontera.add(estadoHijo);
                    }
                }
            }
        }
    }

    private String intercambiarFichas(String estado, int pos1, int pos2) {
        char[] chars = estado.toCharArray();
        char temp = chars[pos1];
        chars[pos1] = chars[pos2];
        chars[pos2] = temp;
        return new String(chars);
    }

    private String generarRetoResoluble() {
        String estadoCandidato;
        do {
            estadoCandidato = barajarTablero();
        } while (!esResoluble(estadoCandidato));
        return estadoCandidato;
    }

    private String barajarTablero() {
        List<Character> fichas = new ArrayList<>();
        for (char c = '0'; c <= '8'; c++) {
            fichas.add(c);
        }
        Collections.shuffle(fichas);
        StringBuilder sb = new StringBuilder();
        for (Character c : fichas) {
            sb.append(c);
        }
        return sb.toString();
    }

    private boolean esResoluble(String estado) {
        String estadoSinCero = estado.replace("0", "");
        int inversiones = 0;
        for (int i = 0; i < estadoSinCero.length() - 1; i++) {
            for (int j = i + 1; j < estadoSinCero.length(); j++) {
                if (estadoSinCero.charAt(i) > estadoSinCero.charAt(j)) {
                    inversiones++;
                }
            }
        }
        return (inversiones % 2 == 0);
    }
}