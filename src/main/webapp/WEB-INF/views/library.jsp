<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="es">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Biblioteca UFG</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"
          integrity="sha384-QWTKZyjpPEjISv5WaRU9OFeRpok6YctnYmDr5pNlyT2bRjXh0JMhjY6hW+ALEwIH" crossorigin="anonymous">
    <link href="${pageContext.request.contextPath}/assets/css/library.css" rel="stylesheet">
</head>
<body>
<nav class="navbar navbar-expand-lg navbar-dark app-navbar">
    <div class="container">
        <a class="navbar-brand fw-semibold" href="${pageContext.request.contextPath}/app">Biblioteca UFG</a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#mainNav"
                aria-controls="mainNav" aria-expanded="false" aria-label="Mostrar navegación">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="mainNav">
            <div class="navbar-nav me-auto">
                <a class="nav-link" href="${pageContext.request.contextPath}/app?tab=home">Inicio</a>
                <a class="nav-link" href="${pageContext.request.contextPath}/app?tab=catalog">Catálogo y favoritos</a>
                <c:if test="${roleName == 'ADMINISTRADOR'}">
                    <a class="nav-link" href="${pageContext.request.contextPath}/app?tab=users">Usuarios</a>
                </c:if>
                <a class="nav-link" href="${pageContext.request.contextPath}/app?tab=loans">Préstamos</a>
            </div>
            <div class="d-flex align-items-center gap-3 text-white">
                <span class="small"><c:out value="${sesion.rol}"/> · <c:out value="${sesion.nombre}"/></span>
                <form method="post" action="${pageContext.request.contextPath}/app" class="m-0">
                    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                    <input type="hidden" name="action" value="logout">
                    <button class="btn btn-outline-light btn-sm" type="submit">Cerrar sesión</button>
                </form>
            </div>
        </div>
    </div>
</nav>

