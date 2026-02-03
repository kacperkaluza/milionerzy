package com.kaluzaplotecka.milionerzy.network;

import com.kaluzaplotecka.milionerzy.events.GameEvent;
import com.kaluzaplotecka.milionerzy.events.GameEventListener;
import com.kaluzaplotecka.milionerzy.model.GameState;
import com.kaluzaplotecka.milionerzy.network.GameMessage.MessageType;

import java.util.function.Supplier;

/**
 * Listener zdarzeń gry odpowiedzialny za synchronizację sieciową.
 * 
 * <p>Nasłuchuje na zdarzenia gry i automatycznie rozsyła je do
 * wszystkich połączonych klientów. Działa tylko w trybie hosta.
 * 
 * <p>Główne funkcjonalności:
 * <ul>
 *   <li>Mapowanie zdarzeń gry na wiadomości sieciowe</li>
 *   <li>Rozgłaszanie stanu gry przy zmianie tury</li>
 *   <li>Synchronizacja aukcji, ruchu i zmian pieniędzy</li>
 * </ul>
 * 
 * @see GameEventListener
 * @see NetworkManager
 */
public class NetworkGameEventListener implements GameEventListener {

    private final NetworkManager networkManager;
    private final Supplier<GameState> gameStateSupplier;

    /**
     * Tworzy nowy listener zdarzeń sieciowych.
     *
     * @param networkManager menedżer sieci do wysyłania wiadomości
     * @param gameStateSupplier dostawca aktualnego stanu gry
     */
    public NetworkGameEventListener(NetworkManager networkManager, Supplier<GameState> gameStateSupplier) {
        this.networkManager = networkManager;
        this.gameStateSupplier = gameStateSupplier;
    }

    @Override
    public void onGameEvent(GameEvent event) {
        // Only Host should broadcast events to clients
        if (networkManager.getMode() != NetworkManager.Mode.HOST) {
            return;
        }

        // Przy zmianie tury - wyślij pełny sync stanu gry do wszystkich klientów
        if (event.getType() == GameEvent.Type.TURN_STARTED) {
            broadcastGameStateSync();
        }

        MessageType msgType = mapEventTypeToMessageType(event.getType());
        if (msgType != null) {
            Object payload = event.getData();
            
            if (msgType == MessageType.MOVE && event.getSource() != null) {
                payload = event.getSource().getPosition();
            } else if (msgType == MessageType.MONEY_UPDATE && event.getSource() != null) {
                payload = event.getSource().getMoney();
            }
            
            // Special handling for AUCTION events
            if (msgType == MessageType.AUCTION_START && event.getData() != null) {
                payload = event.getData();
            } else if (msgType == MessageType.AUCTION_BID) {
                if ("pass".equals(event.getData())) {
                    msgType = MessageType.AUCTION_PASS;
                    payload = "pass";
                } else {
                    payload = event.getData();
                }
            } else if (msgType == MessageType.AUCTION_ENDED) {
                if (event.getData() != null) {
                    payload = event.getData();
                }
            }

            String senderId = networkManager.getPlayerId();
            if (event.getSource() != null) {
                senderId = event.getSource().getId();
            }

            GameMessage msg = new GameMessage(
                msgType,
                senderId,
                payload
            );
            msg.setBroadcast(true);
            networkManager.send(msg);
        }
    }

    /**
     * Wysyła pełny stan gry do wszystkich klientów.
     * Wywoływane przy każdej zmianie tury dla pełnej synchronizacji.
     */
    private void broadcastGameStateSync() {
        GameState currentState = gameStateSupplier.get();
        if (currentState != null) {
            GameMessage syncMsg = new GameMessage(
                MessageType.GAME_STATE_SYNC,
                networkManager.getPlayerId(),
                currentState
            );
            syncMsg.setBroadcast(true);
            networkManager.send(syncMsg);
        }
    }

    private MessageType mapEventTypeToMessageType(GameEvent.Type eventType) {
        return switch (eventType) {
            case PLAYER_MOVED -> MessageType.MOVE;
            case DICE_ROLLED -> MessageType.DICE_RESULT;
            case TURN_STARTED -> MessageType.NEXT_TURN;
            case TURN_ENDED -> MessageType.END_TURN;
            case PROPERTY_BOUGHT -> MessageType.BUY_PROPERTY;
            case PROPERTY_LANDED_NOT_OWNED -> MessageType.PROPERTY_OFFER;
            case AUCTION_STARTED -> MessageType.AUCTION_START;
            case AUCTION_BID -> MessageType.AUCTION_BID;
            case AUCTION_ENDED -> MessageType.AUCTION_ENDED;
            case MONEY_CHANGED -> MessageType.MONEY_UPDATE;
            case RENT_PAID -> MessageType.MONEY_UPDATE;
            default -> null;
        };
    }
}
