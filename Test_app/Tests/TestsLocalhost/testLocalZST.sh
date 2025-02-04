#Test Local ZST: 
#Une seule source disponible avec compression

cd ../../

# Delete all files in the data folder recursively
rm -rf data/data*/*
# Delete all former log files
rm -rf logs/*
# Start the server in a new terminal and run it in the background
gnome-terminal -- bash -c "java -jar app.jar server; exec pkill -f "java -jar app.jar"; exec echo All processes terminated."

# Wait for the server to start
sleep 2

# Start the first machine in a new terminal (waits for this terminal to close before continuing)
gnome-terminal -- bash -c "java -jar app.jar machine localhost 8090 data/data0/shared/ data/data0/received/ zst 75 3"

# Wait for directories to be created
sleep 1

# Copy test files to the shared folder
cp -r sharedBak/* data/data0/shared/

# Start the second machine in a new terminal (waits for this terminal to close before continuing)
gnome-terminal -- bash -c "java -jar app.jar machine localhost 8091 data/data1/shared/ data/data1/received/ zst 75 3"

# Start the third machine in a new terminal (waits for this terminal to close before continuing)
gnome-terminal -- bash -c "java -jar app.jar machine localhost 8092 data/data2/shared/ data/data2/received/ zst 75 3"

echo "Close the Diary to terminate all the App.jar processes"