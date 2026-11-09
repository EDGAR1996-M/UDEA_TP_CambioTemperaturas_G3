package servicios;

import modelos.Temperatura;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TemperaturasServicio {
    private static final String NOMBRE_ARCHIVO = "temperaturas_capitales_2015_2024_v2.csv";
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);

    public List<Temperatura> cargarDatosPorDefecto() throws IOException {
        var rutaFuente = Path.of("src", "datos", NOMBRE_ARCHIVO);
        if (Files.isRegularFile(rutaFuente)) {
            return cargarDatos(rutaFuente);
        }

        var recurso = getClass().getResourceAsStream("/datos/" + NOMBRE_ARCHIVO);
        if (recurso == null) {
            throw new IOException("No se encontró " + rutaFuente + " ni el recurso /datos/" + NOMBRE_ARCHIVO);
        }

        try (InputStream entrada = recurso;
             var lector = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8))) {
            return convertirLineas(lector.lines().toList());
        }
    }

    public List<Temperatura> cargarDatos(Path ruta) throws IOException {
        return convertirLineas(Files.readAllLines(ruta, StandardCharsets.UTF_8));
    }

    private List<Temperatura> convertirLineas(List<String> lineas) {
        if (lineas.isEmpty() || !lineas.get(0).replace("\uFEFF", "").trim().equals("Ciudad,Fecha,Temperatura")) {
            throw new IllegalArgumentException("El CSV debe iniciar con las columnas Ciudad,Fecha,Temperatura.");
        }

        var temperaturas = IntStream.range(1, lineas.size())
                .filter(indice -> !lineas.get(indice).isBlank())
                .mapToObj(indice -> convertirLinea(lineas.get(indice), indice + 1))
                .toList();

        if (temperaturas.isEmpty()) {
            throw new IllegalArgumentException("El CSV no contiene registros de temperatura.");
        }
        return temperaturas;
    }

    private Temperatura convertirLinea(String linea, int numeroLinea) {
        var columnas = linea.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        if (columnas.length != 3) {
            throw new IllegalArgumentException("La línea " + numeroLinea + " no tiene tres columnas: " + linea);
        }

        try {
            var ciudad = quitarComillas(columnas[0]);
            var fecha = LocalDate.parse(quitarComillas(columnas[1]), FORMATO_FECHA);
            var valor = Double.parseDouble(quitarComillas(columnas[2]));
            if (ciudad.isBlank() || !Double.isFinite(valor)) {
                throw new IllegalArgumentException("La línea " + numeroLinea + " contiene datos inválidos: " + linea);
            }
            return new Temperatura(ciudad, fecha, valor);
        } catch (DateTimeParseException | NumberFormatException e) {
            throw new IllegalArgumentException("No se pudo interpretar la línea " + numeroLinea + ": " + linea, e);
        }
    }

    private String quitarComillas(String columna) {
        var valor = columna.trim();
        if (valor.length() >= 2 && valor.startsWith("\"") && valor.endsWith("\"")) {
            return valor.substring(1, valor.length() - 1).replace("\"\"", "\"");
        }
        return valor;
    }

    public List<String> obtenerCiudades(List<Temperatura> temperaturas) {
        return temperaturas.stream()
                .map(Temperatura::ciudad)
                .distinct()
                .sorted()
                .toList();
    }

    public LocalDate obtenerFechaMinima(List<Temperatura> temperaturas) {
        return temperaturas.stream()
                .map(Temperatura::fecha)
                .min(LocalDate::compareTo)
                .orElseThrow(() -> new IllegalArgumentException("No hay temperaturas para consultar."));
    }

    public LocalDate obtenerFechaMaxima(List<Temperatura> temperaturas) {
        return temperaturas.stream()
                .map(Temperatura::fecha)
                .max(LocalDate::compareTo)
                .orElseThrow(() -> new IllegalArgumentException("No hay temperaturas para consultar."));
    }

    public Map<String, Double> obtenerPromediosPorCiudad(
            List<Temperatura> temperaturas, LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        return temperaturas.stream()
                .filter(temperatura -> estaEnRango(temperatura.fecha(), desde, hasta))
                .collect(Collectors.groupingBy(
                        Temperatura::ciudad,
                        TreeMap::new,
                        Collectors.averagingDouble(Temperatura::valor)
                ));
    }

    public Map<String, Double> obtenerTemperaturasPorCiudad(
            List<Temperatura> temperaturas, LocalDate fecha) {
        return temperaturas.stream()
                .filter(temperatura -> temperatura.fecha().equals(fecha))
                .collect(Collectors.groupingBy(
                        Temperatura::ciudad,
                        TreeMap::new,
                        Collectors.averagingDouble(Temperatura::valor)
                ));
    }

    public Map<Integer, Double> obtenerPromediosAnualesCiudad(
            List<Temperatura> temperaturas, String ciudad, LocalDate desde, LocalDate hasta) {
        return obtenerPromediosAnuales(temperaturas, List.of(ciudad), desde, hasta).entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entrada -> entrada.getValue().get(ciudad),
                        (primero, segundo) -> primero,
                        TreeMap::new
                ));
    }

    public Map<Integer, Map<String, Double>> obtenerComparacionAnual(
            List<Temperatura> temperaturas, List<String> ciudades, LocalDate desde, LocalDate hasta) {
        if (ciudades.size() < 2 || ciudades.stream().distinct().count() != ciudades.size()) {
            throw new IllegalArgumentException("Seleccione al menos dos ciudades diferentes para comparar.");
        }
        return obtenerPromediosAnuales(temperaturas, ciudades, desde, hasta);
    }

    private Map<Integer, Map<String, Double>> obtenerPromediosAnuales(
            List<Temperatura> temperaturas, List<String> ciudades, LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        return temperaturas.stream()
                .filter(temperatura -> ciudades.contains(temperatura.ciudad()))
                .filter(temperatura -> estaEnRango(temperatura.fecha(), desde, hasta))
                .collect(Collectors.groupingBy(
                        temperatura -> temperatura.fecha().getYear(),
                        TreeMap::new,
                        Collectors.groupingBy(
                                Temperatura::ciudad,
                                TreeMap::new,
                                Collectors.averagingDouble(Temperatura::valor)
                        )
                ));
    }

    private boolean estaEnRango(LocalDate fecha, LocalDate desde, LocalDate hasta) {
        return !fecha.isBefore(desde) && !fecha.isAfter(hasta);
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha inicial debe ser anterior o igual a la fecha final.");
        }
    }
}
