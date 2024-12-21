package hagimule;

import hagimule.client.ClientFileCreator;
import hagimule.client.ClientUser;
import hagimule.client.DiaryClient;
import hagimule.diary.DiaryServer;

/**
 * Classe principale pour exécuter différents fichiers en fonction des arguments passés.
 */
public class App {

    public static void main(String[] args) {
        String diaryAddress = "localhost";

        switch (args.length) {
            case 0 -> {
                System.out.println("Veuillez fournir un argument pour spécifier le programme à exécuter.");
                System.out.println("Options disponibles :");
                System.out.println("  server       : Démarrer le serveur Diary");
                System.out.println("  daemon       : Démarrer le daemon");
                System.out.println("  create-files : Créer des fichiers pour différents clients");
                System.out.println("  client       : Télécharger un fichier en tant que client");
                System.exit(1);
            }
            case 1 -> {
                System.out.println("Exécution du programme en localhost...");
            }
            case 2 -> {
                if (args[2].equals("enseeiht")) {
                    System.out.println("Serveur exécuté sur un PC de l'ENSEEIHT...");
                    diaryAddress = args[1] + ".enseeiht.fr";
                } else {
                    System.out.println("Serveur exécuté sur l'hôte dont l'adresse est : " + args[1]);
                    diaryAddress = args[1];
                }
            }
            default -> {
                System.out.println("Exécution du programme sur l'hôte : " + args[1]);
                diaryAddress = args[1];
            }
        }

        // Exécuter le programme correspondant à l'argument
        // le choix le application à lancer 
        switch (args[0].toLowerCase()) {
            case "server" -> startDiaryServer(diaryAddress); // Appelle la méthode startDiaryServer
            case "create-files" -> startFileCreator(diaryAddress); // Appelle ClientfileCreator
            case "daemon" -> startDaemon(diaryAddress); // Appelle ClientUser
            case "client" -> startFileDownloader(diaryAddress); // appelle DiaryClient (qui va demander la téléchargement)
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
    private static void startDiaryServer(String diaryAddress) {
        try {
            System.out.println("Démarrage du serveur Diary...");
            DiaryServer.main(new String[]{diaryAddress}); // Appelle le main du serveur
        } catch (Exception e) {
            System.out.println("Erreur lors du démarrage du serveur Diary :");
            e.printStackTrace();
        }
    }

    /**
     * Crée des fichiers pour différents clients et les enregistre dans le Diary.
     */
    private static void startFileCreator(String diaryAddress) {
        try {
            System.out.println("Création des fichiers pour les clients...");
            ClientFileCreator.main(new String[]{diaryAddress}); // Appelle le main du créateur de fichiers
        } catch (Exception e) {
            System.out.println("Erreur lors de la création des fichiers :");
            e.printStackTrace();
        }
    }

    /**
     * Démarre un Daemon pour un client.
     */
    private static void startDaemon(String diaryAdress) {
        try {
            System.out.println("Démarrage du Daemon...");
            ClientUser.main(new String[]{"Client1", "8080", diaryAdress}); // Appelle le main du client
        } catch (Exception e) {
            System.out.println("Erreur lors du démarrage du Daemon :");
            e.printStackTrace();
        }
    }

    /**
     * Télécharge un fichier depuis les Daemons enregistrés dans le Diary.
     */
    private static void startFileDownloader(String diaryAdress) {
        try {
            System.out.println("Téléchargement d'un fichier...");
            DiaryClient.main(new String[]{diaryAdress}); // Appelle le main du client
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
