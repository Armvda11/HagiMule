#Test N7: 
#Une seule source disponible sans compression
PASSWORD=$(< /home/kaldah/Documents/Projets/logs.txt)

# Variables
USER="ccy6321"
HOST="c202-13"
# HOST="vador"
DIARY="goldorak"
MAJGIT="cd Documents/S7/Projet_Hagimule/HagiMule && git pull"

# Delete all files in the data folder recursively
CLEAN="cd Documents/S7/Projet_Hagimule/HagiMule; exec rm -rf data/data*/*; exec rm -rf logs/*"
COMMAND_SERVER="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar server; bash"
COMMAND_SERVER="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar server; exec pkill -f "java -jar app.jar"; exec echo All processes terminated."
COMMAND="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar machine "$DIARY" 8088 data/data30/shared/ data/data30/received/ zst 25 5"


gnome-terminal -- bash -c sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$DIARY" ""$CLEAN"; exec pkill -f "java -jar app.jar"; echo Cleaned""
sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST" "$CLEAN"
gnome-terminal -- sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$DIARY" "$COMMAND_SERVER; bash"
# On attend que le serveur Diary soit bien lancé
sleep 3

gnome-terminal -- sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST" "$COMMAND; bash"

HOST2="c202-14"
COMMAND2="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar machine "$DIARY" 8089 data/data31/shared/ data/data31/received/ zst 25 5"
cp -r sharedBak/* data/data30/shared/

gnome-terminal -- sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST2" "$COMMAND2; bash"

HOST3="c202-11"
COMMAND3="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar machine "$DIARY" 8089 data/data32/shared/ data/data32/received/ zst 25 5"

gnome-terminal -- sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST3" "$COMMAND3; bash"

HOST4="c202-15"
COMMAND4="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar machine "$DIARY" 8090 data/data33/shared/ data/data33/received/ zst 25 5"

gnome-terminal -- sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST4" "$COMMAND4; bash"


HOST5="c202-16"
COMMAND5="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar machine "$DIARY" 8091 data/data34/shared/ data/data34/received/ zst 25 5"

gnome-terminal -- sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST5" "$COMMAND5; bash"


HOST6="c202-17"
COMMAND6="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar machine "$DIARY" 8092 data/data35/shared/ data/data35/received/ zst 25 5"

gnome-terminal -- sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST6" "$COMMAND6; bash"

# On nettoie les fichiers et les process après le test
sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST" "$CLEAN; bash"
PID0=$!

# Wait for background processes to finish
wait $PID0
# Kill all gnome-terminal processes after execution
pkill gnome-terminal


HOST5="c202-16"
COMMAND5="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar machine "$DIARY" 8095 data/data342/shared/ data/data342/received/ vide 25 5"

gnome-terminal -- sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST5" "$COMMAND5; bash"


HOST6="c202-17"
COMMAND6="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar machine "$DIARY" 8094 data/data352/shared/ data/data352/received/ vide 25 5"

gnome-terminal -- sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST6" "$COMMAND6; bash"


HOST6="c202-17"
COMMAND6="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar machine "$DIARY" 8094 data/data452/shared/ data/data452/received/ vide 25 5"

gnome-terminal -- sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST6" "$COMMAND6; bash"

