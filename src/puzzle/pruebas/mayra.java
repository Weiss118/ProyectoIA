package puzzle.pruebas;

import puzzle.busqueda.BusquedaGreedy;
import puzzle.busqueda.Hamming;
import puzzle.comun.Resultado;


public class mayra {

    public static void main(String[] args) {
        String inicial = "182043765";

        BusquedaGreedy greedy = new BusquedaGreedy(new Hamming());
        greedy.setTraza(true);

        Resultado resultado = greedy.resolver(inicial);

        System.out.println("\nResultado: " + resultado);
    }
}