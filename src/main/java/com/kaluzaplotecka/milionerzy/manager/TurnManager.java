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

    public TurnManager(List<Player> players) {
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
     * Sets the current player by their ID. Used by clients to synchronize turn state
     * when receiving turn changes from the host.
     * @param playerId the ID of the player to set as current
     * @return true if the player was found and set as current, false otherwise
     */
    public boolean setCurrentPlayerById(String playerId) {
        if (playerId == null) return false;
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getId().equals(playerId)) {
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
