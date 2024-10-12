package Tanks;

/**
 * The {@code Projectile} class represents a projectile fired by a tank in the game.
 * It manages the projectile's movement, interactions with game objects such as terrain, and visual representation.
 * The class also handles collision detection, projectile removal, and triggering explosions upon impact.
 */
public class Projectile {

    private App app;  
    private float x, y;  // Current x and y coordinates of the projectile.
    private int[] colors;  
    private char firingPlayerSymbol;  
    private float velocityX, velocityY;  // Current velocity components of the projectile.
    private boolean markForRemoval = false;  // Flag to indicate whether the projectile should be removed from the game.
    private static final float GRAVITY = 3.6f;  
    private float radius;  // Radius of the projectile, influencing its visual size and impact area.
    private boolean isLarger;  // Flag indicating whether the projectile is considered larger based on its radius.

    /**
     * Constructs a new Projectile instance. Initializes the projectile's position, velocity, appearance, and other properties.
     *
     * @param app The main application managing game resources and state.
     * @param x Initial x-coordinate of the projectile.
     * @param y Initial y-coordinate of the projectile.
     * @param velocityX Initial horizontal velocity of the projectile.
     * @param velocityY Initial vertical velocity of the projectile.
     * @param colors RGB color values of the projectile.
     * @param firingPlayerSymbol Character symbol identifying the firing player.
     * @param radius Radius of the projectile, affecting its size and explosion impact.
     */
    public Projectile(App app, float x, float y, float velocityX, float velocityY, int[] colors, char firingPlayerSymbol, float radius) {
        this.app = app;
        this.x = x;
        this.y = y;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.colors = colors;
        this.firingPlayerSymbol = firingPlayerSymbol;
        this.radius = radius;
        this.isLarger = radius > 30;  // Projectiles with a radius greater than 30 are considered larger.
    }

    /**
     * Draws the projectile on the game canvas. Adjusts visual attributes based on projectile properties.
     */
    public void draw() {
        app.stroke(128, 0, 128);  
        app.fill(colors[0], colors[1], colors[2]);  // Fill color from provided RGB values
        app.strokeWeight(isLarger ? 5 : 0);  // Thicker stroke for larger projectiles
        float drawingRadius = isLarger ? radius * 0.3f : 6;  // Adjust drawing radius based on projectile size
        app.ellipse(x, y, drawingRadius * 2, drawingRadius * 2);  // Draw the projectile
    }

    /**
     * Updates the projectile's position and state. Applies gravity, adjusts for wind, checks for terrain collision, and handles explosion triggering.
     */
    public void update() {
        x += velocityX;  // Update horizontal position
        y += velocityY;  // Update vertical position

        velocityY += GRAVITY;  // Apply gravity to vertical velocity

        // Wind effect adjustments
        if (y < app.getTerrain().getHeightAtTerrain(x)) {
            float windEffect = app.getWind().getStrength() * 0.01f;  // Calculate wind effect on velocity
            velocityX += windEffect;
        }

        // Check for collision with terrain and handle potential explosion
        if (checkCollisionWithTerrain()) {
            markForRemoval = true;  // Mark the projectile for removal after explosion
        }
    }

    /**
     * Checks for collision between the projectile and the terrain. Triggers an explosion if a collision occurs.
     * @return true if a collision occurs, otherwise false.
     */
    private boolean checkCollisionWithTerrain() {
        int terrainHeight = app.getTerrain().getHeightAtTerrain(x);
        if (y >= terrainHeight) {
            handleExplosion();  // Trigger explosion upon collision
            return true;
        }
        return false;
    }

    /**
     * Triggers an explosion at the current projectile's position.
     */
    private void handleExplosion() {
        app.triggerExplosion(x, y, radius, firingPlayerSymbol);  // Trigger explosion using app's method
    }

    /**
     * Checks if the projectile is offscreen, which would require its removal.
     * @return true if the projectile is offscreen, otherwise false.
     */
    public boolean isOffscreen() {
        return x < 0 || x > app.width || y > app.height;
    }

    /**
     * Determines if the projectile should be removed from the game.
     * @return true if the projectile is marked for removal or is offscreen, otherwise false.
     */
    public boolean shouldRemove() {
        return markForRemoval || isOffscreen();
    }

    /**
     * Returns the RGB color values of the projectile.
     * @return Array of integers representing the colors of the projectile.
     */
    public int[] getColors() {
        return colors;
    }

    public float getX() {
        return this.x;
    }
    
    public float getY() {
        return this.y;
    }
    
    public void setY(float y) {
        this.y = y;
    }
}
