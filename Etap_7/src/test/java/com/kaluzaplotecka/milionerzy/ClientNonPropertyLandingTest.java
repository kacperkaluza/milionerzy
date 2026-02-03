package com.kaluzaplotecka.milionerzy;

import com.kaluzaplotecka.milionerzy.model.Board;
import com.kaluzaplotecka.milionerzy.model.GameState;
import com.kaluzaplotecka.milionerzy.model.Player;
import com.kaluzaplotecka.milionerzy.model.tiles.Tile;
import com.kaluzaplotecka.milionerzy.network.GameMessage;
import com.kaluzaplotecka.milionerzy.network.NetworkManager;
import com.kaluzaplotecka.milionerzy.view.GameView;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
public class ClientNonPropertyLandingTest {

    private NetworkManager hostNetwork;
    private NetworkManager clientNetwork;
    private GameView hostView;
    private GameView clientView;
    private Stage hostStage;
    private Stage clientStage;

    private static final String ROOM_CODE = "TESTRoom";

    @BeforeEach
    public void setup() throws Exception {
        hostNetwork = new NetworkManager("host-id");
        clientNetwork = new NetworkManager("client-id");
        hostNetwork.startHost(9999, ROOM_CODE);
        
        CountDownLatch connectLatch = new CountDownLatch(1);
        
        hostNetwork.setMessageHandler(msg -> {
            if (msg.getType() == GameMessage.MessageType.CONNECT) {
                 connectLatch.countDown();
            }
        });
        
        clientNetwork.connectToHost("localhost", 9999, "ClientPlayer", ROOM_CODE);
        assertTrue(connectLatch.await(5, TimeUnit.SECONDS), "Client failed to connect");
        
        // Setup Views/GameStates on JavaFX thread
        CountDownLatch uiLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                Player hostPlayer = new Player("host-id", "HostPlayer", 1500);
                Player clientPlayer = new Player(clientNetwork.getPlayerId(), "ClientPlayer", 1500);
                List<Player> players = new ArrayList<>();
                players.add(hostPlayer);
                players.add(clientPlayer);
                
                hostStage = new Stage();
                clientStage = new Stage();
                
                hostView = new GameView(hostStage, new ArrayList<>(players), hostNetwork, hostPlayer.getId());
                clientView = new GameView(clientStage, new ArrayList<>(players), clientNetwork, clientPlayer.getId());
                
                // Force sync initial state
                GameState gs = hostView.getGameState();
                // Ensure correct turn order (Host first?)
                // Actually start game triggers turn.
                // But for test we assume game started.
                
                uiLatch.countDown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        assertTrue(uiLatch.await(5, TimeUnit.SECONDS));
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (clientNetwork != null) clientNetwork.stop();
        if (hostNetwork != null) hostNetwork.stop();
        Platform.runLater(() -> {
            if (hostStage != null) hostStage.close();
            if (clientStage != null) clientStage.close();
        });
    }

    @Test
    public void testClientLandsOnJailEndsTurn() throws Exception {
        // Wait for sync to complete (client receiving GameState)
        Thread.sleep(1000); 

        // Ensure Client is current player
        // By default Host is first player (index 0).
        // Let's force NEXT_TURN to make Client current.
        Platform.runLater(() -> {
            hostView.getGameState().nextTurn();
        });
        
        Thread.sleep(500);
        // Verify Client is now current
        assertEquals(clientNetwork.getPlayerId(), clientView.getGameState().getCurrentPlayer().getId());
        
        System.out.println("Sending ROLL_DICE(10) from Client...");
        
        // Prepare to wait for turn change back to Host
        CountDownLatch turnLatch = new CountDownLatch(1);
        
        // Listen for NEXT_TURN message on client or check GameState changes
        // We can hook into client View's logic or just poll GameState
        // Let's poll GameState for 5 seconds
        
        clientNetwork.send(new GameMessage(GameMessage.MessageType.ROLL_DICE, clientNetwork.getPlayerId(), 10)); // 10 steps -> Jail
        
        long start = System.currentTimeMillis();
        boolean turnChanged = false;
        while (System.currentTimeMillis() - start < 5000) {
            String currentId = clientView.getGameState().getCurrentPlayer().getId();
            if ("host-id".equals(currentId)) {
                turnChanged = true;
                break;
            }
            Thread.sleep(100);
        }
        
        assertTrue(turnChanged, "Turn should change to Host after Client lands on Jail/Non-Property");
        
        // Verify Host state (Regression check for hasRolled bug)
        AtomicBoolean hostHasRolled = new AtomicBoolean(true);
        Platform.runLater(() -> {
            hostHasRolled.set(hostView.getGameState().hasRolled());
        });
        // Give time for UI/Platform runLater
        Thread.sleep(200);
        
        assertEquals(false, hostHasRolled.get(), "Host should NOT have rolled yet (fresh turn)");
    }

    @Test
    void testClientLandsOnChanceEndsTurn() throws Exception {
        // Wait for sync to complete (client receiving GameState)
        Thread.sleep(1000); 

        // Ensure Client is current player
        Platform.runLater(() -> {
            hostView.getGameState().nextTurn();
        });
        
        Thread.sleep(500);
        assertEquals(clientNetwork.getPlayerId(), clientView.getGameState().getCurrentPlayer().getId());
        
        System.out.println("Sending ROLL_DICE(2) from Client (Chance)...");
        
        clientNetwork.send(new GameMessage(GameMessage.MessageType.ROLL_DICE, clientNetwork.getPlayerId(), 2)); // 2 steps -> Chance
        
        long start = System.currentTimeMillis();
        boolean turnChanged = false;
        while (System.currentTimeMillis() - start < 5000) {
            String currentId = clientView.getGameState().getCurrentPlayer().getId();
            if ("host-id".equals(currentId)) {
                turnChanged = true;
                break;
            }
            Thread.sleep(100);
        }
        
        assertTrue(turnChanged, "Turn should change to Host after Client lands on Chance");
    }
}
