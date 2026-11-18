package com.app.biblioteca2;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@WebServlet("/app")
public final class BibliotecaServlet extends HttpServlet {
	private static final String SESSION_USER = "biblioteca.sesion";
	private static final String SESSION_TOKEN = "biblioteca.csrf";
	private static final String SESSION_BOOKS = "biblioteca.openLibraryBooks";
	private static final String SESSION_QUERY = "biblioteca.openLibraryQuery";
	private static final SecureRandom RANDOM = new SecureRandom();

	private BibliotecaServicio servicio;
	private AccountStore accountStore;
	private FavoritosStore favoritosStore;
	private OpenLibraryClient openLibraryClient;

	@Override
	public void init() throws ServletException {
		Path dataDirectory = Path.of(System.getProperty("biblioteca.data.dir", "data"))
				.toAbsolutePath().normalize();
		try {
			servicio = new BibliotecaServicio(dataDirectory);
			accountStore = new AccountStore(dataDirectory.resolve("cuentas.json"));
			favoritosStore = new FavoritosStore(dataDirectory.resolve("favoritos.json"));
			openLibraryClient = new OpenLibraryClient();
		} catch (IOException | RuntimeException exception) {
			throw new ServletException("No se pudo iniciar Biblioteca UFG con los datos de " + dataDirectory + ".",
					exception);
		}
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		response.setContentType("text/html; charset=UTF-8");
		response.setHeader("Cache-Control", "no-store");

		HttpSession httpSession = request.getSession(true);
		request.setAttribute("csrfToken", csrfToken(httpSession));
		consumeFlash(httpSession, request, "flashSuccess", "success");
		consumeFlash(httpSession, request, "flashError", "error");

		Sesion sesion = (Sesion) httpSession.getAttribute(SESSION_USER);
		request.setAttribute("sesion", sesion);
		request.setAttribute("roleName", sesion == null ? "" : sesion.getRol().name());
		if (sesion == null) {
			request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
			return;
		}

		synchronized (servicio) {
			request.setAttribute("catalogo", librosVista(servicio.listarLibros(sesion)));
			request.setAttribute("prestamos", servicio.listarPrestamos(sesion));
			request.setAttribute("favoritos", favoritosVista(sesion));
			request.setAttribute("openBooks", librosOpenLibraryVista(httpSession, sesion));
			request.setAttribute("openQuery", httpSession.getAttribute(SESSION_QUERY));
			if (sesion.tienePermiso(Permiso.GESTIONAR_USUARIOS)) {
				request.setAttribute("usuarios", servicio.listarUsuarios(sesion));
			}
		}

		String activeTab = request.getParameter("tab");
		request.setAttribute("activeTab", tabPermitida(activeTab, sesion) ? activeTab : "home");
		request.getRequestDispatcher("/WEB-INF/views/library.jsp").forward(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		request.setCharacterEncoding("UTF-8");
		HttpSession httpSession = request.getSession(false);
		if (httpSession == null || !tokenValido(request, httpSession)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "La solicitud no superó la validación CSRF.");
			return;
		}

		String action = request.getParameter("action");
		try {
			if ("login".equals(action)) {
				synchronized (servicio) {
					iniciarSesion(request, response, httpSession);
				}
				return;
			}
			if ("register".equals(action)) {
				synchronized (servicio) {
					registrarCuenta(request, response, httpSession);
				}
				return;
			}

			Sesion sesion = (Sesion) httpSession.getAttribute(SESSION_USER);
			if (sesion == null) {
				flash(httpSession, "flashError", "Inicia sesión para continuar.");
				redirect(request, response, null);
				return;
			}

			if ("search".equals(action)) {
				ejecutarAccion(action, request, httpSession, sesion);
			} else {
				synchronized (servicio) {
					ejecutarAccion(action, request, httpSession, sesion);
				}
			}
			redirect(request, response, tabDestino(action));
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			flash(httpSession, "flashError", "La operación fue interrumpida: " + mensaje(exception));
			redirect(request, response, tabDestino(action));
		} catch (IOException | RuntimeException exception) {
			flash(httpSession, "flashError", mensaje(exception));
			redirect(request, response, tabDestino(action));
		}
	}

