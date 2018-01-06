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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * BrickTest
 * 04.01.2018
 * @author Frank Kopp
 */
class BrickTest {

	/**
	 * Test method for {@link fko.breakout.model.Brick#Brick(fko.breakout.model.BrickType, fko.breakout.model.BrickPowerType)}.
	 */
	@Test
	void testBrick() {
		Brick brick = new Brick(BrickType.GREY, BrickPowerType.NONE);
		assertNotNull(brick);
	}

	/**
	 * Test method for {@link fko.breakout.model.Brick#increaseHitCount()}.
	 */
	@Test
	void testIncreaseHitCount() {
		Brick brick = new Brick(BrickType.SILVER, BrickPowerType.NONE);
		int remaining = brick.getRemainingHits();
		int newRemaining = brick.increaseHitCount();
		assertTrue(remaining-newRemaining==1);
	}

	/**
	 * Test method for {@link fko.breakout.model.Brick#getHitCount()}.
	 */
	@Test
	void testGetHitCount() {
		Brick brick = new Brick(BrickType.GREY, BrickPowerType.NONE);
		assertTrue(brick.getHitCount()==0);
		brick.increaseHitCount();
		assertTrue(brick.getHitCount()==1);
	}

	/**
	 * Test method for {@link fko.breakout.model.Brick#getRemainingHits()}.
	 */
	@Test
	void testGetRemainingHits() {
		Brick brick = new Brick(BrickType.GREY, BrickPowerType.NONE);
		brick.increaseHitCount();
		assertTrue(brick.getRemainingHits()==0);
	}

	/**
	 * Test method for {@link fko.breakout.model.Brick#isKilled()}.
	 */
	@Test
	void testIsKilled() {
		Brick brick = new Brick(BrickType.SILVER, BrickPowerType.NONE);
		int remainingHits = brick.getRemainingHits(); 
		for (int i=0; i<remainingHits; i++) {
			brick.increaseHitCount();	
		}
		assertTrue(brick.isKilled());
	}

	/**
	 * Test method for {@link fko.breakout.model.Brick#getType()}.
	 */
	@Test
	void testGetType() {
		Brick brick = new Brick(BrickType.SILVER, BrickPowerType.NONE);
		assertEquals(BrickType.SILVER, brick.getType());
	}

	/**
	 * Test method for {@link fko.breakout.model.Brick#getPowerType()}.
	 */
	@Test
	void testGetPowerType() {
		Brick brick = new Brick(BrickType.SILVER, BrickPowerType.CATCH);
		assertEquals(BrickPowerType.CATCH, brick.getPowerType());
	}

	/**
	 * Test method for {@link fko.breakout.model.Brick#getPoints()}.
	 */
	@Test
	void testGetScore() {
		Brick brick = new Brick(BrickType.SILVER, BrickPowerType.NONE);
		assertEquals(150, brick.getPoints());
	}

	/**
	 * Test method for {@link fko.breakout.model.Brick#getColor()}.
	 */
	@Test
	void testGetColor() {
		Brick brick = new Brick(BrickType.SILVER, BrickPowerType.NONE);
		assertEquals(150, brick.getPoints());
	}

	/**
	 * Test method for {@link fko.breakout.model.Brick#toShortString()}.
	 */
	@Test
	void testToShortString() {
		Brick brick = new Brick(BrickType.SILVER, BrickPowerType.NONE);
		System.out.println(brick.toShortString());
		assertEquals("SINO", brick.toShortString());
	}

	/**
	 * Test method for {@link fko.breakout.model.Brick#clone()}.
	 */
	@Test
	void testClone() {
		Brick brick = new Brick(BrickType.SILVER, BrickPowerType.NONE);
		Brick cloneBrick = brick.clone();
		assertTrue(brick.equals(cloneBrick));
		assertFalse(brick == cloneBrick);
	}

	/**
	 * Test method for {@link fko.breakout.model.Brick#equals()}.
	 */
	@Test
	void testEquals() {
		Brick brick1 = new Brick(BrickType.SILVER, BrickPowerType.NONE);
		Brick brick2 = new Brick(BrickType.SILVER, BrickPowerType.NONE);
		assertTrue(brick1.equals(brick2));
		brick2.increaseHitCount();
		assertFalse(brick1.equals(brick2));
		Brick brick3 = new Brick(BrickType.SILVER, BrickPowerType.CATCH);
		assertFalse(brick1.equals(brick3));
		Brick brick4 = new Brick(BrickType.GREY, BrickPowerType.NONE);
		assertFalse(brick1.equals(brick4));
	}

}