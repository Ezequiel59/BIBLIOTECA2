package com.app.biblioteca2;

public class Usuario {
	private final String id;
	private String nombre;
	private String email;
	private String telefono;

	public Usuario(String id, String nombre, String email, String telefono) {
		this.id = Validaciones.identificador(id, "El ID del usuario");
		actualizarDatos(nombre, email, telefono);
	}

	public String getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public String getEmail() {
		return email;
	}

	public String getTelefono() {
		return telefono;
	}

	public void actualizarDatos(String nombre, String email, String telefono) {
		this.nombre = Validaciones.nombre(nombre, "El nombre");
		this.email = Validaciones.correo(email);
		this.telefono = Validaciones.telefono(telefono);
	}
}
