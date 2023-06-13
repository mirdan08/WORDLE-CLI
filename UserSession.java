import java.util.Scanner;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.net.Socket;
import java.net.MulticastSocket;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.util.concurrent.TimeUnit;

/**
 * Questa classe rappresenta una sessione del client lato server
 * Quando questa viene eseguita da un Thread, avvengono i seguenti passaggi:
 * 1) il server dice al client quale input si aspetta di ricevere(comandi, username, guessed word, ecc...)
 * 2) il server invia la riga ">"
 * 3) il server attende l'input del client con hasNextLine()
 * 4) il server esegue l'operazione richiesta dal client comunicandone l'esito e tornando a 1)
 * questi 4 passaggi sono ripetuti più volte nel metodo run
 */
public class UserSession implements Runnable {
    // tengo traccia degli utenti online perché non posso permettere che un utente faccia login mentre sta giocando su un'altra connessione
    private static Set<String> onlineUsers;
    // tengo traccia di tutte le connessioni attive, perché poi dovrò chiuderle per far terminare il CachedThreadPool(che è nel ServerMain)
    
    // se non chiudo le UserSession allo shutdown, allora dovrei aspettare la disconnessione di tutti i client
    private static Set<Socket> clients;
    private static boolean running = true;
    // il riferimento a Wordle è necessario per la registrazione, il login, giocare, ecc...
    private Wordle wordle;
    // il riferimento a WordsContainer è necessario per sapere se una parola è presente o meno nel dizionario
    private WordsContainer words;
    // questa è il Socket usato dal server per comunicare con il client
    private Socket socket;
    // questi sono l'indirizzo multicast e la porta per spedire le notifiche, usando il protocollo UDP
    private InetAddress multicastAddress;
    private int multicastPort;

    /**
     * Costruttore. Oltre a inizializzare gli attributi, inizializza gli insiemi onlineUsers e clients
     */
    public UserSession(Wordle wordle, WordsContainer words, Socket s, InetAddress multicastAddress, int multicastPort) {
	if(onlineUsers == null) onlineUsers = new HashSet<String>();
	if(clients == null) clients = new HashSet<Socket>();
	this.wordle = wordle;
	this.words = words;
	this.socket = s;
	this.multicastAddress = multicastAddress;
	this.multicastPort = multicastPort;
    }
    public void run() {
	synchronized(clients) {
	    clients.add(socket);
	}
	User u = null;
	try(Scanner in = new Scanner(socket.getInputStream());
	    PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
	    System.out.println("Nuova connessione");
	    //questo do-while si ripete fino a che l'utente non ha fatto login
	    do {
		out.println("commands:\npress 1 to login, 2 to register, q to quit\n>");
		if(!in.hasNextLine()) break;
		String scelta = in.nextLine();
		if(scelta.equals("q")) {
		    out.println("bye");
		    break;
		}
		if(!scelta.equals("1") && !scelta.equals("2")) continue;
		out.println("username:\n>");
		if(!in.hasNextLine()) break;
		String username = in.nextLine();
		out.println("password:\n>");
		if(!in.hasNextLine()) break;
		if(scelta.equals("1")) {
		    //senza sinchronized, potrei avere problemi con 2 client che cercano di fare login con stesso username e stessa password
		    synchronized(onlineUsers) {
			if(!onlineUsers.contains(username)) u = wordle.login(username, in.nextLine());
			if(u != null) onlineUsers.add(username);
			else out.println("can't login");
		    }
		} else if(wordle.register(username, in.nextLine())) {
		    System.out.println(username + " registrato");
		    out.println("user " + username + " registered\nyou can login now");
		}
		else out.println("can't register");
	    } while(u == null);
	    if(u != null) {
		//messaggio di benvenuto
		System.out.println(u.getUsername() + " login");
		out.println("Hello, " + u.getUsername());
		String currentGame = u.getCurrentGame();
		if(!currentGame.isEmpty()) out.println("your game so far:\n" + currentGame);

		//questo ciclo si ripete fino a che l'utente non fa logout
		while(true) {
		    out.println("q: logout");
		    boolean canPlay = u.canPlay();
		    if(canPlay) out.println("p: playWordle");
		    // se non posso giocare, posso condividere la partita che ho finito
		    else out.println("s: share game result");
		    out.println("S: show statistics");
		    out.println("n: show notifications\n>");
		    if(!in.hasNextLine()) break;
		    String scelta = in.nextLine();
		    if(scelta.equals("q")) break;
		    else if(scelta.equals("p")) {
			if(!canPlay) {
			    long remaining = wordle.getRemainingMillis();
			    //se il tempo rimanente è minore di un'ora
			    if(remaining < 3600000)
				//allora lo mostro in minuti
				out.println("you have to wait " + TimeUnit.MILLISECONDS.toMinutes(remaining) + " minutes to play");
			    //altrimenti in ore
			    else out.println("you have to wait " + TimeUnit.MILLISECONDS.toHours(remaining) + " hours to play");
			    continue;
			}
			out.println("insert guessed word:\n>");
			if(!in.hasNextLine()) break;
			String word = in.nextLine();
			if(!words.contains(word)) {
			    out.println("word is not in dictionary");
			    continue;
			}
			String hint = wordle.getHint(word);
			System.out.println(u.getUsername() + " prova a indovinare:" + word + " -> indizio:" + hint);
			u.addHint(hint);
			out.println(hint);
		    } else if(scelta.equals("s")) {
			//non si può condividere il risultato prima di aver finito il tentativo
			if(canPlay) {
			    out.println("finish the game first");
			    continue;
			}
			System.out.println(u.getUsername() + " condivide la partita");
			try(MulticastSocket msoc = new MulticastSocket()) {
			    byte[] msg = (u.getUsername() + "\n" + u.getCurrentGame()).getBytes();
			    msoc.send(new DatagramPacket(msg, msg.length, multicastAddress, multicastPort));
			    out.println("done");
			} catch (IOException e) {out.println("Error:"+e);}
		    } else if(scelta.equals("S")) {
			out.println("total guess attempts:" + u.getTotalAttempts() +
				    "\nsuccessful attempts:" + u.getSuccessfulAttempts());
		    }
		}
		//se è stato fatto un terminateAll, allora non è stato l'utente a disconnettersi o a fare logout, ma è stato l'amministratore che ha disconnesso tutti i client dal server
		if(running) {
		    System.out.println(u.getUsername() + " logout");
		    u.logout();
		}
		synchronized(onlineUsers) {
		    onlineUsers.remove(u.getUsername());
		}
	    }
	    out.println("bye");
	    socket.close();
	} catch(IOException e) {
	    System.out.println(e);
	}
	
	synchronized(clients) {
	    clients.remove(socket);
	}
    }

    // questo è il metodo per disconnettere tutti i client dal server. Quando un socket si chiude il metodo run() termina
    public static void terminateAll() {
	running = false;
	if(clients == null) return;
	synchronized(clients) {
	    for(Socket s:clients) {
		try(PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {
		    out.println("bye");
		    s.close();
		} catch (IOException e) {
		    System.out.println(e);
		}
	    }
	}
    }
}
