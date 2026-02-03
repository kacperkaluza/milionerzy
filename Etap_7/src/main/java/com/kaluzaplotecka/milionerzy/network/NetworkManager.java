package com.kaluzaplotecka.milionerzy.network;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.kaluzaplotecka.milionerzy.model.GameState;

/**
 * Zarządza komunikacją sieciową między graczami.
 * Obsługuje tryb hosta (serwera) i klienta.
 */
public class NetworkManager {
    
    public enum Mode { HOST, CLIENT, OFFLINE }
    
    private Mode mode = Mode.OFFLINE;
    private String playerId;
    
    // Serwer (host)
    private ServerSocket serverSocket;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private ExecutorService serverExecutor;
    
    // Klient
    private Socket clientSocket;
    private ObjectOutputStream clientOut;
    private ObjectInputStream clientIn;
    private Thread clientThread;
    
    // Callback na otrzymane wiadomości
    private Consumer<GameMessage> messageHandler;
    
    // Callback na zmiany połączenia
    private Consumer<String> connectionHandler;

    // Provider stanu gry (dla hosta)
    private Supplier<GameState> gameStateProvider;
    
    // System ACK
    private final PendingMessageTracker pendingTracker;
    private Consumer<GameMessage> sendingCallback;    // wywoływane przy wysyłaniu
    private Consumer<GameMessage> ackCallback;        // wywoływane po ACK
    private Consumer<GameMessage> nackCallback;       // wywoływane po NACK
    private Consumer<GameMessage> timeoutCallback;    // wywoływane po timeout
    
    private volatile boolean running = false;
    
    public static final int DEFAULT_PORT = 5555;
    
    public NetworkManager(String playerId) {
        this.playerId = playerId;
        this.pendingTracker = new PendingMessageTracker();
        
        // Skonfiguruj callbacki trackera
        pendingTracker.setResendCallback(this::resendMessage);
        pendingTracker.setAckCallback(msg -> {
            if (ackCallback != null) ackCallback.accept(msg);
        });
        pendingTracker.setNackCallback(msg -> {
            if (nackCallback != null) nackCallback.accept(msg);
        });
        pendingTracker.setTimeoutCallback(msg -> {
            if (timeoutCallback != null) timeoutCallback.accept(msg);
        });
    }
    
    // === TRYB HOSTA (SERWERA) ===
    
    /**
     * Uruchamia serwer na podanym porcie.
     */
    private String roomCode;

    // === TRYB HOSTA (SERWERA) ===
    
