package com.kaluzaplotecka.milionerzy.manager;

import com.kaluzaplotecka.milionerzy.model.Player;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TurnManager implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // Players list is managed here now
    private List<Player> players;
    private int currentPlayerIndex;
    private int roundNumber;

    /**
     * Creates a new TurnManager with the given list of players.
     * 
     * @param players the list of players (must not be null; may be empty for fallback scenarios
     *                like corrupted save files, though an empty list results in a non-functional game state)
     * @throws IllegalArgumentException if players is null
     */
    public TurnManager(List<Player> players) {
        if (players == null) {
            throw new IllegalArgumentException("Players list cannot be null");
        }
        this.players = new ArrayList<>(players);
        this.currentPlayerIndex = 0;
        this.roundNumber = 0;
    }

    public Player getCurrentPlayer() {
        if (players.isEmpty()) return null;
        return players.get(currentPlayerIndex);
    }
    
    public List<Player> getPlayers() {
        return new ArrayList<>(players);
    }

    /**
     * Advances to the next turn.
     * 
     * Note: This class is not thread-safe. If GameState is accessed from multiple threads
     * (e.g., in a networked game with concurrent message processing), external synchronization
     * is required to prevent race conditions.
     * 
     * @return true if a new round started
     */
    public boolean nextTurn() {
        if (players.isEmpty()) return false;
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        if (currentPlayerIndex == 0) {
            roundNumber++;
            return true;
        }
        return false;
    }

    public int getRoundNumber() {
        return roundNumber;
    }
    
    /**
     * Sets the round number. Used by GameState when migrating old saves.
     * @param roundNumber the round number to set
     */
    public void setRoundNumber(int roundNumber) {
        this.roundNumber = roundNumber;
    }
    
    /**
     * Sets the current player by their ID. Used by clients to synchronize turn state
     * when receiving turn changes from the host.
     * @param playerId the ID of the player to set as current
     * @return true if the player was found and set as current, false otherwise
     */
    public boolean setCurrentPlayerById(String playerId) {
        if (playerId == null) return false;
        for (int i = 0; i < players.size(); i++) {
            if (playerId.equals(players.get(i).getId())) {
                currentPlayerIndex = i;
                return true;
            }
        }
        return false;
    }
    
    public void removePlayer(Player p) {
        int removedIndex = players.indexOf(p);
        if (removedIndex >= 0) {
            players.remove(removedIndex);
            // Adjust index if necessary
            if (removedIndex <= currentPlayerIndex && currentPlayerIndex > 0) {
                currentPlayerIndex--;
            }
            // Sanity check
            if (players.isEmpty()) {
                currentPlayerIndex = 0;
            } else {
                currentPlayerIndex = currentPlayerIndex % players.size();
            }
        }
    }
    
    public boolean isGameOver() {
        return players.size() <= 1;
    }
    
    public Player getWinner() {
        if (players.isEmpty()) return null;
        return players.get(0);
    }
}
