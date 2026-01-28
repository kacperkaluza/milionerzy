package com.kaluzaplotecka.milionerzy;

import org.junit.jupiter.api.*;

import com.kaluzaplotecka.milionerzy.model.Board;
import com.kaluzaplotecka.milionerzy.model.GameState;
import com.kaluzaplotecka.milionerzy.model.Player;
import com.kaluzaplotecka.milionerzy.model.tiles.Tile;
import com.kaluzaplotecka.milionerzy.network.GameMessage;
import com.kaluzaplotecka.milionerzy.network.NetworkManager;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Testy integracyjne symulujące połączenie sieciowe i przebieg gry.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NetworkGameTest {

    private static final int TEST_PORT = 15555; // inny port niż domyślny
    
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
        // Daj czas na zamknięcie socketów
        Thread.sleep(100);
    }
    
    @Test
    @Order(1)
    @DisplayName("Host uruchamia serwer i klienci się łączą")
    void testHostAndClientsConnect() throws IOException, InterruptedException {
        // Latch do oczekiwania na połączenia
        CountDownLatch connectionsLatch = new CountDownLatch(2);
        List<String> connectionMessages = new ArrayList<>();
        
        hostManager.setConnectionHandler(msg -> {
            connectionMessages.add(msg);
            connectionsLatch.countDown();
        });
        
        // Host startuje
        hostManager.startHost(TEST_PORT);
        assertEquals(NetworkManager.Mode.HOST, hostManager.getMode());
        assertTrue(hostManager.isRunning());
        
        // Klient 1 łączy się
        client1Manager.connectToHost("localhost", TEST_PORT, "player1");
        assertEquals(NetworkManager.Mode.CLIENT, client1Manager.getMode());
        
        // Klient 2 łączy się
        client2Manager.connectToHost("localhost", TEST_PORT, "player2");
        assertEquals(NetworkManager.Mode.CLIENT, client2Manager.getMode());
        
        // Czekaj na połączenia
        boolean connected = connectionsLatch.await(3, TimeUnit.SECONDS);
        assertTrue(connected, "Klienci powinni się połączyć w czasie 3 sekund");
        
        // Sprawdź liczbę połączonych klientów
        assertEquals(2, hostManager.getConnectedClientsCount());
        assertEquals(2, connectionMessages.size());
    }
    
    @Test
    @Order(2)
    @DisplayName("Host wysyła wiadomość do wszystkich klientów (broadcast)")
    void testBroadcastMessage() throws IOException, InterruptedException {
        CountDownLatch messageLatch = new CountDownLatch(2);
        AtomicReference<GameMessage> receivedByClient1 = new AtomicReference<>();
        AtomicReference<GameMessage> receivedByClient2 = new AtomicReference<>();
        
        // Ustaw handlery wiadomości
        client1Manager.setMessageHandler(msg -> {
            if (msg.getType() == GameMessage.MessageType.CONNECT) return;
            receivedByClient1.set(msg);
            messageLatch.countDown();
        });
        
        client2Manager.setMessageHandler(msg -> {
            if (msg.getType() == GameMessage.MessageType.CONNECT) return;
            receivedByClient2.set(msg);
            messageLatch.countDown();
        });
        
        // Start i połączenie
        hostManager.startHost(TEST_PORT);
        Thread.sleep(100);
        client1Manager.connectToHost("localhost", TEST_PORT, "player1");
        client2Manager.connectToHost("localhost", TEST_PORT, "player2");
        Thread.sleep(200);
        
        // Host wysyła broadcast
        GameMessage startGame = new GameMessage(
            GameMessage.MessageType.START_GAME, 
            "host", 
            "Gra rozpoczęta!"
        );
        hostManager.send(startGame);
        
        // Czekaj na odbiór
        boolean received = messageLatch.await(2, TimeUnit.SECONDS);
        assertTrue(received, "Obaj klienci powinni otrzymać wiadomość");
        
        assertNotNull(receivedByClient1.get());
        assertNotNull(receivedByClient2.get());
        assertEquals(GameMessage.MessageType.START_GAME, receivedByClient1.get().getType());
        assertEquals("Gra rozpoczęta!", receivedByClient1.get().getPayload());
    }
    
    @Test
    @Order(3)
    @DisplayName("Klient wysyła wiadomość do hosta")
    void testClientToHostMessage() throws IOException, InterruptedException {
        CountDownLatch messageLatch = new CountDownLatch(1);
        AtomicReference<GameMessage> receivedByHost = new AtomicReference<>();
        
        hostManager.setMessageHandler(msg -> {
            if (msg.getType() == GameMessage.MessageType.ROLL_DICE) {
                receivedByHost.set(msg);
                messageLatch.countDown();
            }
        });
        
        // Start i połączenie
        hostManager.startHost(TEST_PORT);
        Thread.sleep(100);
        client1Manager.connectToHost("localhost", TEST_PORT, "player1");
        Thread.sleep(200);
        
        // Klient wysyła rzut kostką
        GameMessage rollDice = new GameMessage(
            GameMessage.MessageType.ROLL_DICE, 
            "player1",
            new int[]{4, 5} // wynik rzutu: 4 i 5
        );
        client1Manager.send(rollDice);
        
        // Czekaj na odbiór
        boolean received = messageLatch.await(2, TimeUnit.SECONDS);
        assertTrue(received, "Host powinien otrzymać wiadomość");
        
        assertNotNull(receivedByHost.get());
        assertEquals("player1", receivedByHost.get().getSenderId());
        assertArrayEquals(new int[]{4, 5}, (int[]) receivedByHost.get().getPayload());
    }
    
    @Test
    @Order(4)
    @DisplayName("Symulacja pełnej tury gry")
    void testFullGameTurn() throws IOException, InterruptedException {
        List<GameMessage> hostReceivedMessages = new ArrayList<>();
        List<GameMessage> client1ReceivedMessages = new ArrayList<>();
        CountDownLatch gameLatch = new CountDownLatch(3); // 3 kroki w turze
        
        hostManager.setMessageHandler(msg -> {
            // Ignoruj wiadomości CONNECT
            if (msg.getType() == GameMessage.MessageType.CONNECT) return;
            synchronized (hostReceivedMessages) {
                hostReceivedMessages.add(msg);
            }
            gameLatch.countDown();
        });
        
        client1Manager.setMessageHandler(msg -> {
            synchronized (client1ReceivedMessages) {
                client1ReceivedMessages.add(msg);
            }
        });
        
        // Start i połączenie
        hostManager.startHost(TEST_PORT);
        Thread.sleep(100);
        client1Manager.connectToHost("localhost", TEST_PORT, "player1");
        Thread.sleep(200);
        
        // === SYMULACJA TURY ===
        
        // Krok 1: Gracz rzuca kostką
        client1Manager.send(new GameMessage(
            GameMessage.MessageType.ROLL_DICE,
            "player1",
            new int[]{3, 4} // wynik: 7
        ));
        Thread.sleep(100);
        
        // Krok 2: Gracz kupuje nieruchomość
        client1Manager.send(new GameMessage(
            GameMessage.MessageType.BUY_PROPERTY,
            "player1",
            "Kielce Centrum" // nazwa nieruchomości
        ));
        Thread.sleep(100);
        
        // Krok 3: Gracz kończy turę
        client1Manager.send(new GameMessage(
            GameMessage.MessageType.END_TURN,
            "player1"
        ));
        
        // Czekaj na wszystkie wiadomości
        boolean allReceived = gameLatch.await(3, TimeUnit.SECONDS);
        assertTrue(allReceived, "Host powinien otrzymać wszystkie wiadomości tury");
        
        // Weryfikuj kolejność i typy wiadomości
        assertEquals(3, hostReceivedMessages.size());
        assertEquals(GameMessage.MessageType.ROLL_DICE, hostReceivedMessages.get(0).getType());
        assertEquals(GameMessage.MessageType.BUY_PROPERTY, hostReceivedMessages.get(1).getType());
        assertEquals(GameMessage.MessageType.END_TURN, hostReceivedMessages.get(2).getType());
    }
    
    @Test
    @Order(5)
    @DisplayName("Symulacja oferty handlowej między graczami")
    void testTradeOffer() throws IOException, InterruptedException {
        CountDownLatch tradeLatch = new CountDownLatch(2);
        AtomicReference<GameMessage> tradeOfferReceived = new AtomicReference<>();
        AtomicReference<GameMessage> tradeResponseReceived = new AtomicReference<>();
        
        hostManager.setMessageHandler(msg -> {
            if (msg.getType() == GameMessage.MessageType.TRADE_OFFER) {
                tradeOfferReceived.set(msg);
                tradeLatch.countDown();
                
                // Host przekazuje ofertę do drugiego gracza (symulacja)
                hostManager.send(msg);
            } else if (msg.getType() == GameMessage.MessageType.TRADE_RESPONSE) {
                tradeResponseReceived.set(msg);
                tradeLatch.countDown();
            }
        });
        
        client2Manager.setMessageHandler(msg -> {
            if (msg.getType() == GameMessage.MessageType.TRADE_OFFER) {
                // Gracz 2 akceptuje ofertę
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {}
                
                client2Manager.send(new GameMessage(
                    GameMessage.MessageType.TRADE_RESPONSE,
                    "player2",
                    "player1",
                    true // akceptacja
                ));
            }
        });
        
        // Start i połączenie
        hostManager.startHost(TEST_PORT);
        Thread.sleep(100);
        client1Manager.connectToHost("localhost", TEST_PORT, "player1");
        client2Manager.connectToHost("localhost", TEST_PORT, "player2");
        Thread.sleep(200);
        
        // Gracz 1 wysyła ofertę handlową (używamy prostej mapy jako payload)
        java.util.Map<String, Object> tradeData = new java.util.HashMap<>();
        tradeData.put("from", "player1");
        tradeData.put("to", "player2");
        tradeData.put("offeredMoney", 200);
        tradeData.put("requestedProperty", "Kielce Centrum");
        
        client1Manager.send(new GameMessage(
            GameMessage.MessageType.TRADE_OFFER,
            "player1",
            "player2",
            tradeData
        ));
        
        // Czekaj na wymianę
        boolean tradeCompleted = tradeLatch.await(3, TimeUnit.SECONDS);
        assertTrue(tradeCompleted, "Wymiana handlowa powinna się zakończyć");
        
        assertNotNull(tradeOfferReceived.get());
        assertNotNull(tradeResponseReceived.get());
        assertEquals(true, tradeResponseReceived.get().getPayload());
    }
    
    @Test
    @Order(6)
    @DisplayName("Symulacja aukcji nieruchomości")
    void testPropertyAuction() throws IOException, InterruptedException {
        List<GameMessage> auctionMessages = new ArrayList<>();
        CountDownLatch auctionLatch = new CountDownLatch(4); // start + 2 licytacje + pass
        
        hostManager.setMessageHandler(msg -> {
            if (msg.getType().name().startsWith("AUCTION")) {
                synchronized (auctionMessages) {
                    auctionMessages.add(msg);
                }
                auctionLatch.countDown();
                
                // Broadcast aukcji do wszystkich
                hostManager.send(msg);
            }
        });
        
        // Start i połączenie
        hostManager.startHost(TEST_PORT);
        Thread.sleep(100);
        client1Manager.connectToHost("localhost", TEST_PORT, "player1");
        client2Manager.connectToHost("localhost", TEST_PORT, "player2");
        Thread.sleep(200);
        
        // Host rozpoczyna aukcję
        hostManager.send(new GameMessage(
            GameMessage.MessageType.AUCTION_START,
            "host",
            "Sandomierz" // nieruchomość na aukcji
        ));
        auctionLatch.countDown();
        Thread.sleep(100);
        
        // Gracz 1 licytuje 120
        client1Manager.send(new GameMessage(
            GameMessage.MessageType.AUCTION_BID,
            "player1",
            120
        ));
        Thread.sleep(100);
        
        // Gracz 2 licytuje 150
        client2Manager.send(new GameMessage(
            GameMessage.MessageType.AUCTION_BID,
            "player2",
            150
        ));
        Thread.sleep(100);
        
        // Gracz 1 pasuje
        client1Manager.send(new GameMessage(
            GameMessage.MessageType.AUCTION_PASS,
            "player1"
        ));
        
        // Czekaj na aukcję
        boolean auctionCompleted = auctionLatch.await(3, TimeUnit.SECONDS);
        assertTrue(auctionCompleted, "Aukcja powinna się zakończyć");
        
        // Sprawdź wiadomości aukcji
        assertTrue(auctionMessages.size() >= 3);
    }
    
    @Test
    @Order(7)
    @DisplayName("Synchronizacja stanu gry")
    void testGameStateSync() throws IOException, InterruptedException {
        CountDownLatch syncLatch = new CountDownLatch(1);
        AtomicReference<GameMessage> syncMessage = new AtomicReference<>();
        
        client1Manager.setMessageHandler(msg -> {
            if (msg.getType() == GameMessage.MessageType.GAME_STATE_SYNC) {
                syncMessage.set(msg);
                syncLatch.countDown();
            }
        });
        
        // Start i połączenie
        hostManager.startHost(TEST_PORT);
        Thread.sleep(100);
        client1Manager.connectToHost("localhost", TEST_PORT, "player1");
        Thread.sleep(200);
        
        // Stwórz przykładowy stan gry
        List<Player> players = List.of(
            new Player("host", "host", 1500),
            new Player("player1", "player1", 1500)
        );
        Board board = new Board(List.of(
            new Tile(0, "START"),
            new Tile(1, "Kielce Centrum")
        ));
        GameState gameState = new GameState(board, players);
        
        // Host wysyła synchronizację stanu
        hostManager.send(new GameMessage(
            GameMessage.MessageType.GAME_STATE_SYNC,
            "host",
            gameState
        ));
        
        // Czekaj na synchronizację
        boolean synced = syncLatch.await(2, TimeUnit.SECONDS);
        assertTrue(synced, "Klient powinien otrzymać synchronizację stanu");
        
        assertNotNull(syncMessage.get());
        assertEquals(GameMessage.MessageType.GAME_STATE_SYNC, syncMessage.get().getType());
    }
    
    @Test
    @Order(8)
    @DisplayName("Obsługa rozłączenia gracza")
    void testPlayerDisconnect() throws IOException, InterruptedException {
        CountDownLatch disconnectLatch = new CountDownLatch(1);
        AtomicReference<String> disconnectMessage = new AtomicReference<>();
        
        hostManager.setConnectionHandler(msg -> {
            if (msg.contains("rozłączony")) {
                disconnectMessage.set(msg);
                disconnectLatch.countDown();
            }
        });
        
        // Start i połączenie
        hostManager.startHost(TEST_PORT);
        Thread.sleep(100);
        client1Manager.connectToHost("localhost", TEST_PORT, "player1");
        Thread.sleep(200);
        
        assertEquals(1, hostManager.getConnectedClientsCount());
        
        // Klient się rozłącza
        client1Manager.stop();
        
        // Czekaj na powiadomienie o rozłączeniu
        boolean disconnected = disconnectLatch.await(2, TimeUnit.SECONDS);
        assertTrue(disconnected, "Host powinien zostać powiadomiony o rozłączeniu");
        
        assertNotNull(disconnectMessage.get());
        assertTrue(disconnectMessage.get().contains("player1"));
        
        // Poczekaj na aktualizację licznika
        Thread.sleep(200);
        assertEquals(0, hostManager.getConnectedClientsCount());
    }
    
    @Test
    @Order(9)
    @DisplayName("Wiadomość czatu")
    void testChatMessage() throws IOException, InterruptedException {
        CountDownLatch chatLatch = new CountDownLatch(1);
        AtomicReference<GameMessage> chatReceived = new AtomicReference<>();
        
        client2Manager.setMessageHandler(msg -> {
            if (msg.getType() == GameMessage.MessageType.CHAT) {
                chatReceived.set(msg);
                chatLatch.countDown();
            }
        });
        
        // Start i połączenie
        hostManager.startHost(TEST_PORT);
        Thread.sleep(100);
        client1Manager.connectToHost("localhost", TEST_PORT, "player1");
        client2Manager.connectToHost("localhost", TEST_PORT, "player2");
        Thread.sleep(200);
        
        // Host przekazuje wiadomości czatu
        hostManager.setMessageHandler(msg -> {
            if (msg.getType() == GameMessage.MessageType.CHAT) {
                hostManager.send(msg); // broadcast
            }
        });
        
        // Gracz 1 wysyła wiadomość czatu
        client1Manager.send(new GameMessage(
            GameMessage.MessageType.CHAT,
            "player1",
            "Cześć wszystkim! 🎲"
        ));
        
        // Czekaj na odbiór
        boolean received = chatLatch.await(2, TimeUnit.SECONDS);
        assertTrue(received, "Gracz 2 powinien otrzymać wiadomość czatu");
        
        assertNotNull(chatReceived.get());
        assertEquals("player1", chatReceived.get().getSenderId());
        assertEquals("Cześć wszystkim! 🎲", chatReceived.get().getPayload());
    }
    
    @Test
    @Order(10)
    @DisplayName("Automatyczna synchronizacja stanu gry przy zmianie tury")
    void testAutoGameStateSyncOnTurnChange() throws IOException, InterruptedException {
        CountDownLatch syncLatch = new CountDownLatch(1);
        AtomicReference<GameMessage> syncMessage = new AtomicReference<>();
        AtomicReference<GameState> receivedState = new AtomicReference<>();
        
        client1Manager.setMessageHandler(msg -> {
            if (msg.getType() == GameMessage.MessageType.GAME_STATE_SYNC) {
                syncMessage.set(msg);
                if (msg.getPayload() instanceof GameState gs) {
                    receivedState.set(gs);
                }
                syncLatch.countDown();
            }
        });
        
        // Start host i połączenie klienta
        hostManager.startHost(TEST_PORT);
        Thread.sleep(100);
        client1Manager.connectToHost("localhost", TEST_PORT, "player1");
        Thread.sleep(200);
        
        // Stwórz prawdziwy stan gry z planszą i graczami
        List<Player> gamePlayers = List.of(
            new Player("host", "Host", 1500),
            new Player("player1", "Player1", 1500)
        );
        Board board = new Board(List.of(
            new Tile(0, "START"),
            new Tile(1, "Kielce Centrum"),
            new Tile(2, "Sandomierz")
        ));
        GameState gameState = new GameState(board, gamePlayers);
        
        // Podepnij NetworkGameEventListener do automatycznego broadcastu przy zmianie tury
        com.kaluzaplotecka.milionerzy.network.NetworkGameEventListener eventListener = 
            new com.kaluzaplotecka.milionerzy.network.NetworkGameEventListener(
                hostManager, 
                () -> gameState
            );
        gameState.addEventListener(eventListener);
        
        // Wywołaj zmianę tury - powinno automatycznie wysłać GAME_STATE_SYNC
        gameState.nextTurn();
        
        // Czekaj na synchronizację
        boolean synced = syncLatch.await(2, TimeUnit.SECONDS);
        assertTrue(synced, "Klient powinien otrzymać automatyczną synchronizację stanu przy zmianie tury");
        
        // Weryfikuj otrzymaną wiadomość
        assertNotNull(syncMessage.get(), "Wiadomość sync nie powinna być null");
        assertEquals(GameMessage.MessageType.GAME_STATE_SYNC, syncMessage.get().getType());
        
        // Weryfikuj otrzymany stan gry
        assertNotNull(receivedState.get(), "Otrzymany stan gry nie powinien być null");
        assertEquals(2, receivedState.get().getPlayers().size(), "Stan gry powinien zawierać 2 graczy");
        // Po nextTurn() aktualny gracz to player1 (indeks 1)
        assertEquals("player1", receivedState.get().getCurrentPlayer().getId(), 
            "Aktualny gracz w otrzymanym stanie powinien być player1");
    }
}
