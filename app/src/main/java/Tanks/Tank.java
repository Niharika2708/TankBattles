package Tanks;

import processing.core.PApplet;
import processing.core.PImage;
import java.util.ArrayList;

/**
 * The {@code Tank} class models a tank in the game, which players can control to move, aim, and fire projectiles. 
 * It maintains various states and behaviors of the tank, such as position, health, power, and actions like deploying parachutes.
 */
public class Tank {

    private PApplet parent; 
    public static final int SIZE = 32; // Constant defining the size of the tank.
    private ArrayList<Tank> tanks; // Collection of all tanks.

    private char symbol; // Sybol representing the tank, used for identification.
    private Terrain terrain; 
    private float x, y; // Current x and y coordinates of the tank.
    private int[] colors; 
    private float fuel = 250; // Amount of fuel the tank starts with, limiting its movement capacity.

    private boolean markForRemoval = false; // Flag to indicate whether the tank should be removed from the game, e.g., if destroyed.
    private boolean hasExplodedThisCycle = false; // Prevents multiple explosion processing in the same update cycle.
    private boolean exploded = false; // Indicates if the tank has already exploded to prevent recursive explosion logic.
    private boolean needsUpdate = false; // Marks the tank for necessary updates post-processing, such as movements or actions.

    private PImage parachuteIcon;
    private int parachuteCount = 3; 
    private boolean descendingWithParachute = false; 

    private boolean largerProjectileReady = false; 

    private double turretAngle = Math.PI / 2; // Initial angle for the turret, pointing upwards.
    private float power = 50; 
    private float maxPower = 100; 
    private float health = 100; 


    /**
     * Constructor for the Tank class.
     *
     * @param parent  The parent PApplet object.
     * @param terrain The terrain object associated with the tank.
     * @param symbol  The symbol representing the tank.
     * @param x       The x-coordinate of the tank's position.
     * @param y       The y-coordinate of the tank's position.
     * @param colors  The RGB color values for the tank.
     */

    public Tank(PApplet parent, Terrain terrain, char symbol, float x, float y, int[] colors) {
        this.parent = parent;
        this.terrain = terrain;
        this.symbol = symbol;
        this.x = x;
        this.y = terrain.getHeightAtTerrain(x); 
        this.colors = colors;
        this.parachuteIcon = parent.loadImage("src/main/resources/Tanks/parachute.png");
    }

    /**
     * Draws the tank on the screen.
     */

     public void draw() {
        // Set the color for the tank body - a filled ellipse with no border.
        parent.fill(colors[0], colors[1], colors[2]);
        parent.noStroke();
    
        // Define dimensions for the tank body based on the size constant.
        float bodyWidthTop = SIZE * 0.6f; // Width of the upper part of the tank body.
        float bodyWidthBottom = SIZE * 0.8f; // Width of the lower part of the tank body.
        float bodyHeight = SIZE * 0.3f; // Height of the tank body.
    
        // Draw the lower part of the tank body.
        parent.ellipse(x, y, bodyWidthBottom, bodyHeight);
        // Draw the upper part of the tank body slightly above the lower part.
        parent.ellipse(x, y - SIZE * 0.2f, bodyWidthTop, bodyHeight);
    
        // Check if the tank is descending with a parachute and draw it.
        if (descendingWithParachute) {
            // Display the parachute image centered on the tank and above it.
            parent.image(parachuteIcon, x - parachuteIcon.width / 2, y - SIZE * 2, parachuteIcon.width, parachuteIcon.height);
        }
        
        // Drawing the turret.
        parent.stroke(0);
        parent.strokeWeight(4);
        // Calculate the end coordinates for the turret line based on the turret angle.
        parent.line(x, y - SIZE * 0.3f, x + (float) (15 * Math.cos(turretAngle)), y - SIZE * 0.3f - (float) (15 * Math.sin(turretAngle)));
    }

