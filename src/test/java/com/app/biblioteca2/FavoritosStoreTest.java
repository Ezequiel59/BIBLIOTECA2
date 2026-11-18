package com.app.biblioteca2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FavoritosStoreTest {
	@TempDir
	Path dataDirectory;

	@Test
	void savesFavoritesPerUserWithStableIdsAndCanImportOnlyWithCatalogPermission() throws Exception {
		Path favoritesPath = dataDirectory.resolve("favoritos.json");
		FavoritosStore favorites = new FavoritosStore(favoritesPath);
		LibroOpenLibrary externalBook = new LibroOpenLibrary("Libro externo", List.of("Autor Ejemplo"),
				2020, 1234, "https://openlibrary.org/works/OL123W");

		FavoritoLibro saved = favorites.guardar("lector", externalBook);
		FavoritoLibro reloaded = new FavoritosStore(favoritesPath).buscar("lector", saved.id());
		assertNotNull(reloaded);
		assertEquals("OL123W", saved.id());
		assertEquals(1, favorites.listar("lector").size());
		assertEquals(0, favorites.listar("moder").size());
		assertEquals(saved.id(), favorites.guardar("moder", externalBook).id());

		BibliotecaServicio service = new BibliotecaServicio(dataDirectory);
		AccountStore accounts = new AccountStore(dataDirectory.resolve("cuentas.json"));
		Sesion moderator = accounts.authenticate(Rol.MODERADOR, "moder", "1234".toCharArray(), service);
		accounts.registrarUsuario(new Usuario("lector", "Lector Ejemplo", "lector@example.com",
				"+503 2222-3333"), "1234".toCharArray(), service);
		Sesion reader = accounts.authenticate(Rol.USUARIO, "lector", "1234".toCharArray(), service);
		assertNotNull(reader);

		service.crearLibro(moderator, reloaded.crearLibroLocal());
		assertEquals(reloaded.id(), service.buscarLibro(reader, reloaded.id()).getId());
		service.prestar(reader, "PRE-1", reloaded.id(), reader.getUsuarioId(), 7);
		assertEquals(2, service.buscarLibro(reader, reloaded.id()).getDisponibles());
		assertNull(service.buscarLibro(reader, "NO-EXISTE"));
	}

	@Test
	void generatesAStableIdEvenWhenOpenLibraryDoesNotProvideAWorkKey() {
		LibroOpenLibrary book = new LibroOpenLibrary("Libro sin clave", List.of("Autor Ejemplo"),
				null, null, null);

		assertEquals(book.generarIdBiblioteca(), book.generarIdBiblioteca());
	}
}
