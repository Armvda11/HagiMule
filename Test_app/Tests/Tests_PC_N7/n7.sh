#Test N7: 
#Une seule source disponible sans compression
PASSWORD=$(< /home/kaldah/Documents/Projets/logs.txt)

# Variables
USER="ccy6321"
DIARY="vador"

# HOST variables
HOST0="c202-10"
HOST1="c202-11"
HOST2="c202-12"
HOST3="c202-13"
HOST4="c202-14"
HOST5="c202-15"
HOST6="c202-16"
HOST7="c202-17"
HOST8="c202-18"
HOST9="c201-10"
HOST10="c201-11"

MAJGIT="cd Documents/S7/Projet_Hagimule/HagiMule ; git pull"
CD="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app"


# Delete all files in the data folder recursively
# Command to kill every instance of the app
CLEAN="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app; rm -rf data/data*; rm -f logs/*; pkill -f 'java -jar app.jar'"

# Functions to clean DIARY and every HOST

clean() {
    sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$DIARY" "$CLEAN"
}

END="rm -rf data/data*; rm -rf logs/*; pkill -f 'java -jar app.jar'"
# Clean at the start
clean

sleep 5

COMMAND_SERVER="$CD; java -jar app.jar server; bash; $END"
gnome-terminal -- bash -c "sshpass -p \"$PASSWORD\" ssh -o StrictHostKeyChecking=no \"$USER@$DIARY\" \"$COMMAND_SERVER\""

# On attend que le diary se lance
sleep 5

for HOST in "$HOST0" "$HOST1" "$HOST2" "$HOST3" "$HOST4" "$HOST5" do
    COMMAND="$CD; java -jar app.jar machine $DIARY 8085 data/${HOST}/shared/ data/${HOST}/received/ zst 25 5; bash"
    gnome-terminal -- bash -c "sshpass -p \"$PASSWORD\" ssh -o StrictHostKeyChecking=no \"$USER@${HOST}\" \"$COMMAND\""
    sleep 1
done

sleep 5

for HOST in "$HOST0" "$HOST1" "$HOST2" "$HOST3"; do
    COPIE="$CD; cp -r sharedBak/* data/${HOST}/shared/"
    sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@${HOST}" "$COPIE"
done

echo Close the Diary to terminate all the App.jar processes

