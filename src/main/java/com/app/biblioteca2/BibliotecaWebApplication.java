package com.app.biblioteca2;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BibliotecaWebApplication {
	private BibliotecaWebApplication() {
	}

	public static void main(String[] args) throws IOException, URISyntaxException, LifecycleException {
		Path webAppDirectory = Path.of("src", "main", "webapp").toAbsolutePath().normalize();
		if (!Files.isDirectory(webAppDirectory)) {
			throw new IOException("No se encontró la carpeta de la aplicación web: " + webAppDirectory);
		}

		Path classesDirectory = Path.of(BibliotecaWebApplication.class.getProtectionDomain()
				.getCodeSource().getLocation().toURI()).toAbsolutePath().normalize();
		if (!Files.isDirectory(classesDirectory)) {
			throw new IOException("No se encontró la carpeta de clases compiladas: " + classesDirectory);
		}

		Path baseDirectory = Path.of("target", "embedded-tomcat").toAbsolutePath().normalize();
		Files.createDirectories(baseDirectory);

		Tomcat tomcat = new Tomcat();
		tomcat.setBaseDir(baseDirectory.toString());
		tomcat.setPort(Integer.getInteger("server.port", 8080));
		tomcat.getConnector();

		Context context = tomcat.addWebapp("", webAppDirectory.toString());
		context.setParentClassLoader(BibliotecaWebApplication.class.getClassLoader());
		StandardRoot resources = new StandardRoot(context);
		resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes",
				classesDirectory.toString(), "/"));
		context.setResources(resources);

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				tomcat.stop();
				tomcat.destroy();
			} catch (Exception exception) {
				System.err.println("No se pudo detener Tomcat correctamente: " + exception.getMessage());
			}
		}, "biblioteca-tomcat-shutdown"));

		tomcat.start();
		System.out.println("Biblioteca UFG está disponible en http://localhost:"
				+ tomcat.getConnector().getLocalPort() + "/");
		tomcat.getServer().await();
	}
}
