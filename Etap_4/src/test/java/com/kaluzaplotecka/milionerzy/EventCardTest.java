package com.kaluzaplotecka.milionerzy;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class EventCardTest {

    @Test
    public void payAndReceive() {
        Player p = new Player("Eve", 1000);

        EventCard pay = new EventCard("Pay fine", EventCard.ActionType.PAY, 200);
        pay.execute(null, p); // PAY doesn't need GameState
        assertEquals(800, p.getMoney());

        EventCard receive = new EventCard("Collect", EventCard.ActionType.RECEIVE, 150);
        receive.execute(null, p);
        assertEquals(950, p.getMoney());
    }

    @Test
    public void moveToTile() {
        Tile t0 = new Tile(0, "Start");
        Tile t1 = new Tile(1, "One");
        Tile t2 = new Tile(2, "Two");
        Board board = new Board(List.of(t0, t1, t2));

        Player p = new Player("Mover", 100);
        GameState state = new GameState(board, List.of(p));

        EventCard move = new EventCard("Go to Two", EventCard.ActionType.MOVE_TO, 2);
        move.execute(state, p);

        assertEquals(2, p.getPosition());
    }

    @Test
    public void goToJail() {
        Player p = new Player("Prisoner", 50);
        EventCard jail = new EventCard("Go to jail", EventCard.ActionType.GO_TO_JAIL, 0);
        assertFalse(p.isInJail());
        jail.execute(null, p);
        assertTrue(p.isInJail());
        assertEquals(0, p.jailTurns);
    }
}
