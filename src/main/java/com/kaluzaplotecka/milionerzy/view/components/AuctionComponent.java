package com.kaluzaplotecka.milionerzy.view.components;

import com.kaluzaplotecka.milionerzy.model.Auction;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.effect.DropShadow;

import java.util.function.Consumer;

/**
 * Komponent interfejsu aukcji nieruchomości.
 * 
 * <p>Wyświetla aktualną cenę, lidera i przyciski do licytacji.
 * 
 * @see com.kaluzaplotecka.milionerzy.model.Auction
 */
public class AuctionComponent extends StackPane {
    
    private final Label titleLabel;
    private final Label priceLabel;
    private final Label leaderLabel;
    private final Button bid10Button;
    private final Button bid100Button;
    private final Button passButton;
    
    private Auction currentAuction;
    
    // Callbacks for actions
    private Consumer<Integer> onBid;
    private Runnable onPass;

    public AuctionComponent() {
        // Semi-transparent background
        this.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7);");
        this.setVisible(false);
        this.setAlignment(Pos.CENTER);
        
        // Card container
        VBox card = new VBox(20);
        card.setMaxSize(400, 300);
        card.setAlignment(Pos.CENTER);
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 15;" +
            "-fx-padding: 30;"
        );
        card.setEffect(new DropShadow(20, Color.BLACK));
        
        // Header
        titleLabel = new Label("LICYTACJA");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.web("#2c3e50"));
        
        // Price
        priceLabel = new Label("0 zł");
        priceLabel.setFont(Font.font("System", FontWeight.BOLD, 48));
        priceLabel.setTextFill(Color.web("#27ae60"));
        
        // Leader
        leaderLabel = new Label("Brak ofert");
        leaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 18));
        leaderLabel.setTextFill(Color.web("#7f8c8d"));
        
        // Controls
        HBox controls = new HBox(15);
        controls.setAlignment(Pos.CENTER);
        
        bid10Button = createButton("Podbij +10", "#3498db");
        bid100Button = createButton("Podbij +100", "#9b59b6");
        passButton = createButton("Pas", "#e74c3c");
        
        bid10Button.setOnAction(e -> {
            if (onBid != null && currentAuction != null) {
                // Logic based on previous implementation
                int base = Math.max(currentAuction.getHighestBid(), currentAuction.getMinimumBid());
                if (currentAuction.getHighestBid() == 0) base = currentAuction.getMinimumBid(); 
                else base = currentAuction.getHighestBid() + 10;
                
                onBid.accept(base); 
            }
        });
        
        bid100Button.setOnAction(e -> {
             if (onBid != null && currentAuction != null) {
                int base = Math.max(currentAuction.getHighestBid(), currentAuction.getMinimumBid());
                if (currentAuction.getHighestBid() == 0) base = currentAuction.getMinimumBid(); 
                else base = currentAuction.getHighestBid() + 100;
                
                onBid.accept(base);
             }
        });
        
        passButton.setOnAction(e -> {
            if (onPass != null) onPass.run();
        });
        
        controls.getChildren().addAll(bid10Button, bid100Button, passButton);
        
        card.getChildren().addAll(titleLabel, priceLabel, leaderLabel, controls);
        this.getChildren().add(card);
    }
    
    private Button createButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle(
            "-fx-background-color: " + color + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 14px;" +
            "-fx-padding: 10 20;" +
            "-fx-background-radius: 5;" +
            "-fx-cursor: hand;"
        );
        return btn;
    }
    
    public void show(Auction auction) {
        this.currentAuction = auction;
        updateUI();
        this.setVisible(true);
    }
    
    public void hide() {
        this.setVisible(false);
        this.currentAuction = null;
    }
    
    public void updateAuction(Auction auction) {
        this.currentAuction = auction;
        updateUI();
    }
    
    private String localPlayerId;

    public void setLocalPlayerId(String playerId) {
        this.localPlayerId = playerId;
    }

    private void updateUI() {
        if (currentAuction == null) return;
        
        titleLabel.setText("LICYTACJA: " + currentAuction.getProperty().getCity());
        priceLabel.setText(currentAuction.getHighestBid() + " zł");
        
        boolean isLeader = false;
        if (currentAuction.getHighestBidder() != null) {
            leaderLabel.setText("Prowadzi: " + currentAuction.getHighestBidder().getUsername());
            if (localPlayerId != null && currentAuction.getHighestBidder().getId().equals(localPlayerId)) {
                isLeader = true;
            }
        } else {
            leaderLabel.setText("Cena wywoławcza: " + currentAuction.getMinimumBid() + " zł");
        }
        
        // Disable bid buttons if I am the leader
        bid10Button.setDisable(isLeader);
        bid100Button.setDisable(isLeader);
    }
    
    public void setOnBid(Consumer<Integer> onBid) {
        this.onBid = onBid;
    }
    
    public void setOnPass(Runnable onPass) {
        this.onPass = onPass;
    }
}
