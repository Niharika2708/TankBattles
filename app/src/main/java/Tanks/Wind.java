package Tanks;

import processing.core.PApplet;

/**
 * The {@code Wind} class simulates wind effects in the game, influencing the trajectory of projectiles.
 * Wind strength is represented as an integer value, where positive values indicate wind blowing to the right and negative values to the left.
 * The strength of the wind can vary randomly within a specified range and is dynamically updated throughout the game.
 */
public class Wind {

    private PApplet parent; 
    private int strength; // Wind strength where positive values blow right and negative values blow left.

    /**
     * Constructor for the Wind class. Initializes the wind strength with a random value within the range of -35 to 35.
     * 
     * @param parent The parent PApplet object which represents the sketch. This is used for accessing various Processing methods including random number generation.
     */
    public Wind(PApplet parent) {
        this.parent = parent; 
        this.strength = (int) parent.random(-35, 36); // Initialize wind strength with a random value from -35 to 35
    }

    /**
     * Updates the wind strength by adding a random incremental value between -5 and 5, and constrains the new strength within the range -35 to 35.
     * This method simulates the changing wind conditions over time.
     */
    public void update() {
        this.strength += (int) parent.random(-5, 6); // Randomly change wind strength by a value between -5 and 5
        this.strength = PApplet.constrain(this.strength, -35, 35); // Ensure the wind strength stays within the range -35 to 35
    }

    /**
     * Retrieves the current wind strength, which can influence how projectiles are moved by the wind in the game.
     *
     * @return The current wind strength, where positive values indicate a rightward direction and negative values a leftward direction.
     */
    public int getStrength() {
        return this.strength;
    }
}