import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.Reader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
/**
 * Questo è un lettore per i file .properties.
 * Ha dei metodi convenienti per leggere indirizzi, stringhe, interi, long.
 */
public class ConfigReader {
    private Properties configFile;
    /**
     * Costruttore.
     * @param fileName il nome del file .properties da caricare
     * @throw FileNotFoundException se il file fileName non esiste
     * @throw IOException se c'è un errore nella lettura del file
     */
    public ConfigReader(String fileName) throws FileNotFoundException,IOException{
	configFile = new Properties();
	Reader r = new FileReader(fileName);
	configFile.load(r);
	r.close();
    }
    public InetAddress getAddr(String name) throws UnknownHostException{
	return InetAddress.getByName(configFile.getProperty(name));
    }
    public String getString(String name) {
	return configFile.getProperty(name);
    }
    public int getInt(String name) {
	return Integer.parseInt(configFile.getProperty(name));
    }
    public long getLong(String name) {
	return Long.parseLong(configFile.getProperty(name));
    }
}
