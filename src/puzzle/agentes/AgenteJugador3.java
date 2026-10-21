package puzzle.agentes;

import puzzle.busqueda.ConflictoLineal;
import puzzle.busqueda.Heuristica;

/** Agente Jugador 3: Greedy con la Heurística 3 (Manhattan + conflicto lineal). */
public class AgenteJugador3 extends Agentejugador {

    @Override
    protected Heuristica crearHeuristica() {
        return new ConflictoLineal();
    }
}
