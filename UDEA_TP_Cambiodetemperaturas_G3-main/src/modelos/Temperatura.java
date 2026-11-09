package modelos;

import java.time.LocalDate;

public record Temperatura(String ciudad, LocalDate fecha, double valor) {
}
