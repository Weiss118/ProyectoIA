package uabc.fernandez.proyectoia;

import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;

public class BotonFlotante extends Button {

    private DropShadow sombra;
    private String colorBase;

    private final EfectoSonido sonidoClick;

    public BotonFlotante(String texto, String colorHex, String rutaSonido) {
        super(texto);
        this.colorBase = colorHex;
        this.sonidoClick = new EfectoSonidoWAV(rutaSonido); // ← STRATEGY

        inicializarEstilo();
        configurarEventos();
    }

    private void inicializarEstilo() {
        this.setStyle(
                "-fx-background-color: rgba(0,0,0,0);" +
                        "-fx-text-fill: #ffffff;" +
                        "-fx-font-size: 22px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 40px;" +
                        "-fx-border-radius: 40px;" +
                        "-fx-border-color: " + colorBase + ";" +
                        "-fx-border-width: 3px;" +
                        "-fx-padding: 18 36 18 36;"
        );

        this.setMinWidth(300);
        this.setMinHeight(70);

        sombra = new DropShadow();
        sombra.setRadius(6);
        sombra.setOffsetY(3);
        sombra.setColor(Color.rgb(0, 0, 0, 0.35));
        this.setEffect(sombra);
    }

    private void configurarEventos() {

        this.setOnMouseEntered(e -> {
            this.setScaleX(1.08);
            this.setScaleY(1.08);
            this.setStyle(
                    "-fx-background-color: " + colorBase + ";" +
                            "-fx-text-fill: #ffffff;" +
                            "-fx-font-size: 22px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-background-radius: 40px;" +
                            "-fx-border-radius: 40px;" +
                            "-fx-border-color: " + colorBase + ";" +
                            "-fx-border-width: 3px;" +
                            "-fx-padding: 18 36 18 36;"
            );
            sombra.setRadius(12);
            sombra.setOffsetY(5);
        });

        this.setOnMouseExited(e -> {
            this.setScaleX(1.0);
            this.setScaleY(1.0);
            this.setStyle(
                    "-fx-background-color: rgba(0,0,0,0);" +
                            "-fx-text-fill: #ffffff;" +
                            "-fx-font-size: 22px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-background-radius: 40px;" +
                            "-fx-border-radius: 40px;" +
                            "-fx-border-color: " + colorBase + ";" +
                            "-fx-border-width: 3px;" +
                            "-fx-padding: 18 36 18 36;"
            );
            sombra.setRadius(6);
            sombra.setOffsetY(3);
        });

        this.setOnMousePressed(e -> sonidoClick.reproducir());
    }
}

