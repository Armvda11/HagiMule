package hagimule.diary;

import java.util.Objects;

public class ClientInfo implements Comparable<ClientInfo> {

    private final String clientName;
    private final String daemonAdresse;

    private int nbUses;

    public ClientInfo(String clientName, String daemonAdresse) {
        this.clientName = clientName;
        this.daemonAdresse = daemonAdresse;
        this.nbUses = 0;
    }

    public String getClientName() {
        return clientName;
    }

    public String getDaemonAdresse() {
        return daemonAdresse;
    }

    public int getNbUses() {
        return nbUses;
    }

    public void startUse() {
        nbUses++;
    }

    public void endUse() {
        nbUses--;
        if (nbUses < 0) {
            System.err.println("Error: nbUses < 0, resetting the number of uses to 0");
            nbUses = 0;
        }
    }

    @Override
    public String toString() {
        return "ClientInfo{" +
               "clientName='" + clientName + '\'' +
               ", daemonAddress='" + daemonAdresse + '\'' +
               ", nbUses='" + nbUses + '\'' +
               '}';
    }

    @Override
    public int compareTo(ClientInfo other) {
        int nbUsesComparison = Integer.compare(this.nbUses, other.nbUses);
        if (nbUsesComparison != 0) {
            return nbUsesComparison;
        }
        int nameComparison = this.clientName.compareTo(other.clientName);
        if (nameComparison != 0) {
            return nameComparison;
        }
        return this.daemonAdresse.compareTo(other.daemonAdresse);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ClientInfo that = (ClientInfo) obj;
        return clientName.equals(that.clientName) && daemonAdresse.equals(that.daemonAdresse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientName, daemonAdresse);
    }
}
