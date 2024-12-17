#!/bin/bash

# ---------------------------------------------
# Script de compilation et d'exécution du serveur Diary
# ---------------------------------------------

# Variables globales
SRC_DIR="app/src/main/java"
BUILD_DIR="app/build/classes"
MAIN_CLASS="hagimule.diary.DiaryServer"
RMI_PORT=8888
RMI_HOST="omble.enseeiht.fr"

# Étape 1 : Nettoyer les fichiers compilés précédents
echo "🔄 Nettoyage des anciens fichiers compilés..."
if [ -d "$BUILD_DIR" ]; then
    rm -rf "$BUILD_DIR"
    echo "✅ Dossier de compilation nettoyé."
else
    echo "ℹ️ Aucun dossier de compilation trouvé, pas besoin de nettoyage."
fi

# Créer le dossier de compilation
mkdir -p "$BUILD_DIR"

# Étape 2 : Compiler les fichiers Java
echo "⚙️ Compilation des fichiers Java en cours..."
cd "$SRC_DIR" || exit 1

# Compiler tous les fichiers .java
javac -d "../../../build/classes" hagimule/diary/*.java hagimule/client/*.java hagimule/client/daemon/*.java

# Vérifiez si la compilation a réussi
if [ $? -ne 0 ]; then
    echo "❌ Erreur lors de la compilation des fichiers Java."
    exit 1
fi
echo "✅ Compilation réussie."

# Étape 3 : Démarrer le registre RMI
echo "🚀 Démarrage du registre RMI sur le port $RMI_PORT..."

# Vérifiez si le registre RMI est déjà lancé
RMI_RUNNING=$(netstat -an | grep ":$RMI_PORT " | grep LISTEN)

if [ -z "$RMI_RUNNING" ]; then
    # Démarrer le registre RMI en arrière-plan sur le port $RMI_PORT
    rmiregistry $RMI_PORT &
    RMI_PID=$!
    echo "✅ Registre RMI démarré avec le PID : $RMI_PID"
else
    echo "ℹ️ Registre RMI déjà en cours d'exécution sur le port $RMI_PORT."
fi

# Attendre quelques secondes pour s'assurer que le registre est actif
sleep 3

# Étape 4 : Exécuter le serveur DiaryServer
echo "🚀 Lancement du serveur DiaryServer..."
cd "../../../build/classes" || exit 1

# Démarrage du serveur avec les propriétés nécessaires
java \
  -Djava.rmi.server.hostname=$RMI_HOST \
  -Djava.security.policy=../../../security.policy \
  $MAIN_CLASS

if [ $? -ne 0 ]; then
    echo "❌ Erreur lors de l'exécution du serveur DiaryServer."
    exit 1
fi

echo "✅ Serveur DiaryServer en cours d'exécution."
