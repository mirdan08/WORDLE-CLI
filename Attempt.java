//classe per poter contenere i tentativi inviati dal server in multicast
public class Attempt {
    private String [] attempts;
    private int attemptsNumber;
    public Attempt(String attempts[],int attemptsNumber){
        this.attempts=attempts;
        this.attemptsNumber=attemptsNumber;
    }
    public String[] getAttempts() {
        return attempts;
    }
    public int getAttemptsNumber() {
        return attemptsNumber;
    }
    public void setAttempts(String[] attempts) {
        this.attempts = attempts;
    }
    public void setAttemptsNumber(int attemptsNumber) {
        this.attemptsNumber = attemptsNumber;
    }
}
