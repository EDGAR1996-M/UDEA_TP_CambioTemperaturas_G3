package vistas;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Map;

public class TemperaturasVista extends JFrame {
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private final JTextField campoDesde;
    private final JTextField campoHasta;
    private final JTextField campoFecha;
    private final JComboBox<String> ciudadSeleccionada;
    private final JComboBox<String> ciudadCompararA;
    private final JComboBox<String> ciudadCompararB;
    private final GraficaTemperaturasPanel graficaPromedios;
    private final GraficaTemperaturasPanel graficaDiaria;
    private final GraficaTemperaturasPanel graficaCiudad;
    private final GraficaTemperaturasPanel graficaComparacion;
    private final JLabel resumenCalor;
    private final JLabel resumenFrio;
    private final JButton botonGraficarPromedios;
    private final JButton botonConsultarFecha;
    private final JButton botonConsultarCiudad;
    private final JButton botonCompararCiudades;
    private final JTabbedPane pestanas;

    public TemperaturasVista(LocalDate fechaMinima, LocalDate fechaMaxima, java.util.List<String> ciudades) {
        setTitle("Clima Colombia | Explorador de temperaturas");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(860, 600));
        setSize(1100, 700);
        setLocationRelativeTo(null);

        campoDesde = crearCampoFecha(fechaMinima);
        campoHasta = crearCampoFecha(fechaMaxima);
        campoFecha = crearCampoFecha(fechaMaxima);
        ciudadSeleccionada = new JComboBox<>(ciudades.toArray(String[]::new));
        ciudadCompararA = new JComboBox<>(ciudades.toArray(String[]::new));
        ciudadCompararB = new JComboBox<>(ciudades.toArray(String[]::new));
        if (ciudades.size() > 1) {
            ciudadCompararB.setSelectedIndex(1);
        }
        botonGraficarPromedios = new JButton("Ver promedios");
        botonConsultarFecha = new JButton("Ver ranking del día");
        botonConsultarCiudad = new JButton("Ver historial anual");
        botonCompararCiudades = new JButton("Comparar año a año");

        graficaPromedios = new GraficaTemperaturasPanel("PROMEDIO POR CIUDAD");
        graficaDiaria = new GraficaTemperaturasPanel("RANKING DE TEMPERATURA");
        graficaCiudad = new GraficaTemperaturasPanel("HISTORIAL ANUAL DE TEMPERATURA");
        graficaComparacion = new GraficaTemperaturasPanel("COMPARATIVO ENTRE CIUDADES");
        conectarDetallesDeBarras();
        resumenCalor = crearEtiquetaResumen();
        resumenFrio = crearEtiquetaResumen();

