package hagimule.diary;

public class ClientInfo {

    private String clientName;
    private String daemonAdresse;

    public ClientInfo(String clientName, String daemonAdresse) {
        this.clientName = clientName;
        this.daemonAdresse = daemonAdresse;
    }

    public String getClientName() {
        return clientName;
    }

    public String getDaemonAdresse() {
        return daemonAdresse;
    }

    public String toString() {
        return "ClientInfo{" +
               "clientName='" + clientName + '\'' +
               ", daemonAddress='" + daemonAdresse + '\'' +
               '}';
    }
}
