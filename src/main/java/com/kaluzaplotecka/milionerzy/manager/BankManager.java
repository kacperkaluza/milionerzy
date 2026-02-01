package com.kaluzaplotecka.milionerzy.manager;

import com.kaluzaplotecka.milionerzy.model.GameState;
import com.kaluzaplotecka.milionerzy.model.Player;
import com.kaluzaplotecka.milionerzy.model.tiles.PropertyTile;
import java.io.Serializable;
import java.util.ArrayList;

public class BankManager implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // Stateless manager, logic only
    
    public void handleBankruptcy(GameState game, Player p) {
        TurnManager turnManager = game.getTurnManager();
        
        // Release properties
        for (PropertyTile prop : new ArrayList<>(p.getOwnedProperties())) {
            prop.setOwner(null);
            p.removeProperty(prop);
        }

        // Remove player via TurnManager
        turnManager.removePlayer(p);
        
        // Logic to clear current player index handled in TurnManager.removePlayer
        
        // Note: GameState might need to check if game over
    }
}
