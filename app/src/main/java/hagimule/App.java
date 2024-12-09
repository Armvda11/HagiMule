package hagimule;

import hagimule.diary.DiaryServer;
import hagimule.client.ClientFileCreator;
import hagimule.client.DiaryClient;

/**
 * Classe principale pour exécuter différents fichiers en fonction des arguments passés.
 */
public class App {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Veuillez fournir un argument pour spécifier le programme à exécuter.");
            System.out.println("Options disponibles :");
            System.out.println("  server       : Démarrer le serveur Diary");
            System.out.println("  create-files : Créer des fichiers pour différents clients");
            System.out.println("  client       : Télécharger un fichier en tant que client");
            System.exit(1);
        }

        // Exécuter le programme correspondant à l'argument
        // le choix le application à lancer 
        switch (args[0].toLowerCase()) {
            case "server" -> startDiaryServer(); // Appelle la méthode startDiaryServer
            case "create-files" -> startFileCreator(); // Appelle ClientfileCreator
            case "client" -> startFileDownloader(); // appelle DiaryClient (qui va demander la téléchargement)
            default -> {
                System.out.println("Option invalide : " + args[0]);
                System.out.println("Options disponibles : server, create-files, client");
                System.exit(1);
            }
        }
    }

    /**
     * Démarre le serveur Diary.
     */
    private static void startDiaryServer() {
        try {
            System.out.println("Démarrage du serveur Diary...");
            DiaryServer.main(new String[]{}); // Appelle le main du serveur
        } catch (Exception e) {
            System.out.println("Erreur lors du démarrage du serveur Diary :");
            e.printStackTrace();
        }
    }

    /**
     * Crée des fichiers pour différents clients et les enregistre dans le Diary.
     */
    private static void startFileCreator() {
        try {
            System.out.println("Création des fichiers pour les clients...");
            ClientFileCreator.main(new String[]{}); // Appelle le main du créateur de fichiers
        } catch (Exception e) {
            System.out.println("Erreur lors de la création des fichiers :");
            e.printStackTrace();
        }
    }

    /**
     * Télécharge un fichier depuis les Daemons enregistrés dans le Diary.
     */
    private static void startFileDownloader() {
        try {
            System.out.println("Téléchargement d'un fichier...");
            DiaryClient.main(new String[]{}); // Appelle le main du client
        } catch (Exception e) {
            System.out.println("Erreur lors du téléchargement du fichier :");
            e.printStackTrace();
        }
    }
}
