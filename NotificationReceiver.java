import java.net.MulticastSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;
import java.io.IOException;

/**
 * Questo è il ricevitore di notifiche usato dal client
 * La struttura dati usata per memorizzare le notifiche è una LinkedList di stringhe, perché una notifica,
 * per come l'ho interpretata, è il nome del giocatore più tutti gli hint che ha ricevuto nel suo tentativo concluso.
 * Quindi, concatenando tutte queste informazioni, ottengo una stringa che viene tenuta in questa lista fino alla terminazione del programma.
 * Le operazioni di lettura e scrittura su questa lista sono sincronizzati con blocchi e metodi synchronized
 */
public class NotificationReceiver extends Thread{
    private MulticastSocket msoc;
    private List<String> notifications;
    /**
     * Costruttore. Fa la join sul gruppo multicast
     * @param maddr l'indirizzo multicast del gruppo sul quale fare join per ricevere le notifiche
     * @param mport la porta su cui mettersi in ascolto per ricevere
     */
    public NotificationReceiver(InetAddress maddr, int mport) throws IOException {
	msoc = new MulticastSocket(mport);
	msoc.joinGroup(maddr);
	notifications = new LinkedList<String>();
    }
    public void run() {
	byte[] rcv = new byte[256];
	while(!msoc.isClosed()) {
	    DatagramPacket dp = new DatagramPacket(rcv, rcv.length);
	    try {
		msoc.receive(dp);
		synchronized(this) {
		    notifications.add(new String(dp.getData(), 0, dp.getLength(), "US-ASCII"));
		}
	    } catch(IOException e) {}
	}
    }
    public synchronized int getNotificationCount() {
	return notifications.size();
    }
    /**
     * ottieni tutte le notifiche ricevute in una sola stringa
     * @return la concatenazione di tutte le notifiche
     */
    public synchronized String getNotifications() {
	StringBuilder sb = new StringBuilder();
	for(String s:notifications) sb.append("User: " + s);
	return sb.toString();
    }
    public void stopReceiving() {
	msoc.close();
    }
}
