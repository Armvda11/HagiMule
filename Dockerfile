# Utiliser une image de base avec JDK
FROM openjdk:21-jdk

# Définir le répertoire de travail dans le conteneur
WORKDIR /app

# Copier le fichier JAR dans le conteneur
COPY app/build/libs/app-1.0-SNAPSHOT.jar /app/app-1.0-SNAPSHOT.jar

# Exposer le port nécessaire
EXPOSE 8080

# Définir une variable d'environnement pour l'argument (par défaut 'client')
ENV APP_ARGUMENT client

# Commande pour lancer l'application avec l'argument passé par la variable d'environnement
CMD ["sh", "-c", "java -jar /app/app-1.0-SNAPSHOT.jar $APP_ARGUMENT"]
