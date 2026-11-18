# Biblioteca UFG

Aplicación web Java con JSP y Bootstrap para administrar los libros, usuarios y préstamos de una biblioteca.

## Inicio

El proyecto requiere JDK 23 y Apache Tomcat 10.1.61 (Jakarta Servlet 6.0 / JSP 3.1).

### Ejecutar directamente desde IntelliJ IDEA

1. Abre `src\main\java\com\app\biblioteca2\BibliotecaWebApplication.java`.
2. Presiona **Run** en el método `main`.
3. Abre `http://localhost:8080/`.

También se puede iniciar desde la terminal con `.\mvnw.cmd compile exec:java`. El servidor integrado usa el puerto 8080; para cambiarlo, configura `-Dserver.port=8081`.

### Desplegar en Tomcat por separado

1. Construye el WAR con `.\mvnw.cmd clean test package`.
2. Copia `target\BIBLIOTECA2-1.0-SNAPSHOT.war` al directorio `webapps` de Tomcat.
3. Abre `http://localhost:8080/BIBLIOTECA2-1.0-SNAPSHOT/`.

Tomcat utiliza `data` relativo al directorio de trabajo como almacenamiento predeterminado. Para elegir una ubicación persistente, define la propiedad JVM `-Dbiblioteca.data.dir=C:\ruta\de\datos`.

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
