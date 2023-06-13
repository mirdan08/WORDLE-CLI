import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.net.Socket;
import java.net.SocketException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.concurrent.*;
import java.util.Set;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Random;
import java.io.*;

/**
 * Questa classe contiene il main.
 * Nel main, viene caricato il file di configurazione config_server.proprieties. Da questo file di configurazione,
 * viene ricavato il nome del file del dizionario e dell'eventuale stato di Wordle salvato in formato json.
 * Poi vengono creati un SingleScheduledThreadPool per cambiare la parola da indovinare,
 * un CachedThreadPool per gestire le sessioni dei client e un Thread per accettare le richieste dei client.
 * Dopo aver creato i due ThreadPool e il Thread, il server si mette in attesa che l'amministratore inserisca
 * q<RET> per far terminare il server.
 * Alla terminazione, Wordle viene serializzato nel file json(il nome del file, come detto prima, sta nel file di configurazione)
 */
public class ServerMain {
    //carica il file di configurazione
    private static ConfigReader loadConfigReader(String filename) {
	try {
	    return new ConfigReader(filename);
	} catch(FileNotFoundException e) {
	    System.out.println(filename + " not present");
	    System.exit(1);
	} catch(IOException e) {
	    System.out.println("error in reading " + filename);
	    System.exit(1);
	}
	return null;
    }

    //carica le parole del dizionario dal file "filename"
    private static WordsContainer loadWords(String filename)  {
	try {
	    return new WordsContainer(filename);
	}catch(FileNotFoundException e) {
	    System.out.println(filename + "not present");
	    System.exit(1);
	}catch(IOException e) {
	    System.out.println("error in reading " + filename);
	    System.exit(1);
	}
	return null;
    }

    //se filename esiste, carica Wordle, altrimenti usa il costruttore
    private static Wordle getWordle(String filename){
	File saveFile = new File(filename);
	if(saveFile.exists()) {
	    try(BufferedReader br = new BufferedReader(new FileReader(saveFile))) {
		Type wordleType = new TypeToken<Wordle>() {}.getType();
		return (new Gson()).fromJson(br, wordleType);
	    }catch(IOException e) {
		System.out.println("error in reading " + filename);
		System.exit(1);
	    }
	} else
	    return new Wordle();
	return null;
    }
    
    public static void main(String[] args) {
	//uso final 3 volte per evitare l'errore delle variabili che vengono riferite dai lambda(se vengono riferite, allora devono essere final)
	
	final ConfigReader cr = loadConfigReader("config_server.properties");

	final WordsContainer words = loadWords(cr.getString("words"));

	//se il file con nome <saveFile> specificato nel file di configurazione è presente, allora deserializza Wordle, altrimenti usa il costruttore
	final Wordle wordle = getWordle(cr.getString("saveFile"));

	ScheduledExecutorService wordChanger = Executors.newSingleThreadScheduledExecutor();
	wordChanger.scheduleAtFixedRate(() -> {
		try {
		    String word = words.getRandom();
		    System.out.println("nuova parola:" + word);
		    wordle.changeWord(word, cr.getLong("wordTimeout"));
		} catch(IOException e) { System.out.println(e); }
	    }, wordle.getRemainingMillis() ,cr.getLong("wordTimeout"), TimeUnit.SECONDS);
	
	
	ExecutorService pl = Executors.newCachedThreadPool();
	try (ServerSocket listener = new ServerSocket(cr.getInt("serverPort"))){
	    //il thread main lo riservo per aspettare il comando di terminazione
	    Thread server = new Thread(() -> {
		    try {
			InetAddress maddr = cr.getAddr("multicastAddress");
			int mport = cr.getInt("multicastPort");
			//il listener viene chiuso automaticamente alla fine del try block
			while(!listener.isClosed()) {
			    try {
				pl.execute(new UserSession(wordle, words, listener.accept(), maddr, mport));
			    } catch (SocketException e) {}
			}
		    } catch(IOException e) {
			System.out.println(e);
		    }
	    });
	    server.start();
	    //q è il comando di terminazione
	    System.out.println("press q to save and quit");
	    Scanner stdin = new Scanner(System.in);
	    while(stdin.hasNextLine())
		if(stdin.nextLine().equals("q")) break;
	} catch(IOException e) {
	    System.out.println(e);
	}
	System.out.println("stopping...");
	wordChanger.shutdown();
	try {
	    wordChanger.awaitTermination(30, TimeUnit.SECONDS); //prima di serializzare, devo assicurarmi che non ci sia un cambio di parola in corso in corso
	} catch (InterruptedException e) {}
	pl.shutdown();
	UserSession.terminateAll();
	try {
	pl.awaitTermination(30, TimeUnit.SECONDS);//sempre prima di serializzare, devo assicurarmi che non ci siano altre attività che cambiano gli attributi degli User(perché anche loro devono essere serializzati)
	} catch (InterruptedException e) {}
	
	//salva lo stato di wordle (il nome del file di salvataggio è nel file di configurazione)
	try(BufferedWriter bw = new BufferedWriter(new FileWriter(cr.getString("saveFile")))){
	    Gson gson = new GsonBuilder()
		.setPrettyPrinting()
		.create();
	    gson.toJson(wordle, bw);
	} catch (IOException e) {
	    System.out.println("can't save");
	}

	


    }
}
