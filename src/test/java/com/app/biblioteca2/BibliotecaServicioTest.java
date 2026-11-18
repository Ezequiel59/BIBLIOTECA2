package com.app.biblioteca2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BibliotecaServicioTest {
	@TempDir
	Path dataDirectory;

	@Test
	void usersCanBorrowOnlyForThemselvesAndSeeOnlyTheirLoans() throws Exception {
		BibliotecaServicio service = new BibliotecaServicio(dataDirectory);
		AccountStore accounts = new AccountStore(dataDirectory.resolve("cuentas.json"));
		Sesion admin = accounts.authenticate(Rol.ADMINISTRADOR, "admin", "1234".toCharArray(), service);
		Sesion moderator = accounts.authenticate(Rol.MODERADOR, "moder", "1234".toCharArray(), service);
		accounts.registrarUsuario(new Usuario("lector", "Lector Uno", "lector@example.com", "+503 2222-3333"),
				"12345".toCharArray(), service);
		accounts.registrarUsuario(new Usuario("otro", "Otro Lector", "otro@example.com", "+503 3333-4444"),
				"12345".toCharArray(), service);
		Sesion reader = accounts.authenticate(Rol.USUARIO, "lector", "12345".toCharArray(), service);
		service.crearLibro(moderator, new Libro("L1", "Libro Uno", new Autor("A1", "Autor Uno"), "Novela", 2));

		service.prestar(reader, "P1", "L1", reader.getUsuarioId(), 7);
		assertEquals(1, service.listarPrestamos(reader).size());
		assertThrows(SecurityException.class, () -> service.prestar(reader, "P2", "L1", "otro", 7));
		assertThrows(SecurityException.class, () -> service.listarUsuarios(moderator));

		Sesion otherReader = accounts.authenticate(Rol.USUARIO, "otro", "12345".toCharArray(), service);
		assertEquals(0, service.listarPrestamos(otherReader).size());
		service.devolver(reader, "P1");
		assertEquals(2, service.listarLibros(reader).getFirst().getDisponibles());
	}

	@Test
	void openLibraryWorksBecomeLocalThreeCopyBooksAndAdminCanHideThem() throws Exception {
		BibliotecaServicio service = new BibliotecaServicio(dataDirectory);
		AccountStore accounts = new AccountStore(dataDirectory.resolve("cuentas.json"));
		accounts.registrarUsuario(new Usuario("lector", "Lector Uno", "lector@example.com", "+503 2222-3333"),
				"12345".toCharArray(), service);
		Sesion admin = accounts.authenticate(Rol.ADMINISTRADOR, "admin", "1234".toCharArray(), service);
		Sesion moderator = accounts.authenticate(Rol.MODERADOR, "moder", "1234".toCharArray(), service);
		Sesion reader = accounts.authenticate(Rol.USUARIO, "lector", "12345".toCharArray(), service);
		LibroOpenLibrary external = new LibroOpenLibrary("1984", List.of("George Orwell"), 1949, 42,
				"https://openlibrary.org/works/OL123W", "A dystopian novel.");

		service.prestarOpenLibrary(reader, external, "P1", "lector", 7);
		Libro imported = service.buscarLibro(reader, "OL123W");
		assertEquals("OL123W", imported.getId());
		assertEquals(3, imported.getTotalEjemplares());
		assertEquals(2, imported.getDisponibles());
		assertEquals("A dystopian novel.", imported.getDescripcion());

		Libro edited = new Libro(imported.getId(), "Nineteen Eighty-Four", imported.getAutor(),
				imported.getCategoria(), imported.getTotalEjemplares(), imported.getDisponibles(),
				"Descripción editada por el moderador.");
		service.actualizarLibro(moderator, edited);
		assertEquals("Descripción editada por el moderador.",
				service.buscarLibro(reader, "OL123W").getDescripcion());

		service.prestarOpenLibrary(reader, external, "P2", "lector", 7);
		assertEquals("Nineteen Eighty-Four", service.buscarLibro(reader, "OL123W").getTitulo());
		service.prestarOpenLibrary(reader, external, "P3", "lector", 7);
		assertEquals(0, service.buscarLibro(reader, "OL123W").getDisponibles());
		assertThrows(IllegalStateException.class,
				() -> service.prestarOpenLibrary(reader, external, "P4", "lector", 7));

		service.eliminarLibro(admin, "OL123W");
		assertEquals(0, service.listarLibros(reader).size());
		assertEquals(0, service.listarLibros(moderator).size());
		assertTrue(service.estaOculto("OL123W"));
		assertNull(service.buscarLibro(reader, "OL123W"));
		assertEquals(1, service.listarLibros(admin).size());
		assertThrows(IllegalStateException.class,
				() -> service.prestarOpenLibrary(reader, external, "P4", "lector", 7));

		service.restaurarLibro(admin, "OL123W");
		BibliotecaServicio reloaded = new BibliotecaServicio(dataDirectory);
		assertEquals("Nineteen Eighty-Four", reloaded.buscarLibro(reader, "OL123W").getTitulo());
		assertFalse(reloaded.estaOculto("OL123W"));
		assertEquals(3, reloaded.listarPrestamos(reader).size());
		assertThrows(SecurityException.class, () -> reloaded.eliminarLibro(moderator, "OL123W"));
	}
}
