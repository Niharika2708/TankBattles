package Tanks;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Collections;
import java.util.Comparator;
import processing.data.JSONObject;

/**
 * The {@code Terrain} class is responsible for managing and rendering the game terrain,
 * which includes handling the terrain's physical layout, visual representation, and interactions such as collisions.
 * It uses a configuration file to set initial parameters and contains methods to manage terrain updates.
 */
public class Terrain {

    private PApplet parent;  
    private JSONObject config;  // JSON object containing configuration details for the terrain.
    private App app;  
    private int[] heights;  // Array storing the vertical heights of the terrain at each horizontal index.
    private char[][] levelLayout;  // 2D array representing the spatial layout of the terrain and any objects on it.
    private int[] currentForegroundColor;  // RGB values for the foreground color used in terrain rendering.
    private HashMap<String, PImage> images = new HashMap<>();  // Map storing terrain images for rendering.
    private ArrayList<PVector> trees = new ArrayList<>();  // List of tree positions for trees.
    private ArrayList<Tank> tanks;  // List of tanks interacting with the terrain.

    public static final int CELLSIZE = 32; 
    public static final int WIDTH = 864;  
    public static final int HEIGHT = 640; 

    /**
     * Constructs a Terrain object with references to the Processing sketch, a list of tanks, the main application, and a configuration JSON.
     *
     * @param parent The Processing sketch, used for drawing.
     * @param tanks An ArrayList of {@code Tank} objects interacting with the terrain.
     * @param app Reference to the main application managing game logic.
     * @param config A JSONObject containing configuration settings for the terrain.
     */
    public Terrain(PApplet parent, ArrayList<Tank> tanks, App app, JSONObject config) {
        this.parent = parent;
        this.tanks = tanks;
        this.app = app;
        this.config = config;

        // Check if the configuration JSON object is correctly loaded
        if (this.config == null) {
            System.out.println("Configuration JSON is null");  // Output error if config is not loaded
        } else {
            System.out.println("Configuration JSON is not null");  // Confirm config is loaded
        }
    }

    /**
     * Loads the background image from a specified file path and stores it in the terrain's image map.
     * 
     * @param imagePath The file path to the background image to be loaded.
     */
    public void loadBackground(String imagePath) {
        PImage background = parent.loadImage(imagePath); // Load the image from the provided path.
        if (background != null) {
            images.put("background", background); // Store the background image in the map if loaded successfully.
        }
    }


    /**
     * Loads the tree image from a specified file path and stores it in the terrain's image map.
     * If the tree image fails to load, the method attempts to load a default image as a diagnostic step.
     * 
     * @param imagePath The file path to the tree image to be loaded.
     */
    public void loadTreeImage(String imagePath) {
        PImage treeImage = parent.loadImage(imagePath); // Load the tree image from the provided path.
        if (treeImage != null) {
            images.put("tree", treeImage); // Store the tree image in the map if loaded successfully.
        } else {
            System.out.println("Failed to load tree image: " + imagePath); // Log an error if the tree image fails to load.

            PImage testImage = parent.loadImage("src/main/resources/Tanks/tree2.png");

            if (testImage != null) {
                System.out.println("Default image loaded successfully, check the path and file of tree images.");
            } else {
                System.out.println("Default image also failed to load. Check the image loading setup.");
            }
        }
    }

    /**
     * Loads level configuration from the given JSONObject that specifies the paths to layout, background,
     * foreground color, and optional tree images. This method also sets the foreground color and loads the
     * necessary images and level layout based on the configuration provided.
     *
     * @param level A JSONObject containing configuration for a specific level including paths to resources.
     */
    public void loadLevelConfig(JSONObject level) {
        String layoutPath = level.getString("layout");  // Get the path to the level layout file.
        String backgroundPath = "src/main/resources/Tanks/" + level.getString("background");  // Construct the full path to the background image.
        String foregroundColor = level.getString("foreground-colour");  // Retrieve the foreground color from the configuration.
        int[] fgColor = parseColorConfig(foregroundColor);  // Parse the foreground color configuration.

        setForegroundColor(fgColor);  // Set the parsed foreground color.

        String treeImagePath = !level.isNull("trees") ? "src/main/resources/Tanks/" + level.getString("trees") : null;  // Get the path to the tree image if specified.

        loadBackground(backgroundPath);  // Load the background image.
        loadLevel(layoutPath);  // Load the level layout.
        if (treeImagePath != null) {
            loadTreeImage(treeImagePath);  // Load the tree image if specified.
        }
    }

