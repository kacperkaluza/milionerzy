package com.kaluzaplotecka.milionerzy.manager;

import javafx.scene.media.AudioClip;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton class creating a bridge to JavaFX audio system.
 * Manages loading and playing of sound effects.
 */
public class SoundManager {

    private static SoundManager instance;
    private final Map<String, AudioClip> soundCache = new HashMap<>();
    private double volume = 0.5;
    private boolean muted = false;

    private SoundManager() {
        // Private constructor for singleton
    }

    public static synchronized SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    /**
     * Plays a sound effect by name (filename in resources/sounds/).
     * Example: playSound("dice.mp3")
     * 
     * @param soundName Name of the sound file
     */
    public void playSound(String soundName) {
        if (muted) return;

        try {
            AudioClip clip = getOrLoadClip(soundName);
            if (clip != null) {
                clip.setVolume(volume);
                clip.play();
            }
        } catch (Exception e) {
            System.err.println("Failed to play sound: " + soundName + ". Error: " + e.getMessage());
        }
    }

    /**
     * Sets the global volume for sound effects.
     * 
     * @param volume Volume between 0.0 and 1.0
     */
    public void setVolume(double volume) {
        this.volume = Math.max(0.0, Math.min(1.0, volume));
        this.muted = (this.volume == 0);
    }

    public double getVolume() {
        return volume;
    }

    private AudioClip getOrLoadClip(String soundName) {
        if (soundCache.containsKey(soundName)) {
            return soundCache.get(soundName);
        }

        try {
            // Adjust path to match resources structure: src/main/resources/sounds/
            URL resource = getClass().getResource("/sounds/" + soundName);
            if (resource == null) {
                System.err.println("Sound file not found: " + soundName);
                return null;
            }

            AudioClip clip = new AudioClip(resource.toExternalForm());
            soundCache.put(soundName, clip);
            return clip;

        } catch (Exception e) {
            System.err.println("Error loading sound: " + soundName + ". Error: " + e.getMessage());
            return null;
        }
    }
}
