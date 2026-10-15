package puzzle.busqueda;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Nodo del árbol de búsqueda: estado, padre, movimiento y h (lo acordado por el equipo),
 * más profundidad y orden:
 *  - profundidad: cuántos movimientos lleva desde la raíz (solo se reporta; Greedy no la usa).
 *  - orden: contador de inserción para desempatar nodos con la misma h.
 *           Sin esto la PriorityQueue no garantiza orden y los resultados cambian entre corridas.
 */
public class Nodo implements Comparable<Nodo> {

    public final String estado;
    public final Nodo padre;
    public final char movimiento;   // movimiento del hueco que llevó aquí; '-' en la raíz
    public final int h;
    public final int profundidad;
    public final long orden;

    public Nodo(String estado, Nodo padre, char movimiento, int h, long orden) {
        this.estado = estado;
        this.padre = padre;
        this.movimiento = movimiento;
        this.h = h;
        this.profundidad = (padre == null) ? 0 : padre.profundidad + 1;
        this.orden = orden;
    }

    /** Greedy: f(n) = h(n). Empate -> el que se insertó primero (FIFO). */
    @Override
    public int compareTo(Nodo otro) {
        int c = Integer.compare(this.h, otro.h);
        return (c != 0) ? c : Long.compare(this.orden, otro.orden);
    }

    /** Movimientos desde la raíz hasta este nodo, p. ej. "RURDDLURD". */
    public String reconstruirCamino() {
        StringBuilder sb = new StringBuilder();
        for (Nodo n = this; n.padre != null; n = n.padre) {
            sb.append(n.movimiento);
        }
        return sb.reverse().toString();
    }

}