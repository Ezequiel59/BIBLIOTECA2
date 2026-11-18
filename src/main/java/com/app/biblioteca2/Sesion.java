package com.app.biblioteca2;

import java.util.EnumSet;
import java.util.Set;

public final class Sesion {
	private final String nombreAcceso;
	private final Rol rol;
	private final Usuario usuario;
	private final Set<Permiso> permisos;

	Sesion(String nombreAcceso, Rol rol, Usuario usuario) {
		this.nombreAcceso = nombreAcceso;
		this.rol = rol;
		this.usuario = usuario;
		this.permisos = switch (rol) {
			case ADMINISTRADOR -> EnumSet.allOf(Permiso.class);
			case MODERADOR -> EnumSet.of(Permiso.GESTIONAR_LIBROS,
					Permiso.GESTIONAR_PRESTAMOS, Permiso.VER_CATALOGO);
			case USUARIO -> EnumSet.of(Permiso.VER_CATALOGO, Permiso.GESTIONAR_SUS_PRESTAMOS);
		};
	}

	public String getNombreAcceso() {
		return nombreAcceso;
	}

	public String getNombre() {
		return usuario.getNombre();
	}

	public String getUsuarioId() {
		return usuario.getId();
	}

	public Rol getRol() {
		return rol;
	}

	public Set<Permiso> getPermisos() {
		return Set.copyOf(permisos);
	}

	boolean tienePermiso(Permiso permiso) {
		return permisos.contains(permiso);
	}

	Usuario getUsuario() {
		return usuario;
	}
}
