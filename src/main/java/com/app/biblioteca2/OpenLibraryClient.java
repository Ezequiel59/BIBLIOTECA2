package com.app.biblioteca2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class OpenLibraryClient {
	private static final int RESULT_LIMIT = 20;
	private static final ObjectMapper JSON = new ObjectMapper();
	private final HttpClient httpClient;

	public OpenLibraryClient() {
		this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
	}

	OpenLibraryClient(HttpClient httpClient) {
		this.httpClient = httpClient;
	}

	public List<LibroOpenLibrary> buscar(String consulta) throws IOException, InterruptedException {
		String query = Validaciones.requerido(consulta, "El término de búsqueda");
		HttpRequest request = HttpRequest.newBuilder(uriBusqueda(query))
				.timeout(Duration.ofSeconds(20))
				.header("Accept", "application/json")
				.header("User-Agent", "BibliotecaUFG/1.0 (web library catalog)")
				.GET()
				.build();
		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() != 200) {
			throw new IOException("Open Library respondió con el estado HTTP " + response.statusCode() + ".");
		}
		return parsearResultados(response.body());
	}

	static URI uriBusqueda(String consulta) {
		String query = URLEncoder.encode(consulta, StandardCharsets.UTF_8);
		return URI.create("https://openlibrary.org/search.json?q=" + query + "&limit=" + RESULT_LIMIT
				+ "&fields=key,title,author_name,first_publish_year,cover_i,first_sentence");
	}

	static List<LibroOpenLibrary> parsearResultados(String json) throws IOException {
		JsonNode root = JSON.readTree(json);
		JsonNode docs = root.path("docs");
		if (!docs.isArray()) {
			throw new IOException("La respuesta de Open Library no contiene una lista de libros válida.");
		}
		List<LibroOpenLibrary> books = new ArrayList<>();
		for (JsonNode doc : docs) {
			String title = texto(doc.path("title"));
			if (title == null || title.isBlank()) {
				continue;
			}
			List<String> authors = new ArrayList<>();
			JsonNode authorNames = doc.path("author_name");
			if (authorNames.isArray()) {
				for (JsonNode author : authorNames) {
					String value = texto(author);
					if (value != null && !value.isBlank()) {
						authors.add(value);
					}
				}
			}
			Integer year = enteroOpcional(doc.path("first_publish_year"));
			Integer cover = enteroOpcional(doc.path("cover_i"));
			String description = descripcion(doc.path("first_sentence"));
			String key = texto(doc.path("key"));
			String openLibraryUrl = key != null && key.startsWith("/works/")
					? "https://openlibrary.org" + key : null;
			books.add(new LibroOpenLibrary(title, authors, year, cover, openLibraryUrl, description));
		}
		return List.copyOf(books);
	}

	private static String texto(JsonNode node) {
		return node.isTextual() ? node.asText() : null;
	}

	private static Integer enteroOpcional(JsonNode node) {
		return node.isIntegralNumber() && node.canConvertToInt() ? node.intValue() : null;
	}

	private static String descripcion(JsonNode node) {
		if (node.isTextual()) {
			return node.asText();
		}
		if (node.isArray() && !node.isEmpty() && node.get(0).isTextual()) {
			return node.get(0).asText();
		}
		return "";
	}
}
