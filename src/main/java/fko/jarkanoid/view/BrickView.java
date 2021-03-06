/*
 * MIT License
 *
 * Copyright (c) 2018 Frank Kopp
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package fko.jarkanoid.view;

import fko.jarkanoid.model.Brick;
import javafx.animation.FillTransition;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * BrickVew
 *
 * <p>Extends <code>Rectangle</code> to represent a Brick view.<br>
 * Adds design and animations.<br>
 *
 * <p>05.01.2018
 *
 * @author Frank Kopp
 */
public class BrickView extends Rectangle {

  private final FillTransition solidBrickHitTimeline;

  private final Brick brick;

  //  private static final InnerShadow effect = new InnerShadow(BlurType.ONE_PASS_BOX, Color.WHITE,
  //          2.0, 0.0, 1.0, 1.0);

  /**
   * Creates a new Brick view which is an extension of a Rectangle
   * @param x
   * @param y
   * @param width
   * @param height
   * @param brick
   */
  BrickView(double x, double y, double width, double height, Brick brick) {
    super(x, y, width, height);
    this.brick = brick;

    // let the CSS determine the look of the ball
    this.setFill(brick.getColor());
    this.getStyleClass().add("brick");
    //    this.setArcWidth(5.0);
    //    this.setArcHeight(5.0);
    //    this.setStroke(Color.BLACK);
    //    this.setStrokeType(StrokeType.INSIDE);
    //    this.setStrokeWidth(1.0);
    //    this.setEffect(effect);

    solidBrickHitTimeline = new FillTransition(Duration.millis(75));
    solidBrickHitTimeline.setFromValue(brick.getColor());
    solidBrickHitTimeline.setToValue(Color.WHITE);
    solidBrickHitTimeline.setCycleCount(2);
    solidBrickHitTimeline.setAutoReverse(true);
    solidBrickHitTimeline.setShape(this);
  }

  /** Called from Controller after BRICK_LOST event to stop any animation still running */
  public void hit() {
    solidBrickHitTimeline.playFromStart();
  }

  /** @return the brick */
  public Brick getBrick() {
    return brick;
  }
}
