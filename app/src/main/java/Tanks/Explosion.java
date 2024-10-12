package Tanks;

import processing.core.PApplet;

/**
 * The {@code Explosion} class models an explosion effect at a specific location in the game.
 * Utilizing the Processing library, it creates a visual explosion effect through animated, expanding circles.
 * This class manages the animation of these circles to simulate the rapid expansion of an explosion visually.
 */
public class Explosion {
    private PApplet parent;  
    private float x, y;  // Coordinates representing the center of the explosion.
    private float duration = 0.2f;  // Duration of the explosion effect in seconds.
    private float elapsedTime = 0;  // Time elapsed since the start of the explosion.
    private float maxRadius = 30;  // Maximum radius of the outermost circle (red).
    private float orangeRadius = 15;  // Maximum radius of the middle circle (orange).
    private float yellowRadius = 6;  // Maximum radius of the innermost circle (yellow).

    /**
     * Constructs an Explosion object with specified parameters.
     * 
     * @param parent The PApplet where the explosion will be rendered. This object is responsible for drawing operations.
     * @param x The x-coordinate of the explosion's center.
     * @param y The y-coordinate of the explosion's center.
     */
    public Explosion(PApplet parent, float x, float y) {
        this.parent = parent;
        this.x = x;
        this.y = y;
    }

    /**
     * Renders the explosion onto the canvas. This method draws three concentric circles with varying colors representing the stages of the explosion.
     * The radii of these circles increase over time to create an expanding effect.
     */
    public void display() {
        float progress = elapsedTime / duration;  // Calculate the progress percentage of the explosion.
        float redRadius = progress * maxRadius;  // Current radius of the red (outer) circle.
        float orangeRadiusProgress = progress * orangeRadius;  // Current radius of the orange (middle) circle.
        float yellowRadiusProgress = progress * yellowRadius;  // Current radius of the yellow (inner) circle.

        // Draw the outer red circle.
        parent.stroke(255, 0, 0);
        parent.fill(255, 0, 0);
        parent.ellipse(x, y, redRadius * 2, redRadius * 2);

        // Draw the middle orange circle.
        parent.stroke(255, 165, 0);
        parent.fill(255, 165, 0);
        parent.ellipse(x, y, orangeRadiusProgress * 2, orangeRadiusProgress * 2);

        // Draw the inner yellow circle.
        parent.stroke(255, 255, 0);
        parent.fill(255, 255, 0);
        parent.ellipse(x, y, yellowRadiusProgress * 2, yellowRadiusProgress * 2);
    }

    /**
     * Updates the explosion's animation state based on the elapsed time. This method is called within the game's main update loop.
     * It advances the animation by increasing the elapsed time and constrains it to the specified duration of the explosion.
     *
     * @param deltaTime The time in seconds since the last update, used to increment the explosion's elapsed time.
     */
    public void update(float deltaTime) {
        elapsedTime += deltaTime;  // Increment the elapsed time by the passed delta time.
        if (elapsedTime > duration) {
            elapsedTime = duration;  // Cap the elapsed time at the explosion's duration to prevent overruns.
        }
    }

    /**
     * Determines if the explosion's animation has completed based on whether the elapsed time has reached or exceeded the preset duration.
     *
     * @return true if the explosion animation has finished; otherwise, false.
     */
    public boolean isFinished() {
        return elapsedTime >= duration;
    }
}