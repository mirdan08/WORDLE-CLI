import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/*
 * questa classi si occupa di mantenere il riferimento al "magazzino" dati
 * che contiene i login e i dati relativi all'utente 
 */
public class UserDataStorage {
    private static ConcurrentHashMap<String,UserEntry> userDataStorage=new ConcurrentHashMap<>();
    private final static String PATH="./user_data.json"; 
    private UserDataStorage(){
    }
    //carica i dati di un singolo utente nella hashmap
    private static void parseUserData(JsonReader jr) throws IOException{
        jr.beginObject();
        jr.skipValue();
        String username = jr.nextString();
        jr.skipValue();
        String password = jr.nextString();
        jr.skipValue();
        int playedGames = jr.nextInt();
        jr.skipValue();
        int gamesWon = jr.nextInt();
        jr.skipValue();
        int lastStreak = jr.nextInt();
        jr.skipValue();
        int longestStreak = jr.nextInt();
        jr.skipValue();
        int guessDistribution[]=new int[12];
        jr.beginArray();
        for(int i=0;i<12;i++){
            guessDistribution[i]=jr.nextInt();
        }
        jr.endArray();
        jr.endObject();
        userDataStorage.put(username, new UserEntry(password, playedGames, gamesWon, lastStreak, longestStreak, false,guessDistribution));
    }
    //scrive i dati di un singolo utente nel file json 
    private static void writeUserData(JsonWriter jw,String username,UserEntry ue) throws IOException{
        jw.beginObject();
        jw.name("username").value(username);
        jw.name("password").value(ue.getPassword());
        jw.name("playedGames").value(ue.getPlayedGames());
        jw.name("gamesWon").value(ue.getGamesWon());
        jw.name("lastStreak").value(ue.getLastStreak());
        jw.name("longestStreak").value(ue.getLongestStreak());
        jw.name("guessDistribution").beginArray();
        for(int n:ue.getGuessDistribution()){
            jw.value(n);
        }
        jw.endArray();
        jw.endObject();
    }
    //dato il percorso del file json salva li dentro i dati della hashmap
    public static void writeData(String path){
        try (JsonWriter jw = new JsonWriter(new BufferedWriter(new FileWriter( PATH)))) {
            jw.beginArray();
            for(var ue : userDataStorage.entrySet())
                writeUserData(jw, ue.getKey(), ue.getValue());
            jw.endArray();
        } catch (IOException e) {
            System.out.println("errore durante l'apertura del file dati");
        }
        
    }
    //legge i dati dal file json nella hashmap
    public static void readData(String path){
        try (JsonReader jr = new JsonReader(new BufferedReader( new FileReader(PATH )))) {
            jr.beginArray();
            while(jr.hasNext())
                parseUserData(jr);
            jr.endArray();
        } catch (IOException e) {
            System.out.println("errore durante l'apertura del file dati");
        }
    }
    public static ConcurrentHashMap<String,UserEntry> getUserDataStorageInstance(){
        return userDataStorage;
    }
}
