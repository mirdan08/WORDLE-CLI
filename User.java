import java.util.List;
import java.util.LinkedList;
/**
 * Classe dell'utente(lato server). Di un utente non ci interessa sapere solo username e password,
 * ma anche delle statistiche di gioco e dello stato della sua partita(o tentativo) per la parola da indovinare corrente.
 * Dallo stato di questa "partita" si può sapere se si è conclusa e, in tal caso, come si è conclusa:
 * - logout = la partita si conclude con "userlogout"
 * - esaurimento degli indizi = ci sono 12 indizi nella partita
 * - vincita = la partita si conclude con "++++++++++" (+ significa lettera giusta alla posizione giusta)
 * Se la partita è conclusa, allora canPlay()=false. addHint(String) e logout() fanno uso di canPlay()
 * Tutti i synchronized di questa classe sono presenti per sincronizzare l'accesso all'attributo currentGame
 */
public class User {
    private static final int maxAttempts = 12;
    private static final String success = "++++++++++";
    private String username;
    private int password;
    private int totalAttempts, successfulAttempts;
    //tutti i metodi che cambiano/leggono questa lista saranno sincronizzati per non avere problemi di concorrenza
    private List<String> currentGame;

    /**
     * Costruttore. Inizializza la lista di hint che rappresenta la partita corrente
     * @param username il nome dell'utente
     * @param password la password. Cerco di non salvarla in plaintext, anche se per avere una vera sicurezza sarebbe necessario fare 
     */
    public User(String username, String password) {
	this.username = username;
	this.password = password.hashCode();
	currentGame = new LinkedList<String>();
    }
    public boolean checkPassword(String password) {
	return this.password == password.hashCode();
    }
    public boolean canPlay() {
	if(currentGame.isEmpty()) return true;
	String lastHint = currentGame.get(currentGame.size()-1);
	return currentGame.size() < maxAttempts &&
	    !lastHint.equals(success) &&
	    !lastHint.equals("userlogout");
    }

    /**
     * Aggiungi un indizio alla partita corrente.
     * @param hint l'indizio che si ottiene da wordle.getHint(String guessedWord)
     */
    public synchronized void addHint(String hint) {
	//se ha terminato il suo tentativo, l'utente non può ricevere indizi
	if(!canPlay()) return;
	//se è il primo indizio, il tentativo viene contato
	if(currentGame.isEmpty()) totalAttempts++;
	currentGame.add(hint);
	//se la parola è stata indovinata, il tentativo con successo viene contato
	if(hint.equals(success))
	    successfulAttempts++;
    }
    public synchronized void logout() {
	//Se l'utente fa logout, il tentativo termina(canPlay() = false se lastHint.equals("user__logout")
	if(canPlay() && !currentGame.isEmpty())
	    currentGame.add("userlogout");
    }

    public String getUsername() {
	return username;
    }

    public int getTotalAttempts() {
	return totalAttempts;
    }
    
    public int getSuccessfulAttempts() {
	return successfulAttempts;
    }

    public synchronized String getCurrentGame() {
	StringBuilder sb = new StringBuilder();
	for(String s:currentGame) sb.append(s+"\n");
	return sb.toString();
    }
    /**
     * Resetta la partita corrente. Questo metodo deve essere chiamato quando la parola da indovinare viene cambiata,
     * altrimenti continuano a valere gli indizi della parola precedente
     */
    public synchronized void resetGame() {
	currentGame.clear();
    }
}
