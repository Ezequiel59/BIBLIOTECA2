package com.app.biblioteca2;

public record Autor(String id, String nombre) {
	public Autor {
		id = Validaciones.identificador(id, "El ID del autor");
		nombre = Validaciones.nombre(nombre, "El nombre del autor");
	}
}