    /**
     * Loads the level layout from text file, setting terrain heights and processing any specific terrain features
     * such as trees. This method reads the level layout file, initializes terrain heights, and
     * sets up the terrain smoothing and other post-processing tasks like adding tanks and initializing tree positions.
     *
     * @param levelPath The file path to the level layout text file.
     */
    public void loadLevel(String levelPath) {
        String[] lines = parent.loadStrings(levelPath);  // Load all lines from the level layout file.
        heights = new int[WIDTH + 32];  // Initialize the heights array to cover the entire level width.
        Arrays.fill(heights, HEIGHT);  // Set initial heights to the maximum, assuming a flat terrain at start.

        for (int y = 0; y < lines.length; y++) {
            for (int x = 0; x < lines[y].length(); x++) {
                if (lines[y].charAt(x) == 'X') {  // Check for terrain markers that indicate elevation changes.
                    int baseHeight = HEIGHT - (20 - y) * CELLSIZE;  // Calculate the base height for this terrain segment.
                    for (int px = x * CELLSIZE; px < (x + 1) * CELLSIZE && px < WIDTH + 32; px++) {
                        heights[px] = Math.min(heights[px], baseHeight);  // Set the terrain height, ensuring it doesn't exceed current heights.
                    }
                }
            }
        }

        smoothTerrain();  
        parseLevelLayout(lines);  // Parse the entire layout for objects and additional features.
        addTanks(levelLayout, config);  // Add tanks based on the level layout and configuration.
        initializeTrees(lines);  // Initialize tree positions based on the layout.
    }

    /**
     * Initializes the trees based on the provided lines, adding them to the game.
     * Clears previous trees to avoid duplication across levels.
     *
     * @param lines The array of strings representing the terrain layout.
     */
    private void initializeTrees(String[] lines) {

        PImage treeImage = images.get("tree"); // Load the tree image if not already loaded
        if (treeImage == null) { 
            treeImage = parent.loadImage("src/main/resources/Tanks/tree1.png"); //Using default tree if null
            images.put("tree", treeImage);
        }
    
        // Clear previous trees to avoid duplication across levels
        trees.clear();
        
        // Proceed to initialize trees if tree image is available
        if (treeImage != null) {
            for (int y = 0; y < lines.length; y++) {  // Iterate through each line
                for (int x = 0; x < lines[y].length(); x++) {    // Iterate through each character in the line
    
                    if (lines[y].charAt(x) == 'T') {
                        // Calculate tree position
                        int treeX = x * CELLSIZE + CELLSIZE / 2;
                        int treeBaseHeight = getHeightAtTerrain(treeX);
                        int randomOffset = (int) parent.random(-30, 31); // Random offset from -30 to +30 pixels
                        int treeY = treeBaseHeight - treeImage.height + randomOffset; 
                     
                        trees.add(new PVector(treeX, Math.max(treeY, 0)));    // Ensure tree is not above the screen
                    }
                }
            }
        }
    }
    
    /**
     * Retrieves the height of the terrain at the specified x-coordinate.
     *
     * @param x The x-coordinate at which to retrieve the terrain height.
     * @return The height of the terrain at the specified x-coordinate.
     */
    public int getHeightAtTerrain(float x) {    
        int constrainedX = PApplet.constrain((int) x, 0, WIDTH + 32 - 1); // Ensure x-coordinate is within bounds
        return heights[constrainedX];  
    } 
    
    /**
     * Smooths the terrain heights to create a more uniform appearance.
     * Performs two iterations of averaging over adjacent terrain heights.
     */
    private void smoothTerrain() { 
        int[] smoothedHeights = new int[WIDTH + 32];  // Temporary array to hold smoothed terrain heights
       
        for (int i = 0; i < 2; i++) {  // Perform two iterations of smoothing
            for (int x = 0; x < WIDTH + 32; x++) {
                int sum = 0, count = 0;
                for (int j = x; j <= x + 32; j++) { // Calculate sum of terrain heights within a window of 32 pixels
                    int nx = PApplet.constrain(j, 0, WIDTH + 32 - 1);  // Constrain the index to ensure it doesn't go out of bounds
                    sum += heights[nx];
                    count++;
                }
                // Compute the average and assign to smoothedHeights array
                smoothedHeights[x] = sum / count;
            }
            // Copy smoothed heights back to the original heights array
            System.arraycopy(smoothedHeights, 0, heights, 0, WIDTH + 32);
        }
    }

