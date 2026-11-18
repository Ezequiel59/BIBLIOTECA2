package com.app.biblioteca2;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class FavoritosStore {
	private static final ObjectMapper JSON = new ObjectMapper()
			.enable(SerializationFeature.INDENT_OUTPUT);
	private static final TypeReference<List<FavoritoLibro>> BOOK_LIST = new TypeReference<>() {
	};
	private final Path path;
	private final List<FavoritoLibro> favorites;

	public FavoritosStore(Path path) throws IOException {
		this.path = path;
		if (Files.exists(path)) {
			favorites = new ArrayList<>(JSON.readValue(path.toFile(), BOOK_LIST));
			if (favorites.stream().anyMatch(favorite -> favorite == null)) {
				throw new IOException("Hay registros nulos en " + path + ".");
			}
		} else {
			favorites = new ArrayList<>();
			persist();
		}
	}

	public List<FavoritoLibro> listar(String propietario) {
		String owner = Validaciones.identificador(propietario, "El usuario favorito");
		return favorites.stream().filter(favorite -> favorite.propietario().equals(owner)).toList();
	}

	public FavoritoLibro buscar(String propietario, String id) {
		return favorites.stream()
				.filter(favorite -> favorite.propietario().equals(propietario) && favorite.id().equals(id))
				.findFirst().orElse(null);
	}

	public FavoritoLibro buscar(String propietario, LibroOpenLibrary libro) {
		String owner = Validaciones.identificador(propietario, "El usuario favorito");
		String candidateId = libro.generarIdBiblioteca();
		return favorites.stream()
				.filter(saved -> saved.propietario().equals(owner) && saved.id().equals(candidateId))
				.findFirst().orElse(null);
	}

	public FavoritoLibro guardar(String propietario, LibroOpenLibrary libro) throws IOException {
		String owner = Validaciones.identificador(propietario, "El usuario favorito");
		FavoritoLibro existing = buscar(owner, libro);
		if (existing != null) {
			return existing;
		}
		FavoritoLibro favorite = FavoritoLibro.desde(owner, libro);
		favorites.add(favorite);
		try {
			persist();
		} catch (IOException exception) {
			favorites.remove(favorite);
			throw exception;
		}
		return favorite;
	}

	private void persist() throws IOException {
		Path directory = path.getParent();
		if (directory != null) {
			Files.createDirectories(directory);
		}
		JSON.writeValue(path.toFile(), favorites);
	}
}
