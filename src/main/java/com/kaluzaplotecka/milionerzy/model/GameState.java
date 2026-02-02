package com.kaluzaplotecka.milionerzy.model;

import com.kaluzaplotecka.milionerzy.network.GameMessage;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

import com.kaluzaplotecka.milionerzy.events.GameEvent;
import com.kaluzaplotecka.milionerzy.events.GameEventListener;
import com.kaluzaplotecka.milionerzy.manager.BankManager;
import com.kaluzaplotecka.milionerzy.manager.MovementManager;
import com.kaluzaplotecka.milionerzy.manager.PropertyManager;
import com.kaluzaplotecka.milionerzy.manager.TurnManager;
import com.kaluzaplotecka.milionerzy.model.cards.EventCard;
import com.kaluzaplotecka.milionerzy.model.tiles.PropertyTile;
import com.kaluzaplotecka.milionerzy.model.tiles.Tile;

public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // Core Data
    private Board board;
    Deque<EventCard> chanceDeck;
    Deque<EventCard> communityChestDeck;
    public static final int PASS_START_REWARD = 200;

    // Managers (Facade pattern)
    private TurnManager turnManager;
    private MovementManager movementManager;
    private BankManager bankManager;
    private PropertyManager propertyManager;

    public GameState(Board board, List<Player> players){
        this.board = board;
        this.chanceDeck = new ArrayDeque<>();
        this.communityChestDeck = new ArrayDeque<>();
        
        // Initialize Managers
        this.turnManager = new TurnManager(players);
        this.movementManager = new MovementManager();
        this.bankManager = new BankManager();
        this.propertyManager = new PropertyManager();
    }
    
    // === Getters for Managers (Optional, but useful for deep access if needed) ===
    public TurnManager getTurnManager() { return turnManager; }
    public MovementManager getMovementManager() { return movementManager; }
    public BankManager getBankManager() { return bankManager; }
    public PropertyManager getPropertyManager() { return propertyManager; }

    public Board getBoard(){ return board; }

    public Player getCurrentPlayer(){
        return turnManager.getCurrentPlayer();
    }

    public int rollDice(){
        return movementManager.rollDice();
    }

    /**
     * Pozwala wstrzyknąć kontrolowany Random dla testów.
     * Użyj new Random(seed) dla deterministycznych wyników.
     */
    public void setRandom(Random rand) {
        movementManager.setRandom(rand);
    }

    public void moveCurrentPlayer(){
        movementManager.moveCurrentPlayer(this);
    }

    public void moveCurrentPlayer(int steps){
        movementManager.moveCurrentPlayer(this, steps);
    }

    /**
     * Move a specific player by given steps, handle passing Start reward and landing effects.
     * This is testable because it accepts explicit steps.
     */
    public void movePlayerBy(Player p, int steps){
        movementManager.movePlayerBy(this, p, steps);
    }

    public void nextTurn(){
        turnManager.nextTurn();
        // Round change handled within turnManager, we can check getRoundNumber() if needed.
        
        fireEvent(new GameEvent(
            GameEvent.Type.TURN_STARTED,
            getCurrentPlayer(),
            "Tura gracza " + getCurrentPlayer().getUsername()
        ));
    }

    public void handleBankruptcy(Player p){
        bankManager.handleBankruptcy(this, p);
    }

    public boolean isGameOver(){
        return turnManager.isGameOver();
    }
    
    public Player getWinner(){
        return turnManager.getWinner();
    }

    /**
     * Return the tile the current player is standing on, or null.
     */
    public Tile getCurrentTile(){
        Player p = getCurrentPlayer();
        if (p == null || board == null) return null;
        return board.getTile(p.getPosition());
    }

    /**
     * Check whether the current player can buy the property they landed on.
     * This does not modify game state.
     */
    public boolean canCurrentPlayerBuy(){
        return propertyManager.canCurrentPlayerBuy(this);
    }

    /**
     * Attempt to buy the property the current player is on. Returns true when purchase succeeded.
     * UI should call this when the player chooses to buy.
     */
    public boolean buyCurrentProperty(){
        boolean success = propertyManager.buyCurrentProperty(this);
        if (success) {
            Player p = getCurrentPlayer();
             fireEvent(new GameEvent(
                GameEvent.Type.PROPERTY_BOUGHT,
                p,
                getCurrentTile(),
                p.getUsername() + " kupił " + ((PropertyTile)getCurrentTile()).getCity()
            ));
            // Implicitly logic might expect turn change after buy, 
            // but original buyCurrentProperty didn't do it. 
            // Host usually calls nextTurn after processing BUY_PROPERTY message.
        }
        return success;
    }

    /* --- Event card / deck helpers --- */

    public void addChanceCard(EventCard card){
        if (card == null) return;
        chanceDeck.addLast(card);
    }

    public void addCommunityChestCard(EventCard card){
        if (card == null) return;
        communityChestDeck.addLast(card);
    }

    /** Draws the top chance card, rotates it to the bottom, and returns it (or null if empty). */
    public EventCard drawChanceCard(){
        if (chanceDeck == null || chanceDeck.isEmpty()) return null;
        EventCard c = chanceDeck.removeFirst();
        chanceDeck.addLast(c);
        return c;
    }

    /** Draws the top community chest card, rotates it to the bottom, and returns it (or null if empty). */
    public EventCard drawCommunityChestCard(){
        if (communityChestDeck == null || communityChestDeck.isEmpty()) return null;
        EventCard c = communityChestDeck.removeFirst();
        communityChestDeck.addLast(c);
        return c;
    }

    public void executeChanceCardFor(Player p){
        EventCard c = drawChanceCard();
        if (c != null) c.execute(this, p);
    }

    public void executeCommunityChestCardFor(Player p){
        EventCard c = drawCommunityChestCard();
        if (c != null) c.execute(this, p);
    }

    // === SYSTEM ZDARZEŃ (OBSERVER PATTERN) ===
    
    private transient List<GameEventListener> eventListeners;
    
    /**
     * Zwraca listę eventListeners, inicjalizując ją jeśli potrzeba (np. po deserializacji).
     */
    private List<GameEventListener> getEventListeners() {
        if (eventListeners == null) {
            eventListeners = new ArrayList<>();
        }
        return eventListeners;
    }
    
    public void addEventListener(GameEventListener listener) {
        if (listener != null && !getEventListeners().contains(listener)) {
            getEventListeners().add(listener);
        }
    }
    
    public void removeEventListener(GameEventListener listener) {
        getEventListeners().remove(listener);
    }
    
    /**
     * Wysyła zdarzenie do wszystkich nasłuchujących.
     */
    public void fireEvent(GameEvent event) {
        for (GameEventListener listener : getEventListeners()) {
            try {
                listener.onGameEvent(event);
            } catch (Exception e) {
                System.err.println("Błąd w listenerze: " + e.getMessage());
            }
        }
    }
    
    // === SYSTEM HANDLU ===
    
    public boolean proposeTrade(TradeOffer offer) {
        return propertyManager.proposeTrade(this, offer);
    }
    
    public boolean acceptTrade() {
        return propertyManager.acceptTrade(this);
    }
    
    public boolean rejectTrade() {
        return propertyManager.rejectTrade(this);
    }
    
    public boolean cancelTrade() {
        return propertyManager.cancelTrade(this);
    }
    
    public TradeOffer getPendingTrade() {
        return propertyManager.getPendingTrade();
    }
    
    // === SYSTEM AUKCJI ===
    
    public boolean startAuction(PropertyTile property) {
        return propertyManager.startAuction(this, property);
    }
    
    public boolean placeBid(Player bidder, int amount) {
        return propertyManager.placeBid(this, bidder, amount);
    }
    
    public void passAuction(Player player) {
        propertyManager.passAuction(this, player);
    }
    
    public void endAuction() {
        propertyManager.endAuction(this);
    }
    
    public Auction getCurrentAuction() {
        return propertyManager.getCurrentAuction();
    }
    
    public boolean hasActiveAuction() {
        return propertyManager.hasActiveAuction();
    }
    
    // === OBSŁUGA SIECI ===
    
    /**
     * Przetwarza wiadomość sieciową i aktualizuje stan gry.
     */
    public void processNetworkMessage(GameMessage msg, boolean isHost) {
        processNetworkMessage(msg, isHost, null);
    }
    
    public void processNetworkMessage(GameMessage msg, boolean isHost, 
                                       com.kaluzaplotecka.milionerzy.network.NetworkManager networkManager) {
        if (msg == null) return;
        
        boolean processed = false;
        
        switch (msg.getType()) {
            case ROLL_DICE -> {
                if (isHost) {
                    String senderId = msg.getSenderId();
                    Player p = getCurrentPlayer();
                    System.out.println("[DEBUG ROLL_DICE] senderId=" + senderId + ", currentPlayer=" + (p != null ? p.getId() : "null"));
                    if (p != null && p.getId().equals(senderId)) {
                        // Jeśli klient przysłał wynik rzutu (int[] lub Integer), użyj go
                        Object payload = msg.getPayload();
                        if (payload instanceof Integer steps) {
                            moveCurrentPlayer(steps);
                        } else if (payload instanceof int[] stepsArr && stepsArr.length > 0) {
                             // Obsługa int[] dla wstecznej kompatybilności (z testami)
                             int sum = 0;
                             for (int s : stepsArr) sum += s;
                             moveCurrentPlayer(sum);
                        } else {
                            // Fallback: serwer losuje
                            moveCurrentPlayer();
                        }
                        processed = true;
                    } else {
                        System.out.println("[DEBUG ROLL_DICE] ID mismatch: not current player's turn!");
                    }
                }
            }
            case BUY_PROPERTY -> {
                if (isHost) {
                   String senderId = msg.getSenderId();
                   Player currentPlayer = getCurrentPlayer();
                   if (currentPlayer != null && currentPlayer.getId().equals(senderId)) {
                        boolean success = buyCurrentProperty();
                        if (success) {
                            nextTurn();
                        }
                        processed = true;
                   }
                }
            }
            case DECLINE_PURCHASE -> {
                if (isHost) {
                   String senderId = msg.getSenderId();
                   Player currentPlayer = getCurrentPlayer();
                   if (currentPlayer != null && currentPlayer.getId().equals(senderId)) {
                       Tile t = getCurrentTile();
                       if (t instanceof PropertyTile pt && !pt.isOwned()) {
                           startAuction(pt);
                       }
                       processed = true;
                   }
                }
            }
            case AUCTION_START -> {
                if (!isHost && msg.getPayload() instanceof Auction auction) {
                    propertyManager.setCurrentAuction(auction);
                     fireEvent(new GameEvent(
                        GameEvent.Type.AUCTION_STARTED,
                        null,
                        auction,
                        "Rozpoczęto aukcję: " + auction.getProperty().getCity()
                    ));
                }
            }
            case AUCTION_BID -> {
                if (isHost) {
                    String senderId = msg.getSenderId();
                    Object payload = msg.getPayload();
                    
                    Player bidder = getPlayers().stream()
                        .filter(p -> p.getId().equals(senderId))
                        .findFirst()
                        .orElse(null);
                        
                    if (bidder != null) {
                        if (payload instanceof Integer amount) {
                            placeBid(bidder, amount);
                            processed = true;
                        } else if (payload instanceof String s && "pass".equals(s)) {
                            processed = true;
                        }
                    }
                } else {
                    if (hasActiveAuction()) {
                       String senderId = msg.getSenderId();
                       Object payload = msg.getPayload();
                       Player bidder = getPlayers().stream()
                            .filter(p -> p.getId().equals(senderId))
                            .findFirst()
                            .orElse(null);
                            
                       if (bidder != null && payload instanceof Integer amount) {
                           propertyManager.getCurrentAuction().placeBid(bidder, amount);
                           
                           fireEvent(new GameEvent(
                                GameEvent.Type.AUCTION_BID,
                                bidder,
                                amount,
                                bidder.getUsername() + " licytuje: " + amount
                           ));
                       }
                    }
                }
            }
            case AUCTION_PASS -> {
                if (isHost) {
                     String senderId = msg.getSenderId();
                     Player bidder = getPlayers().stream()
                        .filter(p -> p.getId().equals(senderId))
                        .findFirst()
                        .orElse(null);
                     if (bidder != null) {
                         passAuction(bidder);
                         processed = true;
                     }
                } else {
                    if (hasActiveAuction()) {
                        String senderId = msg.getSenderId();
                        Player bidder = getPlayers().stream()
                            .filter(p -> p.getId().equals(senderId))
                            .findFirst()
                            .orElse(null);
                        
                        if (bidder != null) {
                            propertyManager.getCurrentAuction().pass(bidder);
                             fireEvent(new GameEvent(
                                GameEvent.Type.AUCTION_BID,
                                bidder,
                                "pass",
                                bidder.getUsername() + " pasuje"
                           ));
                        }
                    }
                }
            }
            case AUCTION_ENDED -> {
                if (!isHost) {
                     Object payload = msg.getPayload();
                     if (payload instanceof Auction finalAuction) {
                         propertyManager.setCurrentAuction(finalAuction);
                         
                         if (finalAuction.getHighestBidder() != null && finalAuction.getProperty() != null) {
                             Tile t = board.getTile(finalAuction.getProperty().getPosition());
                             if (t instanceof PropertyTile pt) {
                                 Player localWinner = getPlayers().stream()
                                     .filter(p -> p.getId().equals(finalAuction.getHighestBidder().getId()))
                                     .findFirst().orElse(null);
                                 if (localWinner != null) {
                                     pt.setOwner(localWinner);
                                     localWinner.addProperty(pt);
                                     localWinner.deductMoney(finalAuction.getHighestBid());
                                 }
                             }
                         }
                     }
                     
                     Player winner = propertyManager.getCurrentAuction() != null 
                             ? propertyManager.getCurrentAuction().getHighestBidder() : null;
                     
                     fireEvent(new GameEvent(
                        GameEvent.Type.AUCTION_ENDED,
                        winner,
                        propertyManager.getCurrentAuction(),
                        "Aukcja zakończona"
                    ));
                     propertyManager.setCurrentAuction(null);
                }
            }

            case END_TURN -> {
                if (isHost) {
                    String senderId = msg.getSenderId();
                    Player p = getCurrentPlayer();
                    if (p != null && p.getId().equals(senderId)) {
                        nextTurn();
                        processed = true;
                    }
                }
            }
            case NEXT_TURN -> {
                if (!isHost) {
                     String newCurrentPlayerId = msg.getSenderId();
                     if (turnManager.setCurrentPlayerById(newCurrentPlayerId)) {
                         Player currentPlayer = turnManager.getCurrentPlayer();
                         if (currentPlayer != null) {
                             fireEvent(new GameEvent(
                                GameEvent.Type.TURN_STARTED,
                                currentPlayer,
                                "Tura gracza " + currentPlayer.getUsername()
                            ));
                         }
                     }
                }
            }

            default -> {
            }
        }
        
        if (isHost && processed && msg.requiresAck() && networkManager != null) {
            networkManager.sendAck(msg.getMessageId(), msg.getSenderId());
        }
    }
    
    public List<Player> getPlayers() {
        return turnManager.getPlayers();
    }
    
    public int getRoundNumber() {
        return turnManager.getRoundNumber();
    }
}
