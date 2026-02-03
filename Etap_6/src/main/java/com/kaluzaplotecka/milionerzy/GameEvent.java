package com.kaluzaplotecka.milionerzy;

/**
 * Reprezentuje zdarzenie w grze, które może być wysłane do innych graczy
 * lub nasłuchiwane przez UI.
 */
public class GameEvent {
    
    public enum Type {
        // Zdarzenia ruchu
        PLAYER_MOVED,           // gracz się przesunął
        DICE_ROLLED,            // rzucono kostką
        
        // Zdarzenia nieruchomości
        PROPERTY_BOUGHT,        // ktoś kupił nieruchomość
        RENT_PAID,              // ktoś zapłacił czynsz
        
        // Zdarzenia gracza
        PLAYER_BANKRUPT,        // gracz zbankrutował
        PLAYER_JOINED,          // gracz dołączył do gry
        PLAYER_LEFT,            // gracz opuścił grę
        PLAYER_JAILED,          // gracz trafił do więzienia
        PLAYER_RELEASED,        // gracz wyszedł z więzienia
        
        // Zdarzenia kart
        CARD_DRAWN,             // wylosowano kartę
        
        // Zdarzenia handlu
        TRADE_PROPOSED,         // propozycja wymiany
        TRADE_ACCEPTED,         // wymiana zaakceptowana
        TRADE_REJECTED,         // wymiana odrzucona
        TRADE_CANCELLED,        // wymiana anulowana
        
        // Zdarzenia aukcji
        AUCTION_STARTED,        // rozpoczęto aukcję
        AUCTION_BID,            // ktoś zalicytował
        AUCTION_ENDED,          // aukcja zakończona
        
        // Zdarzenia gry
        GAME_STARTED,           // gra rozpoczęta
        GAME_OVER,              // koniec gry
        TURN_STARTED,           // rozpoczęcie tury
        TURN_ENDED,             // zakończenie tury
        
        // Komunikacja
        CHAT_MESSAGE            // wiadomość na czacie
    }

    private final Type type;
    private final Player source;      // gracz, który wywołał zdarzenie (może być null)
    private final Object data;        // dodatkowe dane zdarzenia
    private final String message;     // czytelna wiadomość
    private final long timestamp;     // czas zdarzenia

    public GameEvent(Type type, Player source, Object data, String message) {
        this.type = type;
        this.source = source;
        this.data = data;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }
    
    public GameEvent(Type type, Player source, String message) {
        this(type, source, null, message);
    }
    
    public GameEvent(Type type, String message) {
        this(type, null, null, message);
    }

    public Type getType() { return type; }
    public Player getSource() { return source; }
    public Object getData() { return data; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
    
    @Override
    public String toString() {
        return String.format("[%s] %s: %s", type, 
            source != null ? source.getUsername() : "System", 
            message);
    }
}
