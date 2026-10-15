package puzzle.busqueda;

/**
 * Heurística 1: fichas fuera de lugar (distancia de Hamming).
 * Cuenta cuántas de las 8 fichas no están en su posición de la meta "123456780".
 * El hueco no se cuenta; si se contara, dejaría de ser admisible.
 *
 * Ejemplo: "182043765" -> fuera de lugar 8, 2, 4, 3, 6, 5 -> h = 6
 */
public class Hamming implements Heuristica {

    @Override
    public int calcular(String estado) {
        int fueraDeLugar = 0;
        for (int i = 0; i < 9; i++) {
            char ficha = estado.charAt(i);
            if (ficha != '0' && ficha != BusquedaConGenerador.META.charAt(i)) {
                fueraDeLugar++;
            }
        }
        return fueraDeLugar;
    }

    @Override
    public String nombre() {
        return "Hamming";
    }
}
