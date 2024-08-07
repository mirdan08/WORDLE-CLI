import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class WORDLEServerMain {
    private static String word="";
    private static MulticastSocket ms=null;
    public static void main(String [] args) {
        //caricamento file di configurazione del server
        try {
            ServerConfig.loadConfig(args.length>0?args[0]:"./serverConfig.properties");
        } catch (IOException e) {
            System.out.println("errore durante il caricamento del file di configurazione");
            System.exit(1);
        }catch(IllegalArgumentException e){
            System.out.println("aprametri di configurazione errati");
            System.exit(1);
        }
        //inizializzazione thread periodico per il cambio della parola
        ScheduledExecutorService ses= Executors.newScheduledThreadPool(1);
        ses.scheduleAtFixedRate(()-> { 
            generateNewWord();
            System.out.println("nuova parola generata:"+readWord());
         } , 0L, ServerConfig.getWaitTime(), TimeUnit.MINUTES);
        UserDataStorage.readData(ServerConfig.getJsonPath());
        ExecutorService es= Executors.newCachedThreadPool();//creazione thread pool di tipo cached
        try {//creazione della socket ed unione al gruppo sociale multicast
            ms = new MulticastSocket();
            ms.joinGroup(InetAddress.getByName(ServerConfig.getMulticastIp()));
        } catch (IOException e1) {
            System.out.println("errore:\nimpossibile connettersi al gruppo sociale");
            System.exit(1);
        }
        //aggiunta gestore segnale di interruzione 
        //consente di chiudere il server e salvare correttamente i dati anche in caso di errori 
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            ses.shutdown();
            es.shutdown();
            try {
                ms.leaveGroup(InetAddress.getByName(ServerConfig.getMulticastIp()));
            } catch (UnknownHostException e) {
            } catch (IOException e) {
            }
            ms.close();
            UserDataStorage.writeData(ServerConfig.getJsonPath());
            System.out.println("chiusura server WORDLE ...");
        }));
        System.out.println("server aperto ed in esecuzione");
        //parte principale 
        try (ServerSocket ss = new ServerSocket(ServerConfig.getServerPort())) {
            while(true){
                Socket s=ss.accept();
                es.execute(new UserSession(s,ms));
            }
            
        } catch (IOException e) {
            System.out.println("errore durante l'apertura del server");
        }
    }
    //consente di leggere la parola corrente ai thread in esecuzione
    public static String readWord(){
        String s;
        synchronized(word){
            s= word;
        }
        return s;
    }
    //genera la nuova parola tramite la ricerca binaria sul dizionario fornito
    private static String generateNewWord(){
        try {
            RandomAccessFile raf=new RandomAccessFile(ServerConfig.getDictPath(), "r");
            long wordsNumber= raf.length()/11L;
            long pos = Math.abs(ThreadLocalRandom.current().nextLong()) % wordsNumber;
            raf.seek(pos*11L);
            byte arr[]=new byte[11];
            raf.read(arr,0,10);
            synchronized(word){
                word= new String(arr,"US-ASCII");
            }
            raf.close();
        } catch (FileNotFoundException e) {
            System.out.println("errore file dizionario non presente");
            System.exit(1);
        } catch (IOException e) {
            System.out.println("errore durante lettura della nuova parola");
            System.exit(1);
        }
        return null;
    }
}
