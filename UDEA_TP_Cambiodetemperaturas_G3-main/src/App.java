import controladores.TemperaturasControlador;
import servicios.TemperaturasServicio;
import vistas.TemperaturasVista;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.nio.file.Path;

public class App {
    public static void main(String[] args) {
        try {
            var servicio = new TemperaturasServicio();
            var temperaturas = args.length > 0
                    ? servicio.cargarDatos(Path.of(args[0]))
                    : servicio.cargarDatosPorDefecto();

            SwingUtilities.invokeLater(() -> {
                var vista = new TemperaturasVista(
                        servicio.obtenerFechaMinima(temperaturas),
                        servicio.obtenerFechaMaxima(temperaturas),
                        servicio.obtenerCiudades(temperaturas)
                );
                new TemperaturasControlador(vista, servicio, temperaturas);
                vista.setVisible(true);
            });
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("No se pudieron cargar las temperaturas: " + e.getMessage());
            JOptionPane.showMessageDialog(
                    null,
                    "No se pudieron cargar los datos de temperaturas.\n" + e.getMessage(),
                    "Error al cargar el CSV",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
