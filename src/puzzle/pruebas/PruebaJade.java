package puzzle.pruebas;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.ContainerController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PruebaJade {

    /** nombre del agente -> clase que lo implementa. El orden importa: jugadores y generador antes del despliegue. */
    private static final String[][] CATALOGO = {
            {"jugador1",   "puzzle.agentes.AgenteJugador1"},
            {"jugador2",   "puzzle.agentes.AgenteJugador2"},
            {"jugador3",   "puzzle.agentes.AgenteJugador3"},
            {"generador",  "puzzle.pruebas.AgenteGeneradorMock"},
            {"despliegue", "puzzle.pruebas.AgenteDespliegueMock"},
    };

    public static void main(String[] args) throws Exception {
        String modo = args.length > 0 ? args[0] : "local";
        List<String> pedidos = args.length > 1
                ? expandir(Arrays.asList(args).subList(1, args.length))
                : modo.equals("local") ? Arrays.asList("jugador1", "jugador2", "jugador3", "generador", "despliegue")
                : Arrays.asList("jugador1", "jugador2", "jugador3");

        Runtime rt = Runtime.instance();
        Profile p;
        ContainerController contenedor;
        if (modo.equals("local") || modo.equals("principal")) {
            p = new ProfileImpl();
            p.setParameter(Profile.GUI, System.getProperty("sinGui") == null ? "true" : "false");
            if (System.getProperty("miIp") != null) p.setParameter(Profile.LOCAL_HOST, System.getProperty("miIp"));
            contenedor = rt.createMainContainer(p);
        } else {
            p = new ProfileImpl(modo, 1099, null);          // modo = IP de la PC con el contenedor principal
            if (System.getProperty("miIp") != null) p.setParameter(Profile.LOCAL_HOST, System.getProperty("miIp"));
            if (System.getProperty("puertoLocal") != null) p.setParameter(Profile.LOCAL_PORT, System.getProperty("puertoLocal"));
            contenedor = rt.createAgentContainer(p);
        }

        // Orden fijo del CATALOGO: primero jugadores y generador, al final la GUI (que es la que envía el reto)
        int creados = 0;
        for (String[] entrada : CATALOGO) {
            if (pedidos.contains(entrada[0])) {
                contenedor.createNewAgent(entrada[0], entrada[1], null).start();
                System.out.println("PruebaJade: inicia " + entrada[0] + " (" + entrada[1] + ")");
                creados++;
            }
        }

        // Un nombre mal escrito dejaba la plataforma vacía sin decir nada: ahora avisa.
        for (String nombre : pedidos) {
            if (!conocido(nombre)) {
                System.out.println("PruebaJade: AVISO, no conozco el agente '" + nombre + "'. Opciones: "
                        + "jugador1 jugador2 jugador3 jugadores generador despliegue");
            }
        }
        if (creados == 0) {
            System.out.println("PruebaJade: AVISO, no se inició ningún agente. La plataforma está vacía.");
        }
    }

    private static boolean conocido(String nombre) {
        for (String[] entrada : CATALOGO) if (entrada[0].equals(nombre)) return true;
        return false;
    }

    /** "jugadores" es atajo de jugador1 + jugador2 + jugador3. */
    private static List<String> expandir(List<String> pedidos) {
        List<String> lista = new ArrayList<>();
        for (String nombre : pedidos) {
            if (nombre.equals("jugadores")) lista.addAll(Arrays.asList("jugador1", "jugador2", "jugador3"));
            else lista.add(nombre);
        }
        return lista;
    }
}
