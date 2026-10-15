package puzzle.agentes;

import puzzle.busqueda.Heuristica;
import puzzle.busqueda.Manhattan;

/** Agente Jugador 2: Greedy con la Heurística 2 (distancia de Manhattan). */
public class AgenteJugador2 extends Agentejugador {

    @Override
    protected Heuristica crearHeuristica() {
        return new Manhattan();
    }
}
