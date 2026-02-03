package com.kaluzaplotecka.milionerzy.manager;

import com.kaluzaplotecka.milionerzy.model.GameState;
import com.kaluzaplotecka.milionerzy.model.Player;
import com.kaluzaplotecka.milionerzy.model.tiles.PropertyTile;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Zarządza operacjami bankowymi i bankructwem graczy.
 * 
 * <p>Klasa bezstanowa (stateless) - nie przechowuje żadnych danych,
 * tylko wykonuje operacje na przekazanych obiektach.
 * 
 * <p>Główne funkcjonalności:
 * <ul>
 *   <li>Obsługa bankructwa gracza</li>
 *   <li>Zwolnienie nieruchomości zbankrutowanego gracza</li>
 * </ul>
 * 
 * @see TurnManager
 * @see Player
 */
public class BankManager implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Obsługuje bankructwo gracza.
     * 
     * <p>Wykonuje następujące czynności:
     * <ul>
     *   <li>Zwalnia wszystkie nieruchomości gracza (stają się niezajęte)</li>
     *   <li>Usuwa gracza z gry poprzez TurnManager</li>
     * </ul>
     * 
     * <p><b>Uwaga:</b> Po wywołaniu tej metody należy sprawdzić,
     * czy gra się zakończyła (isGameOver()).
     *
     * @param game stan gry
     * @param p gracz, który zbankrutował
     */
    public void handleBankruptcy(GameState game, Player p) {
        TurnManager turnManager = game.getTurnManager();
        
        for (PropertyTile prop : new ArrayList<>(p.getOwnedProperties())) {
            prop.setOwner(null);
            p.removeProperty(prop);
        }

        turnManager.removePlayer(p);
    }
}
