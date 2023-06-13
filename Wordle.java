import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.HashMap;

/**
 * Questa classe contiene l'implementazione del gioco(cambia parola, genera indizi in base a guessed word) e la gestione degli utenti(registrazione, login).
 * Un'istanza di questa classe ha lo stato completo del gioco. Quindi, per salvare il gioco, è sufficiente serializzarla.
 */
public class Wordle {
    //map: username -> utente
    private Map<String, User> users;
    private String secretWord;
    
    //questo attributo dice l'attimo(in millisecondi) in cui dovrà essere cambiata la parola da indovinare(e quindi resettare tutti i tentativi relativi alla parola precedente)
    private long timeToChange;
    /**
     * Il costruttore viene usato solamente nel caso non sia presente il file con un' istanza di questa classe già serializzata in formato json.
     * Inizializza la Map: username -> utente
     */
    public Wordle() {
	users = new ConcurrentHashMap<String, User>();
    }

    /**
     * Metodo per cambiare la parola.
     * Oltre alla parola nuova, bisogna specificare tra quanti secondi sarà il momento di cambiarla ancora.
     * Dopo averla cambiata, tutti gli hint che sono stati dati agli utenti non servono più, quindi le loro partite vengono resettate
     * @param word la nuova parola
     * @param secondsForNextWord intervallo di tempo in secondi(tra quanto tempo ci sarà il prossimo cambiamento della parola?)
     */
    public synchronized void changeWord(String word, long secondsForNextWord) {
	secretWord = word;
	timeToChange = System.currentTimeMillis() + secondsForNextWord*1000;
	//tutti i tentativi vengono resettati.
	users.forEach((n,u) -> u.resetGame());
    }
    public boolean register(String username, String password) {
	return users.putIfAbsent(username, new User(username, password)) == null;
    }
    //precondizione: l'utente non ha fatto login
    public User login(String username, String password) {
	User u = users.get(username);
	return u != null && u.checkPassword(password) ? u : null;
    }

    /**
     * Ottieni un indizio
     * @return un indizio formato in questo modo:
     * ? la lettera è presente, ma nel posto sbagliato
     * + la lettera è presente ed è nel posto giusto
     * X non ci sono altre occorrenze di questa lettera in secretWord
     */
    public synchronized String getHint(String guessedWord){
    	char[] hint = new char[secretWord.length()];
	//se una lettera è presente n volte in secret, m volte in guessed e n<m, allora devo mettere in hint 'X' per m-n volte
	Map<Character, Integer> secretCharCount = new HashMap<Character, Integer>();
	//ma per farlo è necessario contare le occorrenze, prima di secret
	for(int i=0; i<secretWord.length(); i++)
	    secretCharCount.put(secretWord.charAt(i), secretCharCount.getOrDefault(secretWord.charAt(i),0)+1);
	for(int i=0; i<secretWord.length(); i++) {
	    char currentChar = guessedWord.charAt(i);
	    if(secretCharCount.getOrDefault(currentChar,0) == 0) hint[i] = 'X';
	    else if(currentChar == secretWord.charAt(i)) {
		hint[i] = '+';
		//poi decremento i contatori per le occorrenze in guessed(prima per i '+')
		secretCharCount.put(currentChar, secretCharCount.get(currentChar)-1);
	    }
	}
	for(int i=0; i<secretWord.length(); i++) {
	    //ho già messo 'X' o '+', che hanno precedenza su '?'
	    if(hint[i] != 0) continue;
	    char currentChar = guessedWord.charAt(i);
	    if(secretCharCount.getOrDefault(currentChar,0) > 0) {
		hint[i] = '?';
		//decremento i contatori per le occorrenze in guessed(ora per i '?')
		secretCharCount.put(currentChar, secretCharCount.get(currentChar)-1);
	    } else hint[i] = 'X';
	}
	
	return new String(hint);
    }

    /**
     * Metodo utile per sapere quanto è necessario aspettare per il cambio di parola.
     * Questo metodo viene usato dal ServerMain per stabilire quando fare il primo cambio, per poi farlo periodicamente con intervalli di tempo fissi
     * Viene usato anche da UserSession, per dire agli utenti che hanno concluso la loro partita quanto dovranno aspettare per giocare ancora
     * @return millisecondi rimanenti per passare alla prossima parola
     */
    public long getRemainingMillis() {
	return timeToChange - System.currentTimeMillis();
    }
}
