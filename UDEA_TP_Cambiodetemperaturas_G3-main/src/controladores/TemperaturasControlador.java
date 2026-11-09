package controladores;

import modelos.Temperatura;
import servicios.TemperaturasServicio;
import vistas.TemperaturasVista;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class TemperaturasControlador {
    private final TemperaturasVista vista;
    private final TemperaturasServicio servicio;
    private final List<Temperatura> temperaturas;

    public TemperaturasControlador(
            TemperaturasVista vista, TemperaturasServicio servicio, List<Temperatura> temperaturas) {
        this.vista = vista;
        this.servicio = servicio;
        this.temperaturas = temperaturas;

        vista.setGraficarPromediosClick(evento -> graficarPromedios());
        vista.setConsultarFechaClick(evento -> consultarFecha());
        vista.setConsultarCiudadClick(evento -> consultarCiudad());
        vista.setCompararCiudadesClick(evento -> compararCiudades());
        graficarPromedios();
    }

    private void graficarPromedios() {
        try {
            var promedios = servicio.obtenerPromediosPorCiudad(
                    temperaturas, vista.getFechaDesde(), vista.getFechaHasta());
            vista.mostrarPromedios(promedios);
            if (promedios.isEmpty()) {
                vista.mostrarSinDatos("No hay registros en el rango de fechas seleccionado.");
            }
        } catch (IllegalArgumentException e) {
            vista.mostrarError(e.getMessage());
        }
    }

    private void consultarFecha() {
        try {
            var fecha = vista.getFechaSeleccionada();
            var datos = servicio.obtenerTemperaturasPorCiudad(temperaturas, fecha);
            vista.mostrarTemperaturasDiarias(datos);
            if (datos.isEmpty()) {
                vista.mostrarSinDatos("No hay registros para la fecha seleccionada.");
                return;
            }

            var comparador = Comparator.<Map.Entry<String, Double>>comparingDouble(Map.Entry::getValue)
                    .thenComparing(Map.Entry::getKey);
            var ciudadCalurosa = datos.entrySet().stream().max(comparador).orElseThrow();
            var ciudadFria = datos.entrySet().stream().min(comparador).orElseThrow();
            vista.mostrarExtremos(
                    ciudadCalurosa.getKey(), ciudadCalurosa.getValue(),
                    ciudadFria.getKey(), ciudadFria.getValue()
            );
        } catch (IllegalArgumentException e) {
            vista.mostrarError(e.getMessage());
        }
    }

    private void consultarCiudad() {
        try {
            var ciudad = vista.getCiudadSeleccionada();
            var promedios = servicio.obtenerPromediosAnualesCiudad(
                    temperaturas, ciudad, vista.getFechaDesde(), vista.getFechaHasta());
            vista.mostrarPromediosAnualesCiudad(ciudad, promedios);
            if (promedios.isEmpty()) {
                vista.mostrarSinDatos("No hay datos de " + ciudad + " en el rango seleccionado.");
            }
        } catch (IllegalArgumentException e) {
            vista.mostrarError(e.getMessage());
        }
    }

    private void compararCiudades() {
        try {
            var ciudades = vista.getCiudadesComparar();
            var comparacion = servicio.obtenerComparacionAnual(
                    temperaturas, ciudades, vista.getFechaDesde(), vista.getFechaHasta());
            vista.mostrarComparacionAnual(comparacion);
            if (comparacion.isEmpty()) {
                vista.mostrarSinDatos("No hay datos de las ciudades seleccionadas en el rango.");
            }
        } catch (IllegalArgumentException e) {
            vista.mostrarError(e.getMessage());
        }
    }
}