<main class="container py-4">
    <c:if test="${not empty error}">
        <div class="alert alert-danger alert-dismissible fade show" role="alert">
            <c:out value="${error}"/>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Cerrar"></button>
        </div>
    </c:if>
    <c:if test="${not empty success}">
        <div class="alert alert-success alert-dismissible fade show" role="status">
            <c:out value="${success}"/>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Cerrar"></button>
        </div>
    </c:if>

    <c:if test="${activeTab == 'home'}">
        <section class="page-heading mb-4">
            <p class="eyebrow mb-2">DESCUBRE TU PRÓXIMA LECTURA</p>
            <h1 class="h2 mb-2">Explora Open Library</h1>
            <p class="text-secondary mb-0">Busca por título o tema. Las portadas y los datos bibliográficos vienen de Open Library.</p>
        </section>
        <form class="search-panel mb-4" method="post" action="${pageContext.request.contextPath}/app">
            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
            <input type="hidden" name="action" value="search">
            <label class="visually-hidden" for="query">Título o tema</label>
            <input class="form-control form-control-lg" id="query" name="query"
                   value="<c:out value='${openQuery}'/>" placeholder="Escribe el título o tema de un libro" required>
            <button class="btn btn-primary btn-lg" type="submit">Buscar libros</button>
        </form>
        <div class="row row-cols-1 row-cols-sm-2 row-cols-lg-3 row-cols-xl-4 g-3">
            <c:forEach items="${openBooks}" var="book">
                <div class="col">
                    <article class="card book-card h-100">
                        <div class="book-cover">
                            <c:choose>
                                <c:when test="${not empty book.coverUrl}">
                                    <img src="<c:out value='${book.coverUrl}'/>" alt="Portada de <c:out value='${book.title}'/>"
                                         loading="lazy" onerror="this.style.display='none';this.nextElementSibling.hidden=false">
                                    <span hidden>Portada no disponible</span>
                                </c:when>
                                <c:otherwise><span>Sin portada disponible</span></c:otherwise>
                            </c:choose>
                        </div>
                        <div class="card-body d-flex flex-column">
                            <h2 class="h5"><c:out value="${book.title}"/></h2>
                            <p class="small text-secondary mb-1"><c:out value="${book.authors}"/></p>
                            <p class="small text-secondary"><c:out value="${book.year}"/></p>
                            <p class="book-description"><c:out value="${book.description}"/></p>
                            <p class="small mb-3">ID de biblioteca: <code><c:out value="${book.id}"/></code></p>
                            <c:choose>
                                <c:when test="${book.hidden}">
                                    <p class="small text-danger">Oculto por el administrador</p>
                                </c:when>
                                <c:otherwise>
                                    <p class="small text-secondary">Disponibles: <c:out value="${book.available}"/> / <c:out value="${book.total}"/></p>
                                    <form method="post" action="${pageContext.request.contextPath}/app" class="mt-auto">
                                        <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                                        <input type="hidden" name="action" value="borrowExternal">
                                        <input type="hidden" name="bookIndex" value="<c:out value='${book.index}'/>">
                                        <div class="input-group input-group-sm mb-2">
                                            <span class="input-group-text">Días</span>
                                            <input class="form-control" type="number" name="days" min="1" max="3650" value="7" required>
                                        </div>
                                        <c:if test="${roleName != 'USUARIO'}">
                                            <input class="form-control form-control-sm mb-2" name="userId"
                                                   placeholder="ID del usuario" required>
                                        </c:if>
                                        <button class="btn btn-primary btn-sm w-100" type="submit"
                                                <c:if test="${book.available == 0}">disabled</c:if>>Prestar libro</button>
                                    </form>
                                </c:otherwise>
                            </c:choose>
                            <form method="post" action="${pageContext.request.contextPath}/app" class="mt-2">
                                <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                                <input type="hidden" name="action" value="favorite">
                                <input type="hidden" name="bookIndex" value="<c:out value='${book.index}'/>">
                                <button class="btn btn-outline-primary btn-sm w-100" type="submit">Guardar en Favoritos</button>
                            </form>
                            <c:if test="${not empty book.openLibraryUrl}">
                                <a class="small mt-2 text-center" href="<c:out value='${book.openLibraryUrl}'/>"
                                   target="_blank" rel="noopener noreferrer">Ver en Open Library</a>
                            </c:if>
                        </div>
                    </article>
                </div>
            </c:forEach>
        </div>
        <c:if test="${empty openBooks}">
            <div class="empty-state mt-4">
                <div class="display-6 mb-2">⌕</div>
                <h2 class="h5">Comienza una búsqueda</h2>
                <p class="text-secondary mb-0">Escribe un título o tema para encontrar libros en Open Library.</p>
            </div>
        </c:if>
    </c:if>

    <c:if test="${activeTab == 'catalog'}">
        <section class="page-heading mb-4">
            <p class="eyebrow mb-2">TU BIBLIOTECA</p>
            <h1 class="h2 mb-2">Catálogo local y favoritos</h1>
            <p class="text-secondary mb-0">Los libros importados desde Open Library se guardan en el catálogo local al prestarlos o importarlos.</p>
        </section>
        <c:if test="${roleName == 'MODERADOR' || roleName == 'ADMINISTRADOR'}">
            <div class="card border-0 shadow-sm mb-4">
                <div class="card-body">
                    <h2 class="h5">Agregar libro al catálogo</h2>
                    <form method="post" action="${pageContext.request.contextPath}/app" class="row g-2 align-items-end">
                        <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                        <input type="hidden" name="action" value="saveBook">
                        <input type="hidden" name="bookMode" value="create">
                        <div class="col-sm-3"><label class="form-label">ID</label><input class="form-control" name="bookId" required></div>
                        <div class="col-sm-3"><label class="form-label">Título</label><input class="form-control" name="title" required></div>
                        <div class="col-sm-2"><label class="form-label">ID autor</label><input class="form-control" name="authorId" required></div>
                        <div class="col-sm-2"><label class="form-label">Autor</label><input class="form-control" name="authorName" required></div>
                        <div class="col-sm-2"><label class="form-label">Categoría</label><input class="form-control" name="category" required></div>
                        <div class="col-sm-2"><label class="form-label">Ejemplares</label><input class="form-control" type="number" name="total" min="1" max="10000" value="3" required></div>
                        <div class="col-sm-8"><label class="form-label">Descripción</label><input class="form-control" name="description"></div>
                        <div class="col-sm-2"><button class="btn btn-primary w-100" type="submit">Agregar libro</button></div>
                    </form>
                </div>
            </div>
        </c:if>

        <div class="d-flex justify-content-between align-items-center mb-3">
            <h2 class="h4 mb-0">Libros del catálogo</h2><span class="badge text-bg-light"><c:out value="${catalogo.size()}"/> libros</span>
        </div>
        <div class="row row-cols-1 g-3 mb-5">
            <c:forEach items="${catalogo}" var="book">
                <div class="col">
                    <article class="card border-0 shadow-sm">
                        <div class="card-body">
                            <div class="d-flex flex-wrap justify-content-between gap-2">
                                <div>
                                    <h3 class="h5 mb-1"><c:out value="${book.title}"/></h3>
                                    <p class="small text-secondary mb-2"><c:out value="${book.authorName}"/> · <c:out value="${book.category}"/>
                                        · ID <code><c:out value="${book.id}"/></code></p>
                                    <p class="mb-2"><c:out value="${book.description}"/></p>
                                </div>
                                <div class="text-end">
                                    <span class="badge ${book.available > 0 ? 'text-bg-success' : 'text-bg-secondary'}">
                                        <c:out value="${book.available}"/> / <c:out value="${book.total}"/> disponibles
                                    </span>
                                    <c:if test="${not book.visible}"><span class="badge text-bg-warning">Oculto</span></c:if>
                                </div>
                            </div>
                            <div class="row g-2 align-items-end mt-2">
                                <c:if test="${roleName == 'MODERADOR' || roleName == 'ADMINISTRADOR'}">
                                    <div class="col-12">
                                        <details>
                                            <summary class="small text-primary">Editar datos del libro</summary>
                                            <form method="post" action="${pageContext.request.contextPath}/app" class="row g-2 mt-1">
                                                <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                                                <input type="hidden" name="action" value="saveBook">
                                                <input type="hidden" name="bookMode" value="edit">
                                                <input type="hidden" name="bookId" value="<c:out value='${book.id}'/>">
                                                <div class="col-sm-3"><label class="form-label">Título</label><input class="form-control" name="title" value="<c:out value='${book.title}'/>" required></div>
                                                <div class="col-sm-2"><label class="form-label">ID autor</label><input class="form-control" name="authorId" value="<c:out value='${book.authorId}'/>" required></div>
                                                <div class="col-sm-2"><label class="form-label">Autor</label><input class="form-control" name="authorName" value="<c:out value='${book.authorName}'/>" required></div>
                                                <div class="col-sm-2"><label class="form-label">Categoría</label><input class="form-control" name="category" value="<c:out value='${book.category}'/>" required></div>
                                                <div class="col-sm-1"><label class="form-label">Total</label><input class="form-control" type="number" name="total" min="1" max="10000" value="<c:out value='${book.total}'/>" required></div>
                                                <div class="col-sm-10"><label class="form-label">Descripción</label><input class="form-control" name="description" value="<c:out value='${book.description}'/>"></div>
                                                <div class="col-sm-2"><button class="btn btn-outline-primary w-100" type="submit">Guardar</button></div>
                                            </form>
                                        </details>
                                    </div>
                                </c:if>
                                <c:if test="${roleName == 'ADMINISTRADOR'}">
                                    <div class="col-auto">
                                        <form method="post" action="${pageContext.request.contextPath}/app">
                                            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                                            <input type="hidden" name="action" value="toggleBook">
                                            <input type="hidden" name="bookId" value="<c:out value='${book.id}'/>">
                                            <button class="btn btn-outline-warning btn-sm" type="submit">
                                                <c:choose><c:when test="${book.visible}">Ocultar</c:when><c:otherwise>Restaurar</c:otherwise></c:choose>
                                            </button>
                                        </form>
                                    </div>
                                </c:if>
                                <c:if test="${book.visible}">
                                    <div class="col-sm-5 col-md-4">
                                        <form method="post" action="${pageContext.request.contextPath}/app" class="d-flex gap-2">
                                            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                                            <input type="hidden" name="action" value="borrowLocal">
                                            <input type="hidden" name="bookId" value="<c:out value='${book.id}'/>">
                                            <input class="form-control form-control-sm" type="number" name="days" min="1" max="3650" value="7" aria-label="Días de préstamo" required>
                                            <c:if test="${roleName != 'USUARIO'}">
                                                <input class="form-control form-control-sm" name="userId" placeholder="ID usuario" aria-label="ID de usuario" required>
                                            </c:if>
                                            <button class="btn btn-primary btn-sm text-nowrap" type="submit"
                                                    <c:if test="${book.available == 0}">disabled</c:if>>Prestar</button>
                                        </form>
                                    </div>
                                </c:if>
                            </div>
                        </div>
                    </article>
                </div>
            </c:forEach>
        </div>

        <div class="d-flex justify-content-between align-items-center mb-3">
            <h2 class="h4 mb-0">Mis favoritos</h2><span class="badge text-bg-light"><c:out value="${favoritos.size()}"/> guardados</span>
        </div>
        <div class="row row-cols-1 row-cols-sm-2 row-cols-lg-3 g-3">
            <c:forEach items="${favoritos}" var="favorite">
                <div class="col">
                    <article class="card book-card h-100">
                        <div class="book-cover">
                            <c:choose>
                                <c:when test="${not empty favorite.coverUrl}">
                                    <img src="<c:out value='${favorite.coverUrl}'/>" alt="Portada de <c:out value='${favorite.title}'/>" loading="lazy">
                                </c:when>
                                <c:otherwise><span>Sin portada disponible</span></c:otherwise>
                            </c:choose>
                        </div>
                        <div class="card-body">
                            <h3 class="h5"><c:out value="${favorite.title}"/></h3>
                            <p class="small text-secondary"><c:out value="${favorite.authors}"/> · <c:out value="${favorite.year}"/></p>
                            <p class="small">ID: <code><c:out value="${favorite.id}"/></code></p>
                            <c:if test="${roleName == 'MODERADOR' || roleName == 'ADMINISTRADOR'}">
                                <form method="post" action="${pageContext.request.contextPath}/app" class="mb-2">
                                    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                                    <input type="hidden" name="action" value="importFavorite">
                                    <input type="hidden" name="bookId" value="<c:out value='${favorite.id}'/>">
                                    <button class="btn btn-outline-primary btn-sm w-100" type="submit">Importar al catálogo (3 ejemplares)</button>
                                </form>
                            </c:if>
                            <form method="post" action="${pageContext.request.contextPath}/app" class="row g-2">
                                <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                                <input type="hidden" name="action" value="borrowFavorite">
                                <input type="hidden" name="bookId" value="<c:out value='${favorite.id}'/>">
                                <div class="col-4"><input class="form-control form-control-sm" type="number" name="days" min="1" max="3650" value="7" aria-label="Días de préstamo" required></div>
                                <c:if test="${roleName != 'USUARIO'}">
                                    <div class="col-5"><input class="form-control form-control-sm" name="userId" placeholder="ID usuario" aria-label="ID de usuario" required></div>
                                </c:if>
                                <div class="col"><button class="btn btn-primary btn-sm w-100" type="submit">Prestar</button></div>
                            </form>
                            <c:if test="${not empty favorite.openLibraryUrl}">
                                <a class="small d-block text-center mt-2" href="<c:out value='${favorite.openLibraryUrl}'/>"
                                   target="_blank" rel="noopener noreferrer">Ver en Open Library</a>
                            </c:if>
                        </div>
                    </article>
                </div>
            </c:forEach>
        </div>
        <c:if test="${empty favoritos}"><p class="text-secondary">Todavía no has guardado favoritos. Puedes agregarlos desde Inicio.</p></c:if>
    </c:if>

    <c:if test="${activeTab == 'users'}">
        <section class="page-heading mb-4">
            <p class="eyebrow mb-2">ADMINISTRACIÓN</p><h1 class="h2">Usuarios</h1>
            <p class="text-secondary">Gestiona las cuentas de la biblioteca. Los usuarios con préstamos activos no se pueden eliminar.</p>
        </section>
        <div class="card border-0 shadow-sm mb-4">
            <div class="card-body">
                <h2 class="h5">Crear cuenta de usuario</h2>
                <form method="post" action="${pageContext.request.contextPath}/app" class="row g-2 align-items-end">
                    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                    <input type="hidden" name="action" value="register">
                    <div class="col-sm-2"><label class="form-label">Usuario</label><input class="form-control" name="username" required></div>
                    <div class="col-sm-2"><label class="form-label">Nombre</label><input class="form-control" name="name" required></div>
                    <div class="col-sm-2"><label class="form-label">Correo</label><input class="form-control" type="email" name="email" required></div>
                    <div class="col-sm-2"><label class="form-label">Teléfono</label><input class="form-control" name="phone" required></div>
                    <div class="col-sm-2"><label class="form-label">Contraseña</label><input class="form-control" type="password" name="password" minlength="4" required></div>
                    <div class="col-sm-2"><label class="form-label">Confirmar</label><input class="form-control" type="password" name="passwordConfirmation" minlength="4" required></div>
                    <div class="col-12"><button class="btn btn-primary" type="submit">Crear cuenta normal</button></div>
                </form>
            </div>
        </div>
        <div class="table-responsive card border-0 shadow-sm">
            <table class="table align-middle mb-0">
                <thead><tr><th>ID</th><th>Nombre y datos</th><th>Actualizar</th><th>Eliminar</th></tr></thead>
                <tbody>
                <c:forEach items="${usuarios}" var="user">
                    <tr>
                        <td><code><c:out value="${user.id}"/></code></td>
                        <td><c:out value="${user.email}"/><br><span class="small text-secondary"><c:out value="${user.telefono}"/></span></td>
                        <td>
                            <form method="post" action="${pageContext.request.contextPath}/app" class="row g-1">
                                <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                                <input type="hidden" name="action" value="saveUser">
                                <input type="hidden" name="userId" value="<c:out value='${user.id}'/>">
                                <div class="col"><input class="form-control form-control-sm" name="name" value="<c:out value='${user.nombre}'/>" aria-label="Nombre" required></div>
                                <div class="col"><input class="form-control form-control-sm" type="email" name="email" value="<c:out value='${user.email}'/>" aria-label="Correo" required></div>
                                <div class="col"><input class="form-control form-control-sm" name="phone" value="<c:out value='${user.telefono}'/>" aria-label="Teléfono" required></div>
                                <div class="col-auto"><button class="btn btn-outline-primary btn-sm" type="submit">Guardar</button></div>
                            </form>
                        </td>
                        <td>
                            <form method="post" action="${pageContext.request.contextPath}/app">
                                <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                                <input type="hidden" name="action" value="deleteUser">
                                <input type="hidden" name="userId" value="<c:out value='${user.id}'/>">
                                <button class="btn btn-outline-danger btn-sm" type="submit">Eliminar</button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </c:if>

    <c:if test="${activeTab == 'loans'}">
        <section class="page-heading mb-4">
            <p class="eyebrow mb-2">SEGUIMIENTO</p><h1 class="h2">Préstamos</h1>
            <p class="text-secondary">Consulta fechas, estado y devoluciones de los libros.</p>
        </section>
        <div class="card border-0 shadow-sm mb-4">
            <div class="card-body">
                <h2 class="h5">Registrar préstamo</h2>
                <form method="post" action="${pageContext.request.contextPath}/app" class="row g-2 align-items-end">
                    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                    <input type="hidden" name="action" value="createLoan">
                    <div class="col-sm-3"><label class="form-label">ID del préstamo (opcional)</label><input class="form-control" name="loanId"></div>
                    <div class="col-sm-3"><label class="form-label">ID del libro</label><input class="form-control" name="bookId" required></div>
                    <c:if test="${roleName != 'USUARIO'}">
                        <div class="col-sm-3"><label class="form-label">ID del usuario</label><input class="form-control" name="userId" required></div>
                    </c:if>
                    <div class="col-sm-2"><label class="form-label">Días</label><input class="form-control" type="number" name="days" min="1" max="3650" value="7" required></div>
                    <div class="col-sm-1"><button class="btn btn-primary w-100" type="submit">Crear</button></div>
                </form>
            </div>
        </div>
        <div class="table-responsive card border-0 shadow-sm">
            <table class="table table-hover align-middle mb-0">
                <thead><tr><th>ID préstamo</th><th>Libro</th><th>Usuario</th><th>Préstamo</th><th>Vencimiento</th><th>Estado</th><th></th></tr></thead>
                <tbody>
                <c:forEach items="${prestamos}" var="loan">
                    <tr>
                        <td><code><c:out value="${loan.id}"/></code></td>
                        <td><c:out value="${loan.libroId}"/></td>
                        <td><c:out value="${loan.usuarioId}"/></td>
                        <td><c:out value="${loan.fechaPrestamo}"/></td>
                        <td><c:out value="${loan.fechaVencimiento}"/></td>
                        <td><c:choose><c:when test="${loan.activo}"><span class="badge text-bg-warning">Activo</span></c:when><c:otherwise><span class="badge text-bg-success">Devuelto</span></c:otherwise></c:choose></td>
                        <td>
                            <c:if test="${loan.activo}">
                                <form method="post" action="${pageContext.request.contextPath}/app">
                                    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                                    <input type="hidden" name="action" value="returnLoan">
                                    <input type="hidden" name="loanId" value="<c:out value='${loan.id}'/>">
                                    <button class="btn btn-outline-primary btn-sm" type="submit">Registrar devolución</button>
                                </form>
                            </c:if>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty prestamos}"><tr><td class="text-center text-secondary py-4" colspan="7">No hay préstamos registrados.</td></tr></c:if>
                </tbody>
            </table>
        </div>
    </c:if>
</main>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"
        integrity="sha384-YvpcrYf0tY3lHB60NNkmXc5s9fDVZLESaAA55NDzOxhy9GkcIdslK1eN7N6jIeHz"
        crossorigin="anonymous"></script>
</body>
</html>