    /**
     * Uruchamia serwer na podanym porcie.
     */
    public void startHost(int port, String roomCode) throws IOException {
        if (running) throw new IllegalStateException("NetworkManager już działa");
        
        mode = Mode.HOST;
        running = true;
        this.roomCode = roomCode;
        serverSocket = new ServerSocket(port);
        serverExecutor = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        });
        
        // Wątek akceptujący połączenia
        serverExecutor.submit(() -> {
            while (running && !serverSocket.isClosed()) {
                try {
                    Socket socket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(socket);
                    clients.add(handler);
                    serverExecutor.submit(handler);
                    
                    if (connectionHandler != null) {
                        connectionHandler.accept("Nowy gracz połączony: " + socket.getInetAddress());
                    }
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Błąd akceptowania połączenia: " + e.getMessage());
                    }
                }
            }
        });
        
        System.out.println("Host uruchomiony na porcie " + port + " kod pokoju: " + roomCode);
    }
    
    public void startHost(String roomCode) throws IOException {
        startHost(DEFAULT_PORT, roomCode);
    }
    
    // === TRYB KLIENTA ===
    
    /**
     * Łączy się z hostem.
     */
    public void connectToHost(String host, int port, String playerName, String roomCode) throws IOException {
        if (running) throw new IllegalStateException("NetworkManager już działa");
        
        mode = Mode.CLIENT;
        running = true;
        
        clientSocket = new Socket(host, port);
        clientOut = new ObjectOutputStream(clientSocket.getOutputStream());
        clientIn = new ObjectInputStream(clientSocket.getInputStream());
        
        // Wysyłamy informację o połączeniu z kodem pokoju
        // Payload: String[] { roomCode, playerName }
        send(new GameMessage(GameMessage.MessageType.CONNECT, playerId, new String[]{roomCode, playerName}));
        
        // Wątek nasłuchujący
        clientThread = new Thread(() -> {
            while (running && !clientSocket.isClosed()) {
                try {
                    GameMessage msg = (GameMessage) clientIn.readObject();
                    
                    // Obsłuż ACK/NACK
                    if (msg.getType() == GameMessage.MessageType.ACK) {
                        pendingTracker.acknowledge(msg.getAckForMessageId());
                        continue;
                    } else if (msg.getType() == GameMessage.MessageType.NACK) {
                        pendingTracker.reject(msg.getAckForMessageId(), msg.getNackReason());
                        continue;
                    }
                    
                    if (messageHandler != null) {
                        messageHandler.accept(msg);
                    }
                } catch (EOFException e) {
                    // Połączenie zamknięte
                    break;
                } catch (IOException | ClassNotFoundException e) {
                    if (running) {
                        System.err.println("Błąd odbioru: " + e.getMessage());
                    }
                    break;
                }
            }
            if (connectionHandler != null) {
                connectionHandler.accept("Rozłączono z hostem");
            }
        });
        clientThread.setDaemon(true);
        clientThread.start();
        
        System.out.println("Połączono z hostem " + host + ":" + port);
    }
    
    public void connectToHost(String host, String playerName, String roomCode) throws IOException {
        connectToHost(host, DEFAULT_PORT, playerName, roomCode);
    }
    
    /**
     * Wysyła wiadomość. Host broadcastuje do wszystkich klientów,
     * klient wysyła do hosta.
     */
    public void send(GameMessage message) {
        if (mode == Mode.OFFLINE) return;
        
        // Powiadom o wysyłaniu (tylko klient śledzi odpowiedzi)
        if (mode == Mode.CLIENT && sendingCallback != null && message.requiresAck()) {
            sendingCallback.accept(message);
        }
        
        // Śledź wiadomości wymagające ACK (tylko klient śledzi)
        if (mode == Mode.CLIENT && message.requiresAck()) {
            pendingTracker.track(message);
        }
        
        if (mode == Mode.HOST) {
            // Broadcast do wszystkich klientów
            for (ClientHandler client : clients) {
                client.send(message);
            }
        } else if (mode == Mode.CLIENT) {
            try {
                if (clientOut != null) {
                    clientOut.writeObject(message);
                    clientOut.flush();
                } else {
                    System.err.println("Błąd wysyłania: brak połączenia (clientOut is null)");
                }
            } catch (IOException e) {
                System.err.println("Błąd wysyłania: " + e.getMessage());
            }
        }
    }
    
    /**
     * Ponownie wysyła wiadomość (przy timeout).
     */
    private void resendMessage(GameMessage message) {
        System.out.println("Ponowne wysyłanie: " + message.getActionName());
        if (mode == Mode.CLIENT) {
            try {
                clientOut.writeObject(message);
                clientOut.flush();
            } catch (IOException e) {
                System.err.println("Błąd ponownego wysyłania: " + e.getMessage());
            }
        }
    }
    
    /**
     * Wysyła wiadomość do konkretnego gracza (tylko host).
     */
    public void sendTo(String targetPlayerId, GameMessage message) {
        if (mode != Mode.HOST) return;
        
        for (ClientHandler client : clients) {
            if (targetPlayerId.equals(client.playerId)) {
                client.send(message);
                break;
            }
        }
    }
    
    // === ZATRZYMANIE ===
    
    public void stop() {
        running = false;
        
        if (mode == Mode.HOST) {
            // Zamknij wszystkie połączenia klientów
            for (ClientHandler client : clients) {
                client.close();
            }
            clients.clear();
            
            try {
                if (serverSocket != null) serverSocket.close();
            } catch (IOException e) { /* ignore */ }
            
            if (serverExecutor != null) serverExecutor.shutdownNow();
            
        } else if (mode == Mode.CLIENT) {
            try {
                if (clientSocket != null) clientSocket.close();
            } catch (IOException e) { /* ignore */ }
        }
        
        mode = Mode.OFFLINE;
        pendingTracker.shutdown();
        System.out.println("NetworkManager zatrzymany");
    }
    
    // === WYSYŁANIE ACK/NACK (dla hosta) ===
    
    /**
     * Wysyła ACK do gracza potwierdzając otrzymanie wiadomości.
     */
    public void sendAck(String originalMessageId, String toPlayerId) {
        if (mode != Mode.HOST) return;
        
        GameMessage ack = GameMessage.createAck(originalMessageId, playerId, toPlayerId);
        sendTo(toPlayerId, ack);
    }
    
    /**
     * Wysyła NACK do gracza odrzucając wiadomość.
     */
    public void sendNack(String originalMessageId, String toPlayerId, String reason) {
        if (mode != Mode.HOST) return;
        
        GameMessage nack = GameMessage.createNack(originalMessageId, playerId, toPlayerId, reason);
        sendTo(toPlayerId, nack);
    }
    
    // === GETTERY I SETTERY ===
    
    public Mode getMode() { return mode; }
    public String getPlayerId() { return playerId; }
    public boolean isRunning() { return running; }
    public int getConnectedClientsCount() { return clients.size(); }
    
    public void setMessageHandler(Consumer<GameMessage> handler) {
        this.messageHandler = handler;
    }
    
    public void setConnectionHandler(Consumer<String> handler) {
        this.connectionHandler = handler;
    }

    public void setGameStateProvider(Supplier<GameState> provider) {
        this.gameStateProvider = provider;
    }
    
    // === Callbacki ACK ===
    
    public void setSendingCallback(Consumer<GameMessage> callback) {
        this.sendingCallback = callback;
    }
    
    public void setAckCallback(Consumer<GameMessage> callback) {
        this.ackCallback = callback;
    }
    
    public void setNackCallback(Consumer<GameMessage> callback) {
        this.nackCallback = callback;
    }
    
    public void setTimeoutCallback(Consumer<GameMessage> callback) {
        this.timeoutCallback = callback;
    }
    
    public PendingMessageTracker getPendingTracker() {
        return pendingTracker;
    }
    
    // === KLASA WEWNĘTRZNA: OBSŁUGA KLIENTA (DLA HOSTA) ===
    
    private class ClientHandler implements Runnable {
        private final Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private String playerId;
        
        ClientHandler(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                
                while (running && !socket.isClosed()) {
                    GameMessage msg = (GameMessage) in.readObject();
                    
                    // Zapisz ID gracza przy pierwszym połączeniu
                    if (msg.getType() == GameMessage.MessageType.CONNECT) {
                        boolean valid = false;
                        String playerName = "Unknown";
                        
                        Object payload = msg.getPayload();
                        if (payload instanceof String[] parts && parts.length >= 2) {
                            String code = parts[0];
                            playerName = parts[1];
                            
                            // Validate Room Code
                            if (NetworkManager.this.roomCode != null && !NetworkManager.this.roomCode.equals(code)) {
                                System.out.println("Odrzucono połączenie: nieprawidłowy kod pokoju. Otrzymano: " + code + ", Oczekiwano: " + NetworkManager.this.roomCode);
                                send(new GameMessage(GameMessage.MessageType.DISCONNECT, NetworkManager.this.playerId, "Invalid Room Code"));
                                close();
                                return;
                            }
                            valid = true;
                        } else if (payload instanceof String name) {
                            // Support legacy/simple connection if code validation disabled
                            if (NetworkManager.this.roomCode != null) {
                                System.out.println("Odrzucono połączenie: brak kodu pokoju (legacy format).");
                                send(new GameMessage(GameMessage.MessageType.DISCONNECT, NetworkManager.this.playerId, "Room Code Required"));
                                close();
                                return;
                            }
                            playerName = name;
                            valid = true;
                        }
                        
                        if (valid) {
                            this.playerId = msg.getSenderId();
                            
                            GameMessage internalMsg = new GameMessage(GameMessage.MessageType.CONNECT, msg.getSenderId(), playerName);
                            
                            // Jeśli mamy providera stanu (jesteśmy hostem), wyślij stan gry
                            if (gameStateProvider != null) {
                                GameState currentState = gameStateProvider.get();
                                if (currentState != null) {
                                    send(new GameMessage(
                                        GameMessage.MessageType.GAME_STATE_SYNC,
                                        NetworkManager.this.playerId,
                                        this.playerId, // Wyślij tylko do tego klienta
                                        currentState
                                    ));
                                }
                            }
                            
                            if (messageHandler != null) {
                                messageHandler.accept(internalMsg);
                            }
                            
                            GameMessage broadcastMsg = new GameMessage(GameMessage.MessageType.CONNECT, msg.getSenderId(), playerName);
                            broadcastMsg.setBroadcast(true);

                            for (ClientHandler other : clients) {
                                if (other != this) {
                                    other.send(broadcastMsg);
                                }
                            }
                        }
                    } else {
                        // Inne wiadomości
                        if (messageHandler != null) {
                            messageHandler.accept(msg);
                        }
                        
                        if (msg.isBroadcast()) {
                            for (ClientHandler other : clients) {
                                if (other != this) {
                                    other.send(msg);
                                }
                            }
                        }
                    }
                }
            } catch (EOFException e) {
                // Klient się rozłączył
            } catch (IOException | ClassNotFoundException e) {
                if (running) {
                    // System.err.println("Błąd klienta: " + e.getMessage());
                }
            } finally {
                close();
                clients.remove(this);
                if (connectionHandler != null && playerId != null) {
                    connectionHandler.accept("Gracz " + playerId + " rozłączony");
                }
            }
        }
        
        void send(GameMessage msg) {
            try {
                if (out != null) {
                    // CRITICAL: reset() czyści cache ObjectOutputStream
                    // Bez tego kolejne wysłania tego samego obiektu zawierają stare dane!
                    out.reset();
                    out.writeObject(msg);
                    out.flush();
                }
            } catch (IOException e) {
                System.err.println("Błąd wysyłania do klienta: " + e.getMessage());
            }
        }

        
        void close() {
            try {
                if (socket != null) socket.close();
            } catch (IOException e) { /* ignore */ }
        }
    }
}
