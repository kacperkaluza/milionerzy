package com.kaluzaplotecka.milionerzy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Reprezentuje aukcję nieruchomości.
 * Aukcja rozpoczyna się gdy gracz rezygnuje z zakupu nieruchomości.
 * Wszyscy gracze mogą licytować, najwyższa oferta wygrywa.
 */
public class Auction implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum Status {
        ACTIVE,     // aukcja trwa
        ENDED,      // aukcja zakończona (ktoś wygrał)
        CANCELLED   // aukcja anulowana (brak ofert)
    }
    
    private final String id;
    private final PropertyTile property;
    private final List<Player> participants;
    private final List<Player> passedPlayers;  // gracze którzy spasowali
    private Player highestBidder;
    private int highestBid;
    private int minimumBid;
    private Status status;
    private final long startedAt;
    
    public static final int DEFAULT_MINIMUM_BID = 10;
    public static final int MINIMUM_INCREMENT = 10;

    public Auction(PropertyTile property, List<Player> participants) {
        this(property, participants, DEFAULT_MINIMUM_BID);
    }
    
    public Auction(PropertyTile property, List<Player> participants, int minimumBid) {
        this.id = java.util.UUID.randomUUID().toString();
        this.property = property;
        this.participants = new ArrayList<>(participants);
        this.passedPlayers = new ArrayList<>();
        this.highestBidder = null;
        this.highestBid = 0;
        this.minimumBid = Math.max(1, minimumBid);
        this.status = Status.ACTIVE;
        this.startedAt = System.currentTimeMillis();
    }

    /**
     * Złóż ofertę w aukcji.
     * @param bidder gracz licytujący
     * @param amount kwota oferty
     * @return true jeśli oferta została przyjęta
     */
    public boolean placeBid(Player bidder, int amount) {
        if (status != Status.ACTIVE) return false;
        if (!participants.contains(bidder)) return false;
        if (passedPlayers.contains(bidder)) return false;
        if (bidder.getMoney() < amount) return false;
        
        // Pierwsza oferta musi być >= minimumBid
        if (highestBid == 0 && amount < minimumBid) return false;
        
        // Kolejne oferty muszą być wyższe o co najmniej MINIMUM_INCREMENT
        if (highestBid > 0 && amount < highestBid + MINIMUM_INCREMENT) return false;

        highestBidder = bidder;
        highestBid = amount;
        return true;
    }

    /**
     * Gracz rezygnuje z dalszej licytacji.
     * @param player gracz który pasuje
     */
    public void pass(Player player) {
        if (status != Status.ACTIVE) return;
        if (!participants.contains(player)) return;
        if (!passedPlayers.contains(player)) {
            passedPlayers.add(player);
        }
        
        // Sprawdź czy aukcja powinna się zakończyć
        checkAuctionEnd();
    }

    /**
     * Sprawdza czy aukcja powinna się zakończyć.
     * Kończy się gdy wszyscy spasowali lub został jeden aktywny gracz.
     */
    private void checkAuctionEnd() {
        int activeParticipants = participants.size() - passedPlayers.size();
        
        if (activeParticipants <= 1 && highestBidder != null) {
            // Został jeden gracz z ofertą - wygrywa
            conclude();
        } else if (activeParticipants == 0) {
            // Wszyscy spasowali, brak ofert
            cancel();
        }
    }

    /**
     * Wymusza zakończenie aukcji (np. timeout).
     */
    public void forceEnd() {
        if (status != Status.ACTIVE) return;
        
        if (highestBidder != null) {
            conclude();
        } else {
            cancel();
        }
    }

    /**
     * Kończy aukcję - zwycięzca kupuje nieruchomość.
     */
    private void conclude() {
        if (status != Status.ACTIVE) return;
        
        status = Status.ENDED;
        
        if (highestBidder != null && highestBid > 0) {
            highestBidder.deductMoney(highestBid);
            property.owner = highestBidder;
            highestBidder.addProperty(property);
        }
    }

    /**
     * Anuluje aukcję (brak zwycięzcy).
     */
    private void cancel() {
        if (status != Status.ACTIVE) return;
        status = Status.CANCELLED;
    }

    /**
     * Zwraca minimalną akceptowalną ofertę.
     */
    public int getMinimumAcceptableBid() {
        if (highestBid == 0) {
            return minimumBid;
        }
        return highestBid + MINIMUM_INCREMENT;
    }

    /**
     * Sprawdza czy gracz może jeszcze licytować.
     */
    public boolean canBid(Player player) {
        if (status != Status.ACTIVE) return false;
        if (!participants.contains(player)) return false;
        if (passedPlayers.contains(player)) return false;
        return player.getMoney() >= getMinimumAcceptableBid();
    }

    /**
     * Zwraca listę graczy którzy jeszcze mogą licytować.
     */
    public List<Player> getActiveBidders() {
        List<Player> active = new ArrayList<>();
        for (Player p : participants) {
            if (!passedPlayers.contains(p)) {
                active.add(p);
            }
        }
        return active;
    }

    // === GETTERY ===
    
    public String getId() { return id; }
    public PropertyTile getProperty() { return property; }
    public List<Player> getParticipants() { return new ArrayList<>(participants); }
    public List<Player> getPassedPlayers() { return new ArrayList<>(passedPlayers); }
    public Player getHighestBidder() { return highestBidder; }
    public int getHighestBid() { return highestBid; }
    public int getMinimumBid() { return minimumBid; }
    public Status getStatus() { return status; }
    public long getStartedAt() { return startedAt; }
    public boolean isActive() { return status == Status.ACTIVE; }
    
    /**
     * Zwraca czytelny opis stanu aukcji.
     */
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("Aukcja: ").append(property.city);
        sb.append(" (cena bazowa: ").append(property.price).append(" zł)");
        
        if (highestBidder != null) {
            sb.append("\nNajwyższa oferta: ").append(highestBid).append(" zł");
            sb.append(" (").append(highestBidder.getUsername()).append(")");
        } else {
            sb.append("\nBrak ofert. Minimalna: ").append(minimumBid).append(" zł");
        }
        
        sb.append("\nAktywni licytujący: ").append(getActiveBidders().size());
        sb.append("/").append(participants.size());
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return String.format("Auction[%s, bid=%d by %s, status=%s]",
            property.city,
            highestBid,
            highestBidder != null ? highestBidder.getUsername() : "none",
            status);
    }
}
