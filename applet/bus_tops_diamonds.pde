import toxi.processing.*;


private static final int WORLD_WIDTH = 512;
private static final int WORLD_HEIGHT = 160;

private static final color BACKGROUND_COLOR = #000000;
private static final color DIAMOND_COLOR = #FF0000;

private static final int NUM_FLOCKS = 3;
private static final int FLOCK_SIZE = 40;


ToxiclibsSupport gfx;

// A collection of flocks of Diamonds.
ArrayList<Flock3D> flocks;


void setup() {
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

void draw() {
  background(BACKGROUND_COLOR);
  
  for (Flock3D flock : flocks) {
    flock.run();
    flock.draw(gfx);
  }
}

