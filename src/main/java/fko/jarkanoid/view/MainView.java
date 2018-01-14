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
package fko.jarkanoid.view;

import java.io.IOException;
import java.util.HashMap;

import fko.jarkanoid.controller.MainController;
import fko.jarkanoid.model.Ball;
import fko.jarkanoid.model.GameModel;
import fko.jarkanoid.model.LaserShot;
import fko.jarkanoid.model.PowerPill;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.StrokeTransition;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;

/**
 * MainView
 * <p>
 * Loads the main view from the MainView.main.resources.fxml resource file.<br>
 * Also add additional view elements e.g. BrickLayoutView.<br> 
 * <p>
 * 02.01.2018
 * @author Frank Kopp
 */
public class MainView {

  @SuppressWarnings("unused")
  private GameModel model;

  @SuppressWarnings("unused")
  private MainController controller;

  private final AnchorPane root;
  private final BrickLayoutView brickLayoutView = new BrickLayoutView();

  // the playfield
  private Pane playFieldPane;

  // Balls
  private final HashMap<Ball, BallView> ballViewMap = new HashMap<>();

  // LaserShots
  private final HashMap<LaserShot, LaserShotView> laserShotViewMap = new HashMap<>();

  // falling PowerPills
  private final HashMap<PowerPill, PowerPillView> powerPillViewMap = new HashMap<>();

  // animations
  private ScaleTransition    hitPaddleScaleTransition;
  private StrokeTransition   hitPaddleStrokeTransition;
  private ParallelTransition paddleHitAnimation;

  /**
   * @param model
   * @param controller
   * @throws IOException 
   */
  public MainView(GameModel model, MainController controller) throws IOException {
    this.model = model;
    this.controller = controller;

    // read FXML file and setup UI
    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/BreakOutMainView.fxml"));
    fxmlLoader.setController(controller);
    root = fxmlLoader.load();

    // add the play field to the root pane
    playFieldPane = (Pane) fxmlLoader.getNamespace().get("playfieldPane");
    playFieldPane.getChildren().add(brickLayoutView);

    // Game Over splash top front
    final Text gameoverText = (Text) fxmlLoader.getNamespace().get("gameOverSplash");
    gameoverText.toFront();

    // Paddle
    final Rectangle paddle = (Rectangle) fxmlLoader.getNamespace().get("paddle");

    prepareAnimations(paddle);
  }

  /**
   * create the animations for later playing
   * @param paddle
   */
  private void prepareAnimations(Rectangle paddle) {
    hitPaddleScaleTransition = new ScaleTransition(Duration.millis(50), paddle);
    hitPaddleScaleTransition.setFromX(1.0);
    hitPaddleScaleTransition.setFromY(1.0);
    hitPaddleScaleTransition.setByX(0.1);
    hitPaddleScaleTransition.setByY(0.1);
    hitPaddleScaleTransition.setCycleCount(2);
    hitPaddleScaleTransition.setAutoReverse(true);

    hitPaddleStrokeTransition = new StrokeTransition(Duration.millis(50), paddle);
    hitPaddleStrokeTransition.setFromValue((Color) paddle.getStroke());
    hitPaddleStrokeTransition.setToValue(Color.BLACK);
    hitPaddleStrokeTransition.setCycleCount(2);
    hitPaddleStrokeTransition.setAutoReverse(true);

    // combined animations
    paddleHitAnimation = new ParallelTransition(hitPaddleScaleTransition, hitPaddleStrokeTransition);

    //      EXAMPLE for animations over arbitrary properties:
    //      final Timeline timeline = new Timeline();
    //      timeline.setCycleCount(Timeline.INDEFINITE);
    //      timeline.setAutoReverse(true);
    //      final KeyValue kv = new KeyValue(rectBasicTimeline.xProperty(), 300);
    //      final KeyFrame kf = new KeyFrame(Duration.millis(500), kv);
    //      timeline.getKeyFrames().add(kf);
    //      timeline.play();
  }

  /**
   * Called when model updates the List of balls
   * @param change
   */
  public void updateBallList(ListChangeListener.Change<Ball> change) {
    while (change.next()) {
      if (change.wasAdded()) {
        for (Ball addedBall : change.getAddedSubList()) {        
          final BallView bv = new BallView(model, addedBall);
          bv.visibleProperty().bind(model.isPlayingProperty());
          ballViewMap.put(addedBall, bv);
          playFieldPane.getChildren().add(bv);
        }

      } else if (change.wasRemoved()) {
        for (Ball removedBall : change.getRemoved()) {    
          final BallView bv = ballViewMap.get(removedBall);
          bv.visibleProperty().unbind();
          playFieldPane.getChildren().remove(bv);
          ballViewMap.remove(removedBall);
          bv.removed();
        }
      }
    }
  }

  /**
   * Called when model updates the Set of balls
   * @param change
   */
  public void updateLaserShotList(ListChangeListener.Change<LaserShot> change) {
    while (change.next()) {
      if (change.wasAdded()) {
        for (LaserShot added : change.getAddedSubList()) {
          final LaserShotView laserShotView = new LaserShotView(model, added);
          laserShotView.visibleProperty().bind(model.isPlayingProperty());
          laserShotView.xProperty().bind(added.xProperty());
          laserShotView.yProperty().bind(added.yProperty());
          laserShotView.widthProperty().bind(added.widthProperty());
          laserShotView.heightProperty().bind(added.heightProperty());
          laserShotViewMap.put(added, laserShotView);
          playFieldPane.getChildren().add(laserShotView);
        }

      } else if (change.wasRemoved()) {
        for (LaserShot removed : change.getRemoved()) {
          final LaserShotView laserShotView = laserShotViewMap.get(removed);
          laserShotView.visibleProperty().unbind();
          laserShotView.xProperty().unbind();
          laserShotView.yProperty().unbind();
          laserShotView.widthProperty().unbind();
          laserShotView.heightProperty().unbind();
          playFieldPane.getChildren().remove(laserShotView);
          laserShotViewMap.remove(removed);
          laserShotView.removed();
        }
      }
    }
  }

  /**
   * Called when fallingBall list in model is changed
   * @param change
   */
  public void updateFallingPillList(ListChangeListener.Change<PowerPill> change) {
    while (change.next()) {
      if (change.wasAdded()) {
        for (PowerPill added : change.getAddedSubList()) {
          PowerPillView ppv = new PowerPillView(added);
          powerPillViewMap.put(added, ppv);
          playFieldPane.getChildren().add(ppv);
        }
      } else if (change.wasRemoved()) {
        for (PowerPill removed : change.getRemoved()) {
          PowerPillView ppv = powerPillViewMap.get(removed);
          playFieldPane.getChildren().remove(ppv);
          ppv.removed();
        }
      }
    }
  }

  /**
   * @return root pane from loaded FXML
   */
  public Parent asParent() {
    return root;
  }

  /**
   * @return the brickLayoutView
   */
  public BrickLayoutView getBrickLayoutView() {
    return brickLayoutView;
  }

  /**
   * Plays hit animation
   */
  public void paddleHit(Ball ball) {
    paddleHitAnimation.play(); 
    ballViewMap.get(ball).hit();
  }

  /**
   * Plays hit animation
   */
  public void ballHit(Ball ball) {
    ballViewMap.get(ball).hit();
  }

  /**
   * Plays hit animation
   * @param row
   * @param col
   */
  public void brickHit(int row, int col) {
    brickLayoutView.getBrickView(row, col).hit();
  }

}