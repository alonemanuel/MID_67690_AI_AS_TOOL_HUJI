package Manager;

// Imports //

import Logic.*;
import javafx.animation.AnimationTimer;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import java.util.LinkedList;
import java.util.Random;

import static Logic.Element.*;

/**
 * A class representing a Slime Mould2 Manager.
 */
public class SlimeManager {

	// Constants //
	/**
	 * Size of tile (height == width == size)
	 */
	public static final int REPR_SIZE = 10;
	/**
	 * Rate of expansion - how many tiles get covered in a single frame.
	 */
	public static final int EXPANSION_RATE = 1;
	/**
	 * Width of screen.
	 */
	private static final int W = (800 / REPR_SIZE) * REPR_SIZE;
	/**
	 * Number of X aligned tiles.
	 */
	protected static final int X_TILES = W / REPR_SIZE;
	/**
	 * Height of screen.
	 */
	private static final int H = (600 / REPR_SIZE) * REPR_SIZE;
	/**
	 * Number of Y aligned tiles.
	 */
	protected static final int Y_TILES = H / REPR_SIZE;
	/**
	 * Number of food items.
	 */
	private static final int NUM_OF_FOODS = 8;
	/**
	 * Below this thresh, moulds disappear.
	 */
	private static final double DISAPPEAR_THRESH = 0.001;

	// Fields //
	/**
	 * Pool of nodes.
	 */
	public NodeMap nodePool;
	/**
	 * Grid (2D Tile array) representing world.
	 */
	private Element[][] worldGrid;
	/**
	 * Pane of Tiles holding all tiles.
	 */
	private Pane worldPane;
	/**
	 * The currently found food.
	 */
	private Node foodFound;
	/**
	 * Current path constructed by A*.
	 */
	private LinkedList<Node> currAStarPath;
	/**
	 * Animation timer of the program.
	 */
	AnimationTimer timer = new AnimationTimer() {
		@Override
		public void handle(long l) {
			moveSlime();
		}
	};

	// Methods //

	/**
	 * Default ctor.
	 */
	public SlimeManager(Pane pane) {
		worldGrid = new Element[X_TILES][Y_TILES];
		nodePool = new NodeMap(X_TILES, Y_TILES);
		worldPane = pane;
		log("Created manager");
	}

	/**
	 * Logs the message to the console.
	 *
	 * @param logMsg msg to be logged.
	 */
	public static void log(String logMsg) {
		System.out.println("Log: " + logMsg);
	}

	/**
	 * Updates the timer and thus the program.
	 */
	public void update() {
		log("Updated");
		timer.start();
	}

	/**
	 * Moves slime.
	 */
	private void moveSlime() {
		// Before each movement, reenergize moulds.
		reenergizeMoulds();
		// Move according to the expansion rate.
		for (int i = 0; i < EXPANSION_RATE; i++) {
			if (Mould.didFindFood()) {
				getFood();
			} else {
				searchForFood();
			}
		}
	}

	/**
	 *
	 */
	private void getFood() {
		Mould currMould = Mould.getMouldHead();
		/**
		 * If food was found, go and find some more
		 */
		if (currAStarPath == null || currAStarPath.size() == 0) {
			AStar astar = new AStar(nodePool, nodePool.getNode(currMould._xPos, currMould._yPos),
					foodFound);
			currAStarPath = astar.search();
		}
		// Get next node from A*s path.
		Node currNode = currAStarPath.pop();
		Element currNeighbor = worldGrid[currNode.xPos][currNode.yPos];
		spreadTo(currNeighbor);
	}

	private boolean canBeHead(Mould potential) {
		Color color = (Color) potential.getElementRepr().getFill();
		return color.getBrightness() > 0.9 && color.getOpacity() > 0.7;
	}

