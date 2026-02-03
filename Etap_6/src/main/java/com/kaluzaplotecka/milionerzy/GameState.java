package com.kaluzaplotecka.milionerzy;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;
    Board board;
    List<Player> players;
    int currentPlayerIndex;
    Deque<EventCard> chanceDeck;
    Deque<EventCard> communityChestDeck;
    int roundNumber;
    transient Random rand;
    public static final int PASS_START_REWARD = 200;

    public GameState(Board board, List<Player> players){
        this.board = board;
        this.players = new ArrayList<>(players);
        this.currentPlayerIndex = 0;
        this.chanceDeck = new ArrayDeque<>();
        this.communityChestDeck = new ArrayDeque<>();
        this.roundNumber = 0;
        this.rand = new Random();
    }

    public Board getBoard(){ return board; }

    public Player getCurrentPlayer(){
        if (players.isEmpty()) return null;
        return players.get(currentPlayerIndex);
    }

    public int rollDice(){
        int d1 = rand.nextInt(6) + 1;
        int d2 = rand.nextInt(6) + 1;
        return d1 + d2;
    }

    public void moveCurrentPlayer(){
        Player p = getCurrentPlayer();
        if (p == null) return;
        if (p.isInJail()){
            p.jailTurns++;
            if (p.jailTurns >= 3){
                p.releaseFromJail();
            }
            nextTurn();
            return;
        }

        int steps = rollDice();
        movePlayerBy(p, steps);

        if (p.isBankrupt()){
            handleBankruptcy(p);
        }

        nextTurn();
    }

    /**
     * Move a specific player by given steps, handle passing Start reward and landing effects.
     * This is testable because it accepts explicit steps.
     */
    public void movePlayerBy(Player p, int steps){
        if (p == null) return;
        int oldPos = p.getPosition();
        int boardSize = board.size();
        if (boardSize <= 0) return;

        int rawNew = oldPos + steps;
        boolean passedStart = rawNew >= boardSize;

        p.moveBy(steps, board);

        if (passedStart){
            p.addMoney(PASS_START_REWARD);
        }

        Tile t = board.getTile(p.getPosition());
        if (t != null) t.onLand(this, p);

        if (p.isBankrupt()){
            handleBankruptcy(p);
        }
    }

    public void nextTurn(){
        if (players.isEmpty()) return;
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        if (currentPlayerIndex == 0) roundNumber++;
    }

    public void handleBankruptcy(Player p){
        for (PropertyTile prop : new ArrayList<>(p.ownedProperties)){
            prop.owner = null;
            p.removeProperty(prop);
        }

        int removedIndex = players.indexOf(p);
        if (removedIndex >= 0){
            players.remove(removedIndex);
            if (removedIndex <= currentPlayerIndex && currentPlayerIndex > 0) {
                currentPlayerIndex--;
            }
            if (players.isEmpty()) currentPlayerIndex = 0;
            else currentPlayerIndex = currentPlayerIndex % players.size();
        }
    }

    public boolean isGameOver(){
        return players.size() <= 1;
    }
    
    public Player getWinner(){
        if (players.isEmpty()) return null;
        return players.get(0);
    }

    public Player runGameLoop(int maxRounds){
        if (maxRounds <= 0) maxRounds = Integer.MAX_VALUE;
        int executed = 0;
        while (!isGameOver() && executed < maxRounds){
            moveCurrentPlayer();
            executed++;
        }
        
        return isGameOver() ? getWinner() : null;
    }

    public Player runGameLoop(){
        return runGameLoop(10000);
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
        Player p = getCurrentPlayer();
        if (p == null) return false;
        Tile t = getCurrentTile();
        if (!(t instanceof PropertyTile)) return false;
        PropertyTile prop = (PropertyTile) t;
        if (prop.isOwned()) return false;
        return p.getMoney() >= prop.price;
    }

    /**
     * Attempt to buy the property the current player is on. Returns true when purchase succeeded.
     * UI should call this when the player chooses to buy.
     */
    public boolean buyCurrentProperty(){
        Player p = getCurrentPlayer();
        if (p == null) return false;
        Tile t = getCurrentTile();
        if (!(t instanceof PropertyTile)) return false;
        PropertyTile prop = (PropertyTile) t;
        return prop.buy(p);
    }

    /**
     * Play a single turn for the current player. If `autoBuy` is true, player will automatically
     * buy an unowned property they land on when they have enough money. If false, purchase must be
     * triggered by UI calling `buyCurrentProperty()`.
     * Returns the player who took the turn (may be null if no players).
     */
    public Player playTurn(boolean autoBuy){
        Player p = getCurrentPlayer();
        if (p == null) return null;

        if (p.isInJail()){
            p.jailTurns++;
            if (p.jailTurns >= 3){
                p.releaseFromJail();
            }
            nextTurn();
            return p;
        }

        int steps = rollDice();
        movePlayerBy(p, steps);

        // After moving, inspect tile for purchase opportunity or rent.
        Tile t = board.getTile(p.getPosition());
        if (t instanceof PropertyTile){
            PropertyTile prop = (PropertyTile) t;
            if (!prop.isOwned()){
                if (autoBuy && p.getMoney() >= prop.price){
                    prop.buy(p);
                }
            } else if (prop.owner != p){
                prop.chargeRent(p);
                if (p.isBankrupt()) handleBankruptcy(p);
            }
        } else if (t != null){
            // non-property tiles handle their own effects
            t.onLand(this, p);
        }

        if (p.isBankrupt()){
            handleBankruptcy(p);
        }

        nextTurn();
        return p;
    }

    public Player playTurn(){
        return playTurn(true);
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
    
    private final transient List<GameEventListener> eventListeners = new ArrayList<>();
    private TradeOffer pendingTrade;
    
    public void addEventListener(GameEventListener listener) {
        if (listener != null && !eventListeners.contains(listener)) {
            eventListeners.add(listener);
        }
    }
    
    public void removeEventListener(GameEventListener listener) {
        eventListeners.remove(listener);
    }
    
    /**
     * Wysyła zdarzenie do wszystkich nasłuchujących.
     */
    public void fireEvent(GameEvent event) {
        for (GameEventListener listener : eventListeners) {
            try {
                listener.onGameEvent(event);
            } catch (Exception e) {
                System.err.println("Błąd w listenerze: " + e.getMessage());
            }
        }
    }
    
    // === SYSTEM HANDLU ===
    
    /**
     * Proponuje wymianę innemu graczowi.
     * @return true jeśli oferta została złożona
     */
    public boolean proposeTrade(TradeOffer offer) {
        if (offer == null) return false;
        if (pendingTrade != null) return false;  // jedna oferta naraz
        if (!offer.isValid()) return false;
        
        pendingTrade = offer;
        fireEvent(new GameEvent(
            GameEvent.Type.TRADE_PROPOSED,
            offer.getProposer(),
            offer,
            offer.getDescription()
        ));
        return true;
    }
    
    /**
     * Akceptuje oczekującą ofertę wymiany.
     * @return true jeśli wymiana się powiodła
     */
    public boolean acceptTrade() {
        if (pendingTrade == null) return false;
        
        boolean success = pendingTrade.execute();
        if (success) {
            fireEvent(new GameEvent(
                GameEvent.Type.TRADE_ACCEPTED,
                pendingTrade.getRecipient(),
                pendingTrade,
                pendingTrade.getRecipient().getUsername() + " zaakceptował wymianę"
            ));
        }
        pendingTrade = null;
        return success;
    }
    
    /**
     * Odrzuca oczekującą ofertę wymiany.
     */
    public boolean rejectTrade() {
        if (pendingTrade == null) return false;
        
        pendingTrade.reject();
        fireEvent(new GameEvent(
            GameEvent.Type.TRADE_REJECTED,
            pendingTrade.getRecipient(),
            pendingTrade,
            pendingTrade.getRecipient().getUsername() + " odrzucił wymianę"
        ));
        pendingTrade = null;
        return true;
    }
    
    /**
     * Anuluje oczekującą ofertę wymiany (przez oferenta).
     */
    public boolean cancelTrade() {
        if (pendingTrade == null) return false;
        
        pendingTrade.cancel();
        fireEvent(new GameEvent(
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
    
    // === SYSTEM AUKCJI ===
    
    private Auction currentAuction;
    
    /**
     * Rozpoczyna aukcję nieruchomości.
     * @param property nieruchomość do licytacji (musi być bez właściciela)
     * @return true jeśli aukcja została rozpoczęta
     */
    public boolean startAuction(PropertyTile property) {
        if (property == null) return false;
        if (property.isOwned()) return false;
        if (currentAuction != null && currentAuction.isActive()) return false;
        if (players.size() < 2) return false;
        
        currentAuction = new Auction(property, players);
        fireEvent(new GameEvent(
            GameEvent.Type.AUCTION_STARTED,
            null,
            currentAuction,
            "Rozpoczęto aukcję: " + property.city + " (min. " + currentAuction.getMinimumBid() + " zł)"
        ));
        return true;
    }
    
    /**
     * Gracz składa ofertę w trwającej aukcji.
     * @param bidder gracz licytujący
     * @param amount kwota oferty
     * @return true jeśli oferta została przyjęta
     */
    public boolean placeBid(Player bidder, int amount) {
        if (currentAuction == null || !currentAuction.isActive()) return false;
        
        boolean success = currentAuction.placeBid(bidder, amount);
        if (success) {
            fireEvent(new GameEvent(
                GameEvent.Type.AUCTION_BID,
                bidder,
                amount,
                bidder.getUsername() + " licytuje: " + amount + " zł"
            ));
        }
        return success;
    }
    
    /**
     * Gracz rezygnuje z dalszej licytacji.
     * @param player gracz który pasuje
     */
    public void passAuction(Player player) {
        if (currentAuction == null || !currentAuction.isActive()) return;
        
        currentAuction.pass(player);
        fireEvent(new GameEvent(
            GameEvent.Type.AUCTION_BID,
            player,
            "pass",
            player.getUsername() + " pasuje"
        ));
        
        // Sprawdź czy aukcja się zakończyła
        if (!currentAuction.isActive()) {
            onAuctionEnded();
        }
    }
    
    /**
     * Wymusza zakończenie aukcji.
     */
    public void endAuction() {
        if (currentAuction == null) return;
        
        currentAuction.forceEnd();
        onAuctionEnded();
    }
    
    /**
     * Wywoływane gdy aukcja się kończy.
     */
    private void onAuctionEnded() {
        if (currentAuction == null) return;
        
        Player winner = currentAuction.getHighestBidder();
        int winningBid = currentAuction.getHighestBid();
        PropertyTile property = currentAuction.getProperty();
        
        String message;
        if (winner != null) {
            message = winner.getUsername() + " wygrał aukcję " + property.city + " za " + winningBid + " zł";
        } else {
            message = "Aukcja " + property.city + " zakończona bez zwycięzcy";
        }
        
        fireEvent(new GameEvent(
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
    
    public List<Player> getPlayers() {
        return new ArrayList<>(players);
    }
    
    public int getRoundNumber() {
        return roundNumber;
    }
}
