//questa classe mantiene le informazioni di un utente

public class UserEntry {
    private String password;
    private int playedGames;
    private int gamesWon;
    private int lastStreak;
    private int longestStreak;
    private boolean loggedIn;
    private String trials;
    private int guessDistribution [];
    private boolean canPlay;
        public UserEntry(String password, int playedGames, int gamesWon, int lastStreak,
                int longestStreak,
                boolean loggedIn,
                int guessDistribution[]) {
            this.password = password;
            this.playedGames = playedGames;
            this.gamesWon = gamesWon;
            this.lastStreak = lastStreak;
            this.longestStreak = longestStreak;
            this.loggedIn= loggedIn;
            this.guessDistribution=guessDistribution;
            canPlay=true;
        }
        public void setPassword(String password) {
            this.password = password;
        }
        public void setPlayedGames(int playedGames) {
            this.playedGames = playedGames;
        }
        public void setGamesWon(int gamesWon) {
            this.gamesWon = gamesWon;
        }
        public void setLastStreak(int lastStreak) {
            this.lastStreak = lastStreak;
    }
    public void setLongestStreak(int longestStreak) {
        this.longestStreak = longestStreak;
    }
    
    public void setGuessDistribution(int[] guessDistribution) {
        this.guessDistribution = guessDistribution;
    }
    public int[] getGuessDistribution() {
        return guessDistribution;
    }
    public void setCanPlay(boolean canPlay){
        this.canPlay=canPlay;
    }
    public void setTrials(String trials) {
        this.trials = trials;
    }
    public String getTrials() {
        return trials;
    }
    public boolean canPlay() {
        return canPlay;
    }
    public String getPassword() {
        return password;
    }
    public int getPlayedGames() {
        return playedGames;
    }
    public int getGamesWon() {
        return gamesWon;
    }
    public int getLastStreak() {
        return lastStreak;
    }
    public int getLongestStreak() {
        return longestStreak;
    }
    public void setLoggedIn(boolean loggedIn){
        this.loggedIn=loggedIn;
    }
    public boolean getLoggedIn(){
        return this.loggedIn;
    }
    @Override
    public String toString(){
        return "["+ password+","+playedGames+","+gamesWon+","+lastStreak+","+longestStreak+","+loggedIn +","+ guessDistribution + "]" ;
    }
}
