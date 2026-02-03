package com.kaluzaplotecka.milionerzy;

import org.junit.jupiter.api.*;

import com.kaluzaplotecka.milionerzy.events.GameEvent;
import com.kaluzaplotecka.milionerzy.model.Board;
import com.kaluzaplotecka.milionerzy.model.GameState;
import com.kaluzaplotecka.milionerzy.model.Player;
import com.kaluzaplotecka.milionerzy.model.tiles.Tile;
import com.kaluzaplotecka.milionerzy.network.GameMessage;
import com.kaluzaplotecka.milionerzy.network.NetworkGameEventListener;
import com.kaluzaplotecka.milionerzy.network.NetworkManager;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Testy integracyjne dla NetworkGameEventListener.
 * Sprawdza czy zdarzenia gry są poprawnie broadcastowane do klientów.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NetworkGameEventListenerTest {

    private static final int TEST_PORT = 16666;
    
    private NetworkManager hostManager;
    private NetworkManager clientManager;
    private GameState gameState;
    private List<Player> players;

    @BeforeEach
    void setUp() {
        hostManager = new NetworkManager("host-id");
        clientManager = new NetworkManager("client-id");
        
        players = new ArrayList<>();
        players.add(new Player("host-id", "Host", 1500));
        players.add(new Player("client-id", "Client", 1500));
        
        Board board = new Board(List.of(
            new Tile(0, "START"),
            new Tile(1, "Test Property 1"),
            new Tile(2, "Test Property 2")
        ));
        gameState = new GameState(board, players);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (clientManager != null) clientManager.stop();
        if (hostManager != null) hostManager.stop();
        Thread.sleep(100);
    }

    @Test
    @Order(1)
    @DisplayName("NetworkGameEventListener wysyła DICE_RESULT do klientów przy rzucie kostką")
    void testDiceResultBroadcastedToClients() throws IOException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<GameMessage> receivedMsg = new AtomicReference<>();
        
        clientManager.setMessageHandler(msg -> {
            if (msg.getType() == GameMessage.MessageType.DICE_RESULT) {
                receivedMsg.set(msg);
                latch.countDown();
            }
        });
        
        hostManager.startHost(TEST_PORT, "TEST");
        Thread.sleep(100);
        clientManager.connectToHost("localhost", TEST_PORT, "Client", "TEST");
        Thread.sleep(200);
        
        NetworkGameEventListener listener = new NetworkGameEventListener(hostManager, () -> gameState);
        gameState.addEventListener(listener);
        
        // GameEvent z poprawnymi argumentami: (Type, Player, Object data, String message)
        gameState.fireEvent(new GameEvent(GameEvent.Type.DICE_ROLLED, players.get(0), 7, "Rzut kostką: 7"));
        
        boolean received = latch.await(2, TimeUnit.SECONDS);
        assertTrue(received, "Klient powinien otrzymać DICE_RESULT");
        
        assertNotNull(receivedMsg.get());
        assertEquals(GameMessage.MessageType.DICE_RESULT, receivedMsg.get().getType());
        assertEquals(7, receivedMsg.get().getPayload());
    }

    @Test
    @Order(2)
    @DisplayName("NetworkGameEventListener wysyła GAME_STATE_SYNC przy zmianie tury")
    void testGameStateSyncOnTurnChange() throws IOException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<GameMessage> receivedMsg = new AtomicReference<>();
        
        clientManager.setMessageHandler(msg -> {
            if (msg.getType() == GameMessage.MessageType.GAME_STATE_SYNC) {
                receivedMsg.set(msg);
                latch.countDown();
            }
        });
        
        hostManager.startHost(TEST_PORT, "TEST");
        Thread.sleep(100);
        clientManager.connectToHost("localhost", TEST_PORT, "Client", "TEST");
        Thread.sleep(200);
        
        NetworkGameEventListener listener = new NetworkGameEventListener(hostManager, () -> gameState);
        gameState.addEventListener(listener);
        
        gameState.nextTurn();
        
        boolean received = latch.await(2, TimeUnit.SECONDS);
        assertTrue(received, "Klient powinien otrzymać GAME_STATE_SYNC przy zmianie tury");
        
        assertNotNull(receivedMsg.get());
        assertEquals(GameMessage.MessageType.GAME_STATE_SYNC, receivedMsg.get().getType());
        assertTrue(receivedMsg.get().getPayload() instanceof GameState);
    }

    @Test
    @Order(3)
    @DisplayName("NetworkGameEventListener wysyła MOVE przy ruchu gracza")
    void testPlayerMoveBroadcasted() throws IOException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<GameMessage> receivedMsg = new AtomicReference<>();
        
        clientManager.setMessageHandler(msg -> {
            if (msg.getType() == GameMessage.MessageType.MOVE) {
                receivedMsg.set(msg);
                latch.countDown();
            }
        });
        
        hostManager.startHost(TEST_PORT, "TEST");
        Thread.sleep(100);
        clientManager.connectToHost("localhost", TEST_PORT, "Client", "TEST");
        Thread.sleep(200);
        
        NetworkGameEventListener listener = new NetworkGameEventListener(hostManager, () -> gameState);
        gameState.addEventListener(listener);
        
        Player player = players.get(0);
        player.setPosition(7);
        gameState.fireEvent(new GameEvent(GameEvent.Type.PLAYER_MOVED, player, 7, "Gracz przesunął się na pole 7"));
        
        boolean received = latch.await(2, TimeUnit.SECONDS);
        assertTrue(received, "Klient powinien otrzymać MOVE");
        
        assertNotNull(receivedMsg.get());
        assertEquals(GameMessage.MessageType.MOVE, receivedMsg.get().getType());
        assertEquals(7, receivedMsg.get().getPayload());
    }

    @Test
    @Order(4)
    @DisplayName("NetworkGameEventListener nie wysyła wiadomości gdy host nie jest aktywny")
    void testNoMessagesWhenOfflineMode() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        
        NetworkGameEventListener listener = new NetworkGameEventListener(hostManager, () -> gameState);
        gameState.addEventListener(listener);
        
        hostManager.setMessageHandler(msg -> {
            latch.countDown();
        });
        
        gameState.fireEvent(new GameEvent(GameEvent.Type.DICE_ROLLED, players.get(0), 5, "Test"));
        
        boolean received = latch.await(500, TimeUnit.MILLISECONDS);
        assertFalse(received, "Żadne wiadomości nie powinny być wysłane gdy NetworkManager jest offline");
    }

    @Test
    @Order(5)
    @DisplayName("Zdarzenia MONEY_CHANGED broadcastują MONEY_UPDATE")
    void testMoneyUpdateBroadcasted() throws IOException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<GameMessage> receivedMsg = new AtomicReference<>();
        
        clientManager.setMessageHandler(msg -> {
            if (msg.getType() == GameMessage.MessageType.MONEY_UPDATE) {
                receivedMsg.set(msg);
                latch.countDown();
            }
        });
        
        hostManager.startHost(TEST_PORT, "TEST");
        Thread.sleep(100);
        clientManager.connectToHost("localhost", TEST_PORT, "Client", "TEST");
        Thread.sleep(200);
        
        NetworkGameEventListener listener = new NetworkGameEventListener(hostManager, () -> gameState);
        gameState.addEventListener(listener);
        
        Player player = players.get(0);
        player.addMoney(200); // Użyj istniejącej metody
        gameState.fireEvent(new GameEvent(GameEvent.Type.MONEY_CHANGED, player, 1700, "Zmiana pieniędzy"));
        
        boolean received = latch.await(2, TimeUnit.SECONDS);
        assertTrue(received, "Klient powinien otrzymać MONEY_UPDATE");
        
        assertNotNull(receivedMsg.get());
        assertEquals(GameMessage.MessageType.MONEY_UPDATE, receivedMsg.get().getType());
    }
}
