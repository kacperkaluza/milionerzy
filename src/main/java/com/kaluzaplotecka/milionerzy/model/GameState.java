package com.kaluzaplotecka.milionerzy.model;

import com.kaluzaplotecka.milionerzy.network.GameMessage;

import com.kaluzaplotecka.milionerzy.manager.SoundManager;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

import com.kaluzaplotecka.milionerzy.events.GameEvent;
import com.kaluzaplotecka.milionerzy.events.GameEventListener;
import com.kaluzaplotecka.milionerzy.model.cards.EventCard;
import com.kaluzaplotecka.milionerzy.model.tiles.PropertyTile;
import com.kaluzaplotecka.milionerzy.model.tiles.Tile;

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
        if (rand == null) rand = new Random();  // Reinicjalizuj po deserializacji
        int d1 = rand.nextInt(6) + 1;
        int d2 = rand.nextInt(6) + 1;
        return d1 + d2;
    }

    /**
     * Pozwala wstrzyknąć kontrolowany Random dla testów.
     * Użyj new Random(seed) dla deterministycznych wyników.
     */
    public void setRandom(Random rand) {
        this.rand = rand;
    }



    public void moveCurrentPlayer(){
        Player p = getCurrentPlayer();
        if (p == null) return;
        if (p.isInJail()){
            p.incrementJailTurns();
            if (p.getJailTurns() >= 3){
                p.releaseFromJail();
            }
            nextTurn();
            return;
        }

        SoundManager.getInstance().playSound("dice.mp3");

        int steps = rollDice();
        
        fireEvent(new GameEvent(
            GameEvent.Type.DICE_ROLLED,
            null,
            steps,
            "Wylosowano: " + steps
        ));
        
        movePlayerBy(p, steps);

        if (p.isBankrupt()){
            handleBankruptcy(p);
            return;
        }

        // Sprawdź czy gracz wylądował na nieruchomości do kupienia
        // Jeśli tak, NIE zmieniaj tury - czekaj na decyzję gracza
        Tile currentTile = getCurrentTile();
        if (currentTile instanceof PropertyTile pt && !pt.isOwned()) {
            // Gracz musi podjąć decyzję - nie zmieniaj tury automatycznie
            return;
        }
        
        // Jeśli nie ma nic do zrobienia, zmień turę
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
        
        fireEvent(new GameEvent(
            GameEvent.Type.PLAYER_MOVED,
            p,
            steps, // Pass steps as data to know how many fields to animate if needed
            p.getUsername() + " przeszedł " + steps + " pól"
        ));

        if (passedStart){
            p.addMoney(PASS_START_REWARD);
            fireEvent(new GameEvent(
                GameEvent.Type.MONEY_CHANGED,
                p,
                PASS_START_REWARD,
                p.getUsername() + " przeszedł Start (+200)"
            ));
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
        
        fireEvent(new GameEvent(
            GameEvent.Type.TURN_STARTED,
            getCurrentPlayer(),
            "Tura gracza " + getCurrentPlayer().getUsername()
        ));
    }

    public void handleBankruptcy(Player p){
        for (PropertyTile prop : new ArrayList<>(p.getOwnedProperties())){
            prop.setOwner(null);
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
        return p.getMoney() >= prop.getPrice();
    }

    /**
     * Attempt to buy the property the current player is on. Returns true when purchase succeeded.
     * UI should call this when the player chooses to buy.
     */
    public boolean buyCurrentProperty(){
        Player p = getCurrentPlayer();
        if (p == null) return false;
        Tile t = getCurrentTile();
        if (!(t instanceof PropertyTile)) {
            return false;
        }
        PropertyTile prop = (PropertyTile) t;
        boolean result = prop.buy(p);
        return result;
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
    private TradeOffer pendingTrade;
    
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
        
        currentAuction = new Auction(property, players, property.getPrice());
        fireEvent(new GameEvent(
            GameEvent.Type.AUCTION_STARTED,
            null,
            currentAuction,
            "Rozpoczęto aukcję: " + property.getCity() + " (min. " + currentAuction.getMinimumBid() + " zł)"
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
            message = winner.getUsername() + " wygrał aukcję " + property.getCity() + " za " + winningBid + " zł";
        } else {
            message = "Aukcja " + property.getCity() + " zakończona bez zwycięzcy";
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
    
    // === OBSŁUGA SIECI ===
    
    /**
     * Przetwarza wiadomość sieciową i aktualizuje stan gry.
     * Używane zarówno przez klienta (aktualizacja stanu) jak i hosta (akcje graczy).
     */
    public void processNetworkMessage(GameMessage msg, boolean isHost) {
        processNetworkMessage(msg, isHost, null);
    }
    
    /**
     * Przetwarza wiadomość sieciową z obsługą ACK.
     * @param msg wiadomość do przetworzenia
     * @param isHost czy przetwarzane przez hosta
     * @param networkManager manager sieci do wysyłania ACK (może być null)
     */
    public void processNetworkMessage(GameMessage msg, boolean isHost, 
                                       com.kaluzaplotecka.milionerzy.network.NetworkManager networkManager) {
        if (msg == null) return;
        
        boolean processed = false;  // Flaga do śledzenia czy wiadomość została przetworzona
        
        switch (msg.getType()) {
            case ROLL_DICE -> {
                if (isHost) { // Tylko host może wykonać logikę gry na żądanie gracza
                    String senderId = msg.getSenderId();
                    Player p = getCurrentPlayer();
                    if (p != null && p.getId().equals(senderId)) {
                        moveCurrentPlayer();
                        processed = true;
                    }
                }
            }
            case BUY_PROPERTY -> {
                if (isHost) {
                   String senderId = msg.getSenderId();
                   Player currentPlayer = getCurrentPlayer();
                   // Verify if sender is current player (compare by ID, not reference)
                   if (currentPlayer != null && currentPlayer.getId().equals(senderId)) {
                        boolean success = buyCurrentProperty();
                        if (success) {
                            fireEvent(new GameEvent(
                                GameEvent.Type.PROPERTY_BOUGHT,
                                currentPlayer,
                                getCurrentTile(),
                                currentPlayer.getUsername() + " kupił " + ((PropertyTile)getCurrentTile()).getCity()
                            ));
                            // Po zakupie zmień turę
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
                   // Verify if sender is current player (compare by ID, not reference)
                   if (currentPlayer != null && currentPlayer.getId().equals(senderId)) {
                       Tile t = getCurrentTile();
                       if (t instanceof PropertyTile pt && !pt.isOwned()) {
                           startAuction(pt);
                       }
                       processed = true;
                   }
                }
            }
            // Aukcje - Klient odbiera aktualizacje
            case AUCTION_START -> {
                if (!isHost && msg.getPayload() instanceof Auction auction) {
                    this.currentAuction = auction;
                    // Fire event locally to update UI
                     fireEvent(new GameEvent(
                        GameEvent.Type.AUCTION_STARTED,
                        null,
                        currentAuction,
                        "Rozpoczęto aukcję: " + currentAuction.getProperty().getCity()
                    ));
                }
            }
            case AUCTION_BID -> {
                if (isHost) {
                    // Host odbiera ofertę od klienta
                    String senderId = msg.getSenderId();
                    Object payload = msg.getPayload();
                    
                    Player bidder = players.stream()
                        .filter(p -> p.getId().equals(senderId))
                        .findFirst()
                        .orElse(null);
                        
                    if (bidder != null) {
                        if (payload instanceof Integer amount) {
                            placeBid(bidder, amount);
                            processed = true;
                        } else if (payload instanceof String s && "pass".equals(s)) { // Can be "pass" via message logic? OR separate AUCTION_PASS type
                            // Actually we mapped pass to AUCTION_PASS in GameMessage, so check logic.
                            // But maybe we receive raw AUCTION_BID with string "pass" from older clients? Safe to handle.
                            processed = true;
                        }
                    }
                } else {
                    // Klient odbiera aktualizację bidding
                    // Payload powinien zawierać zaktualizowaną aukcję lub dane o bidzie.
                    // Ale NetworkGameEventListener wysyła to co było w evencie.
                    // Event AUCTION_BID ma payload: kwota (int) LUB "pass" (String).
                    // Ale to nie aktualizuje stanu lokalnego `currentAuction`.
                    // Klient musi zaktualizować swój obiekt Auction.
                    // Najlepiej gdyby Host wysyłał zaktualizowany obiekt Auction przy każdej zmianie.
                    // Ale to dużo danych.
                    // Spróbujmy zaktualizować lokalny obiekt na podstawie danych.
                    if (currentAuction != null) {
                       String senderId = msg.getSenderId();
                       Object payload = msg.getPayload();
                       Player bidder = players.stream()
                            .filter(p -> p.getId().equals(senderId))
                            .findFirst()
                            .orElse(null);
                            
                       if (bidder != null && payload instanceof Integer amount) {
                           // Force update local state visually (logic validation skipped on client)
                           currentAuction.placeBid(bidder, amount);
                           
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
                     Player bidder = players.stream()
                        .filter(p -> p.getId().equals(senderId))
                        .findFirst()
                        .orElse(null);
                     if (bidder != null) {
                         passAuction(bidder);
                         processed = true;
                     }
                } else {
                    // Klient odbiera pass
                    if (currentAuction != null) {
                        String senderId = msg.getSenderId();
                        Player bidder = players.stream()
                            .filter(p -> p.getId().equals(senderId))
                            .findFirst()
                            .orElse(null);
                        
                        if (bidder != null) {
                            currentAuction.pass(bidder);
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
                if (!isHost) { // Klient kończy aukcję
                     Object payload = msg.getPayload();
                     if (payload instanceof Auction finalAuction) {
                         // Update final state properties (winner etc)
                         // Wait, property ownership needs to be updated too on client!
                         // Sync property owner?
                         this.currentAuction = finalAuction;
                         
                         // Manually update property ownership on client
                         if (finalAuction.getHighestBidder() != null && finalAuction.getProperty() != null) {
                             // Find local property tile
                             Tile t = board.getTile(finalAuction.getProperty().getPosition());
                             if (t instanceof PropertyTile pt) {
                                 // Find local player object
                                 Player localWinner = players.stream()
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
                     
                     // Helper to fire end event
                     Player winner = currentAuction != null ? currentAuction.getHighestBidder() : null;
                     
                     fireEvent(new GameEvent(
                        GameEvent.Type.AUCTION_ENDED,
                        winner,
                        currentAuction,
                        "Aukcja zakończona"
                    ));
                     this.currentAuction = null;
                }
            }

            case END_TURN -> {
                // Gracz sygnalizuje koniec tury
                if (isHost) {
                    String senderId = msg.getSenderId();
                    Player p = getCurrentPlayer();
                    // Tylko aktualny gracz może zakończyć turę
                    if (p != null && p.getId().equals(senderId)) {
                        nextTurn();
                        processed = true;
                    }
                }
            }
            case NEXT_TURN -> {
                // Klient otrzymuje informację o zmianie tury
                if (!isHost) {
                     String newCurrentPlayerId = msg.getSenderId();
                     // Aktualizujemy indeks gracza
                     for (int i = 0; i < players.size(); i++) {
                         if (players.get(i).getId().equals(newCurrentPlayerId)) {
                             currentPlayerIndex = i;
                             break;
                         }
                     }
                     
                     // Informujemy UI
                     fireEvent(new GameEvent(
                        GameEvent.Type.TURN_STARTED,
                        getCurrentPlayer(),
                        "Tura gracza " + getCurrentPlayer().getUsername()
                    ));
                }
            }

            default -> {
                 // Handle or ignore other message types
            }
        }
        
        // Wyślij ACK jeśli wiadomość została przetworzona i wymaga potwierdzenia
        if (isHost && processed && msg.requiresAck() && networkManager != null) {
            networkManager.sendAck(msg.getMessageId(), msg.getSenderId());
        }
    }
    
    public List<Player> getPlayers() {
        return new ArrayList<>(players);
    }
    
    public int getRoundNumber() {
        return roundNumber;
    }
}
