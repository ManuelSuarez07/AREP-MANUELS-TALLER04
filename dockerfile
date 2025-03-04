FROM openjdk:17

# Directorio de trabajo dentro del contenedor
WORKDIR /usrapp

# Copiar el archivo JAR de la aplicación al contenedor
COPY target/WebServerT4-1.0-SNAPSHOT.jar app.jar

# Exponer el puerto 35000 (el puerto interno de la aplicación)
EXPOSE 35000

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar", "com.eci.MainApplication"]