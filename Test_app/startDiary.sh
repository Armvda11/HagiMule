cd ../
./gradlew build
cp app/build/libs/app-1.0-SNAPSHOT.jar Test_app/app.jar
cd Test_app
java -jar app.jar server
