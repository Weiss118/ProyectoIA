package uabc.fernandez.proyectoia;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ControladorMusica {
    private static ControladorMusica instancia;
    private Clip clipActual;
    private boolean enFade = false;

    // Flyweight
    private final Map<String, Clip> sonidosCache = new HashMap<>();

    private ControladorMusica() {}

    public Clip obtenerSonido(String ruta) {
        if (sonidosCache.containsKey(ruta)) {
            return sonidosCache.get(ruta);
        }
        Clip clip = cargarSonido(ruta);
        sonidosCache.put(ruta, clip);
        return clip;
    }

    // Singleton
    public static ControladorMusica getInstancia() {
        if (instancia == null) {
            instancia = new ControladorMusica();
        }
        return instancia;
    }

    // Cargar sonido desde archivo
    public Clip cargarSonido(String ruta) {
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(new File(ruta));
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            return clip;
        } catch (Exception e) {
            System.out.println("Error cargando sonido: " + ruta);
            return null;
        }
    }

    // Reproducción centralizada

    public void reproducirUnaVez(Clip clip) {
        if (clip == null) return;
        clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }

    public void reproducirEnLoop(String ruta) {
        Clip clip = obtenerSonido(ruta);
        if (clip == null) return;

        if (clipActual != null && clipActual.isRunning()) {
            clipActual.stop();
        }

        clipActual = clip;
        clipActual.loop(Clip.LOOP_CONTINUOUSLY);
    }

    // Fade-out

    public void fadeOut(int milisegundos) {
        if (clipActual == null || enFade) return;

        enFade = true;

        new Thread(() -> {
            try {
                FloatControl volumen = (FloatControl) clipActual.getControl(FloatControl.Type.MASTER_GAIN);
                float inicio = volumen.getValue();
                float fin = volumen.getMinimum();

                int pasos = 30;
                float decremento = (inicio - fin) / pasos;
                int espera = milisegundos / pasos;

                for (int i = 0; i < pasos; i++) {
                    volumen.setValue(inicio - decremento * i);
                    Thread.sleep(espera);
                }

                clipActual.stop();
                volumen.setValue(inicio);
            } catch (Exception ignored) {}

            enFade = false;
        }).start();
    }

}

