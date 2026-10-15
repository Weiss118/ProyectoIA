package puzzle.comun;

public class Resultado {

    // Motivos de falla (en el proyecto JADE se usan las mismas cadenas desde Protocolo)
    public static final String FALLA_NO_RESOLUBLE = "no-resoluble";
    public static final String FALLA_LIMITE = "limite-excedido";
    public static final String FALLA_FORMATO = "estado-invalido";

    public final boolean exito;
    public final String secuencia;
    public final int longitud;
    public final int nodosExpandidos;
    public final long tiempoMs;
    public final String motivoFalla;

    private Resultado(boolean exito, String secuencia,
                      int nodos, long tiempoMs, String motivo) {
        this.exito = exito;
        this.secuencia = secuencia;
        this.longitud = secuencia.length();
        this.nodosExpandidos = nodos;
        this.tiempoMs = tiempoMs;
        this.motivoFalla = motivo;
    }

    public static Resultado exito(String secuencia, int nodos, long tiempoMs) {
        return new Resultado(true, secuencia, nodos, tiempoMs, null);
    }

    public static Resultado falla(String motivo, int nodos, long tiempoMs) {
        return new Resultado(false, "", nodos, tiempoMs, motivo);
    }

    /*public String aContenido() {
        return secuencia + ";" + longitud + ";" + nodosExpandidos + ";" + tiempoMs + ";"
                + String.join(",", estados);
    }*/


    @Override
    public String toString() {
        return exito
                ? "secuencia=" + secuencia + " longitud=" + longitud
                + " nodos=" + nodosExpandidos + " tiempo=" + tiempoMs + "ms"
                : "FALLA(" + motivoFalla + ") nodos=" + nodosExpandidos;
    }
}