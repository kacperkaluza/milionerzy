package com.kaluzaplotecka.milionerzy;

import com.kaluzaplotecka.milionerzy.model.Auction;
import com.kaluzaplotecka.milionerzy.model.Player;
import com.kaluzaplotecka.milionerzy.model.tiles.PropertyTile;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionIntegrationTest {

    @Test
    public void testAuctionSerializationAndIdentity() throws IOException, ClassNotFoundException {
        // Setup original objects
        Player hostP1 = new Player("p1", "Host", 1000);
        Player clientP2 = new Player("p2", "Client", 1000);
        List<Player> players = new ArrayList<>();
        players.add(hostP1);
        players.add(clientP2);

        PropertyTile property = new PropertyTile(0, "TestCity", 200, 20);
        Auction auction = new Auction(property, players);

        // Verify initial state
        assertTrue(auction.isActive());
        assertTrue(auction.canBid(hostP1));
        assertTrue(auction.canBid(clientP2));

        // Simulate Network Transmission (Serialization)
        // Server sends Auction to Client
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(auction);
        byte[] bytes = bos.toByteArray();

        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bis);
        Auction receivedAuction = (Auction) ois.readObject();

        // Client (P2) tries to bid on the received auction object
        // NOTE: The received auction contains COPIES of players (deserialized), not the original references.
        // But the Client also has its own local Player object (represented here by clientP2, or a deserialized version of it)
        
        // Scenario A: Client uses the Player instance FROM the auction
        Player p2InAuction = receivedAuction.getParticipants().stream()
                .filter(p -> p.getId().equals("p2"))
                .findFirst().orElseThrow();
        
        boolean bidResultA = receivedAuction.placeBid(p2InAuction, 100);
        assertTrue(bidResultA, "Client should be able to bid using player object obtained from auction participants");

        // Scenario B: Client uses their OWN local Player instance (which might be different from the one in Auction if Auction was serialized separately from GameState sync)
        // Let's create a 'local' copy of P2 that simulates the client's local state
        Player localP2 = new Player("p2", "Client", 1000); 
        
        // This fails if equals() is identity-based or if deserialization breaks equality
        boolean bidResultB = receivedAuction.placeBid(localP2, 120);
        assertTrue(bidResultB, "Client should be able to bid using local player object with same ID");
    }

    @Test
    public void testAuctionPassLogic() throws IOException, ClassNotFoundException {
        Player p1 = new Player("p1", "P1", 1000);
        Player p2 = new Player("p2", "P2", 1000);
        List<Player> players = new ArrayList<>();
        players.add(p1);
        players.add(p2);
        Auction auction = new Auction(new PropertyTile(0, "X", 100, 10), players);

        auction.pass(p1);
        assertFalse(auction.canBid(p1), "Player who passed cannot bid");
        assertTrue(auction.getPassedPlayers().contains(p1));
        
        // Deserialize auction and try to pass with a new instance of p2
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        new ObjectOutputStream(bos).writeObject(auction);
        Auction received = (Auction) new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray())).readObject();
        
        Player p2Clone = new Player("p2", "P2", 1000);
        int passedCountBefore = received.getPassedPlayers().size();
        received.pass(p2Clone);
        
        // Check if pass was accepted
        assertTrue(received.getPassedPlayers().contains(p2Clone), "Should contain p2Clone since it equals p2");
        assertEquals(passedCountBefore + 1, received.getPassedPlayers().size(), "Passed players count should increment");
    }
}
