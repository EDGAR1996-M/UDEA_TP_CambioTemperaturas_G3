package vistas;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

public class GraficaTemperaturasPanel extends JPanel {
    private static final Color COLOR_REJILLA = new Color(225, 232, 236);
    private static final Color COLOR_TEXTO = new Color(35, 54, 68);
    private static final Color COLOR_SECUNDARIO = new Color(93, 108, 118);

    private String titulo;
    private Map<String, Double> datos = Map.of();
    private Map<Integer, Map<String, Double>> comparacion = Map.of();
    private double progreso = 1.0;
    private int indiceResaltado = -1;
    private Timer animacion;
    private String contextoDetalle = "";
    private Consumer<String> accionAlSeleccionarBarra = detalle -> { };

    public GraficaTemperaturasPanel(String titulo) {
        this.titulo = titulo;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(900, 460));
        setToolTipText("");
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent evento) {
                actualizarResaltado(evento.getX());
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evento) {
                var detalle = obtenerDetalleBarra(evento.getX(), evento.getY());
                if (detalle != null) {
                    accionAlSeleccionarBarra.accept(detalle);
                }
            }

            @Override
            public void mouseExited(MouseEvent evento) {
                indiceResaltado = -1;
                repaint();
            }
        });
    }

    public void setDatos(Map<String, Double> datos) {
        comparacion = Map.of();
        this.datos = new TreeMap<>(datos);
        actualizarTamano(this.datos.size());
    }

    public void setComparacion(Map<Integer, Map<String, Double>> comparacion) {
        this.comparacion = new TreeMap<>(comparacion);
        datos = Map.of();
        actualizarTamano(this.comparacion.size());
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
        repaint();
    }

    public void setContextoDetalle(String contextoDetalle) {
        this.contextoDetalle = contextoDetalle;
    }

    public void setAccionAlSeleccionarBarra(Consumer<String> accion) {
        accionAlSeleccionarBarra = accion;
    }

    public String obtenerDetalleBarra(int x, int y) {
        return comparacion.isEmpty()
                ? obtenerDetalleDato(x, y)
                : obtenerDetalleComparacion(x, y);
    }

    private String obtenerDetalleDato(int x, int y) {
        if (datos.isEmpty()) {
            return null;
        }

        var entradas = datos.entrySet().stream().toList();
        int izquierda = 76;
        int arriba = 58;
        int anchoGrafica = Math.max(1, getWidth() - izquierda - 30);
        int altoGrafica = Math.max(1, getHeight() - arriba - 128);
        int base = arriba + altoGrafica;
        double paso = (double) anchoGrafica / entradas.size();
        int anchoBarra = (int) Math.max(8, Math.min(40, paso * 0.68));
        double escala = Math.max(1.0, Math.ceil(
                entradas.stream().mapToDouble(Map.Entry::getValue).max().orElse(1.0) / 5.0
        ) * 5.0);

        for (int indice = 0; indice < entradas.size(); indice++) {
            var entrada = entradas.get(indice);
            int centro = izquierda + (int) (paso * (indice + 0.5));
            int alto = (int) (altoGrafica * entrada.getValue() / escala * progresoSuave());
            int arribaBarra = base - alto;
            if (x >= centro - anchoBarra / 2 && x <= centro + anchoBarra / 2
                    && y >= arribaBarra && y <= base) {
                return String.format(
                        Locale.ROOT,
                        "%s%nCiudad: %s%nTemperatura promedio: %.2f °C",
                        contextoDetalle, entrada.getKey(), entrada.getValue()
                );
            }
        }
        return null;
    }

    private String obtenerDetalleComparacion(int x, int y) {
        if (comparacion.isEmpty()) {
            return null;
        }

        var anios = comparacion.keySet().stream().toList();
        var ciudades = obtenerCiudadesComparadas();
        if (ciudades.isEmpty()) {
            return null;
        }
        int izquierda = 76;
        int arriba = 94;
        int anchoGrafica = Math.max(1, getWidth() - izquierda - 30);
        int altoGrafica = Math.max(1, getHeight() - arriba - 70);
        int base = arriba + altoGrafica;
        double pasoAnio = (double) anchoGrafica / anios.size();
        double pasoBarra = pasoAnio / ciudades.size();
        int anchoBarra = (int) Math.max(5, Math.min(32, pasoBarra * 0.72));
        double escala = Math.max(1.0, Math.ceil(comparacion.values().stream()
                .flatMap(anuales -> anuales.values().stream())
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(1.0) / 5.0) * 5.0);

        for (int indiceAnio = 0; indiceAnio < anios.size(); indiceAnio++) {
            int anio = anios.get(indiceAnio);
            var datosAnio = comparacion.get(anio);
            for (int indiceCiudad = 0; indiceCiudad < ciudades.size(); indiceCiudad++) {
                var ciudad = ciudades.get(indiceCiudad);
                var valor = datosAnio.get(ciudad);
                if (valor == null) {
                    continue;
                }

                int centro = izquierda + (int) (pasoAnio * indiceAnio
                        + pasoBarra * (indiceCiudad + 0.5));
                int alto = (int) (altoGrafica * valor / escala * progresoSuave());
                int arribaBarra = base - alto;
                if (x >= centro - anchoBarra / 2 && x <= centro + anchoBarra / 2
                        && y >= arribaBarra && y <= base) {
                    var comparadas = datosAnio.entrySet().stream()
                            .filter(entrada -> !entrada.getKey().equals(ciudad))
                            .map(entrada -> String.format(
                                    Locale.ROOT,
                                    "Comparada con %s: %.2f °C (diferencia: %+.2f °C)",
                                    entrada.getKey(), entrada.getValue(), valor - entrada.getValue()
                            ))
                            .collect(java.util.stream.Collectors.joining(System.lineSeparator()));
                    var contexto = contextoDetalle.isBlank()
                            ? ""
                            : contextoDetalle.stripTrailing() + System.lineSeparator();
                    return String.format(
                            Locale.ROOT,
                            "Año: %d%nCiudad: %s%nTemperatura promedio: %.2f °C%n%s%s",
                            anio, ciudad, valor, contexto,
                            comparadas.isEmpty() ? "No hay otra ciudad con datos para comparar este año."
                                    : comparadas
                    );
                }
            }
        }
        return null;
    }

    private List<String> obtenerCiudadesComparadas() {
        return comparacion.values().stream()
                .flatMap(anuales -> anuales.keySet().stream())
                .distinct()
                .sorted()
                .toList();
    }

    private void actualizarTamano(int cantidad) {
        setPreferredSize(new Dimension(Math.max(900, cantidad * 88 + 100), 460));
        indiceResaltado = -1;
        progreso = 0.0;
        if (animacion != null) {
            animacion.stop();
        }
        animacion = new Timer(16, evento -> {
            progreso = Math.min(1.0, progreso + 0.045);
            repaint();
            if (progreso >= 1.0) {
                ((Timer) evento.getSource()).stop();
            }
        });
        animacion.start();
        revalidate();
        repaint();
    }

    private void actualizarResaltado(int x) {
        int izquierda = 76;
        int anchoGrafica = Math.max(1, getWidth() - izquierda - 30);
        int cantidad = !comparacion.isEmpty() ? comparacion.size() : datos.size();
        int nuevoIndice = cantidad == 0 || x < izquierda || x >= izquierda + anchoGrafica
                ? -1
                : Math.min(cantidad - 1, (x - izquierda) * cantidad / anchoGrafica);
        if (nuevoIndice != indiceResaltado) {
            indiceResaltado = nuevoIndice;
            if (!comparacion.isEmpty() && nuevoIndice >= 0) {
                var anio = comparacion.keySet().stream().skip(nuevoIndice).findFirst().orElseThrow();
                setToolTipText("Promedios del año " + anio + ": " + comparacion.get(anio));
            } else if (nuevoIndice >= 0) {
                var entrada = datos.entrySet().stream().skip(nuevoIndice).findFirst().orElseThrow();
                setToolTipText(String.format("%s: %.2f °C", entrada.getKey(), entrada.getValue()));
            } else {
                setToolTipText("");
            }
            repaint();
        }
    }

    private Color colorCiudad(int indice, int cantidad) {
        float tono = cantidad <= 1 ? 0.52f : (float) indice / cantidad;
        return Color.getHSBColor(tono, 0.76f, 0.84f);
    }

    private void pintarBarra(Graphics2D g, int centro, int base, int ancho, int alto, Color color, boolean resaltada) {
        var colorClaro = Color.getHSBColor(
                Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null)[0],
                0.48f,
                1.0f
        );
        int y = base - alto;
        g.setPaint(new GradientPaint(centro, y, colorClaro, centro, base, color));
        g.fillRoundRect(centro - ancho / 2, y, ancho, alto, 9, 9);
        if (resaltada) {
            g.setColor(color.darker());
            g.setStroke(new BasicStroke(2f));
            g.drawRoundRect(centro - ancho / 2, y, ancho, alto, 9, 9);
        }
    }

    private double progresoSuave() {
        return 1.0 - Math.pow(1.0 - progreso, 3);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        var g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(COLOR_TEXTO);
        g.setFont(getFont().deriveFont(Font.BOLD, 17f));
        g.drawString(titulo, 24, 30);

        if (comparacion.isEmpty() && datos.isEmpty()) {
            g.setFont(getFont().deriveFont(14f));
            g.drawString("No hay datos disponibles para la selección.", 30, 80);
            g.dispose();
            return;
        }

        if (!comparacion.isEmpty()) {
            pintarComparacion(g);
        } else {
            pintarDatos(g);
        }
        g.dispose();
    }

    private void pintarDatos(Graphics2D g) {
        var entradas = datos.entrySet().stream().toList();
        int izquierda = 76;
        int arriba = 58;
        int anchoGrafica = Math.max(1, getWidth() - izquierda - 30);
        int altoGrafica = Math.max(1, getHeight() - arriba - 128);
        int base = arriba + altoGrafica;
        double maximo = entradas.stream().mapToDouble(Map.Entry::getValue).max().orElse(1.0);
        double escala = Math.max(1.0, Math.ceil(maximo / 5.0) * 5.0);

        pintarRejilla(g, izquierda, arriba, anchoGrafica, altoGrafica, escala);
        double paso = (double) anchoGrafica / entradas.size();
        int anchoBarra = (int) Math.max(8, Math.min(40, paso * 0.68));
        double avance = progresoSuave();
        for (int indice = 0; indice < entradas.size(); indice++) {
            var entrada = entradas.get(indice);
            int centro = izquierda + (int) (paso * (indice + 0.5));
            int altoCompleto = (int) (altoGrafica * entrada.getValue() / escala);
            int altoBarra = (int) (altoCompleto * avance);
            int y = base - altoBarra;
            var color = colorCiudad(indice, entradas.size());
            pintarBarra(g, centro, base, anchoBarra, altoBarra, color, indice == indiceResaltado);

            if (progreso >= 1.0 || indice == indiceResaltado) {
                g.setColor(COLOR_TEXTO);
                g.setFont(getFont().deriveFont(10f));
                g.drawString(String.format(Locale.ROOT, "%.1f", entrada.getValue()), centro - 13, y - 6);
            }

            var transformacion = g.getTransform();
            g.rotate(-Math.PI / 4, centro, base + 14);
            g.setColor(indice == indiceResaltado ? color.darker() : COLOR_TEXTO);
            g.setFont(getFont().deriveFont(indice == indiceResaltado ? Font.BOLD : Font.PLAIN, 10f));
            g.drawString(entrada.getKey(), centro, base + 14);
            g.setTransform(transformacion);
        }
        pintarEtiquetaEje(g, arriba);
    }

    private void pintarComparacion(Graphics2D g) {
        var anios = comparacion.keySet().stream().toList();
        var ciudades = comparacion.values().stream()
                .flatMap(anuales -> anuales.keySet().stream())
                .distinct()
                .sorted()
                .toList();
        int izquierda = 76;
        int arriba = 94;
        int anchoGrafica = Math.max(1, getWidth() - izquierda - 30);
        int altoGrafica = Math.max(1, getHeight() - arriba - 70);
        int base = arriba + altoGrafica;
        double maximo = comparacion.values().stream()
                .flatMap(anuales -> anuales.values().stream())
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(1.0);
        double escala = Math.max(1.0, Math.ceil(maximo / 5.0) * 5.0);
        pintarRejilla(g, izquierda, arriba, anchoGrafica, altoGrafica, escala);

        double pasoAnio = (double) anchoGrafica / anios.size();
        double pasoBarra = pasoAnio / ciudades.size();
        int anchoBarra = (int) Math.max(5, Math.min(32, pasoBarra * 0.72));
        double avance = progresoSuave();
        for (int indiceAnio = 0; indiceAnio < anios.size(); indiceAnio++) {
            int anio = anios.get(indiceAnio);
            int centroAnio = izquierda + (int) (pasoAnio * (indiceAnio + 0.5));
            var datosAnio = comparacion.get(anio);
            for (int indiceCiudad = 0; indiceCiudad < ciudades.size(); indiceCiudad++) {
                var ciudad = ciudades.get(indiceCiudad);
                var valor = datosAnio.get(ciudad);
                if (valor == null) {
                    continue;
                }

                int centroBarra = izquierda + (int) (pasoAnio * indiceAnio
                        + pasoBarra * (indiceCiudad + 0.5));
                int altoCompleto = (int) (altoGrafica * valor / escala);
                int altoBarra = (int) (altoCompleto * avance);
                pintarBarra(g, centroBarra, base, anchoBarra, altoBarra,
                        colorCiudad(indiceCiudad, ciudades.size()),
                        indiceAnio == indiceResaltado);
                if (progreso >= 1.0 || indiceAnio == indiceResaltado) {
                    g.setColor(COLOR_TEXTO);
                    g.setFont(getFont().deriveFont(9f));
                    g.drawString(String.format(Locale.ROOT, "%.1f", valor), centroBarra - 12, base - altoBarra - 4);
                }
            }
            g.setColor(indiceAnio == indiceResaltado ? new Color(235, 112, 52) : COLOR_TEXTO);
            g.setFont(getFont().deriveFont(indiceAnio == indiceResaltado ? Font.BOLD : Font.PLAIN, 11f));
            g.drawString(Integer.toString(anio), centroAnio - 14, base + 20);
        }

        g.setFont(getFont().deriveFont(10f));
        for (int indiceCiudad = 0; indiceCiudad < ciudades.size(); indiceCiudad++) {
            int x = izquierda + indiceCiudad * Math.max(140, anchoGrafica / ciudades.size());
            g.setColor(colorCiudad(indiceCiudad, ciudades.size()));
            g.fillRoundRect(x, 58, 13, 13, 5, 5);
            g.setColor(COLOR_TEXTO);
            g.drawString(ciudades.get(indiceCiudad), x + 18, 69);
        }
        pintarEtiquetaEje(g, arriba);
    }

    private void pintarRejilla(Graphics2D g, int izquierda, int arriba, int ancho, int alto, double escala) {
        int base = arriba + alto;
        g.setFont(getFont().deriveFont(11f));
        g.setStroke(new BasicStroke(1f));
        for (int marca = 0; marca <= 5; marca++) {
            int y = base - (int) (alto * marca / 5.0);
            g.setColor(COLOR_REJILLA);
            g.drawLine(izquierda, y, izquierda + ancho, y);
            g.setColor(COLOR_SECUNDARIO);
            g.drawString(String.format(Locale.ROOT, "%.0f", escala * marca / 5.0), 35, y + 4);
        }
    }

    private void pintarEtiquetaEje(Graphics2D g, int arriba) {
        g.setColor(COLOR_SECUNDARIO);
        g.setFont(getFont().deriveFont(11f));
        g.drawString("Temperatura (°C)", 12, arriba - 12);
    }
}
