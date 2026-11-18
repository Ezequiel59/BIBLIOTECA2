package com.app.biblioteca2;

public enum Rol {
	USUARIO("Usuario normal"),
	MODERADOR("Moderador"),
	ADMINISTRADOR("Administrador");

	private final String etiqueta;

	Rol(String etiqueta) {
		this.etiqueta = etiqueta;
	}

	@Override
	public String toString() {
		return etiqueta;
	}
}
