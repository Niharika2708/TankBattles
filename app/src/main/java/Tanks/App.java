package Tanks;

import processing.core.PApplet;
import processing.core.PImage;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;
import processing.data.JSONObject;

/**
 * The {@code App} class represents the main application controlling the Tanks game.
 * It manages game components such as terrain, tanks, projectiles, and game state.
 * Additionally, it handles setup, initialization, and rendering of the game.
 */
public class App extends PApplet {

    public static final int WIDTH = 864; 
    public static final int HEIGHT = 640; 

    private Terrain terrain; 
    private Wind wind; 
    private LevelManager levelManager; 
    private int currentPlayerIndex = 0; 
    private int parachuteCount = 3; 
    private int[] foregroundColor;

    private PImage fuelIcon; 
    private PImage windLeftIcon, windRightIcon;
    private PImage parachuteIcon; 

    private ArrayList<Tank> tanks; // List containing all tanks in the game
    private ArrayList<Projectile> projectiles; // List containing all projectiles in the game
    private ArrayList<Explosion> explosions; // List containing all explosions in the game
    private Map<Character, Integer> scores; // Map containing scores for each player

    private int scoreDisplayIndex = 0; // Index used to display scores
    private int frameCounter = 0; // Counter for frame updates
    private final int framesPerScore = 21; // Number of frames before displaying scores (about 0.7 seconds at 30 FPS)
    private List<Map.Entry<Character, Integer>> sortedScores; // List containing sorted scores

    // Variables controlling visibility and duration of the direction arrow above the current tank
    private boolean arrowVisible = true; 
    private float arrowTimer = 0; 
    private final float arrowDuration = 2.0f; 
    private boolean isGameOver = false; 

    /**
     * Sets up the initial settings for the game window.
     */
    public void settings() {
        size(WIDTH, HEIGHT);
    }

    /**
     * Initializes game components such as terrain, tanks, and game utilities.
     * Loads images and game levels.
     */
    public void setup() {
        frameRate(30); 
        tanks = new ArrayList<>(); // Initialize list to store tanks
        projectiles = new ArrayList<>(); // Initialize list to store projectiles
        explosions = new ArrayList<>(); // Initialize list to store explosions
        scores = new HashMap<>(); // Initialize map to store scores
        wind = new Wind(this); 
       
        JSONObject config = loadJSONObject("config.json");  // Load game configuration from JSON file

        terrain = new Terrain(this, tanks, this, config);
        levelManager = new LevelManager(this, config);
      
        levelManager.loadNextLevel();   // Load the next level

        // Load images for game elements
        fuelIcon = loadImage("src/main/resources/Tanks/fuel.png");
        windLeftIcon = loadImage("src/main/resources/Tanks/wind-1.png");
        windRightIcon = loadImage("src/main/resources/Tanks/wind.png");
        parachuteIcon = loadImage("src/main/resources/Tanks/parachute.png");

        // Initialize scores if they are empty
        if (scores.isEmpty()) {
            initializeScores();
        }
    }

    /**
     * Updates and renders all game components each frame.
     * This method serves as the main game loop, responsible for updating the game state,
     * handling all visual outputs, and managing timing and game events.
     * It first checks if the game is over, then either updates the game components or displays the end game screen accordingly.
     */
    public void draw() {
        if (!isGameOver) { // If the game is not over, update the game components
            updateGame();
        } else {  // If the game is over, display the end game screen
            displayEndGame();
        }
    }

     /**
     * Updates and renders all game components each frame.
     * This method serves as the main game loop, responsible for updating the game state,
     * handling all visual outputs, and managing timing and game events.
     * It includes the following steps:
     * - Drawing the background and terrain.
     * - Updating and drawing tanks and projectiles.
     * - Managing explosions.
     * - Displaying game elements such as fuel, wind, health bars, power indicators, and scores.
     * - Checking for level completion.
     *
     * @see Terrain#drawTerrain() 
     * @see Projectile#update() 
     * @see Projectile#isOffscreen() 
     * @see Projectile#shouldRemove() 
     * @see Projectile#draw() 
     * @see Explosion#update(float) 
     * @see Explosion#display()
     * @see Explosion#isFinished() 
     */
    private void updateGame() {

        background(200); // Set the background for the canvas
        float dt = 1.0f / frameRate; // Calculate time step for physics updates

        handleArrowVisibility(dt); // Handle visibility of the arrow indicating the current tank's turn

        // Draw the background image if available
        if (terrain != null && terrain.getImages().containsKey("background")) {
            image(terrain.getImages().get("background"), 0, 0, WIDTH, HEIGHT);
        }

        // Draw the terrain
        if (terrain != null && terrain.getHeights() != null) {
            terrain.drawTerrain();
        }

        // Update and draw tanks
        if (!tanks.isEmpty()) {
            for (Tank tank : tanks) {
                tank.update();
                tank.draw();
            }

            handlePostUpdateLogic(); // Perform post-update logic for tanks
            drawCurrentPlayerIndicator();
            displayCurrentPlayer();
        }

        // Update and draw projectiles
        for (int i = 0; i < projectiles.size(); i++) {
            Projectile p = projectiles.get(i);
            p.update();
            // Remove projectiles that are offscreen or expired
            if (p.isOffscreen() || p.shouldRemove()) {
                projectiles.remove(i);
                i--;
            } else {
                p.draw();
            }
        }

        // Update and display explosions
        for (int i = explosions.size() - 1; i >= 0; i--) {
            Explosion exp = explosions.get(i);
            exp.update(dt);
            exp.display();
            // Remove finished explosions
            if (exp.isFinished()) {
                explosions.remove(i);
            }
        }

        // Display game elements such as fuel, wind, health bars, power indicators, and scores
        displayFuel();
        displayWind();
        displayHealthBar();
        displayPowerIndicator();
        displayScores();

        // Check for completion of the current level
        checkForLevelCompletion();
    }

   /**
     * Displays the final scores at the end of the game.
     * It draws the background and terrain, keeps displaying current scores during the end game,
     * and shows the winner announcement along with the final scores in a table format.
     */
    public void displayFinalScores() {
        if (terrain != null && terrain.getImages().containsKey("background")) {
            image(terrain.getImages().get("background"), 0, 0, WIDTH, HEIGHT);
        }

        // Keep displaying the current HUD during the end game
        terrain.drawTerrain(); 
        displayScores(); 
        displayFuel();
        displayHealthBar();
        displayWind();
        displayPowerIndicator();
    
        textAlign(CENTER, CENTER);
        textSize(24);
        int startY = HEIGHT / 2 - 100;
        int tableWidth = 250;
        int rowHeight = 40;
        int headerHeight = 50;
        int borderThickness = 5; 
    
        // Retrieve winner color from the config, fallback to default if needed
        String winner = getWinner();
        JSONObject config = loadJSONObject("config.json"); // Assuming the config is accessible here
        JSONObject playerColors = config.getJSONObject("player_colours");
        int[] winnerColor = terrain.parseColorConfig(playerColors.getString(winner));
    
        // Draw the winner announcement
        fill(winnerColor[0], winnerColor[1], winnerColor[2]);
        text(winner + " wins!", WIDTH / 2, startY + 10);
    
        // Drawing the outline of the scores table with transparency
        stroke(0);
        strokeWeight(borderThickness);
        fill(winnerColor[0], winnerColor[1], winnerColor[2], 70); 
        rect(WIDTH / 2 - tableWidth / 2, startY + 40, tableWidth, rowHeight * sortedScores.size() + headerHeight, 10);
    
        // Displaying the "Final Scores" table
        fill(0);
        textSize(20);
        text("Final Scores", WIDTH / 2, startY + 60);
        stroke(0);
        line(WIDTH / 2 - tableWidth / 2, startY + 80, WIDTH / 2 + tableWidth / 2, startY + 80); // Underline
    
        int yPos = startY + 100; // Position for the first score entry
    
        for (int i = 0; i < sortedScores.size(); i++) {
            if (frameCounter >= i * 3) { // Changed the delay factor for faster display, approx. 0.1 seconds
                Map.Entry<Character, Integer> entry = sortedScores.get(i);
                int[] color = terrain.parseColorConfig(playerColors.getString(String.valueOf(entry.getKey())));
                fill(color[0], color[1], color[2]);
                text("Player " + entry.getKey() + ": " + entry.getValue(), WIDTH / 2, yPos);
                yPos += rowHeight;
            }
        }
        frameCounter++;
    }
    
