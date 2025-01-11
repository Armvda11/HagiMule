@echo off
REM Build the project using Gradle
./gradlew build

REM Copy the built JAR file to the Test_app directory
copy app\build\libs\app-1.0-SNAPSHOT.jar Test_app\app.jar

REM Change directory to Test_app
cd Test_app

REM Run the JAR file with the server argument
java -jar app.jar server