package com.kaluzaplotecka.milionerzy.network;

import com.kaluzaplotecka.milionerzy.events.GameEvent;
import com.kaluzaplotecka.milionerzy.events.GameEventListener;
import com.kaluzaplotecka.milionerzy.network.GameMessage.MessageType;

public class NetworkGameEventListener implements GameEventListener {

    private final NetworkManager networkManager;

    public NetworkGameEventListener(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    @Override
    public void onGameEvent(GameEvent event) {
        // Only Host should broadcast events to clients
        if (networkManager.getMode() != NetworkManager.Mode.HOST) {
            return;
        }

        MessageType msgType = mapEventTypeToMessageType(event.getType());
        if (msgType != null) {
            // Determine if payload acts as the main data
            Object payload = event.getData();
            
            // Special handling for MOVE which usually expects just the position or full player info
            // In GameState.movePlayerBy, event data is "steps" (Integer)
            // But we might want to send the absolute position to be safe, or just the steps.
            // Let's check GameMessage.GameMessage: payload is Object.
            // NetworkManager doesn't enforce payload type, but receiver expects it.
            // In GameBoardView.handleMove, it casts payload to int (position).
            // But wait, GameState fires PLAYER_MOVED with 'steps' as data.
            // If the client expects absolute position, we need to send that.
            // The event source is the Player object, which has the NEW position.
            
            if (msgType == MessageType.MOVE && event.getSource() != null) {
                payload = event.getSource().getPosition();
            } else if (msgType == MessageType.MONEY_UPDATE && event.getSource() != null) {
                // Determine what to send. We can send just the new balance, or the change amount?
                // Event data for MONEY_CHANGED is change amount (e.g. 200). 
                // But full sync is safer. Let's send the new money balance.
                payload = event.getSource().getMoney();
            }
            
            // Special handling for AUCTION events
            if (msgType == MessageType.AUCTION_START && event.getData() != null) {
                // Ensure we send the Auction object
                payload = event.getData();
            } else if (msgType == MessageType.AUCTION_BID) {
                // Check if it's a "pass"
                if ("pass".equals(event.getData())) {
                    msgType = MessageType.AUCTION_PASS;
                    payload = "pass";
                } else {
                    // Regular bid amount
                    payload = event.getData();
                }
            } else if (msgType == MessageType.AUCTION_START) {
                 // AUCTION_ENDED maps to null in default so handle it explicitly here if mapped
            } else if (msgType == MessageType.AUCTION_ENDED) {
                // Ensure we send the full message or winner info if available in payload
                // GameState.onAuctionEnded payload is String message, but we might want data?
                // Event data for AUCTION_ENDED is "message" string, but source is Auction?
                // Let's check GameState.onAuctionEnded.
                // fireEvent(..., winner, currentAuction, message)
                // So data is 'message' (String). Source is 'winner' (Player). 
                // Wait, logic says: fireEvent(type, source, data, message)
                // In onAuctionEnded: fireEvent(AUCTION_ENDED, winner, currentAuction, message)
                // So source=winner, data=currentAuction.
                // We should send the Auction object (or null) to sync state.
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
                payload // Broadcast to all
            );
            msg.setBroadcast(true);
            networkManager.send(msg);
        }
    }

    private MessageType mapEventTypeToMessageType(GameEvent.Type eventType) {
        return switch (eventType) {
            case PLAYER_MOVED -> MessageType.MOVE;
            case DICE_ROLLED -> MessageType.DICE_RESULT; // Or ROLL_DICE? DICE_RESULT seems more appropriate for "happened"
            case TURN_STARTED -> MessageType.NEXT_TURN; // Or distinct type
            case TURN_ENDED -> MessageType.END_TURN;
            case PROPERTY_BOUGHT -> MessageType.BUY_PROPERTY;
            case PROPERTY_LANDED_NOT_OWNED -> MessageType.PROPERTY_OFFER;
            case AUCTION_STARTED -> MessageType.AUCTION_START;
            case AUCTION_BID -> MessageType.AUCTION_BID; // logic in onGameEvent handles 'pass'
            case AUCTION_ENDED -> MessageType.AUCTION_ENDED;
            case MONEY_CHANGED -> MessageType.MONEY_UPDATE;
            case RENT_PAID -> MessageType.MONEY_UPDATE; // Treat rent paid as generic money update for simplicity, or add explicit
            default -> null;
        };
    }
}