    /**
     * Updates the state of the tank based on its interaction with the game, specifically terrain and gravity.
     * The method handles the physics of falling and parachuting, and also determines if the tank needs to be removed from the game.
     * 
     * It performs several checks:
     * 1. Determines if the tank is airborne.
     * 2. Deploys a parachute if available and necessary.
     * 3. Adjusts the tank's vertical position based on whether it is descending with a parachute.
     * 4. Resets parachute usage and count upon landing.
     * 5. Marks the tank for removal if it falls below the visible game area or sinks below the terrain without parachutes.
     *
     * Falling speed is adjusted based on whether the tank is using a parachute. Without a parachute, the fall is faster.
     */
    public void update() {
        float dt = 1.0f / parent.frameRate; // Compute time step based on the frame rate for smooth animation.
        float terrainHeight = terrain.getHeightAtTerrain(x); // Get the current terrain height at the tank's x position.
        boolean isAirborne = y > terrainHeight; // Check if the tank is airborne.

        if (isAirborne) {
            if (parachuteCount > 0 && !descendingWithParachute) {
                deployParachute(); // Deploy parachute if available and not already descending with one.
            }
            if (!descendingWithParachute) {
                y += 120 * dt; // Apply faster falling speed when not using a parachute.
            } else {
                y += 60 * dt; // Apply slower descent speed with parachute.
            }
        } else {
            y = terrainHeight; // Adjust y position to match the terrain level.
            descendingWithParachute = false; // Reset parachute status upon landing.
        }

        // Additional logic to handle landing with a parachute.
        if (y <= terrainHeight && descendingWithParachute) {
            descendingWithParachute = false;
            parachuteCount = Math.max(0, parachuteCount - 1); // Reduce parachute count after landing.
        }

        // Check if the tank should be removed from the game due to falling off the screen.
        if (y >= parent.height) {
            markForRemoval = true; // Mark the tank for removal if it goes below the visible area of the screen.
        }

        // Check for tank removal when there are no parachutes left and the tank is below terrain level.
        if (isAirborne && !descendingWithParachute && parachuteCount == 0) {
            markForRemoval = true; // Mark the tank for removal if it sinks below terrain without parachutes.
        }
    }

    /**
     * Deploys a parachute for the tank if available and not already deployed.
     * This method checks if the tank has parachutes left and if it is not already descending with one.
     * Upon deploying a parachute, it reduces the parachute count by one and sets the descending flag.
     * Additionally, it updates the global parachute count in the {@code App} class to ensure UI and game state synchronization.
     * 
     * @see App#setParachuteCount(int) Method in the App class that updates the parachute count in the game's main application state.
     */
    public void deployParachute() {
        if (parachuteCount > 0 && !descendingWithParachute) {
            descendingWithParachute = true;
            parachuteCount--;
            // Inform the App class about the updated parachute count
            App app = (App) parent;
            app.setParachuteCount(parachuteCount);
        }
    }

    /**
     * Moves the tank horizontally by a specified amount. The movement is constrained by the amount of available fuel.
     * If there is enough fuel to cover the distance, the tank's position is updated, and fuel is consumed accordingly.
     * The tank's vertical position is also adjusted to match the terrain height at its new horizontal position.
     * 
     * @param dx The amount to move the tank horizontally, can be negative for leftward movement or positive for rightward movement.
     */
    public void move(float dx) {
        float distance = Math.abs(dx); // Calculate the absolute distance to ensure positive fuel deduction.
        if (fuel >= distance) { 
            this.x += dx; // Update the tank's horizontal position.
            this.y = terrain.getHeightAtTerrain(this.x); // Adjust the tank's vertical position to align with the terrain.
            fuel -= distance; // Deduct the traveled distance from the tank's fuel reserve.
        }
    }

    /**
     * Adjusts the turret's angle by a specified delta, allowing for aiming adjustments.
     * The angle adjustment is scaled for quicker responsiveness.
     * 
     * @param deltaAngle The amount by which to change the turret's angle, positive to increase and negative to decrease.
     */
    public void adjustTurretAngle(double deltaAngle) {
        this.turretAngle += 2 * deltaAngle; // Apply the delta angle, scaled by 2 for increased sensitivity.
        parent.redraw(); // Trigger a redraw in the parent PApplet to update the display with the new turret angle.
    }
    
