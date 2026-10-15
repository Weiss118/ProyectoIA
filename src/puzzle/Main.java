package puzzle;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.ContainerController;


public class Main {

    public static void main(String[] args) throws Exception {
        Runtime rt = Runtime.instance();
        Profile p = new ProfileImpl();
        p.setParameter(Profile.GUI, "true");   // abre la consola RMA
        ContainerController contenedor = rt.createMainContainer(p);

        // contenedor.createNewAgent("generador", "puzzle.agentes.AgenteGenerador", null).start();
        // contenedor.createNewAgent("jugador1",  "puzzle.agentes.AgenteJugador1",  null).start();
        // contenedor.createNewAgent("jugador2",  "puzzle.agentes.AgenteJugador2",  null).start();
        // contenedor.createNewAgent("jugador3",  "puzzle.agentes.AgenteJugador3",  null).start();
        // contenedor.createNewAgent("despliegue","puzzle.agentes.AgenteDespliegue",null).start();
    }
}