    /**
     * Parses the layout of the level based on the provided array of strings.
     * Converts each string into a character array and stores them in levelLayout.
     *
     * @param lines The array of strings representing the layout of the level.
     */
    private void parseLevelLayout(String[] lines) {
        levelLayout = new char[lines.length][];  // Initialize levelLayout array with dimensions matching the number of lines    
        for (int i = 0; i < lines.length; i++) { // Iterate through each line
            levelLayout[i] = lines[i].toCharArray();  // Convert the line to a character array and store in levelLayout
        }
    }

    /**
     * Sets the foreground color used for drawing terrain.
     *
     * @param fgColor An integer array representing the RGB components of the foreground color.
     */
    public void setForegroundColor(int[] fgColor) {
        currentForegroundColor = fgColor;  
    }
    
    /**
     * Draws the terrain based on the current terrain heights.
     * Uses the current foreground color for drawing.
     * Additionally, draws tanks and trees on the terrain.
     */
    public void drawTerrain() {
        parent.stroke(currentForegroundColor[0], currentForegroundColor[1], currentForegroundColor[2]);  // Set the stroke color using the foreground color
        for (int x = 0; x < WIDTH + 32; x++) { // Draw rectangles representing the terrain heights 
            parent.rect(x, getHeightAtTerrain(x), 1, HEIGHT-getHeightAtTerrain(x));
        }
        drawTanks();
        drawTrees();
    }

    /**
     * Draws trees on the terrain using the tree image.
     * Tree positions are adjusted based on terrain height and screen boundaries.
     * Uses the tree image with a width and height of 32 pixels.
     */
    private void drawTrees() {
        PImage treeImage = images.get("tree");  // Get the tree image from the images map

        if (treeImage != null) {
            for (PVector tree : trees) {   // Iterate through each tree vector
                int treeBaseHeight = getHeightAtTerrain(tree.x);  // Get the base height of terrain at tree position
                int treeY = treeBaseHeight - 32;   // Adjust tree position based on height and image dimensions
                treeY = Math.max(treeY, 0); // Ensure tree does not go above the screen
                parent.image(treeImage, tree.x - 16, treeY, 32, 32);  // Draw tree image at adjusted position with width and height of 32 pixels
            }
        }
    }

    /**
     * Draws tanks on the terrain based on their positions.
     * Tank colors and positions are retrieved from tank objects.
     * Tanks are positioned at the terrain height and drawn with their bottom aligned to it.
     */
    public void drawTanks() {
       
        for (Tank tank : tanks) {  // Iterate through each tank object
            
            parent.stroke(0);   
            int[] colors = tank.getColors();
            parent.fill(colors[0], colors[1], colors[2]);
           
            float terrainHeight = getHeightAtTerrain(tank.getX());  // Get the terrain height at tank's x-coordinate 
            float tankBottomY = terrainHeight; // Set tank's y-coordinate to the terrain height
            tank.setY(tankBottomY);
            
            tank.draw(); // Draw the tank at its position
        }
    }

    /**
     * Adds tanks to the game based on the provided level layout and configuration.
     * Uses player colors defined in the configuration to assign colors to tanks.
     * Tank positions are determined by the layout, excluding specified symbols ('X' and 'T').
     *
     * @param levelLayout The layout of the level represented as a 2D array of characters.
     * @param config      The JSON object containing configuration data, including player colors.
     */
    public void addTanks(char[][] levelLayout, JSONObject config) {
       
        if (config == null) {  // If configuration is null, return without adding tanks
            return;
        }
        JSONObject playerColors = config.getJSONObject("player_colours");  // Retrieve player colors from the configuration
        
        for (int y = 0; y < levelLayout.length; y++) {  // Iterate through each cell in the level layout
            for (int x = 0; x < levelLayout[y].length; x++) {
                char symbol = levelLayout[y][x];

                if (Character.isUpperCase(symbol) && symbol != 'X' && symbol != 'T') {      
                    String colorConfig = "255,255,255"; // Default color configuration

                    try {
                        colorConfig = playerColors.getString(String.valueOf(symbol));  // Retrieve color configuration from player colors JSON object
                    } catch (Exception e) {
            
                        System.out.println("No color defined for player " + symbol + ", using default color.");  // If color is not defined, use default color and print a message
                    }         
                    int[] colors = parseColorConfig(colorConfig); // Parse color configuration into RGB values
                    // Calculate tank position based on cell position and terrain height
                    int tankX = x * CELLSIZE;
                    int terrainHeight = getHeightAtTerrain(tankX);
                    tanks.add(new Tank(parent, this, symbol, tankX, terrainHeight, colors)); // Create and add tank object to the tanks list
                }
            }
        }
        sortTanks();   // Sort tanks after adding them
    }

    /**
     * Parses color configuration string into an array of RGB values.
     * Supports "random" keyword for generating random colors.
     * Returns default white color if parsing fails or configuration is invalid.
     *
     * @param colorConfig The color configuration string in the format "R,G,B" or "random".
     * @return An integer array containing RGB values.
     */
    public int[] parseColorConfig(String colorConfig) {
        try {
            if (colorConfig.equals("random")) {  // Check if color configuration is set to "random"
                // Generate random RGB values and return
                return new int[]{(int) parent.random(256), (int) parent.random(256), (int) parent.random(256)};
            } else {
                String[] colorValues = colorConfig.split(","); // Parse RGB values from the configuration string
                return new int[]{
                        Integer.parseInt(colorValues[0]),
                        Integer.parseInt(colorValues[1]),
                        Integer.parseInt(colorValues[2])
                };
            }
        } catch (Exception e) {
            return new int[]{255, 255, 255};  // Return default white color in case of parsing error
        }
    }

   /**
     * Destroys terrain within the specified impact radius centered at the given x-coordinate.
     * Updates terrain heights within the affected area based on the explosion.
     * Additionally, adjusts tank positions and initiates parachute deployment if necessary.
     *
     * @param impactX The x-coordinate of the explosion impact.
     * @param radius  The radius of the explosion affecting the terrain.
     */
    public void destroyTerrain(float impactX, float radius) {
        // Determine the range of terrain affected by the explosion
        int startX = Math.max(0, (int) (impactX - radius));
        int endX = Math.min(WIDTH - 1, (int) (impactX + radius));
        float centerY = getHeightAtTerrain(impactX);  // Original center height at the impact point
    
        // Iterate over the affected terrain area and update terrain heights
        for (int x = startX; x <= endX; x++) {
            float distance = Math.abs(x - impactX);
            if (distance < radius) {
                float depth = (float) Math.sqrt(radius * radius - distance * distance);
                int newY = (int) (centerY + depth);
                heights[x] = Math.max(heights[x], newY);
            }
        }
    
        // Update tank positions after modifying the terrain
        for (Tank tank : tanks) {
            float terrainHeight = getHeightAtTerrain(tank.getX());
            
            // Adjust tank position if terrain is below tank's base
            if (getHeightAtTerrain(tank.getX()) > tank.getY() + Tank.SIZE / 2) {
                tank.setY(getHeightAtTerrain(tank.getX()) - Tank.SIZE / 2);
            }
            // Deploy parachute if tank is above the modified terrain
            if (terrainHeight < tank.getY()) {
                tank.checkAndDeployParachute();
            }
        }
    
        // Adjust tanks after impact if terrain below them is destroyed
        adjustTanksAfterImpact();
    }

    /**
     * Adjusts tank behavior after an impact, setting tanks to descend with parachutes
     * if terrain below them is destroyed.
     */
    private void adjustTanksAfterImpact() {
        for (Tank tank : tanks) {
            if (getHeightAtTerrain(tank.getX()) < tank.getY()) {
                tank.setDescendingWithParachute(true);  // Terrain below tank is destroyed, adjust tank behavior
            }
        }
    }

    /**
     * Sorts the tanks based on their symbols (player IDs).
     */
    private void sortTanks() {
        Collections.sort(tanks, Comparator.comparing(Tank::getSymbol));
    }

    /**
     * Retrieves the images used in the game.
     *
     * @return A HashMap containing image names as keys and corresponding PImage objects.
     */
    public HashMap<String, PImage> getImages() {
        return images;
    }

    /**
     * Retrieves the current terrain heights array.
     *
     * @return An array containing the current terrain heights.
     */
    public int[] getHeights() {
        return heights;
    }

    /**
     * Checks if terrain is below the given tank's position.
     *
     * @param tank The tank to check terrain below.
     * @return True if terrain is below the tank's position, false otherwise.
     */
    public boolean isTerrainBelowTank(Tank tank) {
        float tankX = tank.getX();
        float terrainHeight = getHeightAtTerrain(tankX);
        return terrainHeight < tank.getY();
    }

    /**
     * Retrieves the parent application object.
     *
     * @return The parent application object.
     */
    public App getApp() {
        return app;
    }

    /**
     * Sets the configuration object for the game.
     *
     * @param config The JSON object containing game configuration data.
     */
    public void setConfig(JSONObject config) {
        this.config = config;  // Method to set configuration when loading levels
    }

}
