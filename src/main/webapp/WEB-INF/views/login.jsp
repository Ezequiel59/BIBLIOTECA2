<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="es">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Biblioteca UFG | Iniciar sesión</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"
          integrity="sha384-QWTKZyjpPEjISv5WaRU9OFeRpok6YctnYmDr5pNlyT2bRjXh0JMhjY6hW+ALEwIH" crossorigin="anonymous">
    <link href="${pageContext.request.contextPath}/assets/css/library.css" rel="stylesheet">
</head>
<body class="login-page">
<main class="container py-5">
    <div class="row justify-content-center align-items-center g-4">
        <div class="col-lg-5">
            <div class="brand-mark mb-3">Biblioteca UFG</div>
            <h1 class="display-5 fw-bold">Un lugar para cada historia.</h1>
            <p class="lead text-secondary">Consulta el catálogo, descubre nuevos libros y lleva el control de tus préstamos.</p>
            <div class="login-illustration" aria-hidden="true">
                <span>📚</span><span>✦</span><span>📖</span>
            </div>
        </div>
        <div class="col-lg-6">
            <c:if test="${not empty error}">
                <div class="alert alert-danger" role="alert"><c:out value="${error}"/></div>
            </c:if>
            <c:if test="${not empty success}">
                <div class="alert alert-success" role="status"><c:out value="${success}"/></div>
            </c:if>
            <div class="card border-0 shadow-sm mb-4">
                <div class="card-body p-4 p-md-5">
                    <h2 class="h3 mb-1">Iniciar sesión</h2>
                    <p class="text-secondary mb-4">Ingresa con la cuenta de tu biblioteca.</p>
                    <form method="post" action="${pageContext.request.contextPath}/app">
                        <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                        <input type="hidden" name="action" value="login">
                        <div class="mb-3">
                            <label class="form-label" for="username">Usuario</label>
                            <input class="form-control" id="username" name="username" autocomplete="username" required>
                        </div>
                        <div class="mb-4">
                            <label class="form-label" for="password">Contraseña</label>
                            <input class="form-control" type="password" id="password" name="password"
                                   autocomplete="current-password" required>
                        </div>
                        <button class="btn btn-primary w-100" type="submit">Entrar a la biblioteca</button>
                    </form>
                </div>
            </div>
            <div class="card border-0 shadow-sm">
                <div class="card-body p-4">
                    <h2 class="h5">¿Aún no tienes cuenta?</h2>
                    <p class="text-secondary">Crea una cuenta normal para solicitar y consultar tus préstamos.</p>
                    <form method="post" action="${pageContext.request.contextPath}/app" class="row g-3">
                        <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                        <input type="hidden" name="action" value="register">
                        <div class="col-md-6">
                            <label class="form-label" for="new-username">Usuario</label>
                            <input class="form-control" id="new-username" name="username" required>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label" for="new-name">Nombre</label>
                            <input class="form-control" id="new-name" name="name" required>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label" for="new-email">Correo</label>
                            <input class="form-control" type="email" id="new-email" name="email" required>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label" for="new-phone">Teléfono</label>
                            <input class="form-control" type="tel" id="new-phone" name="phone" required>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label" for="new-password">Contraseña</label>
                            <input class="form-control" type="password" id="new-password" name="password"
                                   minlength="4" autocomplete="new-password" required>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label" for="password-confirmation">Confirmar contraseña</label>
                            <input class="form-control" type="password" id="password-confirmation"
                                   name="passwordConfirmation" minlength="4" autocomplete="new-password" required>
                        </div>
                        <div class="col-12">
                            <button class="btn btn-outline-primary" type="submit">Crear cuenta normal</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
</main>
</body>
</html>
