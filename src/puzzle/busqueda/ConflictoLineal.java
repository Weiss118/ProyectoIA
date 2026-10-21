package puzzle.busqueda;

/**
 * Heurística 3: distancia de Manhattan + 2 por cada conflicto lineal.
 * Conflicto lineal: dos fichas que están en su fila (o columna) correcta, pero en orden invertido.
 * La meta es  1 2 3 / 4 5 6 / 7 8 0 : la ficha '1' va en la posición 0, la '2' en la 1, etc.
 */
public class ConflictoLineal implements Heuristica {

    @Override
    public int calcular(String estado) {
        int h = 0;

        // 1) Manhattan: cuánto le falta a cada ficha para llegar a su lugar
        for (int i = 0; i < 9; i++) {
            if (estado.charAt(i) == '0') continue;
            int meta = estado.charAt(i) - '1';              // posición correcta de la ficha
            h += Math.abs(i / 3 - meta / 3) + Math.abs(i % 3 - meta % 3);
        }

        // 2) Conflictos en filas
        for (int fila = 0; fila < 3; fila++) {
            for (int a = 0; a < 3; a++) {
                for (int b = a + 1; b < 3; b++) {
                    char x = estado.charAt(fila * 3 + a);
                    char y = estado.charAt(fila * 3 + b);
                    if (x == '0' || y == '0') continue;
                    int metaX = x - '1', metaY = y - '1';
                    // las dos pertenecen a esta fila, pero x debería ir después de y
                    if (metaX / 3 == fila && metaY / 3 == fila && metaX % 3 > metaY % 3) h += 2;
                }
            }
        }

        // 3) Conflictos en columnas (igual que las filas)
        for (int col = 0; col < 3; col++) {
            for (int a = 0; a < 3; a++) {
                for (int b = a + 1; b < 3; b++) {
                    char x = estado.charAt(a * 3 + col);
                    char y = estado.charAt(b * 3 + col);
                    if (x == '0' || y == '0') continue;
                    int metaX = x - '1', metaY = y - '1';
                    if (metaX % 3 == col && metaY % 3 == col && metaX / 3 > metaY / 3) h += 2;
                }
            }
        }
        return h;
    }

    @Override
    public String nombre() {
        return "Conflicto lineal";
    }
}
