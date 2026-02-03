package com.kaluzaplotecka.milionerzy;

import org.junit.jupiter.api.Test;

import com.kaluzaplotecka.milionerzy.model.Board;
import com.kaluzaplotecka.milionerzy.model.Player;
import com.kaluzaplotecka.milionerzy.model.tiles.Tile;

import static org.junit.jupiter.api.Assertions.*;

public class PlayerTest {

    @Test
    public void moneyAndBankruptcy() {
        Player p = new Player("Alice", "Alice", 1000);
        assertEquals(1000, p.getMoney());

        boolean stillOk = p.deductMoney(500);
        assertTrue(stillOk);
        assertEquals(500, p.getMoney());

        // cause bankruptcy
        boolean after = p.deductMoney(600);
        assertFalse(after);
        assertTrue(p.isBankrupt());
    }

    @Test
    public void movementAndJail() {
        // create small board of size 4
        Board board = new Board(java.util.List.of(
                new Tile(0, "Start"),
                new Tile(1, "A"),
                new Tile(2, "B"),
                new Tile(3, "C")
        ));

        Player p = new Player("Bob", "Bob", 500);
        assertEquals(0, p.getPosition());

        p.moveBy(3, board);
        assertEquals(3, p.getPosition());

        p.moveBy(2, board); // wraps around
        assertEquals(1, p.getPosition());

        p.goToJail();
        assertTrue(p.isInJail());
        p.releaseFromJail();
        assertFalse(p.isInJail());
    }
}
