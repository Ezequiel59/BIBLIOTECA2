package com.app.biblioteca2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BibliotecaServicio {
	private final Path dataDirectory;
	private final Map<String, Libro> libros = new LinkedHashMap<>();
	private final Map<String, Usuario> usuarios = new LinkedHashMap<>();
	private final Map<String, Prestamo> prestamos = new LinkedHashMap<>();

	public BibliotecaServicio(Path dataDirectory) throws IOException {
		this.dataDirectory = dataDirectory;
		cargarLibros();
		cargarUsuarios();
		cargarPrestamos();
	}

	public List<Libro> listarLibros(Sesion sesion) {
		exigirCatalogo(sesion);
		return libros.values().stream()
				.filter(libro -> libro.isVisible() || sesion.getRol() == Rol.ADMINISTRADOR).toList();
	}

	public Libro buscarLibro(Sesion sesion, String id) {
		exigirCatalogo(sesion);
		Libro libro = libros.get(id);
		return libro != null && (libro.isVisible() || sesion.getRol() == Rol.ADMINISTRADOR) ? libro : null;
	}

	public boolean estaOculto(String id) {
		Libro libro = libros.get(id);
		return libro != null && !libro.isVisible();
	}

	public List<Usuario> listarUsuarios(Sesion sesion) {
		exigir(sesion, Permiso.GESTIONAR_USUARIOS);
		return List.copyOf(usuarios.values());
	}

	public List<Prestamo> listarPrestamos(Sesion sesion) {
		exigirPrestamos(sesion);
		if (sesion.getRol() == Rol.USUARIO) {
			return prestamos.values().stream()
					.filter(prestamo -> prestamo.getUsuarioId().equals(sesion.getUsuarioId())).toList();
		}
		return List.copyOf(prestamos.values());
	}

	public void crearLibro(Sesion sesion, Libro libro) throws IOException {
		exigir(sesion, Permiso.GESTIONAR_LIBROS);
		if (libros.containsKey(libro.getId())) {
			throw new IllegalArgumentException("Ya existe un libro con ese ID.");
		}
		libros.put(libro.getId(), libro);
		persistirConRollback(libro.getId(), null, libros, "libros.txt", this::serializarLibros);
	}

	public void actualizarLibro(Sesion sesion, Libro libro) throws IOException {
		exigir(sesion, Permiso.GESTIONAR_LIBROS);
		Libro anterior = libros.get(libro.getId());
		if (anterior == null) {
			throw new IllegalArgumentException("El libro no existe.");
		}
		if (anterior.isVisible()) {
			libro.mostrar();
		} else {
			libro.ocultar();
		}
		libros.put(libro.getId(), libro);
		persistirConRollback(libro.getId(), anterior, libros, "libros.txt", this::serializarLibros);
	}

	public void eliminarLibro(Sesion sesion, String id) throws IOException {
		exigir(sesion, Permiso.GESTIONAR_USUARIOS);
		if (sesion.getRol() != Rol.ADMINISTRADOR) {
			throw new SecurityException("Solo el administrador puede ocultar libros.");
		}
		Libro libro = libros.get(id);
		if (libro == null) {
			throw new IllegalArgumentException("El libro no existe.");
		}
		libro.ocultar();
		try {
			escribir("libros.txt", serializarLibros());
		} catch (IOException exception) {
			libro.mostrar();
			throw exception;
		}
	}

	public void restaurarLibro(Sesion sesion, String id) throws IOException {
		exigir(sesion, Permiso.GESTIONAR_USUARIOS);
		if (sesion.getRol() != Rol.ADMINISTRADOR) {
			throw new SecurityException("Solo el administrador puede restaurar libros.");
		}
		Libro libro = libros.get(id);
		if (libro == null) {
			throw new IllegalArgumentException("El libro no existe.");
		}
		libro.mostrar();
		try {
			escribir("libros.txt", serializarLibros());
		} catch (IOException exception) {
			libro.ocultar();
			throw exception;
		}
	}

	public void crearUsuario(Sesion sesion, Usuario usuario) throws IOException {
		exigir(sesion, Permiso.GESTIONAR_USUARIOS);
		if (usuarios.containsKey(usuario.getId())) {
			throw new IllegalArgumentException("Ya existe un usuario con ese ID.");
		}
		usuarios.put(usuario.getId(), usuario);
		persistirConRollback(usuario.getId(), null, usuarios, "usuarios.txt", this::serializarUsuarios);
	}

	public void actualizarUsuario(Sesion sesion, Usuario usuario) throws IOException {
		exigir(sesion, Permiso.GESTIONAR_USUARIOS);
		Usuario anterior = usuarios.get(usuario.getId());
		if (anterior == null) {
			throw new IllegalArgumentException("El usuario no existe.");
		}
		usuarios.put(usuario.getId(), usuario);
		persistirConRollback(usuario.getId(), anterior, usuarios, "usuarios.txt", this::serializarUsuarios);
	}

	public void eliminarUsuario(Sesion sesion, String id) throws IOException {
		exigir(sesion, Permiso.GESTIONAR_USUARIOS);
		if (prestamos.values().stream().anyMatch(p -> p.getUsuarioId().equals(id) && p.estaActivo())) {
			throw new IllegalStateException("No se puede eliminar un usuario con préstamo activo.");
		}
		eliminar(id, usuarios, "usuarios.txt", this::serializarUsuarios, "usuario");
	}

	public void prestar(Sesion sesion, String prestamoId, String libroId, String usuarioId, int dias)
			throws IOException {
		exigirPrestamos(sesion);
		prestamoId = Validaciones.identificador(prestamoId, "El ID del préstamo");
		libroId = Validaciones.identificador(libroId, "El ID del libro");
		usuarioId = Validaciones.identificador(usuarioId, "El ID del usuario");
		if (sesion.getRol() == Rol.USUARIO && !sesion.getUsuarioId().equals(usuarioId)) {
			throw new SecurityException("Solo puedes solicitar préstamos para tu propia cuenta.");
		}
		if (prestamos.containsKey(prestamoId)) {
			throw new IllegalArgumentException("Ya existe un préstamo con ese ID.");
		}
		Libro libro = libros.get(libroId);
		if (libro == null || !libro.isVisible() || !usuarios.containsKey(usuarioId)) {
			throw new IllegalArgumentException("El libro o el usuario seleccionado no existe.");
		}
		if (dias < 1 || dias > 3650) {
			throw new IllegalArgumentException("Los días de préstamo deben estar entre 1 y 3650.");
		}
		LocalDate hoy = LocalDate.now();
		Prestamo prestamo = new Prestamo(prestamoId, libroId, usuarioId, hoy, hoy.plusDays(dias), null);
		List<String> librosAnteriores = serializarLibros();
		List<String> prestamosAnteriores = serializarPrestamos();
		libro.prestarEjemplar();
		prestamos.put(prestamoId, prestamo);
		try {
			escribir("libros.txt", serializarLibros());
			escribir("prestamos.txt", serializarPrestamos());
		} catch (IOException | RuntimeException exception) {
			prestamos.remove(prestamoId);
			libro.devolverEjemplar();
			restaurar("libros.txt", librosAnteriores, exception);
			restaurar("prestamos.txt", prestamosAnteriores, exception);
			throw exception;
		}
	}

	public void prestarOpenLibrary(Sesion sesion, LibroOpenLibrary externo, String prestamoId,
			String usuarioId, int dias) throws IOException {
		exigirPrestamos(sesion);
		String id = externo.generarIdBiblioteca();
		Libro existente = libros.get(id);
		if (existente != null && !existente.isVisible()) {
			throw new IllegalStateException("Este libro fue ocultado por el administrador.");
		}
		boolean nuevo = existente == null;
		if (nuevo) {
			Libro libro = crearLibroOpenLibrary(externo, 3);
			libros.put(id, libro);
			try {
				escribir("libros.txt", serializarLibros());
			} catch (IOException exception) {
				libros.remove(id);
				throw exception;
			}
		}
		try {
			prestar(sesion, prestamoId, id, usuarioId, dias);
		} catch (IOException | RuntimeException exception) {
			if (nuevo) {
				Libro imported = libros.remove(id);
				try {
					escribir("libros.txt", serializarLibros());
				} catch (IOException rollbackException) {
					if (imported != null) {
						libros.put(id, imported);
					}
					exception.addSuppressed(rollbackException);
				}
			}
			throw exception;
		}
	}

	public void guardarLibroOpenLibrary(Sesion sesion, LibroOpenLibrary externo) throws IOException {
		exigir(sesion, Permiso.GESTIONAR_LIBROS);
		String id = externo.generarIdBiblioteca();
		Libro existente = libros.get(id);
		if (existente != null) {
			if (!existente.isVisible()) {
				throw new IllegalStateException("El administrador ocultó este libro.");
			}
			return;
		}
		Libro libro = crearLibroOpenLibrary(externo, 3);
		libros.put(id, libro);
		try {
			escribir("libros.txt", serializarLibros());
		} catch (IOException exception) {
			libros.remove(id);
			throw exception;
		}
	}

	private Libro crearLibroOpenLibrary(LibroOpenLibrary externo, int ejemplares) {
		String autor = externo.autores().isEmpty() ? "Desconocido" : externo.autores().getFirst();
		if (!autor.matches("[\\p{L}\\p{M}][\\p{L}\\p{M} .'-]*")) {
			autor = "Desconocido";
		}
		String description = limpiarCampoExterno(externo.descripcion());
		return new Libro(externo.generarIdBiblioteca(), limpiarCampoExterno(externo.titulo()),
				new Autor("AUT-" + externo.generarIdBiblioteca(), limpiarCampoExterno(autor)),
				"Open Library", ejemplares, ejemplares, description);
	}

	private String limpiarCampoExterno(String value) {
		return value == null ? "" : value.replace('|', ' ').replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", " ")
				.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ').trim();
	}

	public void devolver(Sesion sesion, String prestamoId) throws IOException {
		exigirPrestamos(sesion);
		Prestamo prestamo = prestamos.get(prestamoId);
		if (prestamo == null || !prestamo.estaActivo()) {
			throw new IllegalArgumentException("El préstamo no existe o ya fue devuelto.");
		}
		if (sesion.getRol() == Rol.USUARIO && !prestamo.getUsuarioId().equals(sesion.getUsuarioId())) {
			throw new SecurityException("Solo puedes devolver tus propios préstamos.");
		}
		Libro libro = libros.get(prestamo.getLibroId());
		if (libro == null) {
			throw new IllegalStateException("El libro asociado ya no existe.");
		}
		List<String> librosAnteriores = serializarLibros();
		List<String> prestamosAnteriores = serializarPrestamos();
		prestamo.registrarDevolucion(LocalDate.now());
		libro.devolverEjemplar();
		try {
			escribir("libros.txt", serializarLibros());
			escribir("prestamos.txt", serializarPrestamos());
		} catch (IOException | RuntimeException exception) {
			prestamo = new Prestamo(prestamo.getId(), prestamo.getLibroId(), prestamo.getUsuarioId(),
					prestamo.getFechaPrestamo(), prestamo.getFechaVencimiento(), null);
			prestamos.put(prestamoId, prestamo);
			libro.prestarEjemplar();
			restaurar("libros.txt", librosAnteriores, exception);
			restaurar("prestamos.txt", prestamosAnteriores, exception);
			throw exception;
		}
	}

	Usuario buscarUsuario(String id) {
		return usuarios.get(id);
	}

	void registrarUsuario(Usuario usuario) throws IOException {
		if (usuarios.containsKey(usuario.getId())) {
			throw new IllegalArgumentException("Ya existe un usuario con ese ID.");
		}
		usuarios.put(usuario.getId(), usuario);
		persistirConRollback(usuario.getId(), null, usuarios, "usuarios.txt", this::serializarUsuarios);
	}

	private void exigir(Sesion sesion, Permiso permiso) {
		if (sesion == null || !sesion.tienePermiso(permiso)) {
			throw new SecurityException("No tienes permiso para realizar esta operación.");
		}
	}

	private void exigirCatalogo(Sesion sesion) {
		if (sesion == null || (!sesion.tienePermiso(Permiso.VER_CATALOGO)
				&& !sesion.tienePermiso(Permiso.GESTIONAR_LIBROS))) {
			throw new SecurityException("No tienes permiso para consultar el catálogo.");
		}
	}

	private void exigirPrestamos(Sesion sesion) {
		if (sesion == null || (!sesion.tienePermiso(Permiso.GESTIONAR_PRESTAMOS)
				&& !sesion.tienePermiso(Permiso.GESTIONAR_SUS_PRESTAMOS))) {
			throw new SecurityException("No tienes permiso para consultar préstamos.");
		}
	}

	private void cargarLibros() throws IOException {
		for (String[] data : cargarLibrosLineas()) {
			Libro libro;
			try {
				libro = new Libro(data[0], data[1], new Autor(data[2], data[3]), data[4],
						Integer.parseInt(data[5]), Integer.parseInt(data[6]),
						data.length >= 8 ? data[7] : "");
				if (data.length == 9) {
					if (!data[8].equals("true") && !data[8].equals("false")) {
						throw new IllegalArgumentException("El estado de visibilidad no es válido.");
					}
					if (Boolean.parseBoolean(data[8])) {
						libro.mostrar();
					} else {
						libro.ocultar();
					}
				}
			} catch (RuntimeException exception) {
				throw errorDatos("libros.txt", exception);
			}
			if (libros.putIfAbsent(libro.getId(), libro) != null) {
				throw new IOException("ID de libro duplicado: " + libro.getId());
			}
		}
	}

	private void cargarUsuarios() throws IOException {
		for (String[] data : cargarLineas("usuarios.txt", 4)) {
			Usuario usuario;
			try {
				usuario = new Usuario(data[0], data[1], data[2], data[3]);
			} catch (RuntimeException exception) {
				throw errorDatos("usuarios.txt", exception);
			}
			if (usuarios.putIfAbsent(usuario.getId(), usuario) != null) {
				throw new IOException("ID de usuario duplicado: " + usuario.getId());
			}
		}
	}

	private void cargarPrestamos() throws IOException {
		for (String[] data : cargarLineas("prestamos.txt", 6)) {
			Prestamo prestamo;
			try {
				LocalDate devolucion = data[5].isBlank() ? null : LocalDate.parse(data[5]);
				prestamo = new Prestamo(data[0], data[1], data[2], LocalDate.parse(data[3]),
						LocalDate.parse(data[4]), devolucion);
			} catch (RuntimeException exception) {
				throw errorDatos("prestamos.txt", exception);
			}
			if (prestamo.estaActivo()
					&& (!libros.containsKey(prestamo.getLibroId()) || !usuarios.containsKey(prestamo.getUsuarioId()))) {
				throw new IOException("Préstamo " + prestamo.getId() + " referencia datos inexistentes.");
			}
			if (prestamos.putIfAbsent(prestamo.getId(), prestamo) != null) {
				throw new IOException("ID de préstamo duplicado: " + prestamo.getId());
			}
		}
		for (Libro libro : libros.values()) {
			long prestamosActivos = prestamos.values().stream()
					.filter(prestamo -> prestamo.estaActivo() && prestamo.getLibroId().equals(libro.getId()))
					.count();
			long ejemplaresPrestados = libro.getTotalEjemplares() - libro.getDisponibles();
			if (prestamosActivos != ejemplaresPrestados) {
				throw new IOException("La disponibilidad del libro " + libro.getId()
						+ " no coincide con sus préstamos activos.");
			}
		}
	}

	private List<String[]> cargarLineas(String fileName, int expectedFields) throws IOException {
		Path path = dataDirectory.resolve(fileName);
		if (Files.notExists(path)) {
			return List.of();
		}
		List<String[]> parsed = new ArrayList<>();
		List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
		for (int index = 0; index < lines.size(); index++) {
			String line = lines.get(index);
			if (line.isBlank()) {
				continue;
			}
			String[] fields = line.split("\\|", -1);
			if (fields.length != expectedFields) {
				throw new IOException("Datos inválidos en " + path + ", línea " + (index + 1) + ".");
			}
			parsed.add(fields);
		}
		return parsed;
	}

	private List<String[]> cargarLibrosLineas() throws IOException {
		Path path = dataDirectory.resolve("libros.txt");
		if (Files.notExists(path)) {
			return List.of();
		}
		List<String[]> parsed = new ArrayList<>();
		List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
		for (int index = 0; index < lines.size(); index++) {
			String line = lines.get(index);
			if (line.isBlank()) {
				continue;
			}
			String[] fields = line.split("\\|", -1);
			if (fields.length != 7 && fields.length != 9) {
				throw new IOException("Datos inválidos en " + path + ", línea " + (index + 1) + ".");
			}
			parsed.add(fields);
		}
		return parsed;
	}

	private IOException errorDatos(String file, RuntimeException cause) {
		return new IOException("Datos inválidos en " + dataDirectory.resolve(file) + ": " + cause.getMessage(),
				cause);
	}

	private List<String> serializarLibros() {
		return libros.values().stream().map(libro -> String.join("|", libro.getId(), libro.getTitulo(),
				libro.getAutor().id(), libro.getAutor().nombre(), libro.getCategoria(),
				String.valueOf(libro.getTotalEjemplares()), String.valueOf(libro.getDisponibles()),
				libro.getDescripcion(), String.valueOf(libro.isVisible()))).toList();
	}

	private List<String> serializarUsuarios() {
		return usuarios.values().stream().map(usuario -> String.join("|", usuario.getId(), usuario.getNombre(),
				usuario.getEmail(), usuario.getTelefono())).toList();
	}

	private List<String> serializarPrestamos() {
		return prestamos.values().stream().map(prestamo -> String.join("|", prestamo.getId(),
				prestamo.getLibroId(), prestamo.getUsuarioId(), prestamo.getFechaPrestamo().toString(),
				prestamo.getFechaVencimiento().toString(),
				prestamo.getFechaDevolucion() == null ? "" : prestamo.getFechaDevolucion().toString())).toList();
	}

	private <T> void eliminar(String id, Map<String, T> map, String file, Serializer<T> serializer, String entity)
			throws IOException {
		T deleted = map.remove(id);
		if (deleted == null) {
			throw new IllegalArgumentException("El " + entity + " no existe.");
		}
		try {
			escribir(file, serializer.serialize());
		} catch (IOException exception) {
			map.put(id, deleted);
			throw exception;
		}
	}

	private <T> void persistirConRollback(String id, T previous, Map<String, T> map, String file,
			Serializer<T> serializer) throws IOException {
		try {
			escribir(file, serializer.serialize());
		} catch (IOException exception) {
			if (previous == null) {
				map.remove(id);
			} else {
				map.put(id, previous);
			}
			throw exception;
		}
	}

	private void escribir(String fileName, List<String> lines) throws IOException {
		Files.createDirectories(dataDirectory);
		Files.write(dataDirectory.resolve(fileName), lines, StandardCharsets.UTF_8);
	}

	private void restaurar(String fileName, List<String> previous, Exception original) {
		try {
			escribir(fileName, previous);
		} catch (IOException rollbackError) {
			original.addSuppressed(rollbackError);
		}
	}

	@FunctionalInterface
	private interface Serializer<T> {
		List<String> serialize();
	}
}
