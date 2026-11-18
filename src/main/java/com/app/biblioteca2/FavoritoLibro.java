package com.app.biblioteca2;

import java.util.List;

public record FavoritoLibro(String propietario, String id, String titulo, List<String> autores,
		Integer anioPublicacion, Integer idPortada, String urlOpenLibrary, String descripcion) {
	public FavoritoLibro {
		propietario = Validaciones.identificador(propietario, "El usuario favorito");
		id = Validaciones.identificador(id, "El ID del libro");
		titulo = Validaciones.descriptivo(titulo, "El título");
		autores = autores == null ? List.of() : List.copyOf(autores);
		descripcion = descripcion == null ? "" : descripcion;
	}

	public static FavoritoLibro desde(String propietario, LibroOpenLibrary libro) {
		return new FavoritoLibro(propietario, libro.generarIdBiblioteca(), libro.titulo(),
				libro.autores(), libro.anioPublicacion(), libro.idPortada(), libro.urlOpenLibrary(),
				libro.descripcion());
	}

	public FavoritoLibro(String propietario, String id, String titulo, List<String> autores,
			Integer anioPublicacion, Integer idPortada, String urlOpenLibrary) {
		this(propietario, id, titulo, autores, anioPublicacion, idPortada, urlOpenLibrary, "");
	}

	public String mostrarAutores() {
		return autores.isEmpty() ? "Autor desconocido" : String.join(", ", autores);
	}

	public LibroOpenLibrary comoLibroOpenLibrary() {
		return new LibroOpenLibrary(titulo, autores, anioPublicacion, idPortada, urlOpenLibrary, descripcion);
	}

	public Libro crearLibroLocal() {
		String autor = autores.isEmpty() ? "Desconocido" : autores.getFirst();
		if (!autor.matches("[\\p{L}\\p{M}][\\p{L}\\p{M} .'-]*")) {
			autor = "Desconocido";
		}
		return new Libro(id, titulo, new Autor("AUT-" + id, autor), "Open Library", 3, 3, descripcion);
	}

	public String urlPortada() {
		return idPortada == null ? null
				: "https://covers.openlibrary.org/b/id/" + idPortada + "-M.jpg";
	}
}
