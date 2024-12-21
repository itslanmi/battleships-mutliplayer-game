package si.um.feri.momcilovic;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import java.util.ArrayList;
import java.util.List;

public class GameManager {

    private static final String RESULTS_FILE = "game_results.json";
    private List<GameResult> gameResults;
    private Json json;

    public GameManager() {
        json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        loadResults();
    }

    public void saveResult(String player1, int score1, String player2, int score2, boolean isSinglePlayer) {
        GameResult result = new GameResult(player1, score1, player2, score2, isSinglePlayer);
        gameResults.add(result);
        saveResults();
    }

    private void saveResults() {
        FileHandle file = Gdx.files.local(RESULTS_FILE);
        file.writeString(json.prettyPrint(gameResults), false);
    }

    private void loadResults() {
        FileHandle file = Gdx.files.local(RESULTS_FILE);
        if (file.exists()) {
            gameResults = json.fromJson(ArrayList.class, GameResult.class, file.readString());
        } else {
            gameResults = new ArrayList<>();
        }
    }

    public List<GameResult> getGameResults() {
        return gameResults;
    }

    public GameResult getGameResult(int index) {
        return gameResults.get(index);
    }

    public static class GameResult {
        public String player1;
        public int score1;
        public String player2;
        public int score2;
        public boolean isSinglePlayer;

        public GameResult() {
            // Default constructor for JSON deserialization
        }

        public GameResult(String player1, int score1, String player2, int score2, boolean isSinglePlayer) {
            this.player1 = player1;
            this.score1 = score1;
            this.player2 = player2;
            this.score2 = score2;
            this.isSinglePlayer = isSinglePlayer;
        }

        public String getPlayer(int player) {
            if(player == 1) {
                return player1;
            } else {
                return player2;
            }
        }

        public int getScore(int score) {
            if(score==1) {
                return score1;
            } else {
                return score2;
            }
        }
    }
}
