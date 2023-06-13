import java.net.Socket;
import java.util.Scanner;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
/**
 * Questa classe implementa il client. La sua interazione con il server ha il seguente svolgimento:
 * 1) il client riceve tutti i messaggi dal server fino a che non riceve ">", passando a 2), o "bye", terminando l'esecuzione
 * 1.1) se tra i messaggi è presente "n: show notifications", allora canShowNnotifications = true
 * 2) il client legge un comando da System.in e lo invia al server, per poi tornare a 1)
 * 2.1) se il comando era "n" e canShowNnotifications == true, allora vengono anche mostrate le notifiche
 */
public class ClientMain {
    public static void main(String[] args) {
	String confFilename = "config_client.properties";
	ConfigReader cr = null;
	try {
	    //carico il file di configurazione
	    cr = new ConfigReader(confFilename);
	}catch(FileNotFoundException e) {
	    System.out.println(confFilename + " not present");
	    System.exit(1);
	}catch(IOException e) {
	    System.out.println("Errore:" + e);
	    System.exit(1);
	}
	try(Socket soc = new Socket(cr.getAddr("serverAddress"), cr.getInt("serverPort"));
	    Scanner in = new Scanner(soc.getInputStream());
	    PrintWriter out = new PrintWriter(soc.getOutputStream(), true);
	    Scanner stdin = new Scanner(System.in)) {

	    //nr si mette in ascolto per le notifiche sul gruppo multicast
	    NotificationReceiver nr = new NotificationReceiver(cr.getAddr("multicastAddress"), cr.getInt("multicastPort"));
	    nr.start();
	    
	    
	    while(true) {
		//se il server mi dice che posso vedere le notifiche, questa variabile viene settata a true
		boolean canShowNotifications = false;
		while(in.hasNextLine()) {
		    String fromServer =in.nextLine();
		    //bye = il messaggio del server che dice al client di disconnettersi
		    if(fromServer.equals("bye")){
			nr.stopReceiving();
			System.out.println(fromServer);
			return;
		    }
		    //se il server dice che possiamo mostrare le notifiche
		    else if(fromServer.equals("n: show notifications")) {
			//allora abilitiamo il client a mostrarle
			canShowNotifications = true;
			//e mostriamo quante notifiche il NotificationReceiver ha ricevuto
			fromServer += "[" + nr.getNotificationCount() + "]";
		    }
		    System.out.print(fromServer);
		    //> = il messaggio del server prima di aspettare l'input dal client
		    if(fromServer.equals(">")) break;
		    System.out.println();
		}
		//se l'utente non ha input, termina
		if(!stdin.hasNextLine()) break;
		String userInput = stdin.nextLine();
		//se siamo abilitati, possiamo mostrare le notifiche con 'n'
		if(userInput.equals("n") && canShowNotifications) System.out.println(nr.getNotifications());
		out.println(userInput);
	    }
	    nr.stopReceiving();
	} catch (IOException e) { System.out.println(e); }
    }
}
