#Test N7: 
#Une seule source disponible sans compression
PASSWORD=$(< /home/kaldah/Documents/Projets/logs.txt)

# Variables
USER="ccy6321"
HOST="c202-13"
# HOST="vador"
DIARY="zia"
MAJGIT="cd Documents/S7/Projet_Hagimule/HagiMule && git pull"

# Delete all files in the data folder recursively
CLEAN="cd Documents/S7/Projet_Hagimule/HagiMule && rm -rf data/data*/* && killall java"
COMMAND_SERVER="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar server; bash"
COMMAND="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar machine "$DIARY" 8088 data/data40/shared/ data/data40/received/ zst 25 5"

sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$DIARY" "$CLEAN"
sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST" "$CLEAN"
gnome-terminal -- sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$DIARY" "$COMMAND_SERVER; bash"
# On attend que le serveur Diary soit bien lancé
sleep 3

sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST" "$COMMAND; bash"

HOST2="c202-14"
COMMAND2="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar machine "$DIARY" 8089 data/data41/shared/ data/data41/received/ zst 25 5"
cp -r sharedBak/* data/data41/shared/

sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST2" "$COMMAND2; bash"

HOST3="c202-11"
COMMAND3="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar machine "$DIARY" 8089 data/data42/shared/ data/data42/received/ zst 1025 5"
cp -r sharedBak/* data/data42/shared/

sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST3" "$COMMAND3; bash"

HOST4="c202-15"
COMMAND4="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar machine "$DIARY" 8090 data/data43/shared/ data/data43/received/ zst 1025 5"

gnome-terminal -- sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST4" "$COMMAND4; bash"


# On nettoie les fichiers et les process après le test
sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST" "$CLEAN; bash"
PID0=$!

# Wait for background processes to finish
wait $PID0
# Kill all gnome-terminal processes after execution
pkill gnome-terminal
