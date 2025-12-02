package com.kaluzaplotecka.milionerzy;

import java.io.Serializable;

/**
 * Wiadomość sieciowa przesyłana między graczami.
 * Serializowalna do przesyłania przez socket.
 */
public class GameMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum MessageType {
        // Kontrola połączenia
        CONNECT,            // gracz próbuje się połączyć
        DISCONNECT,         // gracz się rozłącza
        PING,               // sprawdzenie połączenia
        PONG,               // odpowiedź na ping
        
        // Synchronizacja stanu
        GAME_STATE_SYNC,    // pełna synchronizacja stanu gry
        PLAYER_LIST,        // lista graczy
        
        // Akcje gracza
        ROLL_DICE,          // gracz rzuca kostką
        BUY_PROPERTY,       // gracz kupuje nieruchomość
        DECLINE_PURCHASE,   // gracz rezygnuje z kupna
        END_TURN,           // gracz kończy turę
        
        // Handel
        TRADE_OFFER,        // oferta wymiany
        TRADE_RESPONSE,     // odpowiedź na ofertę (akceptacja/odrzucenie)
        
        // Aukcja
        AUCTION_START,      // rozpoczęcie aukcji
        AUCTION_BID,        // licytacja
        AUCTION_PASS,       // rezygnacja z licytacji
        
        // Czat
        CHAT,               // wiadomość czatu
        
        // Kontrola gry
        START_GAME,         // host rozpoczyna grę
        PAUSE_GAME,         // pauza
        RESUME_GAME,        // wznowienie
        
        // Błędy
        ERROR               // komunikat błędu
    }
    
    private final MessageType type;
    private final String senderId;      // ID gracza wysyłającego
    private final String targetId;      // ID gracza docelowego (null = broadcast)
    private final Object payload;       // dane wiadomości
    private final long timestamp;
    
    public GameMessage(MessageType type, String senderId, String targetId, Object payload) {
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

    public MessageType getType() { return type; }
    public String getSenderId() { return senderId; }
    public String getTargetId() { return targetId; }
    public Object getPayload() { return payload; }
    public long getTimestamp() { return timestamp; }
    
    public boolean isBroadcast() {
        return targetId == null;
    }
    
    @Override
    public String toString() {
        return String.format("GameMessage[%s from %s to %s]", 
            type, senderId, targetId != null ? targetId : "ALL");
    }
}
