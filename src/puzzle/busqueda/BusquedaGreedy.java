package puzzle.busqueda;

import puzzle.busqueda.Heuristica;
import puzzle.comun.Resultado;
import puzzle.comun.Tablero;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Greedy Best-First Search: f(n) = h(n).
 * La heurística se recibe por constructor: el mismo código sirve para los 3 jugadores.
 * Con setTraza(true) imprime cada iteración para ver qué está haciendo.
 */

public class BusquedaGreedy {

    /** El espacio resoluble tiene 181,440 estados; con esto nunca se corta un caso válido. */
    public static final int LIMITE_NODOS_DEFAULT = 200_000;

    private final Heuristica heuristica;
    private final int limiteNodos;
    private boolean traza = false;

    public BusquedaGreedy(Heuristica heuristica) {
        this(heuristica, LIMITE_NODOS_DEFAULT);
    }

    public BusquedaGreedy(Heuristica heuristica, int limiteNodos) {
        this.heuristica = heuristica;
        this.limiteNodos = limiteNodos;
    }

    public void setTraza(boolean traza) {
        this.traza = traza;
    }

    public Resultado resolver(String inicial) {
        long t0 = System.nanoTime();

        // Validaciones antes de buscar
        if (!Tablero.esValido(inicial)) {
            return Resultado.falla(Resultado.FALLA_FORMATO, 0, 0);
        }
        if (!Tablero.esResoluble(inicial)) {
            // Sin esto recorrería 181,440 estados antes de rendirse.
            return Resultado.falla(Resultado.FALLA_NO_RESOLUBLE, 0, ms(t0));
        }

        long contador = 0;                                      // para desempatar en la cola
        PriorityQueue<Nodo> frontera = new PriorityQueue<>();   // ordenada por Nodo.compareTo
        Set<String> visitados = new HashSet<>();                // estados ya generados

        frontera.add(new Nodo(inicial, null, '-', heuristica.calcular(inicial), contador++));
        visitados.add(inicial);
        int expandidos = 0;


        while (!frontera.isEmpty()) {
            Nodo actual = frontera.poll(); // el de menor h
            expandidos++;

            if (actual.estado.equals(Tablero.META)) {
                if (traza) imprimir(expandidos, actual, "META", frontera.size());
                return Resultado.exito(actual.reconstruirCamino(),expandidos, ms(t0));
            }
            if (expandidos >= limiteNodos) {
                return Resultado.falla(Resultado.FALLA_LIMITE, expandidos, ms(t0));
            }

            // 4. Generar sucesores (2, 3 o 4)
            StringBuilder generados = traza ? new StringBuilder() : null;
            for (char m : Tablero.movimientos()) {
                String siguiente = Tablero.mover(actual.estado, m);
                if (siguiente == null) continue;                 // se sale del tablero
                if (!visitados.add(siguiente)) {                 // ya visto -> evita ciclos
                    if (traza) generados.append(m).append(":visto ");
                    continue;
                }
                int h = heuristica.calcular(siguiente);
                frontera.add(new Nodo(siguiente, actual, m, h, contador++));
                if (traza) generados.append(m).append("(h=").append(h).append(") ");
            }
            if (traza) imprimir(expandidos, actual, "genera " + generados, frontera.size());
        }
        return Resultado.falla(Resultado.FALLA_NO_RESOLUBLE, expandidos, ms(t0));
    }

    public Heuristica getHeuristica() {
        return heuristica;
    }

    private static void imprimir(int it, Nodo n, String detalle, int tamFrontera) {
        System.out.printf("it %3d | saca %s h=%d mov=%c prof=%d | %s| frontera=%d%n",
                it, n.estado, n.h, n.movimiento, n.profundidad, detalle, tamFrontera);
    }

    private static long ms(long t0) {
        return (System.nanoTime() - t0) / 1_000_000;
    }
}