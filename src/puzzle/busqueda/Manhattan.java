package puzzle.busqueda;

/**
 * Heurística 2: distancia de Manhattan.
 * Suma, para cada ficha, cuántas filas y columnas le faltan para llegar a su lugar en la meta.
 * La ficha 'k' va en la posición k-1 (la '1' en la 0, ..., la '8' en la 7). El hueco no cuenta.
 *
 * Ejemplo: "182043765" -> 8:2, 2:1, 4:1, 3:1, 6:2, 5:2 -> h = 9
 */
public class Manhattan implements Heuristica {

    @Override
    public int calcular(String estado) {
        int h = 0;
        for (int i = 0; i < 9; i++) {
            char ficha = estado.charAt(i);
            if (ficha == '0') continue;
            int meta = ficha - '1';                         // posición correcta de la ficha
            h += Math.abs(i / 3 - meta / 3) + Math.abs(i % 3 - meta % 3);
        }
        return h;
    }

    @Override
    public String nombre() {
        return "Manhattan";
    }
}