	private void iniciarSesion(HttpServletRequest request, HttpServletResponse response, HttpSession oldSession)
			throws IOException {
		Sesion sesion = accountStore.authenticate(request.getParameter("username"),
				valor(request, "password").toCharArray(), servicio);
		if (sesion == null) {
			flash(oldSession, "flashError", "Usuario o contraseña incorrectos.");
			redirect(request, response, null);
			return;
		}

		oldSession.invalidate();
		HttpSession newSession = request.getSession(true);
		newSession.setAttribute(SESSION_USER, sesion);
		newSession.setAttribute(SESSION_TOKEN, nuevoToken());
		flash(newSession, "flashSuccess", "Bienvenido, " + sesion.getNombre() + ".");
		redirect(request, response, "home");
	}

	private void registrarCuenta(HttpServletRequest request, HttpServletResponse response, HttpSession session)
			throws IOException {
		String password = valor(request, "password");
		if (!password.equals(valor(request, "passwordConfirmation"))) {
			throw new IllegalArgumentException("Las contraseñas no coinciden.");
		}
		Usuario usuario = new Usuario(valor(request, "username"), valor(request, "name"),
				valor(request, "email"), valor(request, "phone"));
		accountStore.registrarUsuario(usuario, password.toCharArray(), servicio);
		boolean alreadySignedIn = session.getAttribute(SESSION_USER) != null;
		flash(session, "flashSuccess", alreadySignedIn
				? "La cuenta de usuario fue creada."
				: "Cuenta creada. Ya puedes iniciar sesión como usuario normal.");
		redirect(request, response, alreadySignedIn ? "users" : null);
	}

	private void ejecutarAccion(String action, HttpServletRequest request, HttpSession session, Sesion sesion)
			throws IOException, InterruptedException {
		switch (action == null ? "" : action) {
			case "logout" -> {
				session.invalidate();
			}
			case "search" -> buscarOpenLibrary(request, session);
			case "favorite" -> guardarFavorito(request, session, sesion);
			case "importFavorite" -> importarFavorito(request, sesion);
			case "borrowExternal" -> prestarOpenLibrary(request, session, sesion);
			case "borrowFavorite" -> prestarFavorito(request, sesion);
			case "borrowLocal" -> prestarLibroLocal(request, sesion);
			case "createLoan" -> crearPrestamo(request, sesion);
			case "returnLoan" -> servicio.devolver(sesion, valor(request, "loanId"));
			case "saveBook" -> guardarLibro(request, sesion);
			case "toggleBook" -> alternarLibro(request, sesion);
			case "saveUser" -> guardarUsuario(request, sesion);
			case "deleteUser" -> eliminarUsuario(request, sesion);
			default -> throw new IllegalArgumentException("La operación solicitada no es válida.");
		}
	}

	private void buscarOpenLibrary(HttpServletRequest request, HttpSession session)
			throws IOException, InterruptedException {
		String query = Validaciones.requerido(request.getParameter("query"), "El término de búsqueda");
		List<LibroOpenLibrary> books = openLibraryClient.buscar(query);
		session.setAttribute(SESSION_BOOKS, books);
		session.setAttribute(SESSION_QUERY, query);
		flash(session, "flashSuccess", books.isEmpty()
				? "No se encontraron libros para \"" + query + "\"."
				: "Resultados para \"" + query + "\": " + books.size() + " libros.");
	}

	private void guardarFavorito(HttpServletRequest request, HttpSession session, Sesion sesion) throws IOException {
		LibroOpenLibrary book = libroOpenLibrary(session, request);
		FavoritoLibro favorite = favoritosStore.guardar(sesion.getNombreAcceso(), book);
		flash(session, "flashSuccess", "Libro guardado en Favoritos: " + favorite.id() + ".");
	}

	private void importarFavorito(HttpServletRequest request, Sesion sesion) throws IOException {
		FavoritoLibro favorite = favoritosStore.buscar(sesion.getNombreAcceso(), valor(request, "bookId"));
		if (favorite == null) {
			throw new IllegalArgumentException("El favorito seleccionado ya no existe.");
		}
		servicio.guardarLibroOpenLibrary(sesion, favorite.comoLibroOpenLibrary());
	}

