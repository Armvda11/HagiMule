#Test 1: 
#Une seule source disponible sans compression

cd ../
# Delete all files in the data folder recursively
rm -rf data/data*/*

gnome-terminal -- java -jar app.jar server

# Run in background and capture PID
gnome-terminal -- java -jar app.jar machine localhost 8080 data/data0/shared/ data/data0/received/ vide 150 3 &
sleep 1
cp -r sharedBak/* data/data0/shared/

# Run in background and capture PID
gnome-terminal -- java -jar app.jar machine localhost 8081 data/data1/shared/ data/data1/received/ vide 75 3 &

# Run in background and capture PID
java -jar app.jar machine localhost 8082 data/data2/shared/ data/data2/received/ vide 75 1
PID0=$!

# Wait for background processes to finish
wait $PID0
# Kill all gnome-terminal processes after execution
killall java
