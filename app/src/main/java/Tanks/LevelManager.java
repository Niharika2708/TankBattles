package Tanks;

import processing.data.JSONObject;
import java.util.Set;
import java.util.HashSet;

/**
 * The {@code LevelManager} class handles the progression of levels within the game,
 * including loading and transitioning between different levels as well as managing the game state at the start of each level.
 */
public class LevelManager {
    private App app;  
    private JSONObject config;  // Configuration object containing level data and other settings.
    private int currentLevelIndex = 0;  
    private Set<Character> destroyedPlayers = new HashSet<>();  // Set to track players that have been destroyed.

    /**
     * Constructs a LevelManager with a reference to the main game application and configuration.
     *
     * @param app    The main game application managing all components.
     * @param config A JSON object containing configuration settings, including level data.
     */
    public LevelManager(App app, JSONObject config) {
        this.app = app;
        this.config = config;
    }

    /**
     * Loads the next level in the sequence defined by the configuration.
     * Clears any previous state including destroyed players and existing tanks.
     * If all levels are completed, triggers the end of the game.
     */
    public void loadNextLevel() {
        destroyedPlayers.clear(); // Clear the set of destroyed players.
        if (currentLevelIndex < config.getJSONArray("levels").size()) {
            app.getTanks().clear();  // Clear the current list of tanks in the game.

            JSONObject level = config.getJSONArray("levels").getJSONObject(currentLevelIndex); // Retrieve the configuration for the next level.
            app.getTerrain().loadLevelConfig(level); // Load terrain configuration from the level data.
            currentLevelIndex++; // Increment the level index to move to the next level.
        } else {
            app.endGame(); // End the game if all levels have been completed.
        }
    }

    /**
     * Restarts the game by resetting the level index and reloading the initial level.
     * This method is used to start the game over from the beginning.
     */
    public void restartGame() {
        currentLevelIndex = 0; // Reset the level index to zero.
        loadNextLevel(); // Load the initial level.
    }

    // Getter for currentLevelIndex
    public int getCurrentLevelIndex() {
        return currentLevelIndex;
    }

    // Setter for currentLevelIndex
    public void setCurrentLevelIndex(int index) {
        this.currentLevelIndex = index;
    }
}