import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.InputMismatchException;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Scanner;

public class WORDLEClientMain {
    //dati per la connessione al server
    private static String SERVER_IP;
    private static int SERVER_PORT;
    private static String MULTICAST_IP;
    private static int MULTICAST_PORT;
    private static Socket connection;
    private static DataOutputStream dos;
    private static DataInputStream dis;
    private static MulticastSocket ms=null;
    //variabili e oggetti per gestire la sessione di gioco
    private static Scanner s=new Scanner(System.in);
    private static String wordGuess []=new String[12];
    private static String wordAdvice [] = new String[12];
    private static int trials=0;
    private static LinkedList<Attempt> advices;

    public static void main(String[] args) throws Exception {
        //lettura file di configurazione
        try{
            loadConfig(args.length>0?args[0]:"./clientConfig.properties");
        }catch(FileNotFoundException e){
            System.out.println("file di configurazone non trovato");
            System.exit(1);
        }
        catch(IOException e){
            System.out.println("errore durante la lattura della configurazione");
            System.exit(1);
        }
        //apertura connessione col server
        try{
            connection = new Socket(SERVER_IP,SERVER_PORT);
        }catch(UnknownHostException e){
            System.out.println("errore : impossibile contattare il server");
            System.exit(1);
        }catch(ConnectException e){
            System.out.println("errore : impossibile contattare il server");
            System.exit(1);
        }
        dos = new DataOutputStream(connection.getOutputStream());
        dis = new DataInputStream(connection.getInputStream());
        //inizializzazione della partita
        if(!initiateSession()){
            System.out.println("uscita dal programma...");
            return;
        }
        System.out.println("autenticazione riuscita\ninserisci la parola per giocare oppure scrivi EXIT per uscire");

        if(!playWORDLE())
            System.out.println("hai gia tentato di indovinare la parola attendi la generazione della prossima");
        String guessWord;
        advices=new LinkedList<>();
        //unione al gruppo sociale multicast
        try{
            ms = new MulticastSocket(MULTICAST_PORT);
            ms.joinGroup( InetAddress.getByName(MULTICAST_IP));
        }catch(IOException e){
            System.out.println("errore durante la connessione col gruppo sociale");
            System.exit(1);
        }
        //thread sempre in esecuzione per ricevere eventuali consigli dal server
        Thread multiCastThread=new Thread(()->{
            try {
                byte buff [] = new byte[10*12 + 4];
                DatagramPacket dp=new DatagramPacket(buff, buff.length);
                dp.setPort(MULTICAST_PORT);
                while(true){
                    ms.receive(dp);//attende continuamente per l'arrivo di un pacchetto
                    if(Thread.currentThread().isInterrupted()) break;
                    DataInputStream dis = new DataInputStream( new ByteArrayInputStream(buff) );//estrae i dati e li mette in una lista colegata apposita
                    int attemptsNumber=dis.readInt();
                    String attempts []=new String [attemptsNumber] ;
                    for(int i=0;i<attemptsNumber;i++){
                        attempts[i]=dis.readUTF();
                    }
                    synchronized(advices){
                        advices.add(new Attempt(attempts,attemptsNumber));
                    }
                }
            } catch (IOException e) {
                return;
            }
            
        });
        multiCastThread.start();
        //menu principale di gioco
        try{
        while(true){
            System.out.print(">");
            guessWord=s.next();
            if(guessWord.equals("EXIT")){
                sendMeStatistics();
                logout();
                dos.writeInt(ClientMessage.EXIT);
                break;
            }
            if(guessWord.equals("SHARE")){
                dos.writeInt(ClientMessage.SHARE);
                dos.writeInt(trials);
                for(int i=0;i<trials;i++){
                    dos.writeUTF(wordAdvice[i]);
                }
                continue;
            }
            if(guessWord.equals("SHOW")){
                synchronized(advices){
                    for(Attempt a:advices){
                        System.out.println("numero di tentativi impiegati:"+a.getAttemptsNumber());
                        for(String s:a.getAttempts()){
                            System.out.println(s);
                        }
                    }
                }
                continue;
            }
            if(guessWord.length()!=10){
                System.out.println("la parola deve essere di 10 caratteri");
                continue;
            }
            //invio della parola 
            dos.writeInt(ClientMessage.WORD);
            dos.writeUTF(guessWord);
            int type=dis.readInt();//risposta del server
            if(type==ServerMessage.WORD_WRONG){
                System.out.println("parola non presente nel dizionario");
                continue;
            }
            //se la risposta e' positiva si procede a leggere il risultato
            String response= dis.readUTF();
            if(response.equals("cambio")){//il server comunica un cambio di parola
                wordGuess=new String[12];
                trials=0;
                System.out.println("e' stata cambiata la parola");
                synchronized(advices){
                    advices=new LinkedList<Attempt>();//svuoto la lista dei tentativi vecchi
                }
            }
            else if(response.equals("++++++++++")){//parola indovinata correttamente
                wordGuess[trials]=guessWord;//registrazione del risultato
                wordAdvice[trials]=response;
                trials++;
                System.out.println("hai vinto, congratulazioni!");
                sendMeStatistics();
            }
            else if(response.equals("finito")){//hia gia indovinato o ha terminato i tentativi
                System.out.println("hai gia tentato di indovinare la parola corrente attendi la prossima parola");
            }
            else if(trials < 12){//sta ancora tentando di indovinare
                wordGuess[trials]=guessWord;
                wordAdvice[trials]=response;
                trials++;
            }
            if(trials >=12 )
                System.out.println("tentativi terminati");
            printWords();
        }
        //chiusura delle connessioni tcp e multicast  ed uscita dal gioco
        ms.leaveGroup(InetAddress.getByName(MULTICAST_IP));
        ms.close();
        dos.close();
        dis.close();
        connection.close();
    }   catch(IOException e){
        System.out.println("errore durante la comunicazione");
        System.exit(1);
    }

    }
    //caricamento del file di configurazione
    public static void loadConfig(String path)throws IOException,FileNotFoundException{
        Properties prop=new Properties();
        try(InputStream is=new FileInputStream(path);){
            prop.load(is);
            try{
                SERVER_IP=prop.getProperty("SERVER_IP");
                SERVER_PORT=Integer.parseInt(prop.getProperty("SERVER_PORT"));
                MULTICAST_IP=prop.getProperty("MULTICAST_IP");
                MULTICAST_PORT=Integer.parseInt(prop.getProperty("MULTICAST_PORT"));
                if(SERVER_PORT<1024 || MULTICAST_PORT<1024) {
                    System.out.println("parametri ci configurazione errati");
                    System.exit(1);
                }
            }catch(NumberFormatException e){
                throw new IOException();
            }
        }
    }
    //fase iniziale dell'inizio della sessione 
    //qui l'utente decide se registrarsi o fare login
    //restitituisce true se la fase va a buon fine e quindi l'utente risulta 
    //registrato o loggato
    public static boolean initiateSession() {
        int command;
        System.out.println("benvenuto su wordle");
        try{
        while(true){
            System.out.print("opzioni:\n0 per registrarti\n1 per effettuare il login\n2 per uscire\n>");
            try{
            command=s.nextInt();
            }catch(InputMismatchException e){
                System.out.println("inserire un numero per favore");
                s.nextLine();
                continue;
            }
            if(command == 2 ) {//l'utente ha deciso di uscire dalla fase di autenticazione
                return false;
            }

            s.nextLine();
            System.out.print("username:");
            String username = s.nextLine();
            System.out.print("password:");
            String password = s.nextLine();
            //registrazione dell'utente
            if(command == 0 ) {
                if(register(username, password)){
                    System.out.println("registrazione effettuata");
                }else{
                    System.out.println("registrazione fallita");
                }
            }
            //tentativo di login 
            //se ha successo puo proseguire la sessione
            if(command == 1 && login(username, password)) return true;
        }
    }catch(IOException e){
        //nel caso di problemi di connessione si interrompe la fase di autenticazione
        return false;
    }
    }
    //registrazione dell'utente
    public static boolean register(String username,String password) throws IOException{
        dos.writeInt(ClientMessage.REGISTER);
        dos.writeUTF(username);
        dos.writeUTF(password);
        int response = dis.readInt();
        if(response==ServerMessage.OK)
            return true;
        return false;
    }
    //login dell'utente
    public static boolean login(String username,String password) throws IOException{
        dos.writeInt(ClientMessage.LOGIN);
        dos.writeUTF(username);
        dos.writeUTF(password);
        int response = dis.readInt();
        if(response == ServerMessage.WRONG_CREDENTIALS){
            return false;
        }
        if(response==ServerMessage.OK){
            return true;
        }
        return false;
    }
    //logout
    public static boolean logout() throws IOException{
        dos.writeInt(ClientMessage.LOGOUT);
        int response=dis.readInt();
        return response == ServerMessage.OK;
    }
    //segnalizione di inizio della partita
    public static boolean playWORDLE() throws IOException{
        dos.writeInt(ClientMessage.PLAY);
        int response=dis.readInt();
        return response == ServerMessage.OK;
    }
    //lettura e stampa a schermo delle statistiche dell'utente
    public static void sendMeStatistics() throws IOException{
        dos.writeInt(ClientMessage.STATS);
        int playedGames=dis.readInt();
        int gamesWon=dis.readInt();
        int lastStreak=dis.readInt();
        int longestStreak=dis.readInt();
        int guessDistribution[]=new int[12];
        for(int i=0;i<guessDistribution.length;i++){
            guessDistribution[i]=dis.readInt();
        }
        System.out.print("statistiche:\nnumero partite giocate:"+playedGames+
                           "\npercentuale partite vinte:"+ ((float)gamesWon/(float)playedGames)*100 +
                           "%\nlunghezza ultima streak:" + lastStreak +
                           "\nlunghezza streak massima:"+ longestStreak +
                           "\ndistribuzione tentativi: ");
        for(int i=0;i<guessDistribution.length;i++){
            System.out.print((i+1)+":"+guessDistribution[i]+" ");
        }
        System.out.println("");
    }
    //stampa della partita attuale
    public static void printWords(){
        System.out.println("+----------+");
        for(int j=0;j<trials;j++){
            System.out.print("|");
            for(int i=0;i<wordGuess[j].length();i++){
                if(wordAdvice[j].charAt(i)=='+') System.out.print("\u001B[42m"+wordGuess[j].charAt(i)+"\u001B[0m");
                else if(wordAdvice[j].charAt(i)=='?') System.out.print("\u001B[43m"+wordGuess[j].charAt(i)+"\u001B[0m");
                else System.out.print(wordGuess[j].charAt(i));
            }
            System.out.println("|");
        }
        System.out.println("+----------+");
    }


}
