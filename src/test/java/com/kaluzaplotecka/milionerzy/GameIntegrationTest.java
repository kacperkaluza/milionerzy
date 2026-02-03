package com.kaluzaplotecka.milionerzy;

import com.kaluzaplotecka.milionerzy.model.GameState;
import com.kaluzaplotecka.milionerzy.model.Player;
import com.kaluzaplotecka.milionerzy.model.tiles.PropertyTile;
import com.kaluzaplotecka.milionerzy.model.tiles.Tile;
import com.kaluzaplotecka.milionerzy.view.GameView;
import javafx.scene.Node;
import javafx.stage.Stage;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GameIntegrationTest {

    private GameView gameView;
    private Stage stage;

    @Start
    public void start(Stage stage) {
        this.stage = stage;
        // Setup players
        List<Player> players = new ArrayList<>();
        players.add(new Player("p1", "Player 1", 1500));
        players.add(new Player("p2", "Player 2", 1500));
        players.add(new Player("p3", "Player 3", 1500));
        players.add(new Player("p4", "Player 4", 1500));

        // Initialize GameView in Local mode (networkManager = null)
        gameView = new GameView(stage, players, null, "p1");
        gameView.show();
    }

    @Test
    @Order(1)
    void testMovement(FxRobot robot) {
        GameState gameState = gameView.getGameState();
        
        // Inject deterministic Random
        // Target safe tile: 10 (Jail / Just Visiting). Start 0. Need 10.
        // Roll 5+5. Mock nextInt(6) -> return 4.
        gameState.setRandom(new Random() {
            @Override
            public int nextInt(int bound) {
                return 4; // 4+1 = 5
            }
        });

        int initialPos = gameState.getCurrentPlayer().getPosition();
        
        org.testfx.util.WaitForAsyncUtils.waitForFxEvents();
        // Use lookup to find button, ensuring we click the right node regardless of text change race conditions
        Node rollButton = robot.lookup(".button").queryAll().stream()
                .filter(n -> n instanceof javafx.scene.control.ButtonBase && ((javafx.scene.control.ButtonBase)n).getText().contains("Losuj"))
                .findFirst().orElseThrow(() -> new AssertionError("Roll button not found"));
        robot.clickOn(rollButton);
        
        // Wait for animation
        try { TimeUnit.SECONDS.sleep(3); } catch (Exception e) {}
        org.testfx.util.WaitForAsyncUtils.waitForFxEvents();

        // Check P1 (index 0) position specifically, as current player might have advanced
        int p1Pos = gameState.getPlayers().get(0).getPosition();
        assertTrue(p1Pos != initialPos, "Player 1 should have moved");
        assertEquals(10, p1Pos, "Player 1 should be on Jail tile (pos 10)");
    }
    
    @Test
    @Order(2)
    void testPropertyPurchase(FxRobot robot) {
        GameState gameState = gameView.getGameState();
        Player p1 = gameState.getCurrentPlayer();
        
        // Find first PropertyTile
        int propIndex = -1;
        for (int i=1; i<gameState.getBoard().size(); i++) {
            if (gameState.getBoard().getTile(i) instanceof PropertyTile) {
                propIndex = i;
                break;
            }
        }
        assertTrue(propIndex != -1, "Board should have a property tile");
        
        // Target: propIndex.
        // We want to roll 2 (min roll).
        // Set pos = propIndex - 2.
        int startPos = (propIndex - 2 + gameState.getBoard().size()) % gameState.getBoard().size();
        p1.setPosition(startPos);
        
        gameState.setRandom(new Random() {
            @Override
            public int nextInt(int bound) { 
                return 0; // 1+1=2
            } 
        });
        
        org.testfx.util.WaitForAsyncUtils.waitForFxEvents();
        robot.clickOn("🎲  Losuj");
        
        try { TimeUnit.SECONDS.sleep(3); } catch (Exception e) {}
        org.testfx.util.WaitForAsyncUtils.waitForFxEvents();
        
        // Verify Dialog
        Node dialogPane = robot.lookup(".dialog-pane").query();
        assertNotNull(dialogPane, "Purchase dialog should appear at pos " + gameState.getCurrentPlayer().getPosition());
        
        robot.clickOn("Kup");
        org.testfx.util.WaitForAsyncUtils.waitForFxEvents();
        
        PropertyTile tile = (PropertyTile) gameState.getBoard().getTile(propIndex);
        assertEquals(p1, tile.getOwner(), "Player 1 should own the property");
    }

    @Test
    @Order(3)
    void testAuction(FxRobot robot) {
        GameState gameState = gameView.getGameState();
        Player current = gameState.getCurrentPlayer();
        
        // Find another PropertyTile (or reuse if we reset owner, but p1 owns first now)
        int propIndex = -1;
        for (int i=1; i<gameState.getBoard().size(); i++) {
            if (gameState.getBoard().getTile(i) instanceof PropertyTile) {
                PropertyTile pt = (PropertyTile) gameState.getBoard().getTile(i);
                if (!pt.isOwned()) {
                    propIndex = i;
                    break;
                }
            }
        }
        if (propIndex == -1) {
            propIndex = 1; // Fallback
            ((PropertyTile)gameState.getBoard().getTile(1)).setOwner(null);
        }
        
        // Target: propIndex. Roll 2.
        int startPos = (propIndex - 2 + gameState.getBoard().size()) % gameState.getBoard().size();
        current.setPosition(startPos);
        
        gameState.setRandom(new Random() {
            @Override
            public int nextInt(int bound) { 
                return 0; // 1+1=2
            }
        });
        
        org.testfx.util.WaitForAsyncUtils.waitForFxEvents();
        robot.clickOn("🎲  Losuj");
        
        try { TimeUnit.SECONDS.sleep(3); } catch (Exception e) {}
        org.testfx.util.WaitForAsyncUtils.waitForFxEvents();
        
        Node dialogPane = robot.lookup(".dialog-pane").query();
        assertNotNull(dialogPane, "Purchase dialog should appear");
        
        robot.clickOn("Licytuj");
        org.testfx.util.WaitForAsyncUtils.waitForFxEvents();
        
        try { TimeUnit.SECONDS.sleep(1); } catch (Exception e) {}
        org.testfx.util.WaitForAsyncUtils.waitForFxEvents();
        
        // Button text is "Podbij +100" in AuctionComponent
        boolean foundAuctionBtn = robot.lookup(".button").queryAll().stream()
               .anyMatch(n -> n instanceof javafx.scene.control.ButtonBase && 
                         ((javafx.scene.control.ButtonBase)n).getText().contains("Podbij +100"));
                         
        assertTrue(foundAuctionBtn, "Auction button +100 should be visible");
        
        assertTrue(foundAuctionBtn, "Auction button +100 should be visible");
        
        robot.clickOn("Podbij +100");
    }

    @Test
    @Order(4)
    void testMultiPlayerTurns(FxRobot robot) {
        GameState gameState = gameView.getGameState();
        
        // Ensure starting state
        assertEquals("p1", gameState.getCurrentPlayer().getId(), "Should start with Player 1");
        
        // P1 Turn: Roll safe (e.g., 5+5=10 -> Jail/Just Visiting, safe)
        gameState.setRandom(new Random() {
            private int callCount = 0;
            @Override
            public int nextInt(int bound) {
                // Call 1 & 2 -> P1 Roll (5,5 -> 10)
                // Call 3 & 4 -> P2 Roll (1,1 -> 2)
                int val;
                if (callCount < 2) val = 4; // 4+1=5
                else val = 0; // 0+1=1
                callCount++;
                return val;
            }
        });
        
        // P1 Rolls
        org.testfx.util.WaitForAsyncUtils.waitForFxEvents();
        robot.clickOn("🎲  Losuj");
        
        try { TimeUnit.SECONDS.sleep(3); } catch (Exception e) {}
        org.testfx.util.WaitForAsyncUtils.waitForFxEvents();
        
        // Verify P1 moved
        assertEquals(10, gameState.getPlayers().get(0).getPosition(), "Player 1 should be at 10");
        
        // Verify Turn passed to P2
        assertEquals("p2", gameState.getCurrentPlayer().getId(), "Turn should pass to Player 2");
        
        // P2 Turn: Roll
        robot.clickOn("🎲  Losuj");
        
        try { TimeUnit.SECONDS.sleep(3); } catch (Exception e) {}
        org.testfx.util.WaitForAsyncUtils.waitForFxEvents();
        
        // Verify P2 moved
        // P2 started at 0. Rolled 1+1=2.
        assertEquals(2, gameState.getPlayers().get(1).getPosition(), "Player 2 should be at 2");
        
        // Verify Turn passed back to P1 (since only 2 players)
        // Note: depends on total players. Test setup has 2.
        // Actually, land on 2 (Community Chest or Property).
        // Tile 2 is Community Chest usually or Property.
        // Using getBoardTiles order:
        // 0: Start, 1: Property, 2: Community Chest?
        // Let's check logic: if Property and unowned -> wait.
        // If Chest -> might wait or auto.
        // Let's check tile 2 type.
        Tile t2 = gameState.getBoard().getTile(2);
        if (t2 instanceof PropertyTile pt && !pt.isOwned()) {
             // Turn strictly won't pass until buy/auction.
             // Assert we are still P2.
             assertEquals("p2", gameState.getCurrentPlayer().getId(), "Turn should remain P2 pending decision");
        } else {
             assertEquals("p1", gameState.getCurrentPlayer().getId(), "Turn should pass back to Player 1");
        }
    }

    @Test
    @Order(5)
    void testFourPlayersTenTurns(FxRobot robot) {
        GameState gameState = gameView.getGameState();
        List<Player> players = gameState.getPlayers();
        assertEquals(4, players.size(), "Should have 4 players");
        
        // Mock Random to return distinct values to avoid doubles and vary movement
        gameState.setRandom(new Random() {
             private int counter = 0;
             private final int[] rolls = {0, 1, 0, 2, 1, 2, 0, 3}; // Pairs: (1,2)=3, (1,3)=4, (2,3)=5, (1,4)=5 ...
             
             @Override
             public int nextInt(int bound) { 
                 int val = rolls[counter % rolls.length];
                 counter++;
                 return val;
             }
        });
        
        int rounds = 10;
        int playersCount = 4;
        
        // Wait specifically for start
        org.testfx.util.WaitForAsyncUtils.waitForFxEvents();
        
        for (int round = 1; round <= rounds; round++) {
            System.out.println("DEBUG: Starting Round " + round);
            for (int pIndex = 0; pIndex < playersCount; pIndex++) {
                Player expectedPlayer = players.get(pIndex);
                
                System.out.println("DEBUG: Round " + round + " Turn " + (pIndex+1) + " (P" + (pIndex+1) + ") start.");
                
                // Ensure correct player turn
                int maxRetries = 15;
                while (!gameState.getCurrentPlayer().getId().equals(expectedPlayer.getId()) && maxRetries-- > 0) {
                     handlePotentialDialog(robot);
                     try { TimeUnit.MILLISECONDS.sleep(200); } catch (Exception e) {}
                }
                
                assertEquals(expectedPlayer.getId(), gameState.getCurrentPlayer().getId(), 
                    "Round " + round + " Turn " + (pIndex+1) + ": Should be " + expectedPlayer.getUsername() + "'s turn");
                
                // Roll
                Node rollBtn = robot.lookup(".button").queryAll().stream()
                    .filter(n -> n instanceof javafx.scene.control.ButtonBase && 
                            (((javafx.scene.control.ButtonBase)n).getText().contains("Losuj") || 
                             ((javafx.scene.control.ButtonBase)n).getText().contains("Rzuć")))
                    .findFirst().orElse(null);
                
                if (rollBtn != null && rollBtn.isVisible() && !rollBtn.isDisabled()) {
                    robot.clickOn(rollBtn);
                } else {
                    // Fallback try clicking by text
                    try { robot.clickOn("🎲  Losuj"); } catch(Exception e) {}
                }
                
                // Wait for move animation
                try { TimeUnit.MILLISECONDS.sleep(1500); } catch (Exception e) {}
                org.testfx.util.WaitForAsyncUtils.waitForFxEvents();
                
                int endPos = expectedPlayer.getPosition();
                System.out.println("DEBUG: P" + (pIndex+1) + " landed at " + endPos);

                // Handle landing actions (Buy/Auction)
                handlePotentialDialog(robot);
                
                // Wait for turn pass
                try { TimeUnit.MILLISECONDS.sleep(500); } catch (Exception e) {}
                org.testfx.util.WaitForAsyncUtils.waitForFxEvents();
            }
        }
    }
    
    private void handlePotentialDialog(FxRobot robot) {
        // Try to verify if dialog IS open
        boolean dialogOpen = robot.lookup(".dialog-pane").tryQuery().isPresent();
        if (!dialogOpen) return;

        System.out.println("DEBUG: Dialog detected. Handling...");
        
        // Priority: Kup -> close dialog.
        try {
            Node buyBtn = robot.lookup(".button").queryAll().stream()
                .filter(n -> n instanceof javafx.scene.control.ButtonBase && 
                         ((javafx.scene.control.ButtonBase)n).getText().equals("Kup"))
                .findFirst().orElse(null);
                
            if (buyBtn != null && buyBtn.isVisible()) {
                System.out.println("DEBUG: Clicking 'Kup'");
                robot.clickOn(buyBtn);
                return;
            }
            
            Node passBtn = robot.lookup(".button").queryAll().stream()
                .filter(n -> n instanceof javafx.scene.control.ButtonBase && 
                          ((javafx.scene.control.ButtonBase)n).getText().equals("Pas"))
                .findFirst().orElse(null);
                
            if (passBtn != null && passBtn.isVisible()) {
                System.out.println("DEBUG: Clicking 'Pas'");
                robot.clickOn(passBtn);
            }
            
            // Also check for 'Licytuj' if we fell through? Not needed if 'Kup' is prio.
            
        } catch (Exception e) {
            System.out.println("DEBUG: Dialog handling exception: " + e.getMessage());
        }
    }
}