        var panelPrincipal = new JPanel(new BorderLayout(12, 12));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(16, 18, 18, 18));
        panelPrincipal.setBackground(new Color(245, 248, 249));
        panelPrincipal.add(crearPanelControles(), BorderLayout.NORTH);

        pestanas = new JTabbedPane();
        pestanas.addTab("Promedios", new JScrollPane(graficaPromedios));
        pestanas.addTab("Ranking diario", crearPanelDiario());
        pestanas.addTab("Historial por ciudad", new JScrollPane(graficaCiudad));
        pestanas.addTab("Comparativa anual", new JScrollPane(graficaComparacion));
        panelPrincipal.add(pestanas, BorderLayout.CENTER);

        setContentPane(panelPrincipal);
    }

    private JTextField crearCampoFecha(LocalDate fecha) {
        var campo = new JTextField(fecha.format(FORMATO_FECHA), 10);
        campo.setToolTipText("Formato de fecha: dd/MM/aaaa");
        return campo;
    }

    private JLabel crearEtiquetaResumen() {
        var etiqueta = new JLabel("Sin consulta");
        etiqueta.setFont(etiqueta.getFont().deriveFont(Font.BOLD, 14f));
        etiqueta.setForeground(new Color(35, 54, 68));
        return etiqueta;
    }

    private void conectarDetallesDeBarras() {
        var mostrarDetalle = (java.util.function.Consumer<String>) detalle ->
                JOptionPane.showMessageDialog(
                        this, detalle, "Detalle de la barra", JOptionPane.INFORMATION_MESSAGE);
        graficaPromedios.setAccionAlSeleccionarBarra(mostrarDetalle);
        graficaDiaria.setAccionAlSeleccionarBarra(mostrarDetalle);
        graficaCiudad.setAccionAlSeleccionarBarra(mostrarDetalle);
        graficaComparacion.setAccionAlSeleccionarBarra(mostrarDetalle);
    }

    private JPanel crearPanelControles() {
        var controles = new JPanel(new GridLayout(4, 1, 0, 6));
        controles.setOpaque(false);

        var panelRango = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        panelRango.setOpaque(false);
        panelRango.add(new JLabel("Desde (dd/MM/aaaa):"));
        panelRango.add(campoDesde);
        panelRango.add(new JLabel("Hasta:"));
        panelRango.add(campoHasta);
        panelRango.add(botonGraficarPromedios);

        var panelFecha = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        panelFecha.setOpaque(false);
        panelFecha.add(new JLabel("Fecha específica (dd/MM/aaaa):"));
        panelFecha.add(campoFecha);
        panelFecha.add(botonConsultarFecha);

        var panelCiudad = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        panelCiudad.setOpaque(false);
        panelCiudad.add(new JLabel("Ciudad:"));
        panelCiudad.add(ciudadSeleccionada);
        panelCiudad.add(botonConsultarCiudad);

        var panelComparacion = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        panelComparacion.setOpaque(false);
        panelComparacion.add(new JLabel("Comparar:"));
        panelComparacion.add(ciudadCompararA);
        panelComparacion.add(new JLabel("con"));
        panelComparacion.add(ciudadCompararB);
        panelComparacion.add(botonCompararCiudades);

        controles.add(panelRango);
        controles.add(panelFecha);
        controles.add(panelCiudad);
        controles.add(panelComparacion);
        return controles;
    }

    private JPanel crearPanelDiario() {
        var panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        var panelResumen = new JPanel(new GridLayout(2, 1, 4, 4));
        panelResumen.setBackground(Color.WHITE);
        panelResumen.add(resumenCalor);
        panelResumen.add(resumenFrio);
        panel.add(panelResumen, BorderLayout.NORTH);
        panel.add(new JScrollPane(graficaDiaria), BorderLayout.CENTER);
        return panel;
    }

    public LocalDate getFechaDesde() {
        return parsearFecha(campoDesde.getText(), "fecha inicial");
    }

    public LocalDate getFechaHasta() {
        return parsearFecha(campoHasta.getText(), "fecha final");
    }

    public LocalDate getFechaSeleccionada() {
        return parsearFecha(campoFecha.getText(), "fecha específica");
    }

    public String getCiudadSeleccionada() {
        return (String) ciudadSeleccionada.getSelectedItem();
    }

    public java.util.List<String> getCiudadesComparar() {
        return java.util.List.of(
                (String) ciudadCompararA.getSelectedItem(),
                (String) ciudadCompararB.getSelectedItem()
        );
    }

    private LocalDate parsearFecha(String texto, String nombreCampo) {
        try {
            return LocalDate.parse(texto.trim(), FORMATO_FECHA);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Ingrese la " + nombreCampo + " con el formato dd/MM/aaaa.", e);
        }
    }

    public void setGraficarPromediosClick(ActionListener evento) {
        botonGraficarPromedios.addActionListener(evento);
    }

    public void setConsultarFechaClick(ActionListener evento) {
        botonConsultarFecha.addActionListener(evento);
    }

    public void setConsultarCiudadClick(ActionListener evento) {
        botonConsultarCiudad.addActionListener(evento);
    }

    public void setCompararCiudadesClick(ActionListener evento) {
        botonCompararCiudades.addActionListener(evento);
    }

    public void mostrarPromedios(Map<String, Double> promedios) {
        graficaPromedios.setContextoDetalle(String.format(
                "Rango: %s al %s%nComparación: promedio frente a las demás ciudades con datos en el rango.",
                FORMATO_FECHA.format(getFechaDesde()), FORMATO_FECHA.format(getFechaHasta())
        ));
        graficaPromedios.setDatos(promedios);
        pestanas.setSelectedIndex(0);
    }

    public void mostrarTemperaturasDiarias(Map<String, Double> temperaturas) {
        graficaDiaria.setContextoDetalle(String.format(
                "Fecha: %s%nComparación: ciudades con datos en esa fecha.",
                FORMATO_FECHA.format(getFechaSeleccionada())
        ));
        graficaDiaria.setDatos(temperaturas);
        pestanas.setSelectedIndex(1);
    }

    public void mostrarPromediosAnualesCiudad(String ciudad, Map<Integer, Double> promedios) {
        graficaCiudad.setTitulo("HISTORIAL ANUAL — " + ciudad);
        graficaCiudad.setContextoDetalle(String.format(
                "Ciudad: %s%nPeriodo consultado: %s al %s%nComparación: promedio anual de esta ciudad.",
                ciudad, FORMATO_FECHA.format(getFechaDesde()), FORMATO_FECHA.format(getFechaHasta())
        ));
        graficaCiudad.setDatos(promedios.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        entrada -> Integer.toString(entrada.getKey()),
                        Map.Entry::getValue
                )));
        pestanas.setSelectedIndex(2);
    }

    public void mostrarComparacionAnual(Map<Integer, Map<String, Double>> comparacion) {
        graficaComparacion.setContextoDetalle(String.format(
                "Periodo consultado: %s al %s%nComparación: promedios anuales de las ciudades seleccionadas.%n",
                FORMATO_FECHA.format(getFechaDesde()), FORMATO_FECHA.format(getFechaHasta())
        ));
        graficaComparacion.setComparacion(comparacion);
        pestanas.setSelectedIndex(3);
    }

    public void mostrarExtremos(String ciudadCalurosa, double maximo, String ciudadFria, double minimo) {
        resumenCalor.setText(String.format("Más calurosa: %s — %.2f °C", ciudadCalurosa, maximo));
        resumenFrio.setText(String.format("Menos calurosa: %s — %.2f °C", ciudadFria, minimo));
    }

    public void mostrarSinDatos(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Sin datos", JOptionPane.INFORMATION_MESSAGE);
    }

    public void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Datos inválidos", JOptionPane.ERROR_MESSAGE);
    }
}
