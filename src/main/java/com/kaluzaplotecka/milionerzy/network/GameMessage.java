package com.kaluzaplotecka.milionerzy.network;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

/**
 * Wiadomość sieciowa przesyłana między graczami.
 * Serializowalna do przesyłania przez socket.
 */
public class GameMessage implements Serializable {
    private static final long serialVersionUID = 2L;  // Incremented for new fields
    
    public enum MessageType {
        // Kontrola połączenia
        CONNECT,            // gracz próbuje się połączyć
        DISCONNECT,         // gracz się rozłącza
        PING,               // sprawdzenie połączenia
        PONG,               // odpowiedź na ping
        
        // System ACK
        ACK,                // potwierdzenie odebrania wiadomości
        NACK,               // odrzucenie/błąd przetwarzania wiadomości
        
        // Synchronizacja stanu
        GAME_STATE_SYNC,    // pełna synchronizacja stanu gry
        PLAYER_LIST,        // lista graczy
        
        // Akcje gracza
        ROLL_DICE,          // gracz rzuca kostką
        MOVE,               // gracz się porusza
        DICE_RESULT,        // wynik rzutu kostką
        NEXT_TURN,          // gracz kończy turę
        BUY_PROPERTY,       // gracz kupuje nieruchomość
        DECLINE_PURCHASE,   // gracz rezygnuje z kupna
        PROPERTY_OFFER,     // gracz stanął na nieruchomości do kupienia
        END_TURN,           // gracz kończy turę
        MONEY_UPDATE,       // aktualizacja pieniędzy

        
        // Handel
        TRADE_OFFER,        // oferta wymiany
        TRADE_RESPONSE,     // odpowiedź na ofertę (akceptacja/odrzucenie)
        
        // Aukcja
        AUCTION_START,      // rozpoczęcie aukcji
        AUCTION_BID,        // licytacja
        AUCTION_PASS,       // rezygnacja z licytacji
        AUCTION_ENDED,      // zakończenie aukcji
        
        // Czat
        CHAT,               // wiadomość czatu
        
        // Kontrola gry
        START_GAME,         // host rozpoczyna grę
        GAME_START,         // gra wystartowała (broadcast do wszystkich)
        PAUSE_GAME,         // pauza
        RESUME_GAME,        // wznowienie
        
        // Błędy
        ERROR               // komunikat błędu
    }
    
    // Typy wiadomości wymagające potwierdzenia ACK
    private static final Set<MessageType> ACK_REQUIRED_TYPES = Set.of(
        MessageType.ROLL_DICE,
        MessageType.BUY_PROPERTY,
        MessageType.DECLINE_PURCHASE,
        MessageType.AUCTION_BID,
        MessageType.AUCTION_PASS,
        MessageType.TRADE_OFFER,
        MessageType.TRADE_RESPONSE,
        MessageType.END_TURN
    );
    
    private final String messageId;        // unikalny identyfikator wiadomości
    private final MessageType type;        // typ wiadomości
    private final String senderId;         // ID gracza wysyłającego
    private final String targetId;         // ID gracza docelowego (null = broadcast)
    private final Object payload;          // dane wiadomości
    private final long timestamp;          // czas wysłania
    private boolean broadcast = false;     // czy rozgłosić do wszystkich
    private String ackForMessageId;        // dla ACK/NACK - ID potwierdzonej wiadomości
    private String nackReason;             // powód odrzucenia (dla NACK)
    
    public GameMessage(MessageType type, String senderId, String targetId, Object payload) {
        this.messageId = UUID.randomUUID().toString();
        this.type = type;
        this.senderId = senderId;
        this.targetId = targetId;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
    }
    
    public GameMessage(MessageType type, String senderId, Object payload) {
        this(type, senderId, null, payload);
    }
    
    public GameMessage(MessageType type, String senderId) {
        this(type, senderId, null, null);
    }
    
    // === Factory methods dla ACK/NACK ===
    
    /**
     * Tworzy wiadomość ACK potwierdzającą otrzymanie innej wiadomości.
     */
    public static GameMessage createAck(String originalMessageId, String senderId, String targetId) {
        GameMessage ack = new GameMessage(MessageType.ACK, senderId, targetId, null);
        ack.ackForMessageId = originalMessageId;
        return ack;
    }
    
    /**
     * Tworzy wiadomość NACK odrzucającą wiadomość z podaniem powodu.
     */
    public static GameMessage createNack(String originalMessageId, String senderId, String targetId, String reason) {
        GameMessage nack = new GameMessage(MessageType.NACK, senderId, targetId, null);
        nack.ackForMessageId = originalMessageId;
        nack.nackReason = reason;
        return nack;
    }

    // === Gettery ===
    
    public String getMessageId() { return messageId; }
    public MessageType getType() { return type; }
    public String getSenderId() { return senderId; }
    public String getTargetId() { return targetId; }
    public Object getPayload() { return payload; }
    public long getTimestamp() { return timestamp; }
    public String getAckForMessageId() { return ackForMessageId; }
    public String getNackReason() { return nackReason; }
    
    /**
     * Sprawdza czy ten typ wiadomości wymaga potwierdzenia ACK.
     */
    public boolean requiresAck() {
        return ACK_REQUIRED_TYPES.contains(type);
    }
    
    /**
     * Zwraca czytelną nazwę akcji (do wyświetlania w UI).
     */
    public String getActionName() {
        return switch (type) {
            case ROLL_DICE -> "Rzut kostką";
            case BUY_PROPERTY -> "Zakup nieruchomości";
            case DECLINE_PURCHASE -> "Odmowa zakupu";
            case AUCTION_BID -> "Licytacja";
            case AUCTION_PASS -> "Pas w aukcji";
            case TRADE_OFFER -> "Oferta wymiany";
            case TRADE_RESPONSE -> "Odpowiedź na wymianę";
            case END_TURN -> "Zakończenie tury";
            default -> type.name();
        };
    }
    
    public boolean isBroadcast() {
        return broadcast || targetId == null;
    }
    
    public void setBroadcast(boolean broadcast) {
        this.broadcast = broadcast;
    }
    
    @Override
    public String toString() {
        return String.format("GameMessage[%s id=%s from %s to %s]", 
            type, messageId.substring(0, 8), senderId, targetId != null ? targetId : "ALL");
    }
}