	private void searchForFood() {
		Random rand = new Random();
		boolean didSpread = false;
		int xMove, newX, xPos;
		int yMove, newY, yPos;
		Mould currMould = Mould.getMouldHead();
		Element currNeighbor;
		do {
			// Generate the X and Y positions for the next move. Logic.Mould will try and move away from the head.
			boolean randPicker = rand.nextBoolean();
			xMove = currMould.generateXMove(randPicker);
			yMove = currMould.generateYMove(randPicker);

			// Get the actual neighbor from the grid of tiles.
			xPos = currMould.getXPos();
			yPos = currMould.getYPos();
			if ((xMove * yMove != 0) || (Math.abs(xMove) + Math.abs(yMove) == 0)) {
				log("No such thing!");
			}
			// TODO: The code below can enter an endless loop when no available tile exists?
			newX = ((xPos + xMove >= 0) && (xPos + xMove <= X_TILES - 1)) ? xPos + xMove : -1;
			newY = ((yPos + yMove >= 0) && (yPos + yMove <= Y_TILES - 1)) ? yPos + yMove : -1;
			currNeighbor = ((newX < 0) || (newY < 0)) ? null : worldGrid[newX][newY];
			if (currNeighbor == null) {
				didSpread = true;
				continue;
			} else if (currNeighbor.getType() != MOULD_TYPE) {
				didSpread = true;
			} else {

				currMould = (Mould) currNeighbor;
			}
			spreadTo(currNeighbor);
		} while (!didSpread);
	}

	private void spreadTo(Element toSpread) {
		// Choose how to act according to the chosen neighbor.
		switch (toSpread.getType()) {
			case EMPTY_TYPE:
				spreadToEmpty(toSpread);
				break;
			case FOOD_TYPE:
				eatFood((Food) toSpread);
				foodFound = nodePool.getNode(toSpread._xPos, toSpread._yPos);
				break;
			case MOULD_TYPE:
				spreadToMould((Mould) toSpread);
				break;
		}
	}

	/**
	 * Expands
	 */
	private void spreadToMould(Mould mould) {
		mould.saturate();
		if (((Color) mould.getElementRepr().getFill()).getOpacity() > 0.8) {
			Mould.setMouldHead(mould);
		}
	}

	/**
	 * Eats given food found.
	 */
	public void eatFood(Food currFood) {
		// TODO: Manage energy consumption
		spreadToEmpty(currFood);
		currFood.desaturate();
		if (((Color) currFood.getElementRepr().getFill()).getOpacity() < DISAPPEAR_THRESH) {
			replace(new Mould(currFood._xPos, currFood._yPos));
			Mould.setFoundFood(false);
		}
	}

	public void spreadToEmpty(Element currNeighbor) {
		int xPos = currNeighbor.getXPos();
		int yPos = currNeighbor.getYPos();
		Mould toSpread = new Mould(xPos, yPos);
		place(toSpread);    // TODO: replace instead of place?
	}

	/**
	 * Populate world with tiles.
	 */
	public void populateElements() {
		for (int y = 0; y < Y_TILES; y++) {
			for (int x = 0; x < X_TILES; x++) {
				place(new Empty(x, y));
			}
		}
	}

	/**
	 * Populate world with food.
	 */
	public void populateFood() {
		Random rand = new Random();
		for (int i = 0; i < NUM_OF_FOODS; i++) {
			int randX = rand.nextInt(X_TILES);
			int randY = rand.nextInt(Y_TILES);
			place(new Food(randX, randY));

//            addFood(randX, randY);
		}
	}

	private void replace(Element toPlace) {
		Element toRemove = worldGrid[toPlace._xPos][toPlace._yPos];
		worldPane.getChildren().remove(toRemove);
		place(toPlace);
	}

	private void place(Element toPlace) {
		worldGrid[toPlace._xPos][toPlace._yPos] = toPlace;
		worldPane.getChildren().add(toPlace.getElementRepr());
	}

	/**
	 * Place mould in world.
	 */
	public void placeMould() {
		Element currElem;
		int randX, randY;
		Random rand = new Random();
		// Wait until en empty spot is found.
		do {
			randX = rand.nextInt(X_TILES);
			randY = rand.nextInt(Y_TILES);
			currElem = worldGrid[randX][randY];
		} while (currElem.getType() != EMPTY_TYPE);
		place(new Mould(randX, randY));
	}

	// Helpers //

	public void restart() {
		timer.stop();
	}

	/**
	 * Goes over all moulds and normalizes them (energy-wise).
	 */
	private void reenergizeMoulds() {
		for (int x = 0; x < X_TILES; x++) {
			for (int y = 0; y < Y_TILES; y++) {
				Element currElem = worldGrid[x][y];
				if (currElem.getType() == MOULD_TYPE) {
					currElem.desaturate();
					// If current elements reaches a minimal threshold opacity, it should disappear.
					if (((Color) currElem.getElementRepr().getFill()).getOpacity() < DISAPPEAR_THRESH) {
						replace(new Empty(x, y));
					}
				}
			}
		}
	}
}
