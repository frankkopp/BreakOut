/**
MIT License

Copyright (c) 2018 Frank Kopp

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package fko.breakout.model;

import java.util.Observable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import fko.breakout.events.GameEvent;
import fko.breakout.events.GameEvent.GameEventType;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.util.Duration;

/**
 * BreakOutModel
 * <p>
 * Handles the BreakOut game status and calculations.<br>
 * <p>
 * It has not yet its own Thread - could become necessary later if performance/rendering issue occur. 
 * <p>
 * 02.01.2018
 * @author Frank Kopp
 * TODO: add separate thread
 */
public class BreakOutGame extends Observable {

	private static final int 	START_LEVEL = 1;

	private static final int 	START_LIVES = 5;
	private static final long 	SLEEP_BETWEEN_LIVES = 1000; // in ms
	private static final long 	SLEEP_BETWEEN_LEVELS = 3000; // in ms

	private static final double 	PADDLE_INITIAL_FRAMERATE = 60.0; // Framerate for paddle movements
	private static final Double 	PADDLE_MOVE_STEPS = 5.0; // steps per animation cycle

	private static final double 	BALL_MAX_3ANGLE = 60;
	private static final int 	BALL_INITIAL_X = 390;
	private static final int 	BALL_INITIAL_Y = 450;
	private static final double 	BALL_INITIAL_FRAMERATE = 60.0;  // Framerate for ball movements
	// Absolute speed of ball 
	// when vertical equals px in y
	// when horizontal equals px in x
	private static final double 	BALL_INITIAL_SPEED = 5.0; 

	private static final double 	BRICK_GAP = 2;

	/* 
	 * These values determine the size and dimension of elements in Breakout.
	 * In normal MVC the View would use them to build the View elements. As we
	 * us JavaFX and FXML with Scene Builder these values are already set by the FXML.
	 * Therefore we duplicate them in den model and make sure they stay synchronized through 
	 * property bindings. 
	 */

	// Playfield dimensions
	private DoubleProperty playfieldWidth = new SimpleDoubleProperty(780); // see FXML 800 - 2 * 10 Walls
	private DoubleProperty playfieldHeight = new SimpleDoubleProperty(710); // see FXML 520 - 1 * 10 Wall
	public DoubleProperty playfieldWidthProperty() {	return playfieldWidth; }
	public DoubleProperty playfieldHeightProperty() { return playfieldHeight; }

	// Paddle dimensions and position
	private DoubleProperty paddleWidth = new SimpleDoubleProperty(150); // see FXML
	private DoubleProperty paddleHeight = new SimpleDoubleProperty(20); // see FXML
	private DoubleProperty paddleX = new SimpleDoubleProperty(315); // see FXML
	private DoubleProperty paddleY = new SimpleDoubleProperty(670); // see FXML
	public DoubleProperty paddleHeightProperty() { return paddleHeight; }
	public DoubleProperty paddleWidthProperty() { return paddleWidth; }
	public DoubleProperty paddleXProperty() { return paddleX; }
	public DoubleProperty paddleYProperty() { return paddleY; }

	// Ball dimensions and position
	private DoubleProperty ballRadius = new SimpleDoubleProperty(8); // see FXML
	private DoubleProperty ballCenterX = new SimpleDoubleProperty(BALL_INITIAL_X); // see FXML
	private DoubleProperty ballCenterY = new SimpleDoubleProperty(BALL_INITIAL_Y); // see FXML
	public DoubleProperty ballRadiusProperty() { return ballRadius; }
	public DoubleProperty ballCenterXProperty() { return ballCenterX; }
	public DoubleProperty ballCenterYProperty() { return ballCenterY; }

