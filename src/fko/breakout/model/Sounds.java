/**
MIT License

Copyright (c) 2017 Frank Kopp

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

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;

import fko.breakout.BreakOut;

/**
 * Sounds
 * <p>
 * Loads all sounds and maps them to an enumeration for easy access.
 * <p>
 *  
 */
public class Sounds {

	private static Sounds instance;
	
	// folder to all sound files
	public static final String SOUND_FOLDER = "/sounds/";

	/**
	 * All available audio clips of this class
	 */
	public enum Clips {
		// ENUM		Filename w/o .wav
		WALL 		("WallHit"),
		PADDLE 		("PaddleHit"),
		BRICK_S 		("BrickHit_Special"),
		BRICK		("BrickHit_Normal"),
		BALL_LOST	("Ball_Lost");

		private final String _name;

		private Clips(String name) {
			_name = name;
		}
	}

	// to play sounds parallel
	ExecutorService _executor = Executors.newScheduledThreadPool(3);

	// available sounds mapped by the enum
	private Map<Clips, URL> _sounds;
	
	// sound on/off
	private boolean soundOn = true;

	/**
	 * Get theSounds instance with all sounds available 
	 */
	public static Sounds getInstance() {
		if (instance == null) {
			instance = new Sounds();
		}
		return instance;
	}
	
	/**
	 * Create an object with all tetris sounds available 
	 */
	private Sounds() {
		_sounds = new HashMap<>();
		// for all defined values in ENUM Clips
		// read in the Clip and store them in the Map
		Arrays.stream(Clips.values())
		.forEach(c -> {
			final String filename = SOUND_FOLDER + c._name+".wav";
			final URL url = BreakOut.class.getResource(filename);
			// create AudioInputStream object
			if (url != null) {
				_sounds.put(c, url);
			} else {
				BreakOut.criticalError("Sound file: "+filename+" cannot be loaded!");
			}
		});
	}

	/**
	 * Plays the give clip once.
	 * @param c enum from Clips
	 */
	public void playClip(Clips c) {
		
		// sound was not available
		if (_sounds.get(c) == null || !soundOn) return;
		
		// execute in a new thread to play sound
		_executor.execute(() -> {
			 AudioInputStream audioIn = null;
             try {
                 audioIn = AudioSystem.getAudioInputStream(_sounds.get(c));
             } catch (Exception e) {
            	 e.printStackTrace();
             }
             if (audioIn == null) {
                 return;
             }
             final Clip clip;
             try {
                 clip = AudioSystem.getClip();
             } catch (LineUnavailableException e) {
            	 e.printStackTrace();
                 return;
             }
             try {
                 clip.open(audioIn);
             } catch (Exception e) {
            	 e.printStackTrace();
                 return;
             }
             clip.addLineListener(event -> {
                 if (event.getType() == LineEvent.Type.STOP) {
                     clip.close();
                 }
             });
             clip.start();
		});			
	}
	
	public void soundOff() { soundOn=false; }
	public void soundOn() {	soundOn=true; }
	public boolean isSoundOn() { return soundOn; }

}