package hagimule;

import hagimule.client.ClientDownloader;
import hagimule.client.ClientFileCreator;
import hagimule.client.ClientUser;
import hagimule.diary.DiaryServer;
import hagimule.client.Machine;

/**
 * Classe principale pour exécuter différents fichiers en fonction des arguments passés.
 */
public class App {

    public static void main(String[] args) {

        String diaryAddress = "localhost";
        String port = null;
        String fileToDownloaString = "aioRick.mp4";
        String receivedFolderPath = System.getProperty("user.dir") + "/receivedFiles/";

        String argAdresseDiary = "localhost"; // Adresse du serveur Diary
        String argPort = null; // Port d'écoute du démon
        String argPathShared = null; // Chemin du dossier partagé
        String argPathDefaultReceived = null; // Chemin du dossier de réception par défaut
        String argCompressor = null; // Compresseur à utiliser
        String argLatence = null; // Latence à simuler
        String NbSourcesMax = null; // Nombre de sources maximum

        // switch (args.length) {
        //     case 0 -> {
        //         System.out.println("Veuillez fournir un argument pour spécifier le programme à exécuter.");
        //         System.out.println("Options disponibles :");
        //         System.out.println("  server       : Démarrer le serveur Diary");
        //         System.out.println("  daemon       : Démarrer le daemon");
        //         System.out.println("  create-files : Créer des fichiers pour différents clients");
        //         System.out.println("  client       : Télécharger un fichier en tant que client");
        //         System.exit(1);
        //     }
        //     case 1 -> {
        //         System.out.println("Exécution du programme en localhost...");
        //     }
        //     case 2 -> {
        //         String arg1 = args[1];
        //         // Expression régulière pour valider une adresse IPv4
        //         String regexIP = "^((25[0-5]|(2[0-4]|1/d|[1-9]|)/d)/.?/b){4}$";
        //         // Expression régulière pour valider un port
        //         String regexPort = "^([0-9]{1,5})$";
        //         if (arg1.matches(regexIP)) {
        //             System.out.println("Serveur exécuté sur l'hôte dont l'adresse est : " + args[1]);
        //             diaryAddress = args[1];
        //         } else if (arg1.matches(regexPort)) {
        //             int portNumber = Integer.parseInt(arg1);
        //             // Vérifier si le port est dans la plage valide (0-65535)
        //             if (portNumber >= 0 && portNumber <= 65535) {
        //                 System.out.println("Exécution sur le port : " + arg1);
        //                 port = arg1;
        //             } else {
        //                 System.out.println(port + " n'est pas un port valide (plage 0-65535).");
        //             }
        //         } else {
        //             if (args[0].equals("client")) {
        //                 fileToDownloaString = arg1;
        //             }
        //             else {
        //                 System.out.println("Serveur exécuté sur un PC de l'ENSEEIHT...");
        //                 diaryAddress = arg1 + ".enseeiht.fr";
        //             }
        //         }
        //     }

        //     case 3 -> {
        //         if (args[0].equals("client")) {
        //             fileToDownloaString = args[1];
        //             receivedFolderPath = args[2];
        //         }
        //     }
        //     default -> {
        //         String arg1 = args[1];
        //         // Expression régulière pour valider une adresse IPv4
        //         String regexIP = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)/.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

        //         if (arg1.length() >= 3 && arg1.substring(0, 3).matches(regexIP)) {
        //             System.out.println("Serveur exécuté sur l'hôte dont l'adresse est : " + arg1);
        //             diaryAddress = arg1;
        //         } else {
        //             System.out.println("Serveur exécuté sur un PC de l'ENSEEIHT...");
        //             diaryAddress = arg1 + ".enseeiht.fr";
        //         }
        //         port = args[2];
        //     }
        // }


        // Expression régulière pour valider une adresse IPv4
        String regexIP = "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$";
        // Expression régulière pour valider un port
        String regexPort = "^([0-9]{1,5})$";

        if (args[0].equals("machine")) {
            try {
                argAdresseDiary = args[1]; // Adresse du serveur Diary
                argPort = args[2]; // Port d'écoute du démon
                argPathShared = args[3]; // Chemin du dossier partagé
                argPathDefaultReceived = args[4]; // Chemin du dossier de réception par défaut
                argCompressor = args[5]; // Compresseur à utiliser
                argLatence = args[6]; // Latence à simuler
                NbSourcesMax = args[7]; // Nombre de sources maximum

                if (argAdresseDiary.matches(regexIP)) {
                    System.out.println("Recherche du Diary : " + args[1]);
                    diaryAddress = args[1];
                } else {
                    System.out.println("Serveur exécuté sur un PC de l'ENSEEIHT...");
                    diaryAddress = argAdresseDiary + ".enseeiht.fr";
                }
                if (argPort.matches(regexPort)) {
                    int portNumber = Integer.parseInt(argPort);
                    // Vérifier si le port est dans la plage valide (0-65535)
                    if (portNumber >= 0 && portNumber <= 65535) {
                    System.out.println("Exécution sur le port : " + argPort);
                    port = argPort;
                    } else {
                    System.out.println(port + " n'est pas un port valide (plage 0-65535).");
                    }
                } else {
                    System.out.println("Erreur lors de la lecture du port");
                }
            } catch (Exception e) {
            System.out.println("Erreur lors de la lecture des arguments :");
            System.out.println("Veuillez fournir les arguments suivants : ");
            System.out.println("1. Adresse du serveur Diary (ex: 192.168.1.1)");
            System.out.println("2. Port d'écoute du démon (ex: 8080)");
            System.out.println("3. Chemin du dossier partagé (ex: /path/to/shared)");
            System.out.println("4. Chemin du dossier de réception par défaut (ex: /path/to/received)");
            System.out.println("5. Algorithme de compression à utiliser (ex: vide, lz4, lzma ou zst )");
            System.out.println("6. Latence à simuler (ex: 100ms)");
            System.out.println("7. Nombre de sources maximum (ex: 5)");
            e.printStackTrace();
            }
        }
        if (args[0].equals("server")) {
            argAdresseDiary = args[1]; // Adresse du serveur Diary
            if (argAdresseDiary.matches(regexIP)) {
                System.out.println("Recherche du Diary : " + args[1]);
                diaryAddress = args[1];
            } else {
                System.out.println("Serveur exécuté sur un PC de l'ENSEEIHT...");
                diaryAddress = argAdresseDiary + ".enseeiht.fr";
            }
        }
        // Exécuter le programme correspondant à l'argument
        // le choix le application à lancer
        switch (args[0].toLowerCase()) {
            case "machine" -> startMachine(argAdresseDiary, argPort, argPathShared, argPathDefaultReceived, argCompressor, argLatence, NbSourcesMax); // Appelle la méthode startMachine
            case "server" -> startDiaryServer(diaryAddress, port); // Appelle la méthode startDiaryServer
            case "create-files" -> startFileCreator(diaryAddress, port); // Appelle ClientfileCreator
            case "daemon" -> startDaemon(diaryAddress, port); // Appelle ClientUser
            case "client" -> startFileDownloader(diaryAddress, fileToDownloaString, receivedFolderPath); // appelle DiaryClient (qui va demander la téléchargement)
            case "client1" -> creerClient1(); // appelle ClientfileCreator avec les paramètres spécifiés
            case "client2" -> creerClient2(); // appelle ClientfileCreator avec les paramètres spécifiés
            default -> {
                System.out.println("Option invalide : " + args[0]);
                System.out.println("Options disponibles : server, create-files, daemon, client");
                System.exit(1);
            }
        }
    }

    /**
     * Démarre le serveur Diary.
     */
    private static void startDiaryServer(String diaryAddress, String port) {
        try {
            System.out.println("Démarrage du serveur Diary...");
            if (port != null) {
                DiaryServer.main(new String[]{diaryAddress, port}); // Appelle le main du serveur
            } else {
                DiaryServer.main(new String[]{diaryAddress}); // Appelle le main du serveur
            }
        } catch (Exception e) {
            System.out.println("Erreur lors du démarrage du serveur Diary :");
            e.printStackTrace();
        }
    }

    public static void startMachine(String argAdresseDiary, String argPort, String argPathShared, String argPathDefaultReceived, String argCompressor, String argLatence, String NbSourcesMax) {
        try {
            System.out.println("Démarrage de la machine...");
            Machine.main(new String[]{argAdresseDiary, argPort, argPathShared, argPathDefaultReceived, argCompressor, argLatence, NbSourcesMax}); // Appelle le main du client
        } catch (Exception e) {
            System.out.println("Erreur lors du démarrage de la machine :");
            e.printStackTrace();
        }
    }

    /**
     * Crée des fichiers pour différents clients et les enregistre dans le Diary.
     */
    private static void startFileCreator(String diaryAddress, String port) {
        try {
            System.out.println("Création des fichiers pour les clients...");
            if (port != null) {
                ClientFileCreator.main(new String[]{diaryAddress, port}); // Appelle le main du créateur de fichiers
            } else {
                ClientFileCreator.main(new String[]{diaryAddress}); // Appelle le main du créateur de fichiers
            }
        } catch (Exception e) {
            System.out.println("Erreur lors de la création des fichiers :");
            e.printStackTrace();
        }
    }

    /**
     * Démarre un Daemon pour un client.
     */
    private static void startDaemon(String diaryAdress, String port) {
        try {
            String clientName = "Client_" + java.net.InetAddress.getLocalHost().getHostName() + "_" + port;
            System.out.println("Démarrage du Daemon...");
            if (port != null) {
                ClientUser.main(new String[]{clientName, port, diaryAdress}); // Appelle le main du client
            } else {
                ClientUser.main(new String[]{clientName, "8080", diaryAdress}); // Appelle le main du client
            }
        } catch (Exception e) {
            System.out.println("Erreur lors du démarrage du Daemon :");
            e.printStackTrace();
        }
    }

    /**
     * Télécharge un fichier depuis les Daemons enregistrés dans le Diary.
     */
    private static void startFileDownloader(String diaryAdress, String fileToDownloaString, String receivedFolderPath) {
        try {
            System.out.println("Téléchargement du fichier : " + fileToDownloaString);
            ClientDownloader.main(new String[]{diaryAdress, fileToDownloaString, receivedFolderPath}); // Appelle le main du client
        } catch (Exception e) {
            System.out.println("Erreur lors du téléchargement du fichier :");
            e.printStackTrace();
        }
    }


    private static void creerClient1() {
        try {
            System.out.println("Création des fichiers pour les clients...");
            ClientFileCreator.main(new String[]{"Client1", "fichier1.txt", "10000000", "8080"}); // Appelle le main du créateur de fichiers
        } catch (Exception e) {
            System.out.println("Erreur lors de la création des fichiers :");
            e.printStackTrace();
        }
    }

    private static void creerClient2() {
        try {
            System.out.println("Création des fichiers pour les clients...");
            ClientFileCreator.main(new String[]{"Client2", "fichier1.txt", "10000000", "8081"}); // Appelle le main du créateur de fichiers
        } catch (Exception e) {
            System.out.println("Erreur lors de la création des fichiers :");
            e.printStackTrace();
        }
    }

}
