package com.kaluzaplotecka.milionerzy;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

public class GameStateTest {

    @Test
    public void bankruptcyHandling() {
        List<Tile> tiles = new ArrayList<>();
        tiles.add(new Tile(0, "Start"));
        Board board = new Board(tiles);

        Player p1 = new Player("P1", 100);
        Player p2 = new Player("P2", 100);

        // give p1 a property
        PropertyTile prop = new PropertyTile(1, "Prop", 50, 10);
        prop.owner = p1;
        p1.addProperty(prop);

        List<Player> players = List.of(p1, p2);
        GameState state = new GameState(board, players);

        // make p1 bankrupt and handle
        p1.deductMoney(200);
        assertTrue(p1.isBankrupt());

        state.handleBankruptcy(p1);
        // p1 should be removed and property returned to bank
        assertFalse(state.players.contains(p1));
        assertNull(prop.owner);
    }

    @Test
    public void turnProgression() {
        Board board = new Board(java.util.List.of(new Tile(0,"S")));
        Player a = new Player("A", 100);
        Player b = new Player("B", 100);
        GameState state = new GameState(board, List.of(a,b));

        assertEquals(a, state.getCurrentPlayer());
        state.nextTurn();
        assertEquals(b, state.getCurrentPlayer());
        state.nextTurn();
        assertEquals(a, state.getCurrentPlayer());
        assertEquals(1, state.roundNumber);
    }

    @Test
    public void passStartReward() {
        Board board = new Board(java.util.List.of(new Tile(0,"Start"), new Tile(1,"A"), new Tile(2,"B")));
        Player p = new Player("P", 100);
        GameState state = new GameState(board, List.of(p));

        // move player from pos 2 by 1 -> wraps to 0 and should pass start
        p.moveTo(2);
        state.movePlayerBy(p, 1);
        assertEquals(GameState.PASS_START_REWARD + 100, p.getMoney());
        assertEquals(0, p.getPosition());
    }
}
