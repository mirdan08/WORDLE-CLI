import java.io.*;
import java.util.Random;
/**
 * La classe permette un facile accesso al dizionario.
 * Internamente viene usato un RandomAccessFile e le operazioni che si possono fare sono:
 * - prendi una parola a caso
 * - controlla se una parola è presente
 */
public class WordsContainer {
    private RandomAccessFile file;
    private Random rng;
    private int lineLength;
    private long nWords;
    /**
     * Costruttore. Inizializza il RandomAccessFile in sola lettura e il Random per prendere le parole dal dizionario
     * @param filename il nome del file del dizionario
     */
    public WordsContainer(String filename) throws FileNotFoundException, IOException{
	file = new RandomAccessFile(filename, "r");
	rng = new Random();
	//assumo che tutte le parole in file siano della stessa lunghezza
	lineLength = file.readLine().getBytes().length+1;
	nWords = file.length() / lineLength;
    }
    /**
     * prende una parola a caso dal dizionario
     * @return la parola presa a caso dal dizionario
     * @throw IOException se c'è un errore nella lettura dal file del dizionario
     */
    public String getRandom() throws IOException{
	file.seek(rng.nextLong(nWords)*lineLength);
	return file.readLine();
    }
    /**
     * controlla se una parola è presente nel dizionario
     * @param word la parola da controllare
     * @return se la parola è presente true, altrimenti false
     */
    public boolean contains(String word) {
	//assumo che tutte le parole in file siano ordinate lessicograficamente
	long start = 0;
	long end = nWords-1;
	//ho messo il blocco try esterno al ciclo, così alla prima eccezione la ricerca viene interrotta
	try {
	    while(start <= end) {
		long middle = (start+end)/2;
		file.seek(middle*lineLength);
		int comparison = file.readLine().compareTo(word);
		if(comparison<0)
		    start = middle+1;
		else if(comparison>0)
		    end = middle-1;
		else return true;
	    }
	}
	catch(IOException e) {
	    System.out.println(e);
	}
	return false;
    }
}
