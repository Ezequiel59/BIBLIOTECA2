package com.app.biblioteca2;

import java.util.List;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

public record LibroOpenLibrary(String titulo, List<String> autores, Integer anioPublicacion, Integer idPortada,
		String urlOpenLibrary, String descripcion) {
	public LibroOpenLibrary {
		autores = autores == null ? List.of() : List.copyOf(autores);
		descripcion = descripcion == null ? "" : descripcion;
	}

	public LibroOpenLibrary(String titulo, List<String> autores, Integer anioPublicacion, Integer idPortada,
			String urlOpenLibrary) {
		this(titulo, autores, anioPublicacion, idPortada, urlOpenLibrary, "");
	}

	public String mostrarAutores() {
		return autores.isEmpty() ? "Autor desconocido" : String.join(", ", autores);
	}

	public String urlPortada() {
		return idPortada == null ? null
				: "https://covers.openlibrary.org/b/id/" + idPortada + "-M.jpg";
	}

	public String generarIdBiblioteca() {
		if (urlOpenLibrary != null && urlOpenLibrary.startsWith("https://openlibrary.org/works/")) {
			String workId = urlOpenLibrary.substring("https://openlibrary.org/works/".length());
			if (workId.matches("[A-Za-z0-9._-]+")) {
				return workId;
			}
		}
		String stableKey = titulo.trim().toLowerCase() + "|" + String.join("|", autores).toLowerCase();
		return "OL" + UUID.nameUUIDFromBytes(stableKey.getBytes(StandardCharsets.UTF_8))
				.toString().replace("-", "").substring(0, 12).toUpperCase();
	}
}
