import processing.core.*; 
import processing.xml.*; 

import toxi.processing.*; 
import toxi.geom.*; 
import toxi.processing.*; 
import toxi.processing.*; 
import toxi.geom.*; 

import java.applet.*; 
import java.awt.Dimension; 
import java.awt.Frame; 
import java.awt.event.MouseEvent; 
import java.awt.event.KeyEvent; 
import java.awt.event.FocusEvent; 
import java.awt.Image; 
import java.io.*; 
import java.net.*; 
import java.text.*; 
import java.util.*; 
import java.util.zip.*; 
import java.util.regex.*; 

public class bus_tops_diamonds extends PApplet {




private static final int WORLD_WIDTH = 512;
private static final int WORLD_HEIGHT = 160;

private static final int BACKGROUND_COLOR = 0xff000000;
private static final int DIAMOND_COLOR = 0xffFF0000;

private static final int NUM_FLOCKS = 3;
private static final int FLOCK_SIZE = 40;


ToxiclibsSupport gfx;

// A collection of flocks of Diamonds.
ArrayList<Flock3D> flocks;


public void setup() {
  size(WORLD_WIDTH, WORLD_HEIGHT);
  frameRate(8);
  smooth();
  noCursor();
  
  gfx = new ToxiclibsSupport(this);
  
	flocks = new ArrayList<Flock3D>();
	for (int i=0; i < NUM_FLOCKS; i++) {
		Flock3D f = new Flock3D();
		// Add an initial set of diamonds into the flock.
		for (int j=0; j < FLOCK_SIZE; j++) {
	    f.addDiamond( new Diamond3D(new Vec3D(random(0, WORLD_WIDTH), random(0, WORLD_HEIGHT), random(-1, 1)), WORLD_WIDTH, WORLD_HEIGHT, DIAMOND_COLOR) );
	  }
		flocks.add(f);
	}
}

public void draw() {
  background(BACKGROUND_COLOR);
  
  for (Flock3D flock : flocks) {
    flock.run();
    flock.draw(gfx);
  }
}




class Diamond3D extends Mover3D {
	
	private static final int LENGTH = 10;
	private static final int WIDTH = 7;
	
	private static final float SEPARATION_FORCE_WEIGHT = 1.5f;
	private static final float ALIGNING_FORCE_WEIGHT = 1.0f;
	private static final float COHESION_FORCE_WEIGHT = 1.0f;
	private static final float Z_BOUNDARY_REPULSION_WEIGHT = 0.8f;
	
	private static final float DESIRED_SEPARATION = 3.0f;
	private static final float NEIGHBOR_DISTANCE = 5.0f;
	
	private static final float Z_SCALE_WEIGHT = 0.3f;
	private static final float Z_SCALE_OFFSET = 4.0f;
	private static final float MAX_Z = 1;
	
	private Polygon2D shape;
	private int fillColor;
	
	// Size of the world.
	private int worldWidth;
	private int worldHeight;
	
	// Properties shown while debugging
	private Vec3D separationForce;   // Force wanting to separate from all other diamonds
	private Vec3D aligningForce;   // Force wanting to align with the same direction of all nearby diamonds
	private Vec3D cohesionForce;   // Force wanting to stay between all nearby diamonds
	private Vec3D zRepulsionForce;   // Force pushing away from Z boundaries
	
	
	Diamond3D(Vec3D pos, int ww, int wh, int c) {
		position = pos;
		worldWidth = ww;
		worldHeight = wh;
		fillColor = c;
		maxSpeed = 10;
		velocity = new Vec3D(random(-maxSpeed, maxSpeed), random(-maxSpeed, maxSpeed), random(-0.2f*maxSpeed, 0.2f*maxSpeed));
		acceleration = new Vec3D();
		maxForce = 0.2f;
	}
	
	
	public void run(ArrayList<Diamond3D> diamonds) {
		flock(diamonds);
		update();
		wrapAroundBorders();
	}
	
