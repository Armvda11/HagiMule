#Test N7: 
#Une seule source disponible sans compression
PASSWORD=$(< /home/kaldah/Documents/Projets/logs.txt)

# Variables
USER="ccy6321"
HOST="c202-13"
# HOST="vador"
DIARY="147.127.135.161"
DIARY="c202-12"
DIARY="melofee"
MAJGIT="cd Documents/S7/Projet_Hagimule/HagiMule && git pull"

# Delete all files in the data folder recursively
CLEAN="cd Documents/S7/Projet_Hagimule/HagiMule && rm -rf data/data*/* && killall java"
COMMAND_SERVER="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar server; bash"
COMMAND="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar machine \"$DIARY\" 8088 data/data70/shared/ data/data70/received/ vide 25 5"

sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST" "$MAJGIT"

sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$DIARY" "$CLEAN"
sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST" "$CLEAN"

sleep 3
gnome-terminal -- sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$DIARY" "$COMMAND_SERVER; bash"
# On attend que le serveur Diary soit bien lancé
sleep 2

gnome-terminal -- sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST" "$COMMAND; bash"
HOST2="c202-14"
COMMAND2="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar machine \"$DIARY\" 8089 data/data71/shared/ data/data71/received/ vide 25 5"
sleep 2
COPY="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && cp -r sharedBak/* data/data71/shared/"
gnome-terminal -- sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST2" "$COPY;bash"

sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST2" "$CLEAN"
gnome-terminal -- sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST2" "$COMMAND2; bash"

HOST3="c202-11"
COMMAND3="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar machine \"$DIARY\" 8089 data/data72/shared/ data/data72/received/ vide 25 5"

sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST3" "$CLEAN"

gnome-terminal -- sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST3" "$COMMAND3; bash"

HOST4="c202-15"
COMMAND4="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar machine \"$DIARY\" 8090 data/data73/shared/ data/data73/received/ vide 25 5"

sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST4" "$CLEAN"

gnome-terminal -- sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST4" "$COMMAND4; bash"


HOST5="c202-16"
COMMAND5="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar machine \"$DIARY\" 8091 data/data74/shared/ data/data74/received/ vide 25 5"

sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST5" "$CLEAN"

gnome-terminal -- sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST5" "$COMMAND5; bash"

# On nettoie les fichiers et les process après le test
sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST" "$CLEAN; bash"
PID0=$!

# Wait for background processes to finish
wait $PID0
# Kill all gnome-terminal processes after execution
pkill gnome-terminal
