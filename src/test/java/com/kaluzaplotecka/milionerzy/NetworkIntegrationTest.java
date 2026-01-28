package com.kaluzaplotecka.milionerzy;

import org.junit.jupiter.api.*;

import com.kaluzaplotecka.milionerzy.model.Board;
import com.kaluzaplotecka.milionerzy.model.GameState;
import com.kaluzaplotecka.milionerzy.model.Player;
import com.kaluzaplotecka.milionerzy.model.tiles.PropertyTile;
import com.kaluzaplotecka.milionerzy.model.tiles.Tile;
import com.kaluzaplotecka.milionerzy.network.GameMessage;
import com.kaluzaplotecka.milionerzy.network.NetworkGameEventListener;
import com.kaluzaplotecka.milionerzy.network.NetworkManager;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Testy integracyjne end-to-end symulujące pełny przebieg gry sieciowej.
 * Eliminują potrzebę ręcznego testowania z dwoma klientami.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NetworkIntegrationTest {

    private static final int TEST_PORT = 16666;
    
    private NetworkManager hostManager;
    private NetworkManager client1Manager;
    private NetworkManager client2Manager;
    
    @BeforeEach
    void setUp() {
        hostManager = new NetworkManager("host");
        client1Manager = new NetworkManager("player1");
        client2Manager = new NetworkManager("player2");
    }
    
    @AfterEach
    void tearDown() throws InterruptedException {
        if (client2Manager != null) client2Manager.stop();
        if (client1Manager != null) client1Manager.stop();
        if (hostManager != null) hostManager.stop();
        Thread.sleep(100);
    }
    
    /**
     * Tworzy planszę testową z nieruchomościami.
     */
    private Board createTestBoard() {
        List<Tile> tiles = new ArrayList<>();
        tiles.add(new Tile(0, "START"));
        tiles.add(new PropertyTile(1, "Kielce", 100, 20));
        tiles.add(new PropertyTile(2, "Sandomierz", 120, 25));
        tiles.add(new PropertyTile(3, "Starachowice", 140, 30));
        tiles.add(new Tile(4, "Szansa"));
        tiles.add(new PropertyTile(5, "Ostrowiec", 160, 35));
        tiles.add(new PropertyTile(6, "Busko", 180, 40));
        tiles.add(new Tile(7, "Więzienie"));
        tiles.add(new PropertyTile(8, "Końskie", 200, 50));
        tiles.add(new PropertyTile(9, "Pińczów", 220, 55));
        return new Board(tiles);
    }
    
    @Test
    @Order(1)
    @DisplayName("Symulacja wielu tur gry z synchronizacją stanu")
    void testFullGameScenario_MultipleTurns() throws IOException, InterruptedException {
        AtomicInteger syncCount = new AtomicInteger(0);
        AtomicReference<GameState> lastState = new AtomicReference<>();
        CountDownLatch syncLatch = new CountDownLatch(3);
        
        client1Manager.setMessageHandler(msg -> {
            if (msg.getType() == GameMessage.MessageType.GAME_STATE_SYNC) {
                if (msg.getPayload() instanceof GameState gs) {
                    syncCount.incrementAndGet();
                    lastState.set(gs);
                    syncLatch.countDown();
                }
            }
        });
        
        hostManager.startHost(TEST_PORT);
        Thread.sleep(100);
        client1Manager.connectToHost("localhost", TEST_PORT, "player1");
        Thread.sleep(200);
        
        List<Player> players = new ArrayList<>();
        players.add(new Player("host", "Host", 1500));
        players.add(new Player("player1", "Player1", 1500));
        
        GameState gameState = new GameState(createTestBoard(), players);
        gameState.setRandom(new Random(42));
        
        NetworkGameEventListener eventListener = new NetworkGameEventListener(hostManager, () -> gameState);
        gameState.addEventListener(eventListener);
        
        // Symuluj 3 tury
        for (int i = 0; i < 3; i++) {
            gameState.nextTurn();
            Thread.sleep(150); // Więcej czasu na async
        }
        
        boolean allReceived = syncLatch.await(3, TimeUnit.SECONDS);
        assertTrue(allReceived, "Klient powinien otrzymać 3 synchronizacje stanu");
        assertEquals(3, syncCount.get(), "Powinny być 3 synchronizacje");
        
        // Weryfikuj stan końcowy
        GameState finalState = lastState.get();
        assertNotNull(finalState);
        assertEquals(2, finalState.getPlayers().size());
    }
    
    @Test
    @Order(2)
    @DisplayName("Kupno nieruchomości i płacenie czynszu z synchronizacją")
    void testPropertyPurchaseAndRent_SyncToClient() throws IOException, InterruptedException {
        AtomicReference<GameState> lastState = new AtomicReference<>();
        CountDownLatch finalSyncLatch = new CountDownLatch(1);
        AtomicInteger syncCount = new AtomicInteger(0);
        
        client1Manager.setMessageHandler(msg -> {
            if (msg.getType() == GameMessage.MessageType.GAME_STATE_SYNC) {
                if (msg.getPayload() instanceof GameState gs) {
                    int count = syncCount.incrementAndGet();
                    lastState.set(gs);
                    // Czekamy na 2. sync (po chargeRent)
                    if (count >= 2) {
                        finalSyncLatch.countDown();
                    }
                }
            }
        });
        
        hostManager.startHost(TEST_PORT);
        Thread.sleep(100);
        client1Manager.connectToHost("localhost", TEST_PORT, "player1");
        Thread.sleep(200);
        
        List<Player> players = new ArrayList<>();
        players.add(new Player("host", "Host", 1500));
        players.add(new Player("player1", "Player1", 1500));
        
        GameState gameState = new GameState(createTestBoard(), players);
        gameState.setRandom(new Random(42));
        
        Player host = gameState.getPlayers().get(0);
        Player player1 = gameState.getPlayers().get(1);
        
        NetworkGameEventListener eventListener = new NetworkGameEventListener(hostManager, () -> gameState);
        gameState.addEventListener(eventListener);
        
        // Host kupuje nieruchomość
        PropertyTile prop = (PropertyTile) gameState.getBoard().getTile(1);
        host.moveTo(1);
        prop.buy(host);
        
        // Zmień turę - sync #1
        gameState.nextTurn();
        Thread.sleep(200);
        
        // player1 płaci czynsz
        player1.moveTo(1);
        prop.chargeRent(gameState, player1);
        
        // Zmień turę - sync #2
        gameState.nextTurn();
        Thread.sleep(200);
        
        boolean synced = finalSyncLatch.await(3, TimeUnit.SECONDS);
        assertTrue(synced, "Klient powinien otrzymać 2 synchronizacje");
        
        // Lokalnie gracz powinien mieć mniej pieniędzy
        assertEquals(1480, player1.getMoney(), "Lokalnie player1 powinien mieć 1480 zł");
        
        // Weryfikacja zsynchronizowanego stanu
        GameState finalState = lastState.get();
        assertNotNull(finalState);
        
        Player syncedPlayer1 = finalState.getPlayers().stream()
            .filter(p -> p.getId().equals("player1"))
            .findFirst().orElse(null);
        
        assertNotNull(syncedPlayer1);
        assertEquals(1480, syncedPlayer1.getMoney(), 
            "Zsynchronizowany player1 powinien mieć 1480 zł po zapłaceniu czynszu 20 zł");
    }
    
    @Test
    @Order(3)
    @DisplayName("Scenariusz bankructwa z synchronizacją")
    void testBankruptcyScenario_SyncToAllClients() throws IOException, InterruptedException {
        AtomicReference<GameState> lastState = new AtomicReference<>();
        CountDownLatch finalSyncLatch = new CountDownLatch(1);
        AtomicInteger syncCount = new AtomicInteger(0);
        
        client1Manager.setMessageHandler(msg -> {
            if (msg.getType() == GameMessage.MessageType.GAME_STATE_SYNC) {
                if (msg.getPayload() instanceof GameState gs) {
                    int count = syncCount.incrementAndGet();
                    lastState.set(gs);
                    if (count >= 2) {
                        finalSyncLatch.countDown();
                    }
                }
            }
        });
        
        hostManager.startHost(TEST_PORT);
        Thread.sleep(100);
        client1Manager.connectToHost("localhost", TEST_PORT, "player1");
        Thread.sleep(200);
        
        List<Player> players = new ArrayList<>();
        players.add(new Player("host", "Rich", 1500));
        players.add(new Player("player1", "Poor", 50));
        
        GameState gameState = new GameState(createTestBoard(), players);
        gameState.setRandom(new Random(42));
        
        Player rich = gameState.getPlayers().get(0);
        Player poor = gameState.getPlayers().get(1);
        
        NetworkGameEventListener eventListener = new NetworkGameEventListener(hostManager, () -> gameState);
        gameState.addEventListener(eventListener);
        
        // Rich "posiada" nieruchomość
        PropertyTile prop = (PropertyTile) gameState.getBoard().getTile(1);
        prop.setOwner(rich);
        rich.addProperty(prop);
        
        // Sync #1
        gameState.nextTurn();
        Thread.sleep(200);
        
        // Poor bankrutuje
        poor.moveTo(1);
        poor.deductMoney(100); // 50 - 100 = -50
        
        assertTrue(poor.isBankrupt(), "Poor powinien być bankrutem");
        gameState.handleBankruptcy(poor);
        
        assertEquals(1, gameState.getPlayers().size(), "Lokalnie powinien zostać 1 gracz");
        
        // Sync #2 - ze zaktualizowanym stanem
        gameState.nextTurn();
        Thread.sleep(200);
        
        boolean synced = finalSyncLatch.await(3, TimeUnit.SECONDS);
        assertTrue(synced, "Klient powinien otrzymać synchronizacje");
        
        GameState finalState = lastState.get();
        assertNotNull(finalState);
        assertEquals(1, finalState.getPlayers().size(), 
            "Zsynchronizowany stan powinien mieć 1 gracza po bankructwie");
        assertTrue(finalState.isGameOver(), "Gra powinna się zakończyć");
    }
    
    @Test
    @Order(4)
    @DisplayName("Spójność stanu po wielu akcjach z dwoma klientami")
    void testStateConsistency_AfterMultipleActions() throws IOException, InterruptedException {
        AtomicReference<GameState> client1State = new AtomicReference<>();
        AtomicReference<GameState> client2State = new AtomicReference<>();
        CountDownLatch syncLatch = new CountDownLatch(2);
        
        client1Manager.setMessageHandler(msg -> {
            if (msg.getType() == GameMessage.MessageType.GAME_STATE_SYNC) {
                if (msg.getPayload() instanceof GameState gs) {
                    client1State.set(gs);
                    syncLatch.countDown();
                }
            }
        });
        
        client2Manager.setMessageHandler(msg -> {
            if (msg.getType() == GameMessage.MessageType.GAME_STATE_SYNC) {
                if (msg.getPayload() instanceof GameState gs) {
                    client2State.set(gs);
                    syncLatch.countDown();
                }
            }
        });
        
        hostManager.startHost(TEST_PORT);
        Thread.sleep(100);
        client1Manager.connectToHost("localhost", TEST_PORT, "player1");
        client2Manager.connectToHost("localhost", TEST_PORT, "player2");
        Thread.sleep(200);
        
        Player host = new Player("host", "Host", 1500);
        Player p1 = new Player("player1", "Player1", 1500);
        Player p2 = new Player("player2", "Player2", 1500);
        
        GameState gameState = new GameState(createTestBoard(), List.of(host, p1, p2));
        gameState.setRandom(new Random(123));
        
        NetworkGameEventListener eventListener = new NetworkGameEventListener(hostManager, () -> gameState);
        gameState.addEventListener(eventListener);
        
        // Zmień turę - broadcast
        gameState.nextTurn();
        Thread.sleep(200);
        
        boolean synced = syncLatch.await(3, TimeUnit.SECONDS);
        assertTrue(synced, "Obaj klienci powinni otrzymać synchronizację");
        
        assertNotNull(client1State.get());
        assertNotNull(client2State.get());
        
        assertEquals(client1State.get().getPlayers().size(), client2State.get().getPlayers().size());
        assertEquals(client1State.get().getRoundNumber(), client2State.get().getRoundNumber());
    }
    
    @Test
    @Order(5)
    @DisplayName("Pełna symulacja tury z rzutem kostką i ruchem")
    void testFullTurnWithDiceRoll() throws IOException, InterruptedException {
        AtomicReference<GameState> lastState = new AtomicReference<>();
        CountDownLatch syncLatch = new CountDownLatch(1);
        
        client1Manager.setMessageHandler(msg -> {
            if (msg.getType() == GameMessage.MessageType.GAME_STATE_SYNC) {
                if (msg.getPayload() instanceof GameState gs) {
                    lastState.set(gs);
                    syncLatch.countDown();
                }
            }
        });
        
        hostManager.startHost(TEST_PORT);
        Thread.sleep(100);
        client1Manager.connectToHost("localhost", TEST_PORT, "player1");
        Thread.sleep(200);
        
        Player host = new Player("host", "Host", 1500);
        Player p1 = new Player("player1", "Player1", 1500);
        
        GameState gameState = new GameState(createTestBoard(), List.of(host, p1));
        gameState.setRandom(new Random(42)); // Deterministyczny seed
        
        // Pobierz hosta z listy graczy (referent jest ten sam)
        Player gameHost = gameState.getPlayers().get(0);
        
        NetworkGameEventListener eventListener = new NetworkGameEventListener(hostManager, () -> gameState);
        gameState.addEventListener(eventListener);
        
        int startPos = gameHost.getPosition();
        assertEquals(0, startPos);
        
        int diceResult = gameState.rollDice();
        gameState.movePlayerBy(gameHost, diceResult);
        
        int expectedPos = diceResult % 10;
        assertEquals(expectedPos, gameHost.getPosition(), "Lokalnie pozycja powinna być zaktualizowana");
        
        gameState.nextTurn();
        Thread.sleep(200);
        
        boolean synced = syncLatch.await(3, TimeUnit.SECONDS);
        assertTrue(synced, "Klient powinien otrzymać synchronizację");
        
        GameState syncedState = lastState.get();
        assertNotNull(syncedState);
        
        Player syncedHost = syncedState.getPlayers().stream()
            .filter(p -> p.getId().equals("host")).findFirst().orElse(null);
        assertNotNull(syncedHost);
        
        assertEquals(expectedPos, syncedHost.getPosition(), 
            "Zsynchronizowana pozycja hosta powinna być zgodna z lokalną");
    }
}
