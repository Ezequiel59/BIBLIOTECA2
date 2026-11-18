package com.app.biblioteca2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class OpenLibraryClientTest {
	@Test
	void parsesBookMetadataAndCoverWhenProvided() throws IOException {
		String json = """
				{
				  "numFound": 1,
				  "docs": [{
				    "key": "/works/OL123W",
				    "title": "Cien años de soledad",
				    "author_name": ["Gabriel García Márquez"],
				    "first_publish_year": 1967,
				    "cover_i": 12345,
				    "first_sentence": ["En una remota población..."]
				  }]
				}
				""";

		List<LibroOpenLibrary> books = OpenLibraryClient.parsearResultados(json);

		assertEquals(1, books.size());
		LibroOpenLibrary book = books.getFirst();
		assertEquals("Cien años de soledad", book.titulo());
		assertEquals("Gabriel García Márquez", book.mostrarAutores());
		assertEquals(1967, book.anioPublicacion());
		assertEquals("https://covers.openlibrary.org/b/id/12345-M.jpg", book.urlPortada());
		assertEquals("https://openlibrary.org/works/OL123W", book.urlOpenLibrary());
		assertEquals("En una remota población...", book.descripcion());
	}

	@Test
	void acceptsMissingOptionalMetadataAndSkipsDocumentsWithoutTitles() throws IOException {
		String json = """
				{"docs":[{"title":"Libro sin portada"},{"author_name":["Autor sin título"]}]}
				""";

		List<LibroOpenLibrary> books = OpenLibraryClient.parsearResultados(json);

		assertEquals(1, books.size());
		assertNull(books.getFirst().anioPublicacion());
		assertNull(books.getFirst().urlPortada());
		assertEquals("Autor desconocido", books.getFirst().mostrarAutores());
	}

	@Test
	void encodesSearchTermsAsUtf8QueryParameters() {
		String uri = OpenLibraryClient.uriBusqueda("Cien años de soledad & mar").toString();

		assertTrue(uri.startsWith("https://openlibrary.org/search.json?q="));
		assertTrue(uri.contains("Cien+a%C3%B1os+de+soledad+%26+mar"));
	}

	@Test
	void usesTheOpenLibraryWorkKeyAsTheLocalCatalogId() {
		LibroOpenLibrary book = new LibroOpenLibrary("The Jungle Book", List.of("Rudyard Kipling"),
				1893, 3344204, "https://openlibrary.org/works/OL19870W");

		assertEquals("OL19870W", book.generarIdBiblioteca());
	}
}