	private void prestarOpenLibrary(HttpServletRequest request, HttpSession session, Sesion sesion) throws IOException {
		LibroOpenLibrary book = libroOpenLibrary(session, request);
		String userId = usuarioPrestamo(request, sesion);
		int days = Validaciones.entero(request.getParameter("days"), "Días de préstamo", 1, 3650);
		servicio.prestarOpenLibrary(sesion, book, nuevoIdPrestamo(), userId, days);
	}

	private void prestarFavorito(HttpServletRequest request, Sesion sesion) throws IOException {
		FavoritoLibro favorite = favoritosStore.buscar(sesion.getNombreAcceso(), valor(request, "bookId"));
		if (favorite == null) {
			throw new IllegalArgumentException("El favorito seleccionado ya no existe.");
		}
		int days = Validaciones.entero(request.getParameter("days"), "Días de préstamo", 1, 3650);
		servicio.prestarOpenLibrary(sesion, favorite.comoLibroOpenLibrary(), nuevoIdPrestamo(),
				usuarioPrestamo(request, sesion), days);
	}

	private void prestarLibroLocal(HttpServletRequest request, Sesion sesion) throws IOException {
		String bookId = valor(request, "bookId");
		Libro book = servicio.buscarLibro(sesion, bookId);
		if (book == null) {
			throw new IllegalArgumentException("El libro no existe o no está disponible para esta cuenta.");
		}
		int days = Validaciones.entero(request.getParameter("days"), "Días de préstamo", 1, 3650);
		servicio.prestar(sesion, nuevoIdPrestamo(), bookId, usuarioPrestamo(request, sesion), days);
	}

	private void crearPrestamo(HttpServletRequest request, Sesion sesion) throws IOException {
		String loanId = request.getParameter("loanId");
		if (loanId == null || loanId.isBlank()) {
			loanId = nuevoIdPrestamo();
		}
		int days = Validaciones.entero(request.getParameter("days"), "Días de préstamo", 1, 3650);
		servicio.prestar(sesion, loanId, valor(request, "bookId"), usuarioPrestamo(request, sesion), days);
	}

	private void guardarLibro(HttpServletRequest request, Sesion sesion) throws IOException {
		String id = valor(request, "bookId");
		String title = valor(request, "title");
		Autor author = new Autor(valor(request, "authorId"), valor(request, "authorName"));
		String category = valor(request, "category");
		int total = Validaciones.entero(request.getParameter("total"), "Ejemplares", 1, 10000);
		String description = request.getParameter("description");
		if ("create".equals(request.getParameter("bookMode"))) {
			servicio.crearLibro(sesion, new Libro(id, title, author, category, total, total, description));
			return;
		}
		if (!"edit".equals(request.getParameter("bookMode"))) {
			throw new IllegalArgumentException("La operación del libro no es válida.");
		}
		Libro previous = servicio.buscarLibro(sesion, id);
		if (previous == null) {
			throw new IllegalArgumentException("El libro no existe.");
		}
		Libro updated = new Libro(previous.getId(), previous.getTitulo(), previous.getAutor(),
				previous.getCategoria(), previous.getTotalEjemplares(), previous.getDisponibles(),
				previous.getDescripcion());
		updated.actualizarDatos(title, author, category, total, description);
		servicio.actualizarLibro(sesion, updated);
	}

	private void alternarLibro(HttpServletRequest request, Sesion sesion) throws IOException {
		String id = valor(request, "bookId");
		Libro book = servicio.buscarLibro(sesion, id);
		if (book == null) {
			throw new IllegalArgumentException("El libro no existe.");
		}
		if (book.isVisible()) {
			servicio.eliminarLibro(sesion, id);
		} else {
			servicio.restaurarLibro(sesion, id);
		}
	}

	private void guardarUsuario(HttpServletRequest request, Sesion sesion) throws IOException {
		String id = valor(request, "userId");
		servicio.actualizarUsuario(sesion, new Usuario(id, valor(request, "name"),
				valor(request, "email"), valor(request, "phone")));
	}

	private void eliminarUsuario(HttpServletRequest request, Sesion sesion) throws IOException {
		String userId = valor(request, "userId");
		servicio.eliminarUsuario(sesion, userId);
		accountStore.eliminarCuentaUsuario(userId);
	}

