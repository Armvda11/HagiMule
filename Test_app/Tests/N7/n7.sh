#Test N7: 
#Une seule source disponible sans compression
PASSWORD=$(< /home/kaldah/Documents/Projets/logs.txt)

# Variables
USER="ccy6321"
DIARY="lamartine"

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
CLEAN="cd Documents/S7/Projet_Hagimule/HagiMule/Test_app; rm -rf data/data*; rm -rf logs/*"
END="rm -rf data/data*; rm -rf logs/*; pkill -f 'java -jar app.jar'"
# Function to clean every DIARY and HOST
clean_diary() {
    sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$DIARY" "$CLEAN"
}

clean_hosts() {
    for HOST in "$HOST0" "$HOST1" "$HOST2" "$HOST3" "$HOST4" "$HOST5" "$HOST6" "$HOST7" "$HOST8" "$HOST9" "$HOST10"; do
        sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@${HOST}" "$CLEAN"
    done
}


# Clean at the start
clean_hosts

clean_diary

sleep 3

COMMAND_SERVER="$CD; java -jar app.jar server; $END"
gnome-terminal -- bash -c "sshpass -p \"$PASSWORD\" ssh -o StrictHostKeyChecking=no \"$USER@$DIARY\" \"$COMMAND_SERVER\""


sleep 3
COMMAND="$CD; java -jar app.jar machine $DIARY 8089 data/data0/shared/ data/data0/received/ zst 25 5"
gnome-terminal -- bash -c "sshpass -p \"$PASSWORD\" ssh -o StrictHostKeyChecking=no \"$USER@$HOST0\" \"$COMMAND\""

sleep 2
COPIE="$CD; cp -r sharedBak/* data/data0/shared/"
sshpass -p "$PASSWORD" ssh -o StrictHostKeyChecking=no "$USER@$HOST0" "$COPIE"