package hagimule;

import hagimule.diary.DiaryClient;
import hagimule.diary.DiaryServer;

// Main class to start the server or the client
public class App {
    // to test the server : ./gradlew run --args="server"
    // to test  the client : ./gradlew run

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("server")) {
            DiaryServer.main(args);
        } else {
            DiaryClient.main(args);
        }
    }
}