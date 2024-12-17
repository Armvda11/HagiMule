#!/bin/bash

# ---------------------------------------------
# Script de compilation et d'exécution du client
# ---------------------------------------------

# Variables globales
SRC_DIR="app/src/main/java"
BUILD_DIR="app/build/classes"
MAIN_CLASS="hagimule.client.ClientFileCreator"
RMI_HOST="omble.enseeiht.fr"  # Adresse du serveur Diary
FILE_NAME="file1.txt"        # Nom du fichier par défaut
FILE_SIZE=$((2 * 1024 * 1024)) # Taille du fichier par défaut (2 Mo)
DAEMON_PORT=8080             # Port du Daemon par défaut

# Vérification si le nom du client est passé en argument
if [ -z "$1" ]; then
    echo "❌ Vous devez spécifier le nom du client comme argument."
    echo "Usage : ./client_lance.sh <client-name> [options]"
    echo "Exemple : ./client_lance.sh Client1 --file-name file1.txt --file-size 2097152 --port 8080"
    exit 1
fi

# Assignation du nom du client depuis l'argument
CLIENT_NAME="$1"
shift  # Décalage des arguments pour traiter les options suivantes

# Lecture des arguments de la ligne de commande
while [[ "$#" -gt 0 ]]; do
    case $1 in
        --file-name) FILE_NAME="$2"; shift ;;
        --file-size) FILE_SIZE="$2"; shift ;;
        --port) DAEMON_PORT="$2"; shift ;;
        *) echo "Argument inconnu : $1" ;;
    esac
    shift
done

# Affichage des paramètres utilisés
echo "========================================="
echo "📂 Lancement du client avec les paramètres suivants :"
echo "🔹 Nom du client    : $CLIENT_NAME"
echo "🔹 Nom du fichier   : $FILE_NAME"
echo "🔹 Taille du fichier: $((FILE_SIZE / 1024 / 1024)) Mo"
echo "🔹 Port du daemon   : $DAEMON_PORT"
echo "🔹 Serveur Diary    : $RMI_HOST"
echo "========================================="

# 🔄 Nettoyage des anciens fichiers compilés
echo "🔄 Nettoyage des anciens fichiers compilés..."
if [ -d "$BUILD_DIR" ]; then
    rm -rf "$BUILD_DIR"
    echo "✅ Dossier de compilation nettoyé."
else
    echo "ℹ️ Aucun dossier de compilation trouvé, pas besoin de nettoyage."
fi

# Créer le dossier de compilation
mkdir -p "$BUILD_DIR"

# ⚙️ Compilation des fichiers Java
echo "⚙️ Compilation des fichiers Java en cours..."
cd "$SRC_DIR" || exit 1

# Compiler uniquement les fichiers nécessaires pour le client
javac -d "../../../build/classes" hagimule/client/ClientFileCreator.java hagimule/client/daemon/Daemon.java hagimule/diary/*.java

# Vérifiez si la compilation a réussi
if [ $? -ne 0 ]; then
    echo "❌ Erreur lors de la compilation des fichiers Java."
    exit 1
fi
echo "✅ Compilation réussie."

# 📘 Définition des options de la JVM
JVM_OPTIONS="-Djava.rmi.server.hostname=$RMI_HOST"

# 🚀 Lancement du client
echo "🚀 Lancement du client $CLIENT_NAME..."
cd "../../../build/classes" || exit 1

# Exécuter le client avec les arguments appropriés
java $JVM_OPTIONS -cp . hagimule.client.ClientFileCreator "$CLIENT_NAME" "$FILE_NAME" "$FILE_SIZE" "$DAEMON_PORT"

if [ $? -ne 0 ]; then
    echo "❌ Erreur lors de l'exécution du client."
    exit 1
fi

echo "✅ Client $CLIENT_NAME exécuté avec succès."
