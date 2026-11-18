package com.app.biblioteca2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class AccountStore {
	private final Path path;
	private final List<Account> accounts = new ArrayList<>();

	public AccountStore(Path path) throws IOException {
		this.path = path;
		if (Files.exists(path)) {
			load();
		}
		boolean changed = addDefaultIfMissing("admin", "1234", Rol.ADMINISTRADOR)
				| addDefaultIfMissing("moder", "1234", Rol.MODERADOR);
		if (!Files.exists(path) || changed) {
			persist();
		}
	}

	public Sesion authenticate(String username, char[] password, BibliotecaServicio service) {
		try {
			if (username == null || password == null) {
				return null;
			}
			String normalizedUsername = username.trim();
			for (Account account : accounts) {
				if (!account.username().equals(normalizedUsername)
						|| !account.password().equals(new String(password))) {
					continue;
				}
				Usuario user = account.role() == Rol.USUARIO
						? service.buscarUsuario(normalizedUsername)
						: crearPerfilSistema(account.username(), account.role());
				return user == null ? null : new Sesion(account.username(), account.role(), user);
			}
			return null;
		} finally {
			if (password != null) {
				java.util.Arrays.fill(password, '\0');
			}
		}
	}

	public Sesion authenticate(Rol selectedRole, String username, char[] password,
			BibliotecaServicio service) {
		Sesion sesion = authenticate(username, password, service);
		return sesion != null && sesion.getRol() == selectedRole ? sesion : null;
	}

	public void registrarUsuario(Usuario user, char[] password, BibliotecaServicio service) throws IOException {
		if (password == null) {
			throw new IllegalArgumentException("La contraseña es obligatoria.");
		}
		try {
			if (password.length < 4) {
				throw new IllegalArgumentException("La contraseña debe tener al menos 4 caracteres.");
			}
			String username = user.getId();
			if (accounts.stream().anyMatch(account -> account.username().equals(username))) {
				throw new IllegalArgumentException("Ese nombre de usuario ya está registrado.");
			}
			Account account = new Account(username, new String(password), Rol.USUARIO);
			accounts.add(account);
			try {
				persist();
			} catch (IOException exception) {
				accounts.remove(account);
				throw exception;
			}
			try {
				service.registrarUsuario(user);
			} catch (IOException | RuntimeException exception) {
				accounts.remove(account);
				try {
					persist();
				} catch (IOException rollbackException) {
					exception.addSuppressed(rollbackException);
				}
				throw exception;
			}
		} finally {
			java.util.Arrays.fill(password, '\0');
		}
	}

	public void eliminarCuentaUsuario(String username) throws IOException {
		Account account = accounts.stream()
				.filter(candidate -> candidate.username().equals(username) && candidate.role() == Rol.USUARIO)
				.findFirst().orElse(null);
		if (account == null) {
			return;
		}
		accounts.remove(account);
		try {
			persist();
		} catch (IOException exception) {
			accounts.add(account);
			throw exception;
		}
	}

	private boolean addDefaultIfMissing(String username, String password, Rol role) {
		if (accounts.stream().anyMatch(account -> account.username().equals(username))) {
			return false;
		}
		accounts.add(new Account(username, password, role));
		return true;
	}

	private Usuario crearPerfilSistema(String username, Rol role) {
		String name = role == Rol.ADMINISTRADOR ? "Administrador" : "Moderador";
		return new Usuario(username, name, username + "@biblioteca.local", "+503 0000-0000");
	}

	private void load() throws IOException {
		String json = Files.readString(path, StandardCharsets.UTF_8);
		try {
			Parser parser = new Parser(json);
			parser.expect('{');
			parser.expectString("accounts");
			parser.expect(':');
			parser.expect('[');
			if (!parser.peek(']')) {
				do {
					parser.expect('{');
					String username = null;
					String password = null;
					Rol role = null;
					boolean first = true;
					while (!parser.peek('}')) {
						if (!first) {
							parser.expect(',');
						}
						String key = parser.string();
						parser.expect(':');
						String value = parser.string();
						switch (key) {
							case "username" -> username = value;
							case "password" -> password = value;
							case "role" -> role = Rol.valueOf(value);
							default -> throw new IllegalArgumentException("Campo de cuenta desconocido: " + key);
						}
						first = false;
					}
					parser.expect('}');
					boolean duplicate = false;
					if (username != null) {
						for (Account account : accounts) {
							if (account.username().equals(username)) {
								duplicate = true;
								break;
							}
						}
					}
					if (username == null || password == null || role == null || duplicate) {
						throw new IllegalArgumentException("Hay una cuenta incompleta o duplicada.");
					}
					accounts.add(new Account(username, password, role));
					if (!parser.peek(']')) {
						parser.expect(',');
					}
				} while (!parser.peek(']'));
			}
			parser.expect(']');
			parser.expect('}');
			parser.expectEnd();
		} catch (RuntimeException exception) {
			throw new IOException("No se pudo leer el archivo de cuentas " + path + ": "
					+ exception.getMessage(), exception);
		}
	}

	private void persist() throws IOException {
		StringBuilder json = new StringBuilder("{\n  \"accounts\": [");
		for (int index = 0; index < accounts.size(); index++) {
			Account account = accounts.get(index);
			json.append(index == 0 ? "\n" : ",\n").append("    {\"username\": \"")
					.append(escape(account.username())).append("\", \"password\": \"")
					.append(escape(account.password())).append("\", \"role\": \"")
					.append(account.role().name()).append("\"}");
		}
		if (!accounts.isEmpty()) {
			json.append('\n').append("  ");
		}
		json.append("]\n}\n");
		Files.createDirectories(path.getParent());
		Files.writeString(path, json, StandardCharsets.UTF_8);
	}

	private String escape(String value) {
		StringBuilder escaped = new StringBuilder();
		for (int index = 0; index < value.length(); index++) {
			char character = value.charAt(index);
			switch (character) {
				case '"' -> escaped.append("\\\"");
				case '\\' -> escaped.append("\\\\");
				case '\b' -> escaped.append("\\b");
				case '\f' -> escaped.append("\\f");
				case '\n' -> escaped.append("\\n");
				case '\r' -> escaped.append("\\r");
				case '\t' -> escaped.append("\\t");
				default -> {
					if (character < 0x20) {
						escaped.append(String.format("\\u%04x", (int) character));
					} else {
						escaped.append(character);
					}
				}
			}
		}
		return escaped.toString();
	}

	private record Account(String username, String password, Rol role) {
	}

	private static final class Parser {
		private final String input;
		private int index;

		private Parser(String input) {
			this.input = input;
		}

		private void expect(char expected) {
			whitespace();
			if (index >= input.length() || input.charAt(index++) != expected) {
				throw new IllegalArgumentException("JSON no válido; se esperaba '" + expected + "'.");
			}
		}

		private void expectString(String expected) {
			if (!string().equals(expected)) {
				throw new IllegalArgumentException("Se esperaba el campo " + expected + ".");
			}
		}

		private boolean peek(char character) {
			whitespace();
			return index < input.length() && input.charAt(index) == character;
		}

		private String string() {
			whitespace();
			if (index >= input.length() || input.charAt(index++) != '"') {
				throw new IllegalArgumentException("Se esperaba una cadena JSON.");
			}
			StringBuilder result = new StringBuilder();
			while (index < input.length()) {
				char character = input.charAt(index++);
				if (character == '"') {
					return result.toString();
				}
				if (character != '\\') {
					if (character < 0x20) {
						throw new IllegalArgumentException("Carácter de control no válido en JSON.");
					}
					result.append(character);
					continue;
				}
				if (index >= input.length()) {
					break;
				}
				char escaped = input.charAt(index++);
				switch (escaped) {
					case '"', '\\', '/' -> result.append(escaped);
					case 'b' -> result.append('\b');
					case 'f' -> result.append('\f');
					case 'n' -> result.append('\n');
					case 'r' -> result.append('\r');
					case 't' -> result.append('\t');
					case 'u' -> {
						if (index + 4 > input.length()) {
							throw new IllegalArgumentException("Escape Unicode JSON incompleto.");
						}
						result.append((char) Integer.parseInt(input.substring(index, index + 4), 16));
						index += 4;
					}
					default -> throw new IllegalArgumentException("Escape JSON no válido.");
				}
			}
			throw new IllegalArgumentException("Cadena JSON sin cerrar.");
		}

		private void expectEnd() {
			whitespace();
			if (index != input.length()) {
				throw new IllegalArgumentException("Hay contenido inesperado después del JSON.");
			}
		}

		private void whitespace() {
			while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
				index++;
			}
		}
	}
}
