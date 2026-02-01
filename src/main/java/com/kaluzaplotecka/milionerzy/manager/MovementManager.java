package com.kaluzaplotecka.milionerzy.manager;

import com.kaluzaplotecka.milionerzy.events.GameEvent;
import com.kaluzaplotecka.milionerzy.model.Board;
import com.kaluzaplotecka.milionerzy.model.GameState;
import com.kaluzaplotecka.milionerzy.model.Player;
import com.kaluzaplotecka.milionerzy.model.tiles.PropertyTile;
import com.kaluzaplotecka.milionerzy.model.tiles.Tile;
import java.io.Serializable;
import java.util.Random;

public class MovementManager implements Serializable {
    private static final long serialVersionUID = 1L;
    private transient Random rand;
    
    public MovementManager() {
        this.rand = new Random();
    }
    
    public int rollDice() {
        if (rand == null) rand = new Random();
        int d1 = rand.nextInt(6) + 1;
        int d2 = rand.nextInt(6) + 1;
        return d1 + d2;
    }
    
    public void setRandom(Random rand) {
        this.rand = rand;
    }
    
    public void moveCurrentPlayer(GameState game) {
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

        int steps = rollDice();
        
        game.fireEvent(new GameEvent(
            GameEvent.Type.DICE_ROLLED,
            null,
            steps,
            "Wylosowano: " + steps
        ));
        
        movePlayerBy(game, p, steps);

        // BankManager handles bankruptcy check potentially? 
        // Or GameState facade does it. 
        // GameState logic had:
        /*
        if (p.isBankrupt()){
            handleBankruptcy(p);
            return;
        }
        */
        if (p.isBankrupt()) {
            game.handleBankruptcy(p);
            return;
        }

        // Check landing
        Tile currentTile = game.getCurrentTile();
        if (currentTile instanceof PropertyTile pt && !pt.isOwned()) {
            // Player needs to decide -> wait
            return;
        }
        
        game.nextTurn();
    }
    
    public void movePlayerBy(GameState game, Player p, int steps) {
        if (p == null) return;
        Board board = game.getBoard();
        int boardSize = board.size();
        if (boardSize <= 0) return;

        int oldPos = p.getPosition();
        // Check for pass start
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
