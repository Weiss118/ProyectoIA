package puzzle.busqueda;

import puzzle.comun.Tablero;

/**
 * Heurística 1: fichas fuera de lugar (distancia de Hamming).
 * Cuenta cuántas de las 8 fichas no están en su posición de la meta.
 */
public class Hamming implements Heuristica {

    @Override
    public int calcular(String estado) {
        int fueraDeLugar = 0;
        for (int i = 0; i < 9; i++) {
            char ficha = estado.charAt(i);
            if (ficha != '0' && ficha != Tablero.META.charAt(i)) {
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