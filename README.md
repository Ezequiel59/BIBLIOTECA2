# Biblioteca UFG (`BIBLIOTECA2`)

Aplicación web en **Java 23** con **JSP 3.1**, **Jakarta Servlet 6.0**, **Apache Tomcat 10.1.61** integrado y **Bootstrap 5.3.3** para administrar el catálogo de libros, usuarios, favoritos y préstamos.

## Tecnologías

- **Lenguaje:** Java 23
- **Web / Servlets:** JSP 3.1 y Jakarta Servlet 6.0
- **Servidor embebido:** Apache Tomcat 10.1.61
- **Interfaz:** Bootstrap 5.3.3
- **Compilación y ejecución:** Maven Wrapper (`mvnw.cmd`)
- **API externa:** Open Library Search API (`[https://openlibrary.org/search.json](https://openlibrary.org/search.json)`) y portadas

## Funcionalidades

### Catálogo, usuarios y préstamos
- **CRUD de libros:** Título, autor, categoría, descripción y control de ejemplares disponibles.
- **CRUD de usuarios:** Gestión de cuentas con validación de IDs, nombres, correos, teléfonos, cantidades y fechas.
- **Préstamos y devoluciones:** Impide prestar ejemplares sin stock y bloquea la eliminación de libros o usuarios con préstamos activos.
- **Moderación y archivado:**
  - Los **moderadores** pueden gestionar libros y préstamos, y editar campos del libro local (incluidos título y descripción).
  - El **administrador** gestiona cuentas de usuarios y puede ocultar o restaurar libros; los títulos ocultos dejan de mostrarse a usuarios y moderadores, pero conservan su historial de préstamos.
  - Los **usuarios normales** consultan el catálogo y gestionan únicamente sus propios préstamos.

### Integración con Open Library y favoritos
- Tras iniciar sesión, todos los roles pueden buscar libros por título o tema en **Open Library** y consultar las portadas disponibles.
- **Inicio:** Busca títulos en Open Library (requiere conexión a Internet para búsquedas y carga de portadas).
- **Favoritos:** Muestra el catálogo local y los favoritos guardados por la cuenta activa usando el identificador de obra `OL...` de Open Library.
- **Alta automática al prestar:** Al prestar por primera vez un libro desde *Inicio* o *Favoritos*, se registra automáticamente en el inventario local con **3 ejemplares disponibles**, permitiendo seguir operando préstamos, devoluciones y stock incluso sin conexión posterior a la API.

## Requisitos previos

- **JDK 23** instalado y configurado en el sistema.
- *(Opcional)* **Visual Studio Code** con la extensión **Extension Pack for Java**.
- Conexión a Internet para consultas a Open Library y carga de portadas.

## Instalación y ejecución

### Desde Visual Studio Code
1. Abre `src/main/java/com/app/biblioteca2/BibliotecaWebApplication.java`.
2. Ejecuta el método `BibliotecaWebApplication` mediante **Run**.
3. Abre `http://localhost:8080/` en tu navegador. Para detenerlo, usa **Stop** en VS Code.

### Desde PowerShell / terminal
En la raíz del proyecto, ejecuta:

```powershell
.\mvnw.cmd compile exec:java
### Desde PowerShell / terminal
En la raíz del proyecto, ejecuta:
```

- Cuando el servidor esté listo, accede a `http://localhost:8080/`.
- Para detener la ejecución, presiona `Ctrl+C`.

### Opciones adicionales de ejecución
- **Cambiar puerto (por defecto `8080`):**
  ```powershell
  .\mvnw.cmd -Dserver.port=8081 compile exec:java
  ```
- **Ruta personalizada para datos:**
  ```powershell
  .\mvnw.cmd -Dbiblioteca.data.dir=C:\ruta\de\datos compile exec:java
  ```
- **Ejecutar pruebas y empaquetar WAR:**
  ```powershell
  .\mvnw.cmd clean test package
  ```

## Almacenamiento de datos

La aplicación guarda los datos locales en la carpeta `data/` del directorio de trabajo (excluida de Git):

| Archivo | Contenido |
| :--- | :--- |
| `data/libros.txt` | Inventario y datos de libros |
| `data/usuarios.txt` | Registros de usuarios |
| `data/prestamos.txt` | Préstamos, devoluciones e historial |
| `data/cuentas.json` | Cuentas y credenciales |
| `data/favoritos.json` | Favoritos por cuenta (`OL...`) |

### Cuentas iniciales
En la primera ejecución se generan automáticamente:
- **Administrador:** usuario `admin` / contraseña `1234`
- **Moderador:** usuario `moder` / contraseña `1234`

El formulario de registro inicial crea cuentas de usuario normal.

> **Aviso de seguridad:** Las credenciales en `data/cuentas.json` se almacenan en texto plano tal como fue diseñado el proyecto. No utilices estas credenciales ni este mecanismo de almacenamiento en entornos expuestos o con información real.
