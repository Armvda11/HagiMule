package hagimule;

import hagimule.diary.DiaryClient;
import hagimule.diary.DiaryServer;

public class App {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("server")) {
            DiaryServer.main(args);
        } else {
            DiaryClient.main(args);
        }
    }
}