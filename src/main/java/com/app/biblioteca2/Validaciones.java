package com.app.biblioteca2;

import java.util.regex.Pattern;

final class Validaciones {
	private static final Pattern IDENTIFICADOR = Pattern.compile("[\\p{L}\\p{N}][\\p{L}\\p{N}._-]*");
	private static final Pattern NOMBRE = Pattern.compile("[\\p{L}\\p{M}][\\p{L}\\p{M} .'-]*");
	private static final Pattern CORREO = Pattern.compile(
			"[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+");
	private static final Pattern TELEFONO = Pattern.compile("\\+?[0-9][0-9 ()-]{5,18}[0-9]");

	private Validaciones() {
	}

	static String requerido(String valor, String campo) {
		if (valor == null || valor.isBlank()) {
			throw new IllegalArgumentException(campo + " es obligatorio.");
		}
		return valor.trim();
	}

	static String identificador(String valor, String campo) {
		String normalizado = requerido(valor, campo);
		if (!IDENTIFICADOR.matcher(normalizado).matches()) {
			throw new IllegalArgumentException(campo + " solo admite letras, números, puntos, guiones y guiones bajos.");
		}
		return normalizado;
	}

	static String descriptivo(String valor, String campo) {
		String normalizado = requerido(valor, campo);
		if (normalizado.matches("[+-]?\\d+(?:[.,]\\d+)?")
				|| normalizado.indexOf('|') >= 0
				|| normalizado.chars().anyMatch(Character::isISOControl)) {
			throw new IllegalArgumentException(campo + " no es válido.");
		}
		return normalizado;
	}

	static String tituloLibro(String valor) {
		String normalizado = requerido(valor, "El título");
		if (normalizado.indexOf('|') >= 0 || normalizado.chars().anyMatch(Character::isISOControl)) {
			throw new IllegalArgumentException("El título contiene caracteres no permitidos.");
		}
		return normalizado;
	}

	static String descripcionLibro(String valor) {
		String normalizado = requerido(valor, "La descripción");
		if (normalizado.indexOf('|') >= 0 || normalizado.chars().anyMatch(Character::isISOControl)) {
			throw new IllegalArgumentException("La descripción contiene caracteres no permitidos.");
		}
		return normalizado;
	}

	static String nombre(String valor, String campo) {
		String normalizado = requerido(valor, campo);
		if (!NOMBRE.matcher(normalizado).matches()) {
			throw new IllegalArgumentException(campo + " debe contener un nombre válido.");
		}
		return normalizado;
	}

	static String correo(String valor) {
		String normalizado = requerido(valor, "El correo");
		if (!CORREO.matcher(normalizado).matches()) {
			throw new IllegalArgumentException("El correo no tiene un formato válido.");
		}
		return normalizado;
	}

	static String telefono(String valor) {
		String normalizado = requerido(valor, "El teléfono");
		long digitos = normalizado.chars().filter(Character::isDigit).count();
		if (!TELEFONO.matcher(normalizado).matches() || digitos < 7 || digitos > 15) {
			throw new IllegalArgumentException("El teléfono debe contener entre 7 y 15 dígitos.");
		}
		return normalizado;
	}

	static int entero(String valor, String campo, int minimo, int maximo) {
		String normalizado = requerido(valor, campo);
		try {
			int numero = Integer.parseInt(normalizado);
			if (numero < minimo || numero > maximo) {
				throw new IllegalArgumentException(campo + " debe estar entre " + minimo + " y " + maximo + ".");
			}
			return numero;
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException(campo + " debe ser un número entero.");
		}
	}
}
