package com.app.biblioteca2;

public final class Libro {
	private final String id;
	private String titulo;
	private Autor autor;
	private String categoria;
	private String descripcion;
	private int totalEjemplares;
	private int disponibles;
	private boolean visible = true;

	public Libro(String id, String titulo, Autor autor, String categoria, int total, int disponibles) {
		this(id, titulo, autor, categoria, total, disponibles, "");
	}

	public Libro(String id, String titulo, Autor autor, String categoria, int total, int disponibles,
			String descripcion) {
		this.id = Validaciones.identificador(id, "El ID del libro");
		actualizarDatos(titulo, autor, categoria, total, disponibles, descripcion);
	}

	public Libro(String id, String titulo, Autor autor, String categoria, int total) {
		this(id, titulo, autor, categoria, total, total);
	}

	public String getId() { return id; }
	public String getTitulo() { return titulo; }
	public Autor getAutor() { return autor; }
	public String getCategoria() { return categoria; }
	public String getDescripcion() { return descripcion; }
	public int getTotalEjemplares() { return totalEjemplares; }
	public int getDisponibles() { return disponibles; }
	public boolean isVisible() { return visible; }

	public void actualizarDatos(String titulo, Autor autor, String categoria, int total) {
		actualizarDatos(titulo, autor, categoria, total, descripcion);
	}

	public void actualizarDatos(String titulo, Autor autor, String categoria, int total, String descripcion) {
		actualizarDatos(titulo, autor, categoria, total, total - (totalEjemplares - disponibles), descripcion);
	}

	private void actualizarDatos(String titulo, Autor autor, String categoria, int total, int disponibles,
			String descripcion) {
		if (autor == null) {
			throw new IllegalArgumentException("El autor es obligatorio.");
		}
		if (total < 1 || total > 10000 || disponibles < 0 || disponibles > total) {
			throw new IllegalArgumentException("La cantidad de ejemplares no es válida.");
		}
		this.titulo = Validaciones.tituloLibro(titulo);
		this.autor = autor;
		this.categoria = Validaciones.descriptivo(categoria, "La categoría");
		this.descripcion = descripcion == null || descripcion.isBlank()
				? "" : Validaciones.descripcionLibro(descripcion);
		this.totalEjemplares = total;
		this.disponibles = disponibles;
	}

	public void ocultar() {
		visible = false;
	}

	public void mostrar() {
		visible = true;
	}

	public void prestarEjemplar() {
		if (disponibles == 0) {
			throw new IllegalStateException("No hay ejemplares disponibles.");
		}
		disponibles--;
	}

	public void devolverEjemplar() {
		if (disponibles >= totalEjemplares) {
			throw new IllegalStateException("Todos los ejemplares ya están disponibles.");
		}
		disponibles++;
	}
}
