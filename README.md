# Biblioteca UFG

Aplicación web Java con JSP y Bootstrap para administrar los libros, usuarios y préstamos de una biblioteca.

## Tecnologías

- Java 23
- JSP 3.1 y Jakarta Servlet 6.0
- Apache Tomcat 10.1.61 integrado
- Bootstrap 5.3.3
- Maven Wrapper para compilar y ejecutar

## Inicio

Usa un JDK 23 para ejecutar la aplicación.

### Desde Visual Studio Code

1. Abre `src\main\java\com\app\biblioteca2\BibliotecaWebApplication.java`.
2. Presiona **Run** sobre el método `main`. Para ejecutar Java desde VS Code, instala la extensión **Extension Pack for Java**.
3. Abre `http://localhost:8080/`.

También puedes iniciar la aplicación desde PowerShell, en la carpeta del proyecto:

```powershell
.\mvnw.cmd compile exec:java
```

Cuando la terminal indique que Biblioteca UFG está disponible, abre `http://localhost:8080/`. Detén el servidor con **Stop** en VS Code o `Ctrl+C` en la terminal.

El servidor integrado usa el puerto 8080. Para elegir otro puerto, ejecuta `.\mvnw.cmd -Dserver.port=8081 compile exec:java` y abre `http://localhost:8081/`.

Para generar el WAR y las pruebas antes de desplegar en un servidor externo, ejecuta `.\mvnw.cmd clean test package`.

La aplicación guarda sus datos en la carpeta `data` del directorio de trabajo. Para elegir otra ubicación, configura la propiedad JVM `-Dbiblioteca.data.dir=C:\ruta\de\datos`.

Las credenciales se guardan en texto plano en `data/cuentas.json`, tal como se solicitó. La primera ejecución crea estas cuentas:

- Administrador: usuario `admin`, contraseña `1234`.
- Moderador: usuario `moder`, contraseña `1234`.

El registro desde la pantalla de inicio solo crea cuentas normales. No uses estas credenciales ni este almacenamiento para un sistema expuesto o con datos reales: las contraseñas en texto plano no son seguras.

## Funcionalidad

- CRUD de libros, con título, autor, categoría y ejemplares.
- CRUD de usuarios.
- Registro de préstamos y devoluciones; no se prestan ejemplares que no estén disponibles.
- Los usuarios normales consultan el catálogo y solo pueden solicitar o devolver sus propios préstamos.
- Los moderadores pueden gestionar libros y préstamos; solo el administrador puede gestionar cuentas de usuarios.
- Después de iniciar sesión, todos los roles pueden buscar libros en Open Library y ver las portadas disponibles.
- El menú Inicio busca libros en Open Library; Favoritos muestra el catálogo local y los favoritos guardados por la cuenta activa. Los favoritos usan el ID de obra `OL...` que entrega Open Library y se guardan en `data/favoritos.json`.
- Al prestar por primera vez desde Inicio o Favoritos, el título se registra automáticamente en el inventario local usando el ID de Open Library, con 3 ejemplares disponibles. Después, los préstamos, devoluciones y existencias continúan funcionando aunque la API no esté disponible.
- Moderador puede editar los campos del libro local, incluidos título y descripción. Administrador puede ocultar o restaurar libros; los títulos ocultos dejan de aparecer para usuarios y moderadores, pero sus préstamos existentes conservan el historial.
- No se permite eliminar libros o usuarios que tengan préstamos activos.
- Validación de IDs, nombres, correos, teléfonos, cantidades y fechas.

La búsqueda consulta `https://openlibrary.org/search.json` por título o tema. Las portadas se muestran cuando Open Library proporciona un identificador de portada; si no, aparece una indicación de que no está disponible. Se necesita conexión a Internet para buscar y cargar imágenes. Los registros del catálogo y préstamos se guardan en `data/libros.txt`, `data/usuarios.txt` y `data/prestamos.txt`; las credenciales, en `data/cuentas.json`; y los favoritos, en `data/favoritos.json`. Los datos locales se excluyen de Git.