    /**
     * Ends the game and prepares for displaying the final scores.
     * It sets the game over flag, sorts the scores, and resets the counters for displaying scores.
     */
    public void endGame() {
        isGameOver = true;
        sortedScores = new ArrayList<>(scores.entrySet());
        sortedScores.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));
        scoreDisplayIndex = 0;  // Reset the index for displaying scores one by one
        frameCounter = 0;  // Reset the frame counter for score display timing
    }
    
     /**
     * Displays the end game screen, showing the final scores.
     * It draws the background and terrain, and then calls the method to display the final scores.
     */
    public void displayEndGame() {
        if (terrain != null && terrain.getImages().containsKey("background")) {
            image(terrain.getImages().get("background"), 0, 0, WIDTH, HEIGHT);
        }
        if (terrain != null && terrain.getHeights() != null) {
            terrain.drawTerrain();
        }
        displayFinalScores();
    }

    /**
     * Restarts the game by resetting game components and reloading the initial setup.
     */
    private void restartGame() {
        isGameOver = false;
        tanks.clear();
        projectiles.clear();
        explosions.clear();
        scores.clear();
        levelManager.restartGame();
        setup();
    }

    /**
     * Switches to the next player's turn.
     * This method advances the game to the next player's turn, updating necessary game state.
     */
    private void nextPlayer() {
        if (tanks.isEmpty()) return; // Exit if no tanks are left
    
        currentPlayerIndex = (currentPlayerIndex + 1) % tanks.size();
        arrowVisible = true;
        arrowTimer = 0;
        wind.update(); // Updating wind each turn
    
        if (!tanks.isEmpty() && currentPlayerIndex < tanks.size()) {
            Tank currentTank = tanks.get(currentPlayerIndex);
        }
    
        // Check if the game should transition to the next level
        checkForLevelCompletion();
    }

    /**
     * Checks for the completion of the current level and loads the next level if necessary.
     * This method removes tanks marked for removal and loads the next level if fewer than two tanks are left.
     */
    public void checkForLevelCompletion() {
        tanks.removeIf(Tank::isMarkedForRemoval);  // Remove tanks that are marked for removal
        if (tanks.size() <= 1) {    // If fewer than two tanks are left, load the next level
            levelManager.loadNextLevel();
        } else {
            currentPlayerIndex %= tanks.size(); // Ensure the current player index is valid
        }
    }

    /**
     * Displays the current fuel status of the active tank on the game screen.
     * This method checks if the current player's tank has fuel and updates the fuel icon accordingly.
     * It is part of the game's UI updates that occur within the game loop.
     *
     * @see Tank#getFuel() for retrieving the current fuel level of the tank.
     */

    private void displayFuel() {
        if (!tanks.isEmpty() && currentPlayerIndex < tanks.size() && fuelIcon != null) {
            Tank currentTank = tanks.get(currentPlayerIndex);
            float fuelLeft = currentTank.getFuel();
            
            // Prepare text and position for displaying fuel status
            String turnText = "Player " + currentTank.getSymbol() + "'s turn";
            float textWidth = textWidth(turnText);  
            float iconX = 20 + textWidth + 20;  
    
            // Displaying the fuel icon next to the player's turn
            image(fuelIcon, iconX, 10, 32, 32);
            fill(0);
            textSize(16);
            text(": " + (int) fuelLeft, iconX + 40, 30); 
        }
    }

    /**
     * Displays the wind strength and icon on the game screen.
     * It retrieves the wind strength from the Wind object and displays the corresponding icon.
     */

    private void displayWind() {
        PImage windIcon = wind.getStrength() >= 0 ? windRightIcon : windLeftIcon;
        float iconX = WIDTH - 100;  
        image(windIcon, iconX, 10, 32, 32);
        fill(0);
        textSize(16);
        text(": " + Math.abs(wind.getStrength()), iconX + 40, 26); // Displaying wind strength next to the icon
    }

     /**
     * Displays the health bar for the current tank on the game screen.
     * It visualizes the current tank's health using a graphical health bar.
     */

    private void displayHealthBar() {
        if (!tanks.isEmpty() && currentPlayerIndex < tanks.size()) {
            Tank currentTank = tanks.get(currentPlayerIndex);
            float health = currentTank.getHealth();
            int[] colors = currentTank.getColors();
            float healthBarWidth = 200;
            float healthBarHeight = 20;
            float healthRemaining = healthBarWidth * (health / 100.0f);
            float healthBarX = WIDTH / 2 - healthBarWidth / 2;
            float healthBarY = 20;
    
            // Draw health bar with remaining health
            noStroke();
            fill(255); 
            rect(healthBarX + healthRemaining, healthBarY, healthBarWidth - healthRemaining, healthBarHeight);
            fill(colors[0], colors[1], colors[2]);
            rect(healthBarX, healthBarY, healthRemaining, healthBarHeight);
    
            // Calculate power indicator position and draw the line
            float power = currentTank.getPower();
            float powerIndicatorX = map(power, 0, 100, healthBarX, healthBarX + healthBarWidth);
    
            stroke(128);
            strokeWeight(3);
            noFill();
            rect(healthBarX, healthBarY, powerIndicatorX - healthBarX, healthBarHeight);

            stroke(0);
            strokeWeight(power < 50 ? 4 : 1); // Thicker black outline when power is less than 50
            noFill();
            rect(powerIndicatorX, healthBarY, healthBarWidth - (powerIndicatorX - healthBarX), healthBarHeight);
    
            // Draw the power indicator line 
            stroke(255, 0, 0);
            strokeWeight(1.5f); 
            line(powerIndicatorX, healthBarY - 5, powerIndicatorX, healthBarY + healthBarHeight + 5);
            fill(0);
            textSize(16);
            text("Health: ", healthBarX - 60, healthBarY + 7);
            text((int) health, healthBarX + healthBarWidth + 10, healthBarY + 7);
        }
    }
    
    /**
     * Displays the power indicator for the current tank on the game screen.
     * It shows the current power level of the tank, indicating the strength of the projectile to be fired.
     */

    private void displayPowerIndicator() {
        if (!tanks.isEmpty() && currentPlayerIndex < tanks.size()) {
            Tank currentTank = tanks.get(currentPlayerIndex);
            float power = currentTank.getPower();
    
            // Coordinates of the power indicator
            float powerX = WIDTH / 2 - 160;
            float powerY = 55; 
    
            fill(0);
            textSize(16);
            text("Power: ", powerX + 2, powerY + 7);
    
            fill(0);
            textSize(16);
            text((int) power, powerX + 60, powerY + 7);
        }
    }

    /**
     * Draws an arrow above the current player's tank to indicate their turn.
     * It visually indicates the current player's turn by drawing an arrow above their tank.
     */

    private void drawCurrentPlayerIndicator() {
        if (tanks.size() > currentPlayerIndex && arrowVisible) {  
            Tank currentTank = tanks.get(currentPlayerIndex);
            drawArrowAboveTank(currentTank);
        }
    }

    /**
     * Draws an arrow above the specified tank.
     * This method draws an arrow indicating the current tank's turn.
     *
     * @param tank The tank for which the arrow is drawn.
     */
    private void drawArrowAboveTank(Tank tank) {
        fill(0); 
        stroke(0); 
        strokeWeight(2); 
    
        float arrowX = tank.getX();
        float arrowStartY = tank.getY() - Tank.SIZE * 3.0f; 
        float arrowLength = 50; 
        float arrowEndY = arrowStartY + arrowLength;
    
        line(arrowX, arrowStartY, arrowX, arrowEndY);

        float arrowheadSize = 10; 
        triangle(arrowX, arrowEndY, 
                 arrowX - arrowheadSize, arrowEndY - arrowheadSize, 
                 arrowX + arrowheadSize, arrowEndY - arrowheadSize);
    }

    /**
     * Updates the visibility of the arrow indicating the current player's turn.
     * This method updates the visibility of the arrow indicating the current player's turn.
     * It hides the arrow after a certain duration.
     *
     * @param dt The time elapsed since the last frame.
     */
    private void handleArrowVisibility(float dt) {
        if (arrowVisible) {
            arrowTimer += dt; // Increment timer
            if (arrowTimer >= arrowDuration) {
                arrowVisible = false;  // Hide the arrow after the duration passes
            }
        }
    }

    /**
     * Displays information about the current player.
     * This method shows the current player's symbol and available parachutes.
     * Additionally, it indicates if the current player has a larger projectile ready.
     */
    private void displayCurrentPlayer() {

        if (!tanks.isEmpty() && currentPlayerIndex < tanks.size()) {
            Tank currentTank = tanks.get(currentPlayerIndex);
            fill(0);
            textSize(16);
            text("Player " + currentTank.getSymbol() + "'s turn", 20, 30); // Displaying current player at the top left corner
            
            // Displaying parachute icon and count
            image(parachuteIcon, 150, 50, 40, 40);  
            text(": " + currentTank.getParachuteCount(), 190, 65); // Displaying updated parachute count

            if (currentTank.isLargerProjectileReady()) {
                fill(160, 32, 240);
                text("Larger Shot Ready", 350, 80); // Displaying this message on the HUD
            }
        }
    }

    /**
     * Initializes the scores for each tank at the beginning of the game. 
     * This method is called once during setup to set all player scores to zero.
     * It ensures that each tank starts the game with a score of zero.
     */

     private void initializeScores() {
        for (Tank tank : tanks) {
            scores.put(tank.getSymbol(), 0); // Initialize score for each tank
        }
    }

    /**
     * Displays the scores of all players on the game screen.
     * It shows the scores of all players participating in the game.
     */
    private void displayScores() {
        int startX = WIDTH - 170; 
        int startY = 50;
        int tableWidth = 160; 
        int rowHeight = 26; 
        int headerHeight = 30; 
        int outlineThickness = 4; 
    
        // Drawing the outline
        stroke(0); 
        strokeWeight(outlineThickness); 
        noFill(); 
        rect(startX, startY, tableWidth, headerHeight + rowHeight * scores.size() + 5);
    
        // Displaying the title
        fill(0); 
        textSize(16);
        textAlign(CENTER, CENTER);
        text("Scores", startX + tableWidth / 2, startY + headerHeight / 2);
        
        strokeWeight(4); 
        line(startX, startY + headerHeight, startX + tableWidth, startY + headerHeight);
    
        int yPos = startY + headerHeight + 5; // Start y-position for scores just below the line
    
        List<Character> sortedKeys = new ArrayList<>(scores.keySet());
        Collections.sort(sortedKeys);
    
        // Displaying each player's score
        textSize(15); // Adjusting text size to fit inside the box
        textAlign(LEFT, CENTER);
        for (char playerSymbol : sortedKeys) {
            int score = scores.get(playerSymbol);
            Tank tank = tanks.stream().filter(t -> t.getSymbol() == playerSymbol).findFirst().orElse(null);
            if (tank != null) {
                int[] colors = tank.getColors();
                fill(colors[0], colors[1], colors[2]); 
            } else {
                fill(0); 
            }
            String playerText = "Player " + playerSymbol + "           " + score;
            text(playerText, startX + 10, yPos + rowHeight / 2);

            yPos += rowHeight; 
        }
    
        // Resetting stroke weight for other uses in draw method
        strokeWeight(1);
    }

    /**
     * Updates the score based on the damage dealt by a projectile.
     * This method retrieves the current score for the player symbol and updates it by adding the damage dealt.
     *
     * @param playerSymbol The symbol of the player whose score is being updated.
     * @param damage       The amount of damage dealt.
     */
    public void updateScoreBasedOnDamage(char playerSymbol, float damage) {
        int currentScore = scores.getOrDefault(playerSymbol, 0); // Get current score or default to 0
        scores.put(playerSymbol, currentScore + (int) damage); // Update score by adding the damage dealt
    }

    /**
     * Handles post-update logic for tanks.
     * This method iterates through all tanks, checks if any tank needs post-update actions,
     * and performs those actions if necessary.
     * 
     * @see Tank#performPostUpdateActions() 
     * 
     */
    private void handlePostUpdateLogic() {
        for (Tank tank : tanks) {
            if (tank.needsUpdate()) {
                tank.performPostUpdateActions(); // Method in Tank that checks and resolves post-update conditions
            }
        }
    }   

    /**
     * Triggers an explosion at the given coordinates.
     * This method creates an explosion at the specified location and calculates the damage dealt to nearby tanks.
     * It also updates the game state, including damage to tanks and destruction of terrain.
     *
     * @param x                  The x-coordinate of the explosion.
     * @param y                  The y-coordinate of the explosion.
     * @param radius             The radius of the explosion.
     * @param firingPlayerSymbol The symbol of the player who initiated the explosion.
     */
    public void triggerExplosion(float x, float y, float radius, char firingPlayerSymbol) {

        explosions.add(new Explosion(this, x, y)); // Add explosion to the list

        for (int i = tanks.size() - 1; i >= 0; i--) {

            Tank tank = tanks.get(i);
            float dist = dist(x, y, tank.getX(), tank.getY()); // Calculate distance between explosion and tank

            if (dist <= radius) { // Check if tank is within the explosion radius
                float damage = (1 - (dist / radius)) * 60; // Calculate damage based on distance from explosion center
                tank.receiveDamage(damage); // Damage tank

                if (tank.getSymbol() != firingPlayerSymbol) {
                    updateScoreBasedOnDamage(firingPlayerSymbol, damage); // Update score if the tank is damaged by another player
                }

                if (tank.getHealth() <= 0 && !tank.hasExplodedThisCycle()) {
                    tank.markExploded(); // Mark tank as exploded if its health drops to or below 0
                    explosions.add(new Explosion(this, tank.getX(), tank.getY())); // Create explosion at tank's location
                }
            }
        }
        terrain.destroyTerrain(x, radius); // Destroy terrain within explosion radius
    }

    /**
     * Fires a projectile from the current tank.
     * This method calculates the initial velocity of the projectile based on the tank's angle and power,
     * then creates a new projectile object and adds it to the list of active projectiles.
     *
     * @param tank The tank from which the projectile is fired.
     */
    public void fireProjectile(Tank tank) {
        float angle = (float) tank.getTurretAngle(); // Get the angle of the tank's turret
        float velocityScale = map(tank.getPower(), 0, 100, 1, 60); // Scale velocity based on tank's power
        float initialVelocityX = velocityScale * (float) Math.cos(angle); // Calculate initial velocity in x direction
        float initialVelocityY = -velocityScale * (float) Math.sin(angle); // Calculate initial velocity in y direction
        float projectileRadius = tank.isLargerProjectileReady() ? 60 : 30; // Set projectile radius based on tank's readiness

        // Reset larger projectile readiness if fired
        if (tank.isLargerProjectileReady()) {
            tank.setLargerProjectileReady(false);
        }

        // Create and add the projectile to the list of active projectiles
        projectiles.add(new Projectile(this, tank.getX(), tank.getY() - Tank.SIZE / 2, initialVelocityX, initialVelocityY, tank.getColors(), tank.getSymbol(), projectileRadius));
    }

     /**
     * Handles keyboard inputs for player actions.
     * This method is invoked automatically when a key is pressed.
     * It allows players to control their tanks, adjust settings, and perform actions such as firing projectiles.
     */
    @Override
    public void keyPressed() {
        if (tanks.isEmpty()) {
            return; // Exit method if there are no tanks
        }

        // Ensure currentPlayerIndex is within bounds
        if (currentPlayerIndex >= tanks.size()) {
            currentPlayerIndex = 0;
        }

        Tank currentTank = tanks.get(currentPlayerIndex);
        char currentSymbol = currentTank.getSymbol();
        int currentPlayerScore = scores.getOrDefault(currentSymbol, 0);

        // Handle arrow key inputs
        if (key == CODED) {
            switch (keyCode) {
                case LEFT:
                    currentTank.move(-80 * 1 / frameRate); // Move tank left
                    break;
                case RIGHT:
                    currentTank.move(80 * 1 / frameRate); // Move tank right
                    break;
                case UP:
                    currentTank.adjustTurretAngle(Math.PI / 40); // Increase turret angle
                    break;
                case DOWN:
                    currentTank.adjustTurretAngle(-Math.PI / 40); // Decrease turret angle
                    break;
            }
        } else { // Handle other key inputs for the INFO1113 and COMP9003 powerups
            switch (key) {
                case 'w':
                    currentTank.increasePower(); // Increase shot power
                    break;
                case 's':
                    currentTank.decreasePower(); // Decrease shot power
                    break;
                case 'f':
                    if (currentPlayerScore >= 10 && currentTank.getFuel() + 200 <= 450) {
                        currentTank.addFuel(200); // Add fuel to the tank
                        scores.put(currentSymbol, currentPlayerScore - 10); // Deduct cost from the score
                    }
                    break;
                case 'p':
                    if (currentPlayerScore >= 15) {
                        currentTank.setParachuteCount(currentTank.getParachuteCount() + 1); // Increase parachute count
                        scores.put(currentSymbol, currentPlayerScore - 15); // Deduct cost from the score
                    }
                    break;
                case 'r':
                    if (!isGameOver) {
                        if (currentPlayerScore >= 20 && currentTank.getHealth() + 20 <= 100) {
                            currentTank.repairHealth(20); // Repair tank health
                            scores.put(currentSymbol, currentPlayerScore - 20); // Deduct cost from the score
                        }
                    } else {
                        restartGame(); // Restart the game if it's over
                    }
                    break;

                case 'x':
                    if (currentPlayerScore >= 20) {
                        currentTank.setLargerProjectileReady(true); // Set the flag for a larger projectile
                        scores.put(currentSymbol, currentPlayerScore - 20); // Deduct cost from the score
                    }
                    break;
                case ' ':
                    fireProjectile(currentTank); // Fire a projectile
                    nextPlayer(); // Switch to the next player's turn
                    break;
            }
        }
    }

        /**
     * Retrieves the list of tanks in the game.
     *
     * @return An ArrayList containing all tanks currently in the game.
     */
    public ArrayList<Tank> getTanks() {
        return tanks;
    }  

    /**
     * Returns the terrain object.
     *
     * @return The terrain object.
     */

    public Terrain getTerrain() {
        return terrain;  
    }

    /**
     * Returns the wind object.
     *
     * @return The wind object.
     */

    public Wind getWind() {
        return this.wind;
    }

    /**
     * Sets the number of parachutes for the tank.
     *
     * @param count The number of parachutes to set.
     */
    public void setParachuteCount(int count) {
        parachuteCount = count;
    }

    /**
     * Returns the parachute icon image.
     *
     * @return The parachute icon image.
     */

    public PImage getParachuteIcon() {
        return parachuteIcon;
    }

    /**
     * Determines the winner of the game based on the scores.
     *
     * @return The character representing the winner.
     */
    private String getWinner() {
        return Collections.max(scores.entrySet(), Map.Entry.comparingByValue()).getKey().toString();
    }

    /**
     * Sets the foreground color of the game components.
     *
     * @param color An array containing RGB values for the foreground color.
     */
    public void setForegroundColor(int[] color) {
        this.foregroundColor = color;
    }

    public boolean isGameOver() {
        return this.isGameOver;
    }

    /**
     * Main method to launch the application.
     *
     * @param args The command-line arguments.
     */

    public static void main(String[] args) {
        PApplet.main("Tanks.App");
    }

}
