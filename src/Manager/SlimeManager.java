package Manager;

// Imports //

import Logic.*;
import javafx.animation.AnimationTimer;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import java.util.HashMap;
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
	HashMap<Mould, LinkedList<Node>> headsToAdd;
	/**
	 * Grid (2D Tile array) representing world.
	 */
	private Element[][] worldGrid;
	/**
	 * Pane of Tiles holding all tiles.
	 */
	private Pane worldPane;
	/**
	 * The currently found foods.
	 */
	private HashMap<Food, LinkedList<Node>> foodsFound;
	/**
	 * Mould heads.
	 */
	private HashMap<Mould, LinkedList<Node>> mouldHeads;
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
		foodsFound = new HashMap<>();
		mouldHeads = new HashMap<>();
		headsToAdd = new HashMap<>();
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
			getFood();
			searchForFood();
		}
	}

	private void getFood() {
		for (Food food : foodsFound.keySet()) {
			getSpecificFood(food);
		}
	}

	private void getSpecificFood(Food food) {
		LinkedList<Node> currAStarPath = foodsFound.get(food);
		if (currAStarPath == null || currAStarPath.size() == 0) {
			AStar astar = new AStar(worldGrid, nodePool, nodePool.getNode(Mould.getMouldHead()),
					nodePool.getNode(food), false);
			currAStarPath = astar.search();
			foodsFound.put(food, currAStarPath);
		}
		// Get next node from A*s path.
		Node currNode = currAStarPath.pop();
		Element currNeighbor = worldGrid[currNode.xPos][currNode.yPos];
		spreadTo(currNeighbor);
	}

	private void searchForFood() {
		for (Mould head : mouldHeads.keySet()) {
			specificExpand(head);
		}
		mouldHeads.putAll(headsToAdd);
		headsToAdd.clear();
	}

	private void specificExpand(Mould head) {
		LinkedList<Node> currAStarPath = mouldHeads.get(head);
		if (currAStarPath == null || currAStarPath.size() == 0) {
			Node toExpandTo = getExpansionNode(head);
			AStar astar = new AStar(worldGrid, nodePool, nodePool.getNode(head), toExpandTo, true);
			currAStarPath = astar.search();
			mouldHeads.put(head, currAStarPath);
		}
		// Get next node from A*s path.
		Node currNode = currAStarPath.pop();
		Element currNeighbor = worldGrid[currNode.xPos][currNode.yPos];
		spreadTo(currNeighbor);
	}

	private Node getExpansionNode(Mould head) {
		Random rand = new Random();
		int xMove, newX, xPos;
		int yMove, newY, yPos;
		Mould currMould = head;
		Element currNeighbor;
		do {
			// Generate the X and Y positions for the next move. Logic.Mould will try and move away from the head.
			boolean randPicker = rand.nextBoolean();
			xMove = currMould.generateXMove(randPicker) * rand.nextInt(8);
			yMove = currMould.generateYMove(randPicker) * rand.nextInt(8);

			// Get the actual neighbor from the grid of tiles.
			xPos = currMould.getXPos();
			yPos = currMould.getYPos();
//			if ((xMove * yMove != 0) || (Math.abs(xMove) + Math.abs(yMove) == 0)) {
//				log("No such thing!");
//			}
			// TODO: The code below can enter an endless loop when no available tile exists?
			newX = ((xPos + xMove >= 0) && (xPos + xMove <= X_TILES - 1)) ? xPos + xMove : -1;
			newY = ((yPos + yMove >= 0) && (yPos + yMove <= Y_TILES - 1)) ? yPos + yMove : -1;
			currNeighbor = ((newX < 0) || (newY < 0)) ? null : worldGrid[newX][newY];
			if (currNeighbor == null) {
				continue;
			} else if (currNeighbor.getType() != MOULD_TYPE) {
				return nodePool.getNode(currNeighbor);
			} else {
				currMould = (Mould) currNeighbor;
			}
			spreadTo(currNeighbor);
		} while (true);
	}

	private void spreadTo(Element toSpread) {
		// Choose how to act according to the chosen neighbor.
		switch (toSpread.getType()) {
			case EMPTY_TYPE:
				spreadToEmpty(toSpread);
				break;
			case FOOD_TYPE:
				eatFood((Food) toSpread);
				foodsFound.put((Food) toSpread, null);
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
		mould.timesPast++;
		if (Mould.maxTimesPast < mould.timesPast) {
			Mould.maxTimesPast = mould.timesPast;
			if (!mouldHeads.containsKey(mould)) {

				headsToAdd.put(mould, null);
			}
		}
		mould.saturate();
		double opacity = ((Color) mould.getElementRepr().getFill()).getOpacity();
	}

	/**
	 * Eats given food found.
	 */
	public void eatFood(Food currFood) {
		Mould.setFoundFood(true);
		spreadToEmpty(currFood);
		currFood.desaturate();
		if (((Color) currFood.getElementRepr().getFill()).getOpacity() < DISAPPEAR_THRESH) {
			Mould newMould = new Mould(currFood._xPos, currFood._yPos);
			headsToAdd.put(newMould, null);
			replace(newMould);
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
		Mould head = new Mould(randX, randY);
		mouldHeads.put(head, null);
		place(head);
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
