package uabc.fernandez.proyectoia;

import javax.sound.sampled.Clip;

public class EfectoSonidoWAV implements EfectoSonido {

    private final Clip clip;

    public EfectoSonidoWAV(String ruta) {
        this.clip = ControladorMusica.getInstancia().obtenerSonido(ruta);
    }

    @Override
    public void reproducir() {
        ControladorMusica.getInstancia().reproducirUnaVez(clip);
    }
}