	// game status
	private ReadOnlyBooleanWrapper isPlaying = new ReadOnlyBooleanWrapper(false);
	public ReadOnlyBooleanProperty isPlayingProperty() { return isPlaying.getReadOnlyProperty(); }
	private ReadOnlyBooleanWrapper isPaused = new ReadOnlyBooleanWrapper(false);
	public ReadOnlyBooleanProperty isPausedProperty() { return isPaused.getReadOnlyProperty(); }
	private ReadOnlyBooleanWrapper gameOver = new ReadOnlyBooleanWrapper(false);
	public ReadOnlyBooleanProperty gameOverProperty() { return gameOver.getReadOnlyProperty(); }

	// game statistics
	private ReadOnlyIntegerWrapper currentLevel = new ReadOnlyIntegerWrapper(START_LEVEL);
	public ReadOnlyIntegerProperty currentLevelProperty() { return currentLevel.getReadOnlyProperty(); };
	private ReadOnlyIntegerWrapper currentRemainingLives = new ReadOnlyIntegerWrapper(START_LIVES);
	public ReadOnlyIntegerProperty currentRemainingLivesProperty() { return currentRemainingLives.getReadOnlyProperty(); };
	private ReadOnlyIntegerWrapper currentScore = new ReadOnlyIntegerWrapper(0);
	public ReadOnlyIntegerProperty currentScoreProperty() { return currentScore.getReadOnlyProperty(); };

	// ball and paddle movement Timelines
	private Timeline paddleMovementTimeline = new Timeline();
	private Timeline ballMovementTimeline = new Timeline();

	// called when key is pressed/released to indicate paddle movement to movement animation
	private boolean paddleLeft;
	private boolean paddleRight;
	public void setPaddleLeft(boolean b) { paddleLeft = b; }
	public void setPaddleRight(boolean b) { paddleRight = b; }

	// ball speeds in each direction
	private double ball_vX = 1;
	private double ball_vY = BALL_INITIAL_SPEED;
	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

	// the brick layout holds all bricks and its positions of the games
	private final BrickLayout brickLayout;

	/**
	 * Constructor - prepares the brick layout and the movement Timelines.
	 */
	public BreakOutGame() {

		// setup BrickLayout
		brickLayout = new BrickLayout(BRICK_GAP, playfieldWidth, playfieldHeight);

		// start the paddle movements
		paddleMovementTimeline.setCycleCount(Timeline.INDEFINITE);
		KeyFrame movePaddle = 
				new KeyFrame(Duration.seconds(1/PADDLE_INITIAL_FRAMERATE), e -> { movePaddles();	});
		paddleMovementTimeline.getKeyFrames().add(movePaddle);
		paddleMovementTimeline.play();

		// prepare ball movements (will be start in startGame())
		ballMovementTimeline.setCycleCount(Timeline.INDEFINITE);
		KeyFrame moveBall = 
				new KeyFrame(Duration.seconds(1/BALL_INITIAL_FRAMERATE), e -> {	moveBall();	});
		ballMovementTimeline.getKeyFrames().add(moveBall);
	}

	/**
	 * Called by the <code>paddleMovementTimeline<code> animation event to move the paddles.
	 */
	private void movePaddles() {
		if (paddleLeft 
				&& paddleX.get() > 0.0) {
			paddleX.setValue(paddleX.getValue() - PADDLE_MOVE_STEPS);
		}
		if (paddleRight 
				&& paddleX.get() + paddleWidth.get() < playfieldWidth.get()) {
			paddleX.setValue(paddleX.getValue() + PADDLE_MOVE_STEPS);
		}
	}

	/**
	 * Called by the <code>ballMovementTimeline</code> animation event to move the ball.<br>
	 * Calls <code>checkCollision()</code>
	 */
	private void moveBall() {
		ballCenterX.set(ballCenterX.get() + ball_vX);
		ballCenterY.set(ballCenterY.get() + ball_vY);
		checkCollision();
	}

