package puzzle.agentes;

import puzzle.busqueda.BusquedaConGenerador.ResultadoBusqueda;
import puzzle.busqueda.Hamming;
import puzzle.busqueda.Heuristica;
import puzzle.gui.VentanaJugador;

/**
 * Agente Jugador 1: Greedy con la Heurística 1 (Hamming, fichas fuera de lugar).
 *
 * Además de responder el INFORM a la GUI (eso lo hace Agentejugador), abre una ventana
 * local con el recorrido paso a paso. La ventana es solo un visor: no manda mensajes.
 */
public class AgenteJugador1 extends Agentejugador {

    private static final String TITULO = "Agente 1 (Hamming)";

    @Override
    protected Heuristica crearHeuristica() {
        return new Hamming();
    }

    /** Se llama después de haber contestado, así que la ventana no retrasa el INFORM. */
    @Override
    protected void alTerminar(ResultadoBusqueda resultado) {
        if (!resultado.exito) {
            System.out.println(getLocalName() + ": no abro ventana, la búsqueda falló ("
                    + resultado.motivoFalla + ")");
            return;
        }
        // Historial de estados del camino: "182043765", "182403765", ... hasta "123456780"
        VentanaJugador.mostrar(TITULO, resultado.estados,
                "Hamming  |  " + resultado.longitud + " movimientos  |  "
                        + resultado.nodosExpandidos + " nodos expandidos  |  "
                        + resultado.tiempoMs + " ms");
    }
}