    /**
     * Increases the firing power of the tank. The power is used to determine the projectile's speed and range.
     * The power level can only increase if it is below both the maximum power level and the tank's current health.
     * This prevents the power from exceeding the tank's operational capability as it gets damaged.
     */
    public void increasePower() {
        if (power < maxPower && power < health) { 
            power += 1; // Increment the power.
            if (power > maxPower) { // Ensure the power does not exceed its maximum limit.
                power = maxPower; // Cap the power at the maximum level.
            }
        }
    }

    /**
     * Decreases the firing power of the tank. This is useful for fine-tuning the range and impact of fired projectiles.
     * The power level can't decrease below zero.
     */
    public void decreasePower() {
        if (power > 0) { // Ensure that power is above zero to avoid unnecessary calculations.
            power -= 1; 
            if (power < 0) { // Prevent power from dropping below zero, which is not feasible.
                power = 0; // Set power to zero if it falls below after decrementing.
            }
        }
    }

    /**
     * Adds a specified amount of fuel to the tank's existing fuel reserve. 
     * The fuel capacity is capped at a maximum limit to prevent overfilling.
     * 
     * @param amount The amount of fuel to add to the tank's reserves.
     */
    public void addFuel(float amount) {
        this.fuel += amount; 
        this.fuel = Math.min(this.fuel, 450); // Cap the fuel at the maximum limit of 450 to prevent overfilling.
    }

    /**
     * Repairs the tank's health by a specified amount. The health of the tank cannot exceed the maximum health limit.
     * 
     * @param repairAmount The amount by which to repair the tank's health.
     */
    public void repairHealth(int repairAmount) {
        health += repairAmount; // Increment the tank's health by the repair amount.
        if (health > 100) {
            health = 100; // Cap health at 100 to ensure it does not exceed maximum health.
        }
    }

    /**
     * Checks if conditions are right for deploying a parachute when the tank is descending.
     * If a parachute is available and not already in use, it deploys one to slow down the descent.
     * This method is called during game updates when the tank is airborne and needs to manage its descent safely.
     */
    public void checkAndDeployParachute() {
        if (parachuteCount > 0 && !descendingWithParachute) {
            deployParachute(); // Deploy a parachute if not already descending with one and parachutes are available.
        }
    }


    /**
     * Performs any necessary actions after the tank's main update logic. 
     * This method is called at the end of an update cycle to finalize changes.
     */
    public void performPostUpdateActions() {
        checkAndDeployParachute(); // Deploy parachutes if conditions are met.
        needsUpdate = false; // Reset the flag indicating the tank needs an update.
    }

    /**
     * Processes damage received by the tank and updates its health accordingly.
     * 
     * @param damage The amount of damage the tank receives.
     */
    public void receiveDamage(float damage) {
        health -= damage; 
        if (health < 0) {
            health = 0; // Ensure health does not drop below zero.
        }
        if (power > health) {
            power = health; 
        }
        markForUpdate(); // Flag the tank for an update following damage reception.
    }

    /**
     * Marks the tank as having exploded during the current game cycle.
     * This method sets the flags to indicate that the tank has exploded and should be removed from the game.
     */
    public void markExploded() {
        this.hasExplodedThisCycle = true;
        this.markForRemoval = true;
    }

    /**
     * Checks if the tank has exploded during the current game cycle.
     * 
     * @return {@code true} if the tank has exploded this cycle, {@code false} otherwise.
     */
    public boolean hasExplodedThisCycle() {
        return hasExplodedThisCycle;
    }

    /**
     * Determines if the tank is marked for removal from the game.
     * 
     * @return {@code true} if the tank is marked for removal, {@code false} otherwise.
     */
    public boolean isMarkedForRemoval() {
        return markForRemoval;
    }

    /**
     * Resets the explosion flag for the tank at the end of a game cycle.
     */
    public void resetExplosionFlag() {
        hasExplodedThisCycle = false;
    }

