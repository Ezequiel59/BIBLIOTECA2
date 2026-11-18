package com.app.biblioteca2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AccountStoreTest {
	@TempDir
	Path dataDirectory;

	@Test
	void createsDefaultAccountsInPlaintextJsonAndDeterminesRoleFromCredentials() throws Exception {
		BibliotecaServicio service = new BibliotecaServicio(dataDirectory);
		Path credentials = dataDirectory.resolve("cuentas.json");
		AccountStore store = new AccountStore(credentials);

		String json = Files.readString(credentials);
		assertTrue(json.contains("\"username\": \"admin\", \"password\": \"1234\""));
		assertTrue(json.contains("\"username\": \"moder\", \"password\": \"1234\""));
		assertEquals(Rol.ADMINISTRADOR, store.authenticate("admin", "1234".toCharArray(), service).getRol());
		assertEquals(Rol.MODERADOR, store.authenticate("moder", "1234".toCharArray(), service).getRol());
		assertNotNull(store.authenticate(Rol.ADMINISTRADOR, "admin", "1234".toCharArray(), service));
		assertNotNull(store.authenticate(Rol.MODERADOR, "moder", "1234".toCharArray(), service));
		assertNull(store.authenticate(Rol.USUARIO, "admin", "1234".toCharArray(), service));
		assertNull(store.authenticate("admin", "incorrecta".toCharArray(), service));
	}

	@Test
	void savesNormalCredentialsAsJsonAndLoadsEscapedPasswords() throws Exception {
		Path credentials = dataDirectory.resolve("cuentas.json");
		BibliotecaServicio service = new BibliotecaServicio(dataDirectory);
		AccountStore store = new AccountStore(credentials);
		store.registrarUsuario(new Usuario("lector", "Lector Uno", "lector@example.com", "+503 2222-3333"),
				"clave\"texto".toCharArray(), service);

		assertTrue(Files.readString(credentials).contains("clave\\\"texto"));
		AccountStore reloaded = new AccountStore(credentials);
		Sesion session = reloaded.authenticate(Rol.USUARIO, "lector", "clave\"texto".toCharArray(),
				new BibliotecaServicio(dataDirectory));

		assertNotNull(session);
		assertEquals(Rol.USUARIO, session.getRol());
		assertEquals("lector", session.getUsuarioId());
	}
}
