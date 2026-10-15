package puzzle.agentes;

import puzzle.busqueda.Hamming;
import puzzle.busqueda.Heuristica;

/** Agente Jugador 1: Greedy con la Heurística 1 (Hamming, fichas fuera de lugar). */
public class AgenteJugador1 extends Agentejugador {

    @Override
    protected Heuristica crearHeuristica() {
        return new Hamming();
    }
}
