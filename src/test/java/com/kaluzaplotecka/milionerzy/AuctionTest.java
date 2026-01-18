package com.kaluzaplotecka.milionerzy;

import org.junit.jupiter.api.Test;

import com.kaluzaplotecka.milionerzy.events.GameEvent;
import com.kaluzaplotecka.milionerzy.model.Auction;
import com.kaluzaplotecka.milionerzy.model.Board;
import com.kaluzaplotecka.milionerzy.model.GameState;
import com.kaluzaplotecka.milionerzy.model.Player;
import com.kaluzaplotecka.milionerzy.model.tiles.PropertyTile;
import com.kaluzaplotecka.milionerzy.model.tiles.Tile;

import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

public class AuctionTest {
    
    private Player alice;
    private Player bob;
    private Player charlie;
    private PropertyTile property;
    private GameState gameState;
    
    @BeforeEach
    void setUp() {
        alice = new Player("Alice", 500);
        bob = new Player("Bob", 300);
        charlie = new Player("Charlie", 200);
        
        property = new PropertyTile(1, "Kielce", 200, 50);
        
        Board board = new Board(List.of(new Tile(0, "Start"), property));
        gameState = new GameState(board, List.of(alice, bob, charlie));
    }
    
    @Test
    void auction_startsWithNoHighestBidder() {
        Auction auction = new Auction(property, List.of(alice, bob));
        
        assertTrue(auction.isActive());
        assertNull(auction.getHighestBidder());
        assertEquals(0, auction.getHighestBid());
        assertEquals(Auction.DEFAULT_MINIMUM_BID, auction.getMinimumAcceptableBid());
    }
    
    @Test
    void auction_acceptsValidBid() {
        Auction auction = new Auction(property, List.of(alice, bob));
        
        assertTrue(auction.placeBid(alice, 50));
        
        assertEquals(alice, auction.getHighestBidder());
        assertEquals(50, auction.getHighestBid());
    }
    
    @Test
    void auction_rejectsBidBelowMinimum() {
        Auction auction = new Auction(property, List.of(alice, bob), 20);
        
        assertFalse(auction.placeBid(alice, 10));
        assertNull(auction.getHighestBidder());
    }
    
    @Test
    void auction_rejectsBidBelowIncrement() {
        Auction auction = new Auction(property, List.of(alice, bob));
        
        assertTrue(auction.placeBid(alice, 50));
        assertFalse(auction.placeBid(bob, 55));  // must be at least 50 + 10 = 60
        
        assertEquals(alice, auction.getHighestBidder());
    }
    
    @Test
    void auction_acceptsHigherBid() {
        Auction auction = new Auction(property, List.of(alice, bob));
        
        assertTrue(auction.placeBid(alice, 50));
        assertTrue(auction.placeBid(bob, 60));
        
        assertEquals(bob, auction.getHighestBidder());
        assertEquals(60, auction.getHighestBid());
    }
    
    @Test
    void auction_rejectsBidFromPlayerWithInsufficientFunds() {
        Auction auction = new Auction(property, List.of(alice, bob, charlie));
        
        assertTrue(auction.placeBid(alice, 150));
        assertFalse(auction.placeBid(charlie, 250));  // Charlie only has 200
        
        assertEquals(alice, auction.getHighestBidder());
    }
    
    @Test
    void auction_playerCanPass() {
        Auction auction = new Auction(property, List.of(alice, bob, charlie));
        
        auction.pass(charlie);
        
        assertTrue(auction.getPassedPlayers().contains(charlie));
        assertFalse(auction.canBid(charlie));
        assertTrue(auction.isActive());  // still 2 active players
    }
    
    @Test
    void auction_endsWhenAllButOnePass() {
        Auction auction = new Auction(property, List.of(alice, bob, charlie));
        
        assertTrue(auction.placeBid(alice, 50));
        auction.pass(bob);
        
        assertTrue(auction.isActive());  // charlie still active
        
        auction.pass(charlie);
        
        assertFalse(auction.isActive());
        assertEquals(Auction.Status.ENDED, auction.getStatus());
        assertEquals(alice, auction.getHighestBidder());
    }
    
    @Test
    void auction_transfersPropertyToWinner() {
        Auction auction = new Auction(property, List.of(alice, bob));
        
        assertTrue(auction.placeBid(alice, 100));
        auction.pass(bob);
        
        // Aukcja zakończona - sprawdź transfer
        assertEquals(alice, property.getOwner());
        assertEquals(400, alice.getMoney());  // 500 - 100
        assertTrue(alice.getOwnedProperties().contains(property), "Highest bidder should own the property");
    }
    
    @Test
    void auction_cancelledWhenNoBids() {
        Auction auction = new Auction(property, List.of(alice, bob));
        
        auction.pass(alice);
        auction.pass(bob);
        
        assertEquals(Auction.Status.CANCELLED, auction.getStatus());
        assertNull(property.getOwner());
    }
    
    @Test
    void auction_forceEndWithBid() {
        Auction auction = new Auction(property, List.of(alice, bob));
        
        assertTrue(auction.placeBid(alice, 80));
        auction.forceEnd();
        
        assertEquals(Auction.Status.ENDED, auction.getStatus());
        assertEquals(alice, property.getOwner());
    }
    
    @Test
    void auction_forceEndWithoutBid() {
        Auction auction = new Auction(property, List.of(alice, bob));
        
        auction.forceEnd();
        
        assertEquals(Auction.Status.CANCELLED, auction.getStatus());
        assertNull(property.getOwner());
    }
    
    @Test
    void auction_passedPlayerCannotBid() {
        Auction auction = new Auction(property, List.of(alice, bob));
        
        auction.pass(alice);
        
        assertFalse(auction.placeBid(alice, 50));
    }
    
    @Test
    void auction_getActiveBidders() {
        Auction auction = new Auction(property, List.of(alice, bob, charlie));
        
        assertEquals(3, auction.getActiveBidders().size());
        
        auction.pass(bob);
        
        assertEquals(2, auction.getActiveBidders().size());
        assertFalse(auction.getActiveBidders().contains(bob));
    }
    
    // === Testy integracji z GameState ===
    
    @Test
    void gameState_startAuction_firesEvent() {
        List<GameEvent> events = new ArrayList<>();
        gameState.addEventListener(events::add);
        
        assertTrue(gameState.startAuction(property));
        
        assertEquals(1, events.size());
        assertEquals(GameEvent.Type.AUCTION_STARTED, events.get(0).getType());
        assertNotNull(gameState.getCurrentAuction());
    }
    
    @Test
    void gameState_cannotStartAuctionForOwnedProperty() {
        property.setOwner(alice);
        
        assertFalse(gameState.startAuction(property));
        assertNull(gameState.getCurrentAuction());
    }
    
    @Test
    void gameState_placeBid_firesEvent() {
        List<GameEvent> events = new ArrayList<>();
        gameState.addEventListener(events::add);
        
        gameState.startAuction(property);
        assertTrue(gameState.placeBid(alice, 50));
        
        assertEquals(2, events.size());  // STARTED + BID
        assertEquals(GameEvent.Type.AUCTION_BID, events.get(1).getType());
        assertEquals(alice, events.get(1).getSource());
    }
    
    @Test
    void gameState_passAuction_firesEventAndEndsAuction() {
        List<GameEvent> events = new ArrayList<>();
        gameState.addEventListener(events::add);
        
        gameState.startAuction(property);
        gameState.placeBid(alice, 50);
        gameState.passAuction(bob);
        gameState.passAuction(charlie);
        
        // STARTED + BID + PASS + PASS + ENDED
        assertEquals(5, events.size());
        assertEquals(GameEvent.Type.AUCTION_ENDED, events.get(4).getType());
        
        assertNull(gameState.getCurrentAuction());
        assertEquals(alice, property.getOwner());
    }
    
    @Test
    void gameState_endAuction_forcesEnd() {
        List<GameEvent> events = new ArrayList<>();
        gameState.addEventListener(events::add);
        
        gameState.startAuction(property);
        gameState.placeBid(bob, 100);
        gameState.endAuction();
        
        assertEquals(GameEvent.Type.AUCTION_ENDED, events.get(events.size() - 1).getType());
        assertEquals(bob, property.getOwner());
        assertEquals(200, bob.getMoney());  // 300 - 100
    }
    
    @Test
    void gameState_cannotStartSecondAuction() {
        PropertyTile property2 = new PropertyTile(2, "Sandomierz", 150, 40);
        
        assertTrue(gameState.startAuction(property));
        assertFalse(gameState.startAuction(property2));
    }
}
