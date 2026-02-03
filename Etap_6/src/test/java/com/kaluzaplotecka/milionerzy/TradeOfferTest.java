package com.kaluzaplotecka.milionerzy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

public class TradeOfferTest {
    
    private Player alice;
    private Player bob;
    private PropertyTile prop1;
    private PropertyTile prop2;
    private GameState gameState;
    
    @BeforeEach
    void setUp() {
        alice = new Player("Alice", 1000);
        bob = new Player("Bob", 1000);
        
        prop1 = new PropertyTile(1, "Kielce", 200, 50);
        prop1.owner = alice;
        alice.addProperty(prop1);
        
        prop2 = new PropertyTile(2, "Sandomierz", 150, 40);
        prop2.owner = bob;
        bob.addProperty(prop2);
        
        Board board = new Board(List.of(new Tile(0, "Start"), prop1, prop2));
        gameState = new GameState(board, List.of(alice, bob));
    }
    
    @Test
    void tradeOffer_isValid_whenBothPartiesHaveAssets() {
        TradeOffer offer = new TradeOffer(
            alice, bob,
            List.of(prop1),     // Alice oferuje Kielce
            List.of(prop2),     // Alice chce Sandomierz
            100,                // Alice daje 100 zł
            0                   // Alice nie żąda pieniędzy
        );
        
        assertTrue(offer.isValid(), "Oferta powinna być prawidłowa");
        assertEquals(TradeOffer.Status.PENDING, offer.getStatus());
    }
    
    @Test
    void tradeOffer_isNotValid_whenProposerDoesNotOwnProperty() {
        TradeOffer offer = new TradeOffer(
            alice, bob,
            List.of(prop2),     // Alice nie ma Sandomierza!
            null,
            0, 0
        );
        
        assertFalse(offer.isValid(), "Oferta powinna być nieprawidłowa - Alice nie ma tej nieruchomości");
    }
    
    @Test
    void tradeOffer_isNotValid_whenNotEnoughMoney() {
        TradeOffer offer = new TradeOffer(
            alice, bob,
            null, null,
            2000,   // Alice nie ma 2000 zł
            0
        );
        
        assertFalse(offer.isValid(), "Oferta powinna być nieprawidłowa - za mało pieniędzy");
    }
    
    @Test
    void tradeOffer_execute_transfersAssetsCorrectly() {
        TradeOffer offer = new TradeOffer(
            alice, bob,
            List.of(prop1),     // Alice daje Kielce
            List.of(prop2),     // Bob daje Sandomierz
            50,                 // Alice daje 50 zł
            100                 // Bob daje 100 zł
        );
        
        assertTrue(offer.execute(), "Wymiana powinna się powieść");
        assertEquals(TradeOffer.Status.ACCEPTED, offer.getStatus());
        
        // Sprawdź transfer nieruchomości
        assertEquals(bob, prop1.owner, "Bob powinien mieć Kielce");
        assertEquals(alice, prop2.owner, "Alice powinna mieć Sandomierz");
        
        // Sprawdź transfer pieniędzy: Alice: 1000 - 50 + 100 = 1050, Bob: 1000 + 50 - 100 = 950
        assertEquals(1050, alice.getMoney());
        assertEquals(950, bob.getMoney());
    }
    
    @Test
    void tradeOffer_reject_changesStatus() {
        TradeOffer offer = new TradeOffer(alice, bob, null, null, 100, 0);
        
        offer.reject();
        
        assertEquals(TradeOffer.Status.REJECTED, offer.getStatus());
        assertFalse(offer.execute(), "Odrzucona oferta nie powinna być wykonywalna");
    }
    
    @Test
    void tradeOffer_cancel_changesStatus() {
        TradeOffer offer = new TradeOffer(alice, bob, null, null, 100, 0);
        
        offer.cancel();
        
        assertEquals(TradeOffer.Status.CANCELLED, offer.getStatus());
    }
    
    @Test
    void gameState_proposeTrade_firesEvent() {
        List<GameEvent> receivedEvents = new ArrayList<>();
        gameState.addEventListener(receivedEvents::add);
        
        TradeOffer offer = new TradeOffer(alice, bob, List.of(prop1), null, 0, 200);
        
        assertTrue(gameState.proposeTrade(offer));
        
        assertEquals(1, receivedEvents.size());
        assertEquals(GameEvent.Type.TRADE_PROPOSED, receivedEvents.get(0).getType());
        assertEquals(alice, receivedEvents.get(0).getSource());
    }
    
    @Test
    void gameState_acceptTrade_executesAndFiresEvent() {
        List<GameEvent> receivedEvents = new ArrayList<>();
        gameState.addEventListener(receivedEvents::add);
        
        TradeOffer offer = new TradeOffer(alice, bob, List.of(prop1), null, 0, 200);
        gameState.proposeTrade(offer);
        
        assertTrue(gameState.acceptTrade());
        
        assertEquals(2, receivedEvents.size());  // PROPOSED + ACCEPTED
        assertEquals(GameEvent.Type.TRADE_ACCEPTED, receivedEvents.get(1).getType());
        
        // Sprawdź że wymiana się wykonała
        assertEquals(bob, prop1.owner);
        assertEquals(1200, alice.getMoney());  // 1000 + 200
        assertEquals(800, bob.getMoney());     // 1000 - 200
    }
    
    @Test
    void gameState_rejectTrade_firesEvent() {
        List<GameEvent> receivedEvents = new ArrayList<>();
        gameState.addEventListener(receivedEvents::add);
        
        TradeOffer offer = new TradeOffer(alice, bob, null, null, 100, 0);
        gameState.proposeTrade(offer);
        
        assertTrue(gameState.rejectTrade());
        
        assertEquals(2, receivedEvents.size());  // PROPOSED + REJECTED
        assertEquals(GameEvent.Type.TRADE_REJECTED, receivedEvents.get(1).getType());
        assertNull(gameState.getPendingTrade());
    }
    
    @Test
    void gameState_onlyOneTradeAtATime() {
        TradeOffer offer1 = new TradeOffer(alice, bob, null, null, 100, 0);
        TradeOffer offer2 = new TradeOffer(bob, alice, null, null, 50, 0);
        
        assertTrue(gameState.proposeTrade(offer1));
        assertFalse(gameState.proposeTrade(offer2), "Nie można złożyć drugiej oferty gdy jedna oczekuje");
    }
}
