#Test 1: 
#Une seule source disponible sans compression
PASSWORD=$(< /home/kaldah/Documents/Projets/logs.txt)

# Variables
USER="ccy6321"
HOST="c202-13"
# HOST="vador"
DIARY="147.127.135.161"
DIARY="c202-12"
COMMAND_SERVER="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar server; bash"
COMMAND="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app && java -jar app.jar machine "$DIARY" 8080 data/data0/shared/ data/data0/received/ zst 75 3"

gnome-terminal -- sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$DIARY" "$COMMAND_SERVER; bash"

sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST" "$COMMAND; bash"




sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST" "$COMMAND"

cd ../
# Delete all files in the data folder recursively
rm -rf data/data*/*
gnome-terminal -- java -jar app.jar server

# Run in background and capture PID
gnome-terminal -- java -jar app.jar machine localhost 8080 data/data0/shared/ data/data0/received/ vide 75 3 &
sleep 2
cp -r sharedBak/* data/data0/shared/

# Run in background and capture PID
gnome-terminal -- java -jar app.jar machine localhost 8081 data/data1/shared/ data/data1/received/ vide 75 3 &

# Run in background and capture PID
java -jar app.jar machine localhost 8082 data/data2/shared/ data/data2/received/ vide 75 3
PID0=$!

# Wait for background processes to finish
wait $PID0
# Kill all gnome-terminal processes after execution
killall java
