package com.kaluzaplotecka.milionerzy;

import org.junit.jupiter.api.Test;

import com.kaluzaplotecka.milionerzy.model.Player;
import com.kaluzaplotecka.milionerzy.model.tiles.PropertyTile;

import static org.junit.jupiter.api.Assertions.*;

public class PropertyTileTest {

    @Test
    public void buyProperty() {
        Player buyer = new Player("Buyer", 500);
        PropertyTile prop = new PropertyTile(1, "SmallTown", 200, 20);

        assertFalse(prop.isOwned());
        boolean bought = prop.buy(buyer);
        assertTrue(bought);
        assertTrue(prop.isOwned());
        assertEquals(buyer, prop.owner);
        assertEquals(300, buyer.getMoney());
    }

    @Test
    public void rentPayment() {
        Player owner = new Player("Owner", 100);
        Player tenant = new Player("Tenant", 150);
        PropertyTile prop = new PropertyTile(2, "City", 50, 30);

        // owner purchases
        boolean bought = prop.buy(owner);
        assertTrue(bought);

        int charged = prop.chargeRent(tenant);
        assertEquals(prop.calculateRent(), charged);
        // owner initially had 100, paid price when buying
        assertEquals(100 - prop.price + charged, owner.getMoney());
        assertEquals(150 - charged, tenant.getMoney());
    }
}
