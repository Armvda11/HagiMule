#Test 1: 
#Une seule source disponible sans compression
PASSWORD=$(< /home/kaldah/Documents/Projets/logs.txt)

# Variables
USER="ccy6321"
HOST="c202-13"
# HOST="vador"
DIARY="147.127.135.161"
DIARY="c202-12"
DIARY="forge"
MAJGIT="cd Documents/S7/Projet_Hagimule/HagiMule && git pull"

# Delete all files in the data folder recursively
CLEAN="cd Documents/S7/Projet_Hagimule/HagiMule && rm -rf data/data*/* && killall java"
COMMAND_SERVER="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar server; bash"
COMMAND="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar machine "$DIARY" 8088 data/data20/shared/ data/data20/received/ zst 25 5"

sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST" "$MAJGIT"

sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST" "$CLEAN"
gnome-terminal -- sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$DIARY" "$COMMAND_SERVER; bash"
# On attend que le serveur Diary soit bien lancé
sleep 3

sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST" "$COMMAND; bash"

HOST2="c202-14"
COMMAND2="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar machine "$DIARY" 8089 data/data21/shared/ data/data21/received/ zst 25 5"
cp -r sharedBak/* data/data21/shared/

sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST2" "$COMMAND2; bash"

HOST3="c202-11"
COMMAND3="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar machine "$DIARY" 8089 data/data22/shared/ data/data22/received/ zst 25 5"

sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST3" "$COMMAND3; bash"

HOST4="c202-15"
COMMAND4="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar machine "$DIARY" 8090 data/data23/shared/ data/data23/received/ zst 25 5"

gnome-terminal -- sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST4" "$COMMAND4; bash"


HOST5="c202-16"
COMMAND5="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar machine "$DIARY" 8091 data/data24/shared/ data/data24/received/ zst 25 5"

gnome-terminal -- sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST5" "$COMMAND5; bash"


HOST6="c202-17"
COMMAND6="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar machine "$DIARY" 8092 data/data25/shared/ data/data25/received/ zst 25 5"

gnome-terminal -- sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST6" "$COMMAND6; bash"



sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST" "$COMMAND"

gnome-terminal -- java -jar app.jar server

# Run in background and capture PID
gnome-terminal -- java -jar app.jar machine localhost 8080 data/data26/shared/ data/data26/received/ vide 25 3 &
sleep 2

# Run in background and capture PID
gnome-terminal -- java -jar app.jar machine localhost 8081 data/data27/shared/ data/data27/received/ vide 25 3 &

# Run in background and capture PID
java -jar app.jar machine localhost 8082 data/data28/shared/ data/data28/received/ vide 25 3
PID0=$!

# Wait for background processes to finish
wait $PID0
# Kill all gnome-terminal processes after execution
pkill gnome-terminal
