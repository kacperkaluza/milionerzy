package com.kaluzaplotecka.milionerzy.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.kaluzaplotecka.milionerzy.model.tiles.PropertyTile;

/**
 * Reprezentuje ofertę wymiany między dwoma graczami.
 * Zawiera nieruchomości i pieniądze oferowane przez obie strony.
 */
public class TradeOffer implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum Status { 
        PENDING,    // oczekuje na odpowiedź
        ACCEPTED,   // zaakceptowana
        REJECTED,   // odrzucona
        CANCELLED,  // anulowana przez oferenta
        EXPIRED     // wygasła
    }

    private final String id;
    private final Player proposer;          // gracz proponujący
    private final Player recipient;          // gracz otrzymujący ofertę
    private final List<PropertyTile> offeredProperties;     // nieruchomości oferowane przez proposer
    private final List<PropertyTile> requestedProperties;   // nieruchomości żądane od recipient
    private final int offeredMoney;          // pieniądze oferowane przez proposer
    private final int requestedMoney;        // pieniądze żądane od recipient
    private Status status;
    private final long createdAt;

    public TradeOffer(Player proposer, Player recipient,
                      List<PropertyTile> offeredProperties,
                      List<PropertyTile> requestedProperties,
                      int offeredMoney, int requestedMoney) {
        this.id = java.util.UUID.randomUUID().toString();
        this.proposer = proposer;
        this.recipient = recipient;
        this.offeredProperties = new ArrayList<>(offeredProperties != null ? offeredProperties : List.of());
        this.requestedProperties = new ArrayList<>(requestedProperties != null ? requestedProperties : List.of());
        this.offeredMoney = Math.max(0, offeredMoney);
        this.requestedMoney = Math.max(0, requestedMoney);
        this.status = Status.PENDING;
        this.createdAt = System.currentTimeMillis();
    }
    
    /**
     * Prosta wymiana: tylko pieniądze.
     */
    public static TradeOffer moneyTrade(Player proposer, Player recipient, int offer, int request) {
        return new TradeOffer(proposer, recipient, null, null, offer, request);
    }
    
    /**
     * Prosta wymiana: nieruchomość za pieniądze.
     */
    public static TradeOffer propertyForMoney(Player proposer, Player recipient, 
                                               PropertyTile property, int money) {
        return new TradeOffer(proposer, recipient, List.of(property), null, 0, money);
    }

    /**
     * Sprawdza czy oferta jest prawidłowa (obie strony mają to, co oferują).
     */
    public boolean isValid() {
        // Sprawdź czy proposer ma oferowane nieruchomości
        for (PropertyTile prop : offeredProperties) {
            if (prop.getOwner() != proposer) return false;
        }
        // Sprawdź czy recipient ma żądane nieruchomości
        for (PropertyTile prop : requestedProperties) {
            if (prop.getOwner() != recipient) return false;
        }
        // Sprawdź czy obie strony mają wystarczająco pieniędzy
        if (proposer.getMoney() < offeredMoney) return false;
        if (recipient.getMoney() < requestedMoney) return false;
        
        return true;
    }

    /**
     * Wykonuje wymianę (jeśli jest prawidłowa i oczekująca).
     * @return true jeśli wymiana się powiodła
     */
    public boolean execute() {
        if (status != Status.PENDING) return false;
        if (!isValid()) return false;

        // Transfer nieruchomości od proposer do recipient
        for (PropertyTile prop : offeredProperties) {
            proposer.removeProperty(prop);
            prop.setOwner(recipient);
            recipient.addProperty(prop);
        }
        
        // Transfer nieruchomości od recipient do proposer
        for (PropertyTile prop : requestedProperties) {
            recipient.removeProperty(prop);
            prop.setOwner(proposer);
            proposer.addProperty(prop);
        }

        // Transfer pieniędzy
        if (offeredMoney > 0) {
            proposer.deductMoney(offeredMoney);
            recipient.addMoney(offeredMoney);
        }
        if (requestedMoney > 0) {
            recipient.deductMoney(requestedMoney);
            proposer.addMoney(requestedMoney);
        }

        status = Status.ACCEPTED;
        return true;
    }

    /**
     * Odrzuca ofertę.
     */
    public void reject() {
        if (status == Status.PENDING) {
            status = Status.REJECTED;
        }
    }

    /**
     * Anuluje ofertę (tylko proposer może anulować).
     */
    public void cancel() {
        if (status == Status.PENDING) {
            status = Status.CANCELLED;
        }
    }

    // === GETTERY ===
    
    public String getId() { return id; }
    public Player getProposer() { return proposer; }
    public Player getRecipient() { return recipient; }
    public List<PropertyTile> getOfferedProperties() { return new ArrayList<>(offeredProperties); }
    public List<PropertyTile> getRequestedProperties() { return new ArrayList<>(requestedProperties); }
    public int getOfferedMoney() { return offeredMoney; }
    public int getRequestedMoney() { return requestedMoney; }
    public Status getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
    
    /**
     * Zwraca czytelny opis oferty.
     */
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(proposer.getUsername()).append(" oferuje: ");
        
        if (!offeredProperties.isEmpty()) {
            sb.append(offeredProperties.stream()
                .map(PropertyTile::getCity)
                .reduce((a, b) -> a + ", " + b)
                .orElse(""));
            if (offeredMoney > 0) sb.append(" + ");
        }
        if (offeredMoney > 0) {
            sb.append(offeredMoney).append(" zł");
        }
        if (offeredProperties.isEmpty() && offeredMoney == 0) {
            sb.append("nic");
        }
        
        sb.append(" za: ");
        
        if (!requestedProperties.isEmpty()) {
            sb.append(requestedProperties.stream()
                .map(PropertyTile::getCity)
                .reduce((a, b) -> a + ", " + b)
                .orElse(""));
            if (requestedMoney > 0) sb.append(" + ");
        }
        if (requestedMoney > 0) {
            sb.append(requestedMoney).append(" zł");
        }
        if (requestedProperties.isEmpty() && requestedMoney == 0) {
            sb.append("nic");
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return String.format("TradeOffer[%s -> %s, status=%s]", 
            proposer.getUsername(), recipient.getUsername(), status);
    }
}
