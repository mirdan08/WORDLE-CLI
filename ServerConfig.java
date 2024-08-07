import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
//consente di caricare i dati dal file di configurazione lato server 
//e' stata resa statica data la necessita dei thread di poter accedere a questi dati 
public class ServerConfig {
    private static int SERVER_PORT;
    private static String MULTICAST_IP;
    private static int MULTICAST_PORT;
    private static long WAIT_TIME;
    private static String JSON_PATH;
    private static String DICT_PATH;
    public static int getServerPort() {
        return SERVER_PORT;
    }
    public static String getMulticastIp() {
        return MULTICAST_IP;
    }
    public static int getMulticastPort() {
        return MULTICAST_PORT;
    }
    public static long getWaitTime() {
        return WAIT_TIME;
    }
    public static String getJsonPath() {
        return JSON_PATH;
    }
    public static String getDictPath() {
        return DICT_PATH;
    }
    //caricamento del file di configurazione
    public static void loadConfig(String path) throws IOException{
        Properties prop=new Properties();
        try(InputStream is=new FileInputStream(path);){
            prop.load(is);
            
            try{
                SERVER_PORT=Integer.parseInt(prop.getProperty("SERVER_PORT"));
                MULTICAST_IP=prop.getProperty("MULTICAST_IP");
                MULTICAST_PORT=Integer.parseInt(prop.getProperty("MULTICAST_PORT"));
                WAIT_TIME= Long.parseLong(prop.getProperty("WAIT_TIME"));
                JSON_PATH= prop.getProperty("JSON_PATH");
                DICT_PATH=prop.getProperty("DICT_PATH");
                if(WAIT_TIME<=0 || SERVER_PORT<1024 || MULTICAST_PORT < 1024)
                    throw new IllegalArgumentException();
            }catch(NumberFormatException e){
                throw new IllegalArgumentException();
            }
        }
    }
}