	/**
	 * Checks if the ball has hit a wall, the paddle, a block or has left 
	 * through the bottom. Calculates new speeds for each direction, tells brickLayout 
	 * if the ball hits a brick and calls <code>ballLost()</code> when ball has left 
	 * through bottom.
	 */
	private void checkCollision() {

		// convenience variables 
		final double ballUpperBound = ballCenterY.get() - ballRadius.get();
		final double ballLowerBound = ballCenterY.get() + ballRadius.get();
		final double ballLeftBound = ballCenterX.get() - ballRadius.get();
		final double ballRightBound = ballCenterX.get() + ballRadius.get();

		final double paddleUpperBound = paddleY.get();
		final double paddleLowerBound = paddleY.get() + paddleHeight.get();
		final double paddleLeftBound = paddleX.get();
		final double paddleRightBound = paddleX.get() + paddleWidth.get();

		// hit wall left or right
		checkSideWallCollision(ballLeftBound, ballRightBound);

		// hit wall top
		checkTopWallCollision(ballUpperBound);

		// hit brick
		checkBrickCollision(ballUpperBound, ballLowerBound, ballLeftBound, ballRightBound);

		// hit paddle
		checkPaddleCollision(ballLowerBound, ballLeftBound, ballRightBound, paddleUpperBound, paddleLowerBound,
				paddleLeftBound, paddleRightBound);

		// lost through bottom
		checkBallLostThroughBottom(ballUpperBound);

	}
	/**
	 * @param ballUpperBound
	 */
	private void checkBallLostThroughBottom(final double ballUpperBound) {
		if (ballUpperBound >= playfieldHeight.get()) {

			if (decreaseRemainingLives() < 0) {
				currentRemainingLives.set(0);
				gameOver();
				setChanged();
				notifyObservers(new GameEvent(GameEventType.GAME_OVER));
				return;
			};

			setChanged();
			notifyObservers(new GameEvent(GameEventType.BALL_LOST));

			// pause animation
			ballMovementTimeline.pause();

			// start new round
			startRound(SLEEP_BETWEEN_LIVES);
		}
	}

	/**
	 * @param ballUpperBound
	 * @param ballLowerBound
	 * @param ballLeftBound
	 * @param ballRightBound
	 */
	private void checkBrickCollision(final double ballUpperBound, final double ballLowerBound,
			final double ballLeftBound, final double ballRightBound) {
		// calculate ball edge's brick cell
		int ballCenterRow = (int) (ballCenterYProperty().get() 
				/ (brickLayout.getBrickHeight()+brickLayout.getBrickGap()));
		int ballCenterCol = (int) (ballCenterXProperty().get() 
				/ (brickLayout.getBrickWidth()+brickLayout.getBrickGap()));

		int ballUpperRow = (int) (ballUpperBound 
				/ (brickLayout.getBrickHeight()+brickLayout.getBrickGap()));
		int ballLowerRow = (int) ((ballLowerBound - brickLayout.getBrickGap())
				/ (brickLayout.getBrickHeight()+brickLayout.getBrickGap()));
		int ballLeftCol = (int) (ballLeftBound 
				/ (brickLayout.getBrickWidth() + brickLayout.getBrickGap()));	
		int ballRightCol = (int) ((ballRightBound - brickLayout.getBrickGap())
				/ (brickLayout.getBrickWidth() + brickLayout.getBrickGap()));	

		// hit above
		if (brickLayout.getBrick(ballUpperRow, ballCenterCol) != null) {
			currentScore.set(currentScore.get() + brickLayout.hitBrick(ballUpperRow, ballCenterCol));
			ball_vY *= -1;
			setChanged();
			notifyObservers(new GameEvent(GameEventType.HIT_BRICK, ballUpperRow, ballCenterCol));
		}
		// hit below
		if (brickLayout.getBrick(ballLowerRow, ballCenterCol) != null) {
			currentScore.set(currentScore.get() + brickLayout.hitBrick(ballLowerRow, ballCenterCol));
			ball_vY *= -1;
			setChanged();
			notifyObservers(new GameEvent(GameEventType.HIT_BRICK, ballLowerRow, ballCenterCol));
		}
		// hit left
		if (brickLayout.getBrick(ballCenterRow, ballLeftCol) != null) {
			currentScore.set(currentScore.get() + brickLayout.hitBrick(ballCenterRow, ballLeftCol));
			ball_vX *= -1;
			setChanged();
			notifyObservers(new GameEvent(GameEventType.HIT_BRICK, ballCenterRow, ballLeftCol));
		}
		// hit right
		if (brickLayout.getBrick(ballCenterRow, ballRightCol) != null) {
			increaseScore(brickLayout.hitBrick(ballCenterRow, ballRightCol));
			ball_vX *= -1;
			setChanged();
			notifyObservers(new GameEvent(GameEventType.HIT_BRICK, ballCenterRow, ballRightCol));
		}

		if (brickLayout.getNumberOfBricks() == 0) {
			// Level done
			setChanged();
			notifyObservers(new GameEvent(GameEventType.LEVEL_COMPLETE));

			// load new level or game over WON
			loadNextLevel();

			// Level done
			setChanged();
			notifyObservers(new GameEvent(GameEventType.LEVEL_START));
		}
	}

	/**
	 * @param ballLowerBound
	 * @param ballLeftBound
	 * @param ballRightBound
	 * @param paddleUpperBound
	 * @param paddleLowerBound
	 * @param paddleLeftBound
	 * @param paddleRightBound
	 * @return true if ball touches paddle
	 */
	private void checkPaddleCollision(final double ballLowerBound, final double ballLeftBound,
			final double ballRightBound, final double paddleUpperBound, final double paddleLowerBound,
			final double paddleLeftBound, final double paddleRightBound) {

		if (ballLowerBound >= paddleUpperBound && ballLowerBound <= paddleLowerBound) { // ball on correct height
			if (ballRightBound > paddleLeftBound && ballLeftBound < paddleRightBound) { // ball touches the paddle

				// determine where the ball hit the paddle
				double hitPointAbsolute = ballCenterX.get() - paddleLeftBound;
				// normalize value to -1 (left), 0 (center), +1 (right)
				double hitPointRelative = 2 * ((hitPointAbsolute / paddleWidth.get()) - 0.5);
				// determine new angle
				double newAngle = hitPointRelative * BALL_MAX_3ANGLE;

				ball_vX = Math.sin(Math.toRadians(newAngle)) * BALL_INITIAL_SPEED;
				ball_vY = -Math.cos(Math.toRadians(newAngle)) * BALL_INITIAL_SPEED;

				setChanged();
				notifyObservers(new GameEvent(GameEventType.HIT_PADDLE));
			}
		}
	}

	/**
	 * @param ballUpperBound
	 * @return true if ball touches top wall
	 */
	private void checkTopWallCollision(final double ballUpperBound) {
		if (ballUpperBound <= 0) {
			ballCenterY.set(ballRadius.get()); // in case it was <0
			ball_vY *= -1;
			setChanged();
			notifyObservers(new GameEvent(GameEventType.HIT_WALL));
		}
	}

	/**
	 * @param ballLeftBound
	 * @param ballRightBound
	 * @return true if ball touches one of the side wall
	 */
	private void checkSideWallCollision(final double ballLeftBound, final double ballRightBound) {
		if (ballLeftBound <= 0) { // left
			ballCenterX.set(0+ballRadius.get()); // in case it was <0
			ball_vX *= -1;
			setChanged();
			notifyObservers(new GameEvent(GameEventType.HIT_WALL));
		} else if (ballRightBound >= playfieldWidth.get()) { // right
			ballCenterX.set(playfieldWidth.get()-ballRadius.get()); // in case it was >playFieldWidth
			ball_vX *= -1;
			setChanged();
			notifyObservers(new GameEvent(GameEventType.HIT_WALL));
		}
	}

	/**
	 * @param i
	 */
	private void loadNextLevel() {
		// pause animation
		ballMovementTimeline.pause();

		// load next level or game is won if non available
		currentLevel.set(currentLevel.get() + 1);
		final Brick[][] newLevel = LevelLoader.getInstance().getLevel(currentLevel.get());
		if (newLevel == null) { 
			gameOver();
			setChanged();
			notifyObservers(new GameEvent(GameEventType.GAME_WON));
			return;
		};

		brickLayout.setMatrix(newLevel);
		startRound(SLEEP_BETWEEN_LEVELS);
	}

	/**
	 * called when out of balls or after last level
	 */
	private void gameOver() {
		stopPlaying();
		gameOver.set(true);
	}

	/**
	 * Starts a new round after loosing a ball or completing a level
	 * @param pause
	 */
	private void startRound(long pause) {
		// reset the ball position and speed
		resetBall();
		// show the ball for a short time then start the animation
		executor.schedule(() -> {
			if (!isPlaying()) return; // maybe the game has already been stopped
			ballMovementTimeline.play(); 
		}, pause, TimeUnit.MILLISECONDS);
	}

	/**
	 * resets the ball's location and speed
	 */
	private void resetBall() {
		// reset ball speed and direction (straight down)
		ball_vX = 0;
		ball_vY = BALL_INITIAL_SPEED;

		// reset ball position
		ballCenterX.set(BALL_INITIAL_X);
		ballCenterY.set(BALL_INITIAL_Y);
	}

	/**
	 * increasing the score after hitting a brick
	 * @param i
	 * @return
	 */
	private int increaseScore(int i) { 
		currentScore.set(currentScore.get()+i); 
		return currentScore.get();
	}
	
	/**
	 * decreases the remaining lives after loosing a ball
	 * @return remaining lives
	 */
	private int decreaseRemainingLives() {
		currentRemainingLives.set(currentRemainingLives.get() - 1);
		return currentRemainingLives.get();
	}
	
	/**
	 * Called from controller by mouse move events. Moves the paddle according to the mouse's x position
	 * when mouse is in window. The paddle's center will be set to the current mouse position. 
	 * @param x
	 */
	public void setMouseXPosition(double x) {
		double halfPaddleWidth = paddleWidthProperty().get()/2;
		if (x - halfPaddleWidth < 0.0) {
			x = halfPaddleWidth;
		} else if (x + halfPaddleWidth > playfieldWidth.get()) {
			x = playfieldWidth.get() - halfPaddleWidth;
		}
		if (paddleX.get() >= 0.0 && paddleX.get() + paddleWidth.get() <= playfieldWidth.get()){
			paddleXProperty().set(x-halfPaddleWidth);
		}
	}

	/**
	 * Starts a new game.
	 */
	public void startPlaying() {
		isPlaying.set(true);
		isPaused.set(false);
		gameOver.set(false);

		// load first level
		brickLayout.setMatrix(LevelLoader.getInstance().getLevel(currentLevel.get()));

		// initialize new game
		currentLevel.set(START_LEVEL);
		currentRemainingLives.set(START_LIVES);
		currentScore.set(0);

		setChanged();
		notifyObservers(new GameEvent(GameEventType.GAME_START));

		// start the ball movement
		startRound(SLEEP_BETWEEN_LIVES);
	}

	/**
	 * stops the current game
	 */
	public void stopPlaying() {
		isPlaying.set(false);
		isPaused.set(false);
		gameOver.set(false);
		// stop ball movement
		ballMovementTimeline.stop();
	}

	/**
	 * @return true of game is running
	 */
	public boolean isPlaying() {
		return isPlaying.get();
	}

	/**
	 * pauses a running game 
	 */
	public void pausePlaying() {
		if (!isPlaying()) return; // ignore if not playing
		isPaused.set(true);
		ballMovementTimeline.pause();
	}

	/**
	 * resumes a paused running game
	 */
	public void resumePlaying() {
		if (!isPlaying() && !isPaused()) return; // ignore if not playing
		isPaused.set(false);
		ballMovementTimeline.play();

	}

	/**
	 * @return true if game is paused
	 */
	public boolean isPaused() {
		return isPaused.get();
	}

	/**
	 * @return the current brick layout
	 */
	public BrickLayout getBrickLayout() {
		return brickLayout;
	}

}