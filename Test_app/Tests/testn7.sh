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
COMMAND_SERVER="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar server; bash"
COMMAND="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar machine "$DIARY" 8088 data/data2/shared/ data/data2/received/ zst 25 5"

gnome-terminal -- sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$DIARY" "$COMMAND_SERVER; bash"
sleep 5

sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST" "$COMMAND; bash"


HOST2="c202-14"
COMMAND2="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar machine "$DIARY" 8089 data/data5/shared/ data/data5/received/ zst 25 5"

sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST2" "$COMMAND2; bash"

HOST3="c202-11"
COMMAND3="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar machine "$DIARY" 8089 data/data6/shared/ data/data6/received/ zst 25 5"

sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST3" "$COMMAND3; bash"

HOST4="c202-15"
COMMAND4="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar machine "$DIARY" 8090 data/data7/shared/ data/data7/received/ zst 25 5"

gnome-terminal -- sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST4" "$COMMAND4; bash"


HOST5="c202-16"
COMMAND5="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar machine "$DIARY" 8091 data/data8/shared/ data/data8/received/ zst 25 5"

gnome-terminal -- sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST5" "$COMMAND5; bash"


HOST6="c202-17"
COMMAND6="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar machine "$DIARY" 8092 data/data9/shared/ data/data9/received/ zst 25 5"

gnome-terminal -- sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST6" "$COMMAND6; bash"



sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST" "$COMMAND"

cd ../
# Delete all files in the data folder recursively
rm -rf data/data*/*
gnome-terminal -- java -jar app.jar server

# Run in background and capture PID
gnome-terminal -- java -jar app.jar machine localhost 8080 data/data0/shared/ data/data0/received/ vide 25 3 &
sleep 2
cp -r sharedBak/* data/data0/shared/

# Run in background and capture PID
gnome-terminal -- java -jar app.jar machine localhost 8081 data/data1/shared/ data/data1/received/ vide 25 3 &

# Run in background and capture PID
java -jar app.jar machine localhost 8082 data/data2/shared/ data/data2/received/ vide 25 3
PID0=$!

# Wait for background processes to finish
wait $PID0
# Kill all gnome-terminal processes after execution
killall java
pkill gnome-terminal
