import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

//rappresenta la sessione di un utente da quando si connette a quando termina la connessione
public class UserSession extends Thread{
    Socket userSocket;
    DataInputStream dis ;
    DataOutputStream dos ;
    MulticastSocket ms;
    UserEntry userStats=null;
    String user=null;
    int trialNumber=0;
    String oldWord;
    ConcurrentHashMap<String,UserEntry> usd=UserDataStorage.getUserDataStorageInstance();
    UserSession(Socket us,MulticastSocket ms){
        userSocket=us;
        try {
            dis = new DataInputStream( us.getInputStream());
            dos = new DataOutputStream (us.getOutputStream());
            this.ms=ms;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }   
    public void run (){ 
        try {
            if( !sessionStart() ){
                return;
            }
            oldWord=WORDLEServerMain.readWord();
            while(true){
                int command=dis.readInt();
                if(command==ClientMessage.WORD){
                    String currentWord=WORDLEServerMain.readWord();
                    
                    String clientWord=dis.readUTF();

                    String answer="";
                    int rightChars=0;
                    boolean found=false;
                    try(RandomAccessFile raf=new RandomAccessFile("./words.txt", "r")){
                        long wordsNumber= raf.length()/11L;
                        byte arr[]=new byte[11];
                        long start=0L;
                        long end=wordsNumber;
                        long mid=wordsNumber/2;
                        String s;
                        while(start<=end && !found){//ricerca binaria asata sulla regolarita del file e ordinamento delle parole del dizionario
                            raf.seek(mid*11L);
                            raf.read(arr,0,10);
                            s=new String(arr,0,10,"US-ASCII");
                            if(clientWord.compareToIgnoreCase(s)<0){
                                end=mid-1;
                            }else if(clientWord.compareToIgnoreCase(s)==0){
                                found=true;
                            }else{
                                start=mid+1;
                            }
                            mid=(start+end)/2;
                        }
                    }
                    if(!found ){//parola non trovata finisce qui questo tentativi ma non vengono alterate le statistiche
                        dos.writeInt(ServerMessage.WORD_WRONG);
                        continue;
                    }
                    dos.writeInt(ServerMessage.WORD_OK);//parola corretta 

                    if(!oldWord.equals(currentWord)){//controllo cambio di parola: se la parola attuale differesce da quella letta l'ultima allora e' cambiata
                        dos.writeUTF("cambio");//segnalo cambio di parola
                        oldWord= currentWord;
                        userStats.setCanPlay(true);//l'utente puo di nuovo giocare
                        trialNumber=0;
                        continue;
                    }  
                    
                    if(!userStats.canPlay()){//indica se l'utente termina i tentativi
                                            //NOTA : i tentativi terminano se sbaglio per 12 volte o vinco e si rinnovano solo col cambio di parola
                        dos.writeUTF("finito");
                        continue;
                    }
                    for(int i=0;i<10;i++){//calcolo consiglio da inviare all'utente
                        if(currentWord.charAt(i)==clientWord.charAt(i)) {
                            answer+='+';
                            rightChars++;
                        }
                        else if( currentWord.contains(clientWord.charAt(i)+"") ) answer+='?';
                        else answer+='*';
                    }
                    dos.writeUTF(answer);
                    //se la parola e' stata indovinata o si esauriscono i tentativi l'utente non deve poter giocare e aggiorno il numero di partite
                    if(rightChars == 10 || trialNumber == 12){
                        userStats.setPlayedGames(userStats.getPlayedGames()+1);
                        userStats.setCanPlay(false);
                    }
                    //nel caso in cui la parola sia stata indovinata si aggiornano le statistiche in modo coerente
                    if(rightChars==10 && trialNumber !=11){
                        userStats.setGamesWon(userStats.getGamesWon()+1);
                        userStats.setLastStreak(userStats.getLastStreak()+1);
                        if( userStats.getLastStreak() > userStats.getLongestStreak() ){
                            userStats.setLongestStreak(userStats.getLastStreak());
                        }

                        userStats.getGuessDistribution()[trialNumber]=userStats.getGuessDistribution()[trialNumber]+1;
                    }else if(trialNumber==11){//se ho controllato che la parola non sia stata gia indovinata viene resettata la streak(l'utente ha perso)
                        userStats.setLastStreak(0);
                    }
                    trialNumber++;
                }
                else if(command==ClientMessage.PLAY){
                    dos.writeInt(userStats.canPlay()?ServerMessage.OK:ServerMessage.CANT_PLAY);//messaggio di conferma ed inizio tentativi 
                    
                }else if(command==ClientMessage.LOGOUT){
                    if(user != null && usd.get(user).getLoggedIn()){//nel caso di un logout l'utente deve essere registrato
                        usd.get(user).setLoggedIn(false);
                        userStats.setCanPlay(false);//il tentativo di indovinare la parola si considera terminato con il logout
                    }
                    dos.writeInt(ServerMessage.OK);
                }else if(command==ClientMessage.STATS){//invio delle statistiche su richiesta
                    dos.writeInt(userStats.getPlayedGames());
                    dos.writeInt(userStats.getGamesWon());
                    dos.writeInt(userStats.getLastStreak());
                    dos.writeInt(userStats.getLongestStreak());
                    for(int i=0;i<12;i++){
                        dos.writeInt(userStats.getGuessDistribution()[i]);
                    }
                }else if(command==ClientMessage.EXIT){//
                    System.out.println(Thread.currentThread()+": utente " + user + " disconnesso");
                    return;
                }else if(command==ClientMessage.SHARE){
                    //inserimento dati nel buffer
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos=new DataOutputStream(baos);
                    int trials=dis.readInt();
                    dos.writeInt(trials);
                    for(int i=0;i<trials;i++){
                        String s=dis.readUTF();
                        dos.writeUTF(s);
                    }
                    //invio in multicast
                    DatagramPacket dp=new DatagramPacket(baos.toByteArray(), baos.size(),InetAddress.getByName(ServerConfig.getMulticastIp()),ServerConfig.getMulticastPort());
                    synchronized(ms){//acceso sincronizzato dato che la socket multicast e' uguale per tutte le sessioni utente
                        ms.send(dp);
                    }
                }
                
            }
        } catch(IOException e){
            if(userStats!=null){
                userStats.setCanPlay(false);
                userStats.setLoggedIn(false);
                System.out.println(Thread.currentThread()+": utente " + user + " ha interrotto la connessione");
            }
        }
    }
    //gestisce la fase iniziale dove un utente non e' ancora autenticato
    public boolean sessionStart() throws IOException{

        while(true){
            int command = dis.readInt();
            String username=dis.readUTF();

            if(command == ClientMessage.REGISTER){
                String password = dis.readUTF();
                if( usd.containsKey(username) || password == ""){//utente gia presente nel sistema non puo regitrarsi di nuovo o password vuota
                                                            //termino l'autenticazione
                    dos.writeInt(ServerMessage.WRONG_CREDENTIALS);
                }else{
                    //se i controlli sono passati posso inserire l'utente nella struttura dati che contiene i dati degli utenti
                    usd.put(username,new UserEntry(password,0,0,0,0,false,new int[12]));
                    dos.writeInt(ServerMessage.OK);
                }
            }
            if(command == ClientMessage.LOGIN){
                String password = dis.readUTF();
                UserEntry ue=usd.get(username);
                if( ue == null ){  //se l'utente non esiste o le credenziali non sono corrette o e' gia loggato il login fallisce
                    dos.writeInt(ServerMessage.WRONG_CREDENTIALS);
                }else {
                    synchronized(ue){//sequenzializzazione dell'oggetto per la sincronizzazione
                        if (!ue.getPassword().equals(password) ||
                            ue.getLoggedIn()){ 
                            dos.writeInt(ServerMessage.WRONG_CREDENTIALS);
                        }else{
                            ue.setLoggedIn(true);
                            dos.writeInt(ServerMessage.OK);
                            user=username;
                            userStats=ue;
                            System.out.println(Thread.currentThread()+": utente " + user + " connesso");
                            break;//la sessione procede solo quando l'utente effettua il login in tutti gli altri casi rimane bloccata nella fase di autenticazione
                        }
                    }
                }
            }
            
        }
        return true;
    }
}
