package puzzle.pruebas;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.ContainerController;

import java.util.Arrays;
import java.util.List;

/**
 * Lanzador para probar el jugador con JADE.  Argumentos:  <modo> [agentes...]
 *
 *   local                  contenedor principal + jugador3 + generador (mock) + despliegue (mock). Todo en tu PC.
 *   principal              contenedor principal en tu PC; los demás se conectan a ti. Por defecto: jugador3.
 *   <IP del principal>     contenedor que se une al principal de otra PC. Por defecto: jugador3.
 *
 *   [agentes]  opcional, cuáles iniciar: jugador3  generador  despliegue
 *              (generador y despliegue son MOCKS: no los inicies si ya existen los reales en otra PC)
 *
 * Opciones de la JVM (-D):  -Destado=182043765   -DmiIp=192.168.1.20   -DsinGui=true
 *   -DpuertoLocal=1100  (solo si dos contenedores corren en la MISMA PC; cada uno necesita un puerto distinto)
 */
public class PruebaJade {

    public static void main(String[] args) throws Exception {
        String modo = args.length > 0 ? args[0] : "local";
        List<String> pedidos = args.length > 1 ? Arrays.asList(args).subList(1, args.length)
                : modo.equals("local") ? Arrays.asList("jugador3", "generador", "despliegue")
                : Arrays.asList("jugador3");

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

        // Orden fijo: primero jugador y generador, al final la GUI (que es la que envía el reto)
        if (pedidos.contains("jugador3"))   contenedor.createNewAgent("jugador3", "puzzle.agentes.AgenteJugador3", null).start();
        if (pedidos.contains("generador"))  contenedor.createNewAgent("generador", "puzzle.pruebas.AgenteGeneradorMock", null).start();
        if (pedidos.contains("despliegue")) contenedor.createNewAgent("despliegue", "puzzle.pruebas.AgenteDespliegueMock", null).start();
    }
}
