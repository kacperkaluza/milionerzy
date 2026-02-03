package com.kaluzaplotecka.milionerzy.manager;

import com.kaluzaplotecka.milionerzy.events.GameEvent;
import com.kaluzaplotecka.milionerzy.model.Board;
import com.kaluzaplotecka.milionerzy.model.GameState;
import com.kaluzaplotecka.milionerzy.model.Player;
import com.kaluzaplotecka.milionerzy.model.tiles.PropertyTile;
import com.kaluzaplotecka.milionerzy.model.tiles.Tile;
import java.io.Serializable;
import java.util.Random;

/**
 * Zarządza ruchem graczy i rzutami kostką.
 * 
 * <p>Klasa odpowiada za:
 * <ul>
 *   <li>Rzuty kostką (2d6)</li>
 *   <li>Przesuwanie graczy po planszy</li>
 *   <li>Obsługę przejścia przez pole Start (bonus 200 zł)</li>
 *   <li>Obsługę więzienia (automatyczne zwolnienie po 3 turach)</li>
 *   <li>Wywoływanie efektów pól po wylądowaniu</li>
 * </ul>
 * 
 * @see GameState
 * @see Player
 * @see Tile
 */
public class MovementManager implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /** Generator liczb losowych (transient - nie jest serializowany). */
    private transient Random rand;
    
    /**
     * Tworzy nowy menedżer ruchu.
     */
    public MovementManager() {
    }
    
    /**
     * Wykonuje rzut dwiema kostkami sześciennymi.
     *
     * @return suma oczek (2-12)
     */
    public int rollDice() {
        if (rand == null) rand = new Random();
        int d1 = rand.nextInt(6) + 1;
        int d2 = rand.nextInt(6) + 1;
        return d1 + d2;
    }
    
    /**
     * Ustawia generator liczb losowych.
     * 
     * <p>Używane w testach do uzyskania deterministycznych wyników.
     *
     * @param rand generator do ustawienia
     */
    public void setRandom(Random rand) {
        this.rand = rand;
    }
    
    /**
     * Wykonuje ruch aktualnego gracza z automatycznym rzutem kostką.
     *
     * @param game stan gry
     */
    public void moveCurrentPlayer(GameState game) {
        moveCurrentPlayer(game, rollDice());
    }

    /**
     * Wykonuje ruch aktualnego gracza o podaną liczbę pól.
     * 
     * <p>Obsługuje specjalne przypadki:
     * <ul>
     *   <li>Gracz w więzieniu - zwiększa licznik tur, automatycznie zwalnia po 3 turach</li>
     *   <li>Gracz staje na niezajętej nieruchomości - czeka na decyzję gracza</li>
     *   <li>Gracz bankrutuje - obsługiwane jest bankructwo</li>
     * </ul>
     *
     * @param game stan gry
     * @param steps liczba pól do przejścia
     */
    public void moveCurrentPlayer(GameState game, int steps) {
        Player p = game.getCurrentPlayer();
        if (p == null) return;

        if (p.isInJail()) {
            p.incrementJailTurns();
            if (p.getJailTurns() >= 3) {
                p.releaseFromJail();
            }
            game.nextTurn();
            return;
        }

        game.fireEvent(new GameEvent(
            GameEvent.Type.DICE_ROLLED,
            null,
            steps,
            "Wylosowano: " + steps
        ));
        
        movePlayerBy(game, p, steps);

        if (p.isBankrupt()) {
            game.handleBankruptcy(p);
            return;
        }

        Tile currentTile = game.getCurrentTile();
        if (currentTile instanceof PropertyTile pt && !pt.isOwned()) {
            return;
        }
        
        game.nextTurn();
    }
    
    /**
     * Przesuwa gracza o podaną liczbę pól.
     * 
     * <p>Automatycznie:
     * <ul>
     *   <li>Przyznaje bonus za przejście przez Start</li>
     *   <li>Emituje zdarzenia ruchu i zmiany pieniędzy</li>
     *   <li>Wywołuje efekt pola, na którym gracz wylądował</li>
     *   <li>Obsługuje bankructwo, jeśli nastąpi</li>
     * </ul>
     *
     * @param game stan gry
     * @param p gracz do przesunięcia
     * @param steps liczba pól
     */
    public void movePlayerBy(GameState game, Player p, int steps) {
        if (p == null) return;
        Board board = game.getBoard();
        int boardSize = board.size();
        if (boardSize <= 0) return;

        int oldPos = p.getPosition();
        int rawNew = oldPos + steps;
        boolean passedStart = rawNew >= boardSize;

        p.moveBy(steps, board);
        
        game.fireEvent(new GameEvent(
            GameEvent.Type.PLAYER_MOVED,
            p,
            steps,
            p.getUsername() + " przeszedł " + steps + " pól"
        ));

        if (passedStart) {
            p.addMoney(GameState.PASS_START_REWARD);
            game.fireEvent(new GameEvent(
                GameEvent.Type.MONEY_CHANGED,
                p,
                GameState.PASS_START_REWARD,
                p.getUsername() + " przeszedł Start (+200)"
            ));
        }

        Tile t = board.getTile(p.getPosition());
        if (t != null) t.onLand(game, p);
        
        if (p.isBankrupt()) {
            game.handleBankruptcy(p);
        }
    }
}
