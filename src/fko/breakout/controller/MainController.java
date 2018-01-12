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
package fko.breakout.controller;

import java.net.URL;
import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;

import fko.breakout.BreakOut;
import fko.breakout.events.GameEvent;
import fko.breakout.events.GameEvent.GameEventType;
import fko.breakout.model.Ball;
import fko.breakout.model.BreakOutGame;
import fko.breakout.model.PowerPill;
import fko.breakout.model.SoundManager;
import fko.breakout.model.SoundManager.Clips;
import fko.breakout.view.MainView;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

/**
 * MainController
 * <p>
 * The Controller sets up additional ui elements after the FXML loader has done its initialization. The FXML loader
 * calls the Controller's initialize() method.<br> 
 * The Controller also receives all input and events from the user interface and the model and executes the appropriate 
 * ui updates and model actions. The UI calls the actions methods directly. The model signals via the Observer Interface 
 * and Property Bindings that the model has changed and the UI should update its views.
 * 
 * @see java.util.Observer#update(java.util.Observable, java.lang.Object) 
 *
 * <p>
 * 02.01.2018
 * @author Frank Kopp
 */
public class MainController implements Initializable, Observer {

  // handles to model and view
  private final BreakOutGame model;
  private MainView view;

  // sounds
  private final SoundManager sounds = SoundManager.getInstance();

  /**
   * Creates the MainController
   * @param model
   */
  public MainController(BreakOutGame model) {
    this.model = model;
  }

  /**
   * Called by FXMLLoader
   * @see javafx.fxml.Initializable#initialize(java.net.URL, java.util.ResourceBundle)
   */
  @Override
  public void initialize(URL location, ResourceBundle resources) {
    // empty
  }

  /**
   * Bind views to model data mostly through property bindings but also through Observer pattern.<br>
   * See update()
   * @param view 
   */
  public void bindModelToView(MainView view) {
    this.view = view;

    // add controller as listener of model for GameEvents
    model.addObserver(this);

    // scene title
    String tmpTitle = BreakOut.getPrimaryStage().getTitle();
    BreakOut.getPrimaryStage().titleProperty().bind(
            new SimpleStringProperty(tmpTitle + " (fps:")
                    .concat(model.fpsProperty().asString("%.2f"))
                            .concat(")"));

    // add keyboard handlers
    view.asParent().getScene().setOnKeyPressed(this::keyPressedAction);
    view.asParent().getScene().setOnKeyReleased(this::keyReleasedAction);

    // add mouse handlers
    view.asParent().getScene().setOnMouseMoved(this::mouseMovedAction);

    // playfield dimensions
    playfieldPane.prefWidthProperty().bind(model.playfieldWidthProperty());
    playfieldPane.prefHeightProperty().bind(model.playfieldHeightProperty());

    // paddle dimensions and location
    paddle.widthProperty().bind(model.paddleWidthProperty());
    paddle.heightProperty().bind(model.paddleHeightProperty());
    paddle.xProperty().bind(model.paddleXProperty());
    paddle.yProperty().bind(model.paddleYProperty());

    // update handler for ball manager
    model.getBallManager().addListener(
        (ListChangeListener.Change<?> change) -> view.updateBallList((Change<Ball>) change));

    // update handler for fallingPills
    model.fallingPowerPillsProperty().addListener(
            (ListChangeListener.Change<?> change) -> view.updateFallingPillList((Change<PowerPill>) change));

    // startstopButton text updater
    model.isPlayingProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue) {
        startStopButton.setText("Stop");
      } else {
        startStopButton.setText("Play");
      }
    });

    // pauseResumeButton text updater
    model.isPausedProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue) {
        pauseResumeButton.setText("Resume");
      } else {
        pauseResumeButton.setText("Pause");
      }
    });

    // Level text
    levelLabel.textProperty().bind(new SimpleStringProperty("Level ").concat(model.currentLevelProperty()));
    // remaining lives text
    livesLabel.textProperty().bind(model.currentRemainingLivesProperty().asString());
    // score text
    pointsLabel.textProperty().bind(model.currentScoreProperty().asString("%06d"));

    // game over splash text
    // TODO: GAME_OVER vs. GAME_WIN
    gameOverSplash.visibleProperty().bind(model.gameOverProperty());

  }

  /**
   * We use the Observable notification for certain events to enable animations and sound.
   * Most other model changes are handled through Property Bindings. 
   * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
   */
  @Override
  public void update(Observable o, Object e) {
    if (!(e instanceof GameEvent)) {
      BreakOut.fatalError("Unknown event type. Event is not of type GameEvent");
    }

    GameEvent gameEvent = (GameEvent) e;

    final Object[] param = (Object[]) gameEvent.getEventParameter();

    // define actions for different events
    switch (gameEvent.getEventType()) {
    case HIT_PADDLE:    
      view.paddleHit((Ball) param[0]); 
      sounds.playClip(Clips.PADDLE); 
      break;
    case HIT_WALL:      
      view.ballHit((Ball) param[0]); 
      //sounds.playClip(Clips.WALL);
      break;
    case HIT_BRICK:     
      handleHitBrickEvent(gameEvent); 
      break;
    case LAST_BALL_LOST:     
      sounds.playClip(Clips.BALL_LOST); 
      break;
    case BALL_LOST: break;
    case LEVEL_COMPLETE: break;
    case LEVEL_START:   break;
    case GAME_START:    break;
    case GAME_OVER:     break;
    case GAME_WON:      break;
    default: 
    }

    // update brickLayoutView
    if (gameEvent.getEventType().equals(GameEventType.LEVEL_START) 
        || gameEvent.getEventType().equals(GameEventType.HIT_BRICK)
        || gameEvent.getEventType().equals(GameEventType.GAME_STOPPED)) {

      view.getBrickLayoutView().draw(model.getBrickLayout());
    }
  }

  /**
   * @param event 
   */
  private void handleHitBrickEvent(GameEvent event) {
    if (event.getEventParameter() != null) {
      final Object[] param = (Object[]) event.getEventParameter();
      final int row = (int) param[0];
      final int col = (int) param[1];
      final Ball ball = (Ball) param[2];
      if (model.getBrickLayout().getBrick(row, col) == null) {
        sounds.playClip(Clips.BRICK);
      } else {
        view.brickHit(row, col);
        sounds.playClip(Clips.BRICK_S);
      }
      view.ballHit(ball); 
    }
  }

  /**
   * Handles key pressed events
   * @param event
   */
  private void keyPressedAction(KeyEvent event) {
    switch (event.getCode()) {
    // game control
    case SPACE: 	startStopButtonAction(new ActionEvent()); break;
    case P: 		pauseResumeButtonAction(new ActionEvent()); break;
    case S:			soundButtonAction(new ActionEvent()); break;
    // paddle control
    case LEFT:		onPaddleLeftAction(true); break;
    case RIGHT:		onPaddleRightAction(true); break;
    default:
    }
  }

  /**
   * Handles key released events
   * @param event
   */
  private void keyReleasedAction(KeyEvent event) {
    switch (event.getCode()) {
    // paddle control
    case LEFT: 		onPaddleLeftAction(false); break;
    case RIGHT:		onPaddleRightAction(false); break;
    default:
    }
  }

  /**
   * Paddle action
   * @param b
   */
  private void onPaddleLeftAction(boolean b) {
    if (b) model.setPaddleLeft(true);
    else model.setPaddleLeft(false);	
  }

  /**
   * Paddle action
   * @param b
   */
  private void onPaddleRightAction(boolean b) {
    if (b) model.setPaddleRight(true);
    else model.setPaddleRight(false);
  }

  /**
   * Mouse action
   */
  private void mouseMovedAction(MouseEvent event) {
    model.setMouseXPosition(event.getX());
  }

  /**
   * Toggles Start/Stop game 
   */
  @FXML
  void startStopButtonAction(ActionEvent event) {
    if (model.isPlaying()) {
      model.stopPlaying();
    } else {
      model.startPlaying();
    }
  }

  /**
   * Toggles Pause/Resume game
   * @param event
   */
  @FXML
  void pauseResumeButtonAction(ActionEvent event) {
    if (model.isPlaying()) {
      if (model.isPaused()) {
        model.resumePlaying();
      } else {
        model.pausePlaying();
      }
    }
  }

  /**
   * Toggles sound option
   * @param event
   */
  @FXML
  void soundButtonAction(ActionEvent event) {
    if (sounds.isSoundOn()) {
      soundButton.setText("Sound On");
      sounds.soundOff();
    } else {
      soundButton.setText("Sound Off");
      sounds.soundOn();
    }
  }

  @FXML
  void paddleMouseClickAction(MouseEvent event) {
    // not used
  }

  @FXML
  void paddleMouseReleasedAction(MouseEvent event) {
    // not used
  }

  /* ********************************************************
   * FXML - injected by FXMLLoader
   * ********************************************************/

  @FXML
  private Button startStopButton;

  @FXML
  private Button pauseResumeButton;

  @FXML
  private Button soundButton;

  @FXML
  private Text levelLabel;

  @FXML
  private Text livesLabel;

  @FXML
  private Text pointsLabel;

  @FXML
  private Pane playfieldPane;

  @FXML
  private Rectangle ceilingWall;

  @FXML
  private Rectangle leftWall;

  @FXML
  private Rectangle rightWall;

  @FXML
  private Rectangle paddle;

  @FXML
  private Text gameOverSplash;

}
