package com.app.biblioteca2;

public enum Permiso {
	GESTIONAR_LIBROS("Crear, editar y eliminar libros"),
	GESTIONAR_USUARIOS("Crear, editar y eliminar usuarios"),
	GESTIONAR_PRESTAMOS("Registrar y devolver préstamos"),
	GESTIONAR_SUS_PRESTAMOS("Solicitar y consultar mis préstamos"),
	VER_CATALOGO("Consultar el catálogo");

	private final String descripcion;

	Permiso(String descripcion) {
		this.descripcion = descripcion;
	}

	public String getDescripcion() {
		return descripcion;
	}
}
