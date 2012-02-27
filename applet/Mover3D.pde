import toxi.geom.*;

/**
 * Representation of an entity that lives in this world and moves.
 */
abstract class Mover3D {
  
  protected Vec3D position;   // Our current position.
  protected Vec3D velocity;   // Our current vector velocity.
  protected Vec3D acceleration;   // Our current vector of acceleration. 
  
  protected float maxSpeed;
  protected float maxForce;
  
  
  /**
   * Update our position based on the current velocity,
   * and our velocity based on the current acceleration.
   * Reset the acceleration to 0 on every cycle.
   */
  public void update() {
    // Update our velocity based on our current acceleration.
    velocity.addSelf(acceleration);
    // Limit our speed.
    velocity.limit(maxSpeed);
    // Update our position based on our current velocity.
    position.addSelf(velocity);
    // Reset acceleration to 0 each cycle.
    acceleration.scaleSelf(0);
  }
  
  
  /**
   * Translate force on this Person into acceleration.
   */
  protected void applyForce(Vec3D force) {
    acceleration.addSelf(force.limit(maxForce));
  }
}
