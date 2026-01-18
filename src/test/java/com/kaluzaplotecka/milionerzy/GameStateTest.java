package com.kaluzaplotecka.milionerzy;

import org.junit.jupiter.api.Test;

import com.kaluzaplotecka.milionerzy.model.Board;
import com.kaluzaplotecka.milionerzy.model.GameState;
import com.kaluzaplotecka.milionerzy.model.Player;
import com.kaluzaplotecka.milionerzy.model.tiles.PropertyTile;
import com.kaluzaplotecka.milionerzy.model.tiles.Tile;

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
        prop.setOwner(p1);
        p1.addProperty(prop);

        List<Player> players = List.of(p1, p2);
        GameState state = new GameState(board, players);

        // make p1 bankrupt and handle
        p1.deductMoney(200);
        assertTrue(p1.isBankrupt());

        state.handleBankruptcy(p1);
        // p1 should be removed and property returned to bank
        assertFalse(state.getPlayers().contains(p1));
        assertNull(prop.getOwner());
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

    @Test
    public void runGameLoop_respectsMaxRounds_returnsNullWhenNoWinner() {
        // simple board with only Start tile -> nothing causes bankruptcy
        Board board = new Board(List.of(new Tile(0, "Start")));
        Player p1 = new Player("A", 1000);
        Player p2 = new Player("B", 1000);
        GameState gs = new GameState(board, List.of(p1, p2));

        // run only a few rounds; there is no mechanism on board to eliminate players
        Player winner = gs.runGameLoop(5);
        assertNull(winner, "No winner should be returned when maxRounds reached and multiple players remain");
        assertEquals(2, gs.players.size(), "Both players should still be present");
    }

    @Test
    public void movePlayerBy_and_bankruptcy_removesBankruptPlayer_and_transfersRent() {
        // Board: 0 Start, 1 Expensive property
        PropertyTile expensive = new PropertyTile(1, "ExpensiveTown", 0, 200);
        Board board = new Board(List.of(new Tile(0, "Start"), expensive));

        Player owner = new Player("Owner", 1000);
        Player tenant = new Player("Tenant", 100);

        // Owner already owns the property
        expensive.setOwner(owner);
        owner.addProperty(expensive);

        GameState gs = new GameState(board, List.of(owner, tenant));

        // Place tenant at start (pos 0) and move him by 1 to land on the expensive property
        assertEquals(0, tenant.getPosition());
        gs.movePlayerBy(tenant, 1);

        // Tenant should have been charged rent; because rent (200) > money (100), tenant becomes bankrupt
        assertTrue(tenant.isBankrupt(), "Tenant should be bankrupt after paying rent larger than his money");

        // Bankruptcy should be handled and tenant removed from game
        assertEquals(1, gs.players.size(), "Tenant should be removed from players list after bankruptcy");
        assertEquals(owner, gs.getWinner(), "Owner should be the remaining player (winner)");

        // Owner should have received some money (at least some of the rent)
        assertTrue(owner.getMoney() > 1000, "Owner money should increase after collecting rent (or partial amount)");
    }
}
