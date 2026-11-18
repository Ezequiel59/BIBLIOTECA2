package com.app.biblioteca2;

import java.time.LocalDate;

public final class Prestamo {
	private final String id;
	private final String libroId;
	private final String usuarioId;
	private final LocalDate fechaPrestamo;
	private final LocalDate fechaVencimiento;
	private LocalDate fechaDevolucion;

	public Prestamo(String id, String libroId, String usuarioId, LocalDate fechaPrestamo,
			LocalDate fechaVencimiento, LocalDate fechaDevolucion) {
		this.id = Validaciones.identificador(id, "El ID del préstamo");
		this.libroId = Validaciones.identificador(libroId, "El ID del libro");
		this.usuarioId = Validaciones.identificador(usuarioId, "El ID del usuario");
		if (fechaPrestamo == null || fechaVencimiento == null || !fechaVencimiento.isAfter(fechaPrestamo)
				|| (fechaDevolucion != null && fechaDevolucion.isBefore(fechaPrestamo))) {
			throw new IllegalArgumentException("Las fechas del préstamo no son válidas.");
		}
		this.fechaPrestamo = fechaPrestamo;
		this.fechaVencimiento = fechaVencimiento;
		this.fechaDevolucion = fechaDevolucion;
	}

	public String getId() { return id; }
	public String getLibroId() { return libroId; }
	public String getUsuarioId() { return usuarioId; }
	public LocalDate getFechaPrestamo() { return fechaPrestamo; }
	public LocalDate getFechaVencimiento() { return fechaVencimiento; }
	public LocalDate getFechaDevolucion() { return fechaDevolucion; }
	public boolean estaActivo() { return fechaDevolucion == null; }
	public boolean isActivo() { return estaActivo(); }

	public void registrarDevolucion(LocalDate fecha) {
		if (!estaActivo()) {
			throw new IllegalStateException("El préstamo ya fue devuelto.");
		}
		if (fecha == null || fecha.isBefore(fechaPrestamo)) {
			throw new IllegalArgumentException("La fecha de devolución no es válida.");
		}
		fechaDevolucion = fecha;
	}
}