    /**
     * Sets the y-coordinate of the tank.
     * 
     * @param y The new y-coordinate of the tank.
     */
     public void setY(float y) {
        float terrainHeight = terrain.getHeightAtTerrain(x);
        float adjustedY = PApplet.constrain(y, terrainHeight - SIZE / 2, terrainHeight); // Adjust tank Y position to ensure it rests on the terrain
        this.y = adjustedY;
    }

    /**
     * Returns the y-coordinate of the tank.
     * 
     * @return The y-coordinate of the tank.
     */

     public float getY() {
        return y;
    }

    /**
     * Returns the x-coordinate of the tank.
     * 
     * @return The x-coordinate of the tank.
     */
    
    public float getX() {
        return x;
    }

    /**
     * Returns the current angle of the turret in radians.
     * 
     * @return The current angle of the turret in radians.
     */
    
    public double getTurretAngle() {
        return turretAngle;
    }

    /**
     * Sets the angle of the turret.
     * 
     * @param turretAngle The new angle of the turret in radians.
     */

    public void setTurretAngle(float turretAngle) {
        this.turretAngle = turretAngle;
    }

    /**
     * Returns the remaining fuel of the tank.
     * 
     * @return The remaining fuel of the tank.
     */
    public float getFuel() {
        return fuel;
    }

     /**
     * Resets the fuel of the tank to its maximum value.
     */
    public void resetFuel() {
        fuel = 250;
    }


    /**
     * Marks the tank as needing an update. 
     */
    public void markForUpdate() {
        needsUpdate = true; 
    }

    /**
     * Checks if the tank needs an update. 
     * 
     * @return true if the tank needs an update, false otherwise.
     */
    public boolean needsUpdate() {
        return needsUpdate; 
    }
    
     /**
     * Returns the current health of the tank.
     * 
     * @return The current health of the tank.
     */
    public float getHealth() {
        return this.health;
    }

     /**
     * Sets the health of the tank to the specified value, clamped between 0 and 100.
     * 
     * @param health The health value to set.
     */
     public void setHealth(float health) {
        this.health = Math.max(0, Math.min(100, health)); 
    }

    /**
     * Checks if the tank is descending with a parachute.
     * 
     * @return True if the tank is descending with a parachute, false otherwise.
     */
     public boolean isDescendingWithParachute() {
        return descendingWithParachute;
    }

    /**
     * Sets whether the tank is descending with a parachute.
     * 
     * @param descending True if the tank is descending with a parachute, false otherwise.
     */
    public void setDescendingWithParachute(boolean descending) {
        descendingWithParachute = descending;
    }

    /**
     * Retrieves the current count of parachutes available for the tank.
     * 
     * @return The number of parachutes currently available.
     */
    public int getParachuteCount() {
        return parachuteCount;
    }

    /**
     * Sets the count of parachutes available to the tank.
     * 
     * @param count The new parachute count to be set.
     */
    public void setParachuteCount(int count) {
        this.parachuteCount = count;  // Set the new parachute count
    }

    /**
     * Checks if a larger projectile is ready to be fired by the tank.
     * 
     * @return true if a larger projectile is ready to be fired, false otherwise.
     */
    public boolean isLargerProjectileReady() {
        return largerProjectileReady;
    }
    
    /**
     * Sets the readiness state of a larger projectile.
     * 
     * @param ready The readiness state to set for firing a larger projectile.
     */
    public void setLargerProjectileReady(boolean ready) {
        this.largerProjectileReady = ready;  
    }

    /**
     * Returns the color components of the tank.
     * 
     * @return An array containing the RGB color components of the tank.
     */
     public int[] getColors() {
        return colors;
    }

    /**
     * Returns the current power level of the tank.
     * 
     * @return The current power level of the tank.
     */
     public float getPower() {
        return power;
    }

    /**
     * Returns the symbol representing the tank.
     * 
     * @return The symbol representing the tank.
     */
    public char getSymbol() {
        return symbol;
    }

    /**
     * Returns the list of all tanks currently managed by this instance. .
     * 
     * @return An ArrayList containing all tank instances.
     */
    public ArrayList<Tank> getTanks() {
        return tanks; // Return the list of tanks.
    }
}