	private String usuarioPrestamo(HttpServletRequest request, Sesion sesion) {
		return sesion.getRol() == Rol.USUARIO ? sesion.getUsuarioId() : valor(request, "userId");
	}

	private LibroOpenLibrary libroOpenLibrary(HttpSession session, HttpServletRequest request) {
		Object stored = session.getAttribute(SESSION_BOOKS);
		if (!(stored instanceof List<?> results)) {
			throw new IllegalArgumentException("Vuelve a buscar el libro antes de realizar esta operación.");
		}
		int index = Validaciones.entero(request.getParameter("bookIndex"), "Libro", 0, results.size() - 1);
		Object result = results.get(index);
		if (!(result instanceof LibroOpenLibrary book)) {
			throw new IllegalStateException("El resultado de búsqueda guardado no es válido.");
		}
		return book;
	}

	private List<BookView> librosVista(List<Libro> books) {
		return books.stream().map(BookView::new).toList();
	}

	private List<FavoriteView> favoritosVista(Sesion sesion) {
		return favoritosStore.listar(sesion.getNombreAcceso()).stream()
				.filter(favorite -> !servicio.estaOculto(favorite.id()) || sesion.getRol() == Rol.ADMINISTRADOR)
				.map(FavoriteView::new).toList();
	}

	private List<OpenBookView> librosOpenLibraryVista(HttpSession session, Sesion sesion) {
		Object stored = session.getAttribute(SESSION_BOOKS);
		if (!(stored instanceof List<?> results)) {
			return List.of();
		}
		List<OpenBookView> books = new ArrayList<>();
		for (int index = 0; index < results.size(); index++) {
			if (results.get(index) instanceof LibroOpenLibrary book) {
				boolean hidden = servicio.estaOculto(book.generarIdBiblioteca());
				if (hidden && sesion.getRol() != Rol.ADMINISTRADOR) {
					continue;
				}
				Libro local = servicio.buscarLibro(sesion, book.generarIdBiblioteca());
				books.add(new OpenBookView(index, book, local, hidden));
			}
		}
		return List.copyOf(books);
	}

	private boolean tabPermitida(String tab, Sesion sesion) {
		return switch (tab == null ? "" : tab) {
			case "home", "catalog" -> true;
			case "users" -> sesion.tienePermiso(Permiso.GESTIONAR_USUARIOS);
			case "loans" -> sesion.tienePermiso(Permiso.GESTIONAR_PRESTAMOS)
					|| sesion.tienePermiso(Permiso.GESTIONAR_SUS_PRESTAMOS);
			default -> false;
		};
	}

	private String tabDestino(String action) {
		return switch (action == null ? "" : action) {
			case "saveBook", "toggleBook", "favorite", "importFavorite", "borrowLocal" -> "catalog";
			case "saveUser", "deleteUser" -> "users";
			case "createLoan", "returnLoan", "borrowFavorite" -> "loans";
			case "borrowExternal" -> "home";
			case "search" -> "home";
			default -> null;
		};
	}

	private String csrfToken(HttpSession session) {
		Object token = session.getAttribute(SESSION_TOKEN);
		if (token instanceof String value) {
			return value;
		}
		String value = nuevoToken();
		session.setAttribute(SESSION_TOKEN, value);
		return value;
	}

	private String nuevoToken() {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private boolean tokenValido(HttpServletRequest request, HttpSession session) {
		Object expected = session.getAttribute(SESSION_TOKEN);
		String supplied = request.getParameter("_csrf");
		return expected instanceof String token && token.equals(supplied);
	}

	private void consumeFlash(HttpSession session, HttpServletRequest request, String sessionKey, String requestKey) {
		Object value = session.getAttribute(sessionKey);
		if (value != null) {
			request.setAttribute(requestKey, value);
			session.removeAttribute(sessionKey);
		}
	}

	private void flash(HttpSession session, String key, String message) {
		session.setAttribute(key, message);
	}

	private void redirect(HttpServletRequest request, HttpServletResponse response, String tab) throws IOException {
		String location = request.getContextPath() + "/app";
		if (tab != null) {
			location += "?tab=" + tab;
		}
		response.sendRedirect(response.encodeRedirectURL(location));
	}

	private String valor(HttpServletRequest request, String name) {
		String value = request.getParameter(name);
		return value == null ? "" : value.trim();
	}

	private String mensaje(Exception exception) {
		return exception.getMessage() == null || exception.getMessage().isBlank()
				? exception.getClass().getSimpleName() : exception.getMessage();
	}

	private static String urlOpenLibrarySegura(String value) {
		if (value == null) {
			return null;
		}
		try {
			URI uri = URI.create(value);
			return "https".equalsIgnoreCase(uri.getScheme())
					&& "openlibrary.org".equalsIgnoreCase(uri.getHost())
					&& uri.getPort() == -1 && uri.getUserInfo() == null
					&& uri.getPath() != null && uri.getPath().matches("/works/[A-Za-z0-9._-]+")
					? uri.toString() : null;
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	private String nuevoIdPrestamo() {
		return "PRE-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
	}

	public static final class BookView {
		private final String id;
		private final String title;
		private final String authorId;
		private final String authorName;
		private final String category;
		private final String description;
		private final int total;
		private final int available;
		private final boolean visible;

		private BookView(Libro book) {
			id = book.getId();
			title = book.getTitulo();
			authorId = book.getAutor().id();
			authorName = book.getAutor().nombre();
			category = book.getCategoria();
			description = book.getDescripcion();
			total = book.getTotalEjemplares();
			available = book.getDisponibles();
			visible = book.isVisible();
		}

		public String getId() { return id; }
		public String getTitle() { return title; }
		public String getAuthorId() { return authorId; }
		public String getAuthorName() { return authorName; }
		public String getCategory() { return category; }
		public String getDescription() { return description; }
		public int getTotal() { return total; }
		public int getAvailable() { return available; }
		public boolean isVisible() { return visible; }
	}

	public static final class FavoriteView {
		private final String id;
		private final String title;
		private final String authors;
		private final String year;
		private final String coverUrl;
		private final String openLibraryUrl;

		private FavoriteView(FavoritoLibro favorite) {
			id = favorite.id();
			title = favorite.titulo();
			authors = favorite.mostrarAutores();
			year = favorite.anioPublicacion() == null ? "No disponible" : favorite.anioPublicacion().toString();
			coverUrl = favorite.urlPortada();
			openLibraryUrl = urlOpenLibrarySegura(favorite.urlOpenLibrary());
		}

		public String getId() { return id; }
		public String getTitle() { return title; }
		public String getAuthors() { return authors; }
		public String getYear() { return year; }
		public String getCoverUrl() { return coverUrl; }
		public String getOpenLibraryUrl() { return openLibraryUrl; }
	}

	public static final class OpenBookView {
		private final int index;
		private final String id;
		private final String title;
		private final String authors;
		private final String year;
		private final String description;
		private final String coverUrl;
		private final String openLibraryUrl;
		private final int available;
		private final int total;
		private final boolean hidden;

		private OpenBookView(int index, LibroOpenLibrary book, Libro local, boolean hidden) {
			this.index = index;
			id = book.generarIdBiblioteca();
			title = book.titulo();
			authors = book.mostrarAutores();
			year = book.anioPublicacion() == null ? "Año no disponible" : book.anioPublicacion().toString();
			description = book.descripcion().isBlank() ? "Descripción no disponible" : book.descripcion();
			coverUrl = book.urlPortada();
			openLibraryUrl = urlOpenLibrarySegura(book.urlOpenLibrary());
			available = local == null ? 3 : local.getDisponibles();
			total = local == null ? 3 : local.getTotalEjemplares();
			this.hidden = hidden;
		}

		public int getIndex() { return index; }
		public String getId() { return id; }
		public String getTitle() { return title; }
		public String getAuthors() { return authors; }
		public String getYear() { return year; }
		public String getDescription() { return description; }
		public String getCoverUrl() { return coverUrl; }
		public String getOpenLibraryUrl() { return openLibraryUrl; }
		public int getAvailable() { return available; }
		public int getTotal() { return total; }
		public boolean isHidden() { return hidden; }
	}

}