	/**
	 * Draw our diamond at its current position.
	 *
	 * @param gfx  A ToxiclibsSupport object to use for drawing.
	 */
	public void draw(ToxiclibsSupport gfx) {
		// Draw a diamond rotated in the direction of velocity.
		float theta = velocity.headingXY() + PI*0.5f;
		float zScale;
		
		// Determine the amount to scale the drawing based on the 
		// z dimension of the position.
		zScale = (position.z()+Z_SCALE_OFFSET) * Z_SCALE_WEIGHT;
		
		noStroke();
		fill(fillColor);
		
		pushMatrix();
		translate(position.x, position.y);
		rotate(theta);
		
		// Define the shape.
		shape = new Polygon2D();
		shape.add(new Vec2D(0, +0.5f*LENGTH*zScale));  // Top
		shape.add(new Vec2D(+0.5f*WIDTH*zScale, 0));  // Right
		shape.add(new Vec2D(0, -0.5f*LENGTH*zScale));  // Bottom
		shape.add(new Vec2D(-0.5f*WIDTH*zScale, 0));  // Left
		
		gfx.polygon2D(shape);
		popMatrix();
	}
	
	/**
	 * Get the diamond's position.
	 */
	public Vec3D getPosition() {
		return position;
	}
	
	/**
	 * Get the diamond's velocity.
	 */
	public Vec3D getVelocity() {
		return velocity;
	}
	
	
	/**
	 * Figure out a new acceleration based on three rules.
	 */
	private void flock(ArrayList<Diamond3D> diamonds) {
		separationForce = determineSeparationForce(diamonds);
		aligningForce = determineAligningForce(diamonds);
		cohesionForce = determineCohesionForce(diamonds);
		zRepulsionForce = determineZRepulsionForce();
		
		// Weight these forces.
		separationForce.scaleSelf(SEPARATION_FORCE_WEIGHT);
		aligningForce.scaleSelf(ALIGNING_FORCE_WEIGHT);
		cohesionForce.scaleSelf(COHESION_FORCE_WEIGHT);
		zRepulsionForce.scaleSelf(Z_BOUNDARY_REPULSION_WEIGHT);
		
		// Add the force vectors to our acceleration
		applyForce(separationForce);
		applyForce(aligningForce);
		applyForce(cohesionForce);
		applyForce(zRepulsionForce);
	}
	
	/**
	 * Check for nearby diamonds and separate from them.
	 */
	private Vec3D determineSeparationForce(ArrayList<Diamond3D> diamonds) {
		Vec3D sepForce = new Vec3D();
		
		// For every diamond in the flock, check if it's too close.
		for (Diamond3D other : diamonds) {
			Vec3D otherPosition = closestWrappedOtherPosition(other);
			float distance = position.distanceTo(otherPosition);
			if (distance > 0 && distance < DESIRED_SEPARATION) {
				// Calculate vector pointing away from the other.
				Vec3D diff = position.sub(otherPosition);
				diff.normalize();
				diff.scaleSelf(1/distance);   // Weight by distance.
				sepForce.addSelf(diff);
			}
		}
		
		if (sepForce.magnitude() > 0) {
			sepForce.normalize();
			sepForce.scaleSelf(maxSpeed);
			sepForce.subSelf(velocity);
			sepForce.limit(maxForce);
		}
		
		return sepForce;
	}
	
	/**
	 * Align velocity with the average of the nearby diamonds.
	 */
	private Vec3D determineAligningForce(ArrayList<Diamond3D> diamonds) {
		Vec3D algnForce = new Vec3D();
		
		for (Diamond3D other : diamonds) {
			if (isCloseTo(other)) {
				algnForce.addSelf(other.getVelocity());
			}
		}
		
		if (algnForce.magnitude() > 0) {
			algnForce.normalize();
			algnForce.scaleSelf(maxSpeed);
			algnForce.subSelf(velocity);
			algnForce.limit(maxForce);
		}
		
		return algnForce;
	}
	
  /**
   * Steer towards the average position of all nearby diamonds.
   */
  private Vec3D determineCohesionForce(ArrayList<Diamond3D> diamonds) {
    Vec3D cohForce = new Vec3D();
    
    for (Diamond3D other : diamonds) {
      if (isCloseTo(other)) {
        cohForce.addSelf(closestWrappedOtherPosition(other));
      }
    }
    
    if (cohForce.magnitude() > 0) {
      cohForce.normalize();
      cohForce.scaleSelf(maxSpeed);
      cohForce.subSelf(velocity);
      cohForce.limit(maxForce);
    }
    
    return cohForce;
  }
	
	/**
	 * Be repulsed from the Z boundaries
	 */
  private Vec3D determineZRepulsionForce() {
		return new Vec3D(0, 0, -(position.z()/MAX_Z));
	}
	
  /**
   * Check whether the distance between us and another Diamond,
   * including wrap-around-borders, is closer than NEIGHBOR_DISTANCE.
   * 
   * @param other  The other Diamond to compare ourselves with.
   */
  private boolean isCloseTo(Diamond3D other) {
    Vec3D otherPosition = other.getPosition().copy();
    float distance = position.distanceTo(otherPosition);
    
    // If the distance is 0, it's most likely us and we should bail.
    if (distance == 0) {
      return false;
    }
    
    // Nearby without wrapping borders is the easy case.
    if (distance < NEIGHBOR_DISTANCE) {
      return true;
    }
    
    // Compare the distance again.
    distance = position.distanceTo(closestWrappedOtherPosition(other));
    
    if (distance < NEIGHBOR_DISTANCE) {
      return true;
    } else {
      return false;
    }
  }
	
	private Vec3D closestWrappedOtherPosition(Diamond3D other) {
		Vec3D otherPosition = other.getPosition().copy();
		float distance = position.distanceTo(otherPosition);
		
		if (position.distanceTo(otherPosition.add(new Vec3D(worldWidth, 0, 0))) < distance) {
			// Move the other over to the right
			otherPosition.addSelf(new Vec3D(worldWidth, 0, 0));
			distance = position.distanceTo(otherPosition);
		} else if (position.distanceTo(otherPosition.sub(new Vec3D(worldWidth, 0, 0))) < distance) {
			// Move the other over to the left
			otherPosition.subSelf(new Vec3D(worldWidth, 0, 0));
			distance = position.distanceTo(otherPosition);
		}
		if (position.distanceTo(otherPosition.add(new Vec3D(0, worldHeight, 0))) < distance) {
			// Move the other down
			otherPosition.addSelf(new Vec3D(0, worldHeight, 0));
			distance = position.distanceTo(otherPosition);
		} else if (position.distanceTo(otherPosition.sub(new Vec3D(0, worldHeight, 0))) < distance) {
			// Move the other up
			otherPosition.subSelf(new Vec3D(0, worldHeight, 0));
			distance = position.distanceTo(otherPosition);
		}
				
		return otherPosition;
	}
	
  /**
   * Make all borders wrap-around so we return to the other side of the canvas.
   */
  private void wrapAroundBorders() {
    if (position.x < -LENGTH) position.x = worldWidth + LENGTH;
    if (position.y < -LENGTH) position.y = worldHeight + LENGTH;
    if (position.x > (worldWidth+LENGTH)) position.x = -LENGTH;
    if (position.y > (worldHeight+LENGTH)) position.y = -LENGTH;
  }
  
}


/**
 * Manages a collection of flocking Diamonds.
 */
class Flock3D {
  
  ArrayList<Diamond3D> diamonds;
  
  Flock3D() {
    diamonds = new ArrayList<Diamond3D>();
  }
  
  public void run() {
    for (Diamond3D d : diamonds) {
      d.run(diamonds);
    }
  }
  
  public void draw(ToxiclibsSupport gfx) {
    for (Diamond3D d : diamonds) {
      d.draw(gfx);
    }
  }
  
  /**
   * Add a new Diamond to the flock.
   *
   * @param d  The Diamond to be added.
   */
  public void addDiamond(Diamond3D d) {
    diamonds.add(d);
  }
}


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
  static public void main(String args[]) {
    PApplet.main(new String[] { "--present", "--bgcolor=#000000", "--hide-stop", "bus_tops_diamonds" });
  }
}
