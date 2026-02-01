package com.kaluzaplotecka.milionerzy.manager;

import com.kaluzaplotecka.milionerzy.events.GameEvent;
import com.kaluzaplotecka.milionerzy.model.Auction;
import com.kaluzaplotecka.milionerzy.model.GameState;
import com.kaluzaplotecka.milionerzy.model.Player;
import com.kaluzaplotecka.milionerzy.model.TradeOffer;
import com.kaluzaplotecka.milionerzy.model.tiles.PropertyTile;
import com.kaluzaplotecka.milionerzy.model.tiles.Tile;
import java.io.Serializable;

public class PropertyManager implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private TradeOffer pendingTrade;
    private Auction currentAuction;
    
    public PropertyManager() {}

    public boolean canCurrentPlayerBuy(GameState game) {
        Player p = game.getCurrentPlayer();
        if (p == null) return false;
        Tile t = game.getCurrentTile();
        if (!(t instanceof PropertyTile)) return false;
        PropertyTile prop = (PropertyTile) t;
        if (prop.isOwned()) return false;
        return p.getMoney() >= prop.getPrice();
    }

    public boolean buyCurrentProperty(GameState game) {
        Player p = game.getCurrentPlayer();
        if (p == null) return false;
        Tile t = game.getCurrentTile();
        if (!(t instanceof PropertyTile)) return false;
        PropertyTile prop = (PropertyTile) t;
        
        boolean result = prop.buy(p);
        return result;
    }
    
    // === TRADES ===

    public boolean proposeTrade(GameState game, TradeOffer offer) {
        if (offer == null) return false;
        if (pendingTrade != null) return false;
        if (!offer.isValid()) return false;
        
        pendingTrade = offer;
        game.fireEvent(new GameEvent(
            GameEvent.Type.TRADE_PROPOSED,
            offer.getProposer(),
            offer,
            offer.getDescription()
        ));
        return true;
    }

    public boolean acceptTrade(GameState game) {
        if (pendingTrade == null) return false;
        
        boolean success = pendingTrade.execute();
        if (success) {
            game.fireEvent(new GameEvent(
                GameEvent.Type.TRADE_ACCEPTED,
                pendingTrade.getRecipient(),
                pendingTrade,
                pendingTrade.getRecipient().getUsername() + " zaakceptował wymianę"
            ));
        }
        pendingTrade = null;
        return success;
    }

    public boolean rejectTrade(GameState game) {
        if (pendingTrade == null) return false;
        
        pendingTrade.reject();
        game.fireEvent(new GameEvent(
            GameEvent.Type.TRADE_REJECTED,
            pendingTrade.getRecipient(),
            pendingTrade,
            pendingTrade.getRecipient().getUsername() + " odrzucił wymianę"
        ));
        pendingTrade = null;
        return true;
    }

    public boolean cancelTrade(GameState game) {
        if (pendingTrade == null) return false;
        
        pendingTrade.cancel();
        game.fireEvent(new GameEvent(
            GameEvent.Type.TRADE_CANCELLED,
            pendingTrade.getProposer(),
            pendingTrade,
            pendingTrade.getProposer().getUsername() + " anulował wymianę"
        ));
        pendingTrade = null;
        return true;
    }

    public TradeOffer getPendingTrade() {
        return pendingTrade;
    }
    
    /**
     * Sets the pending trade. Package-private for use by GameState when migrating old saves.
     * @param trade the trade offer to set as pending
     */
    void setPendingTrade(TradeOffer trade) {
        this.pendingTrade = trade;
    }
    
    // === AUCTIONS ===

    public boolean startAuction(GameState game, PropertyTile property) {
        if (property == null) return false;
        if (property.isOwned()) return false;
        if (currentAuction != null && currentAuction.isActive()) return false;
        if (game.getPlayers().size() < 2) return false;
        
        currentAuction = new Auction(property, game.getPlayers(), property.getPrice());
        game.fireEvent(new GameEvent(
            GameEvent.Type.AUCTION_STARTED,
            null,
            currentAuction,
            "Rozpoczęto aukcję: " + property.getCity() + " (min. " + currentAuction.getMinimumBid() + " zł)"
        ));
        return true;
    }

    public void setCurrentAuction(Auction auction) {
        this.currentAuction = auction;
    }

    public boolean placeBid(GameState game, Player bidder, int amount) {
        if (currentAuction == null || !currentAuction.isActive()) return false;
        
        boolean success = currentAuction.placeBid(bidder, amount);
        if (success) {
            game.fireEvent(new GameEvent(
                GameEvent.Type.AUCTION_BID,
                bidder,
                amount,
                bidder.getUsername() + " licytuje: " + amount + " zł"
            ));
        }
        return success;
    }

    public void passAuction(GameState game, Player player) {
        if (currentAuction == null || !currentAuction.isActive()) return;
        
        currentAuction.pass(player);
        game.fireEvent(new GameEvent(
            GameEvent.Type.AUCTION_BID,
            player,
            "pass",
            player.getUsername() + " pasuje"
        ));
        
        if (!currentAuction.isActive()) {
            onAuctionEnded(game);
        }
    }

    public void endAuction(GameState game) {
        if (currentAuction == null) return;
        currentAuction.forceEnd();
        onAuctionEnded(game);
    }

    private void onAuctionEnded(GameState game) {
        if (currentAuction == null) return;
        
        Player winner = currentAuction.getHighestBidder();
        int winningBid = currentAuction.getHighestBid();
        PropertyTile property = currentAuction.getProperty();
        
        String message;
        if (winner != null) {
            message = winner.getUsername() + " wygrał aukcję " + property.getCity() + " za " + winningBid + " zł";
        } else {
            message = "Aukcja " + property.getCity() + " zakończona bez zwycięzcy";
        }
        
        game.fireEvent(new GameEvent(
            GameEvent.Type.AUCTION_ENDED,
            winner,
            currentAuction,
            message
        ));
        
        currentAuction = null;
    }

    public Auction getCurrentAuction() {
        return currentAuction;
    }

    public boolean hasActiveAuction() {
        return currentAuction != null && currentAuction.isActive();
    }
}
