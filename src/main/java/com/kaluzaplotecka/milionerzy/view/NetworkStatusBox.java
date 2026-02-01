package com.kaluzaplotecka.milionerzy.view;

import javafx.animation.FadeTransition;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * Komponent UI wyświetlający status komunikacji sieciowej.
 * Pokazuje informacje o wysyłanych wiadomościach, potwierdzeniach i błędach.
 */
public class NetworkStatusBox extends VBox {
    
    private static final int MAX_MESSAGES = 5;
    private static final Duration FADE_DURATION = Duration.seconds(3);
    private static final Duration SUCCESS_VISIBLE_DURATION = Duration.seconds(2);
    
    public enum StatusType {
        SENDING("🔄", "#f39c12", "Wysyłanie"),      // żółty
        CONFIRMED("✅", "#27ae60", "Potwierdzone"), // zielony
        ERROR("❌", "#e74c3c", "Błąd"),             // czerwony
        TIMEOUT("⏱️", "#e74c3c", "Timeout"),        // czerwony
        RETRYING("🔁", "#f39c12", "Ponowna próba")  // żółty
        ;
        
        final String icon;
        final String color;
        final String defaultLabel;
        
        StatusType(String icon, String color, String defaultLabel) {
            this.icon = icon;
            this.color = color;
            this.defaultLabel = defaultLabel;
        }
    }
    
    private final VBox messageContainer;
    
    public NetworkStatusBox() {
        setAlignment(Pos.TOP_RIGHT);
        setSpacing(5);
        setPadding(new Insets(10));
        setPrefWidth(220);
        setMaxWidth(220);
        setMinWidth(180);
        
        // Półprzezroczyste tło z efektem glassmorphism
        setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.85);" +
            "-fx-background-radius: 12;" +
            "-fx-border-radius: 12;" +
            "-fx-border-color: rgba(0, 0, 0, 0.1);" +
            "-fx-border-width: 1;"
        );
        
        // Cień
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.15));
        shadow.setRadius(10);
        shadow.setOffsetY(3);
        setEffect(shadow);
        
        // Nagłówek
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label headerIcon = new Label("📡");
        headerIcon.setFont(Font.font(14));
        
        Label headerLabel = new Label("Status sieci");
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        headerLabel.setTextFill(Color.web("#2d3436"));
        
        header.getChildren().addAll(headerIcon, headerLabel);
        
        // Kontener na wiadomości
        messageContainer = new VBox(4);
        messageContainer.setAlignment(Pos.TOP_LEFT);
        
        getChildren().addAll(header, messageContainer);
        
        // Domyślnie ukryty gdy brak wiadomości
        setVisible(false);
        setManaged(false);
    }
    
    public NetworkStatusBox(com.kaluzaplotecka.milionerzy.network.NetworkManager networkManager) {
        this();
        if (networkManager != null) {
            networkManager.setSendingCallback(msg -> 
                javafx.application.Platform.runLater(() -> showSending(msg.getType().toString())));
                
            networkManager.setAckCallback(msg -> 
                javafx.application.Platform.runLater(() -> showConfirmed(msg.getType().toString())));
                
            networkManager.setNackCallback(msg -> 
                javafx.application.Platform.runLater(() -> showError(msg.getType().toString(), "Błąd wysyłania"))); // Use NACK payload if available?
                
            networkManager.setTimeoutCallback(msg -> 
                javafx.application.Platform.runLater(() -> showTimeout(msg.getType().toString())));
        }
    }
    
    /**
     * Wyświetla status o podanym typie i wiadomości.
     */
    public void showStatus(StatusType type, String message) {
        // Upewnij się, że box jest widoczny
        setVisible(true);
        setManaged(true);
        
        // Utwórz nowy element statusu
        HBox statusItem = createStatusItem(type, message);
        
        // Dodaj na początek listy
        messageContainer.getChildren().add(0, statusItem);
        
        // Ogranicz liczbę wiadomości
        while (messageContainer.getChildren().size() > MAX_MESSAGES) {
            messageContainer.getChildren().remove(messageContainer.getChildren().size() - 1);
        }
        
        // Dla sukcesu - automatyczne usunięcie po czasie
        if (type == StatusType.CONFIRMED) {
            scheduleRemoval(statusItem);
        }
    }
    
    /**
     * Pokazuje status wysyłania akcji.
     */
    public void showSending(String actionName) {
        showStatus(StatusType.SENDING, actionName + "...");
    }
    
    /**
     * Pokazuje status potwierdzenia akcji.
     */
    public void showConfirmed(String actionName) {
        // Usuń poprzedni status "Wysyłanie" dla tej akcji
        removeStatusByMessage(actionName + "...");
        showStatus(StatusType.CONFIRMED, actionName);
    }
    
    /**
     * Pokazuje status błędu.
     */
    public void showError(String actionName, String reason) {
        removeStatusByMessage(actionName + "...");
        String message = actionName + ": " + reason;
        showStatus(StatusType.ERROR, message);
    }
    
    /**
     * Pokazuje status timeout.
     */
    public void showTimeout(String actionName) {
        removeStatusByMessage(actionName + "...");
        showStatus(StatusType.TIMEOUT, actionName + " - brak odpowiedzi");
    }
    
    /**
     * Pokazuje status ponownej próby.
     */
    public void showRetrying(String actionName, int attempt) {
        removeStatusByMessage(actionName + "...");
        showStatus(StatusType.RETRYING, actionName + " (próba " + attempt + ")");
    }
    
    /**
     * Tworzy pojedynczy element statusu.
     */
    private HBox createStatusItem(StatusType type, String message) {
        HBox item = new HBox(6);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(4, 8, 4, 8));
        item.setStyle(
            "-fx-background-color: " + type.color + "15;" +  // 15 = ~10% opacity
            "-fx-background-radius: 6;"
        );
        
        Label icon = new Label(type.icon);
        icon.setFont(Font.font(12));
        
        // Animacja obracania dla "Wysyłanie"
        if (type == StatusType.SENDING || type == StatusType.RETRYING) {
            RotateTransition rotate = new RotateTransition(Duration.seconds(1), icon);
            rotate.setByAngle(360);
            rotate.setCycleCount(Timeline.INDEFINITE);
            rotate.play();
        }
        
        Label text = new Label(message);
        text.setFont(Font.font("System", FontWeight.NORMAL, 11));
        text.setTextFill(Color.web(type.color));
        text.setWrapText(true);
        text.setMaxWidth(180);
        
        item.getChildren().addAll(icon, text);
        item.setUserData(message);  // Zapisz wiadomość do późniejszego wyszukiwania
        
        return item;
    }
    
    /**
     * Planuje automatyczne usunięcie elementu po czasie.
     */
    private void scheduleRemoval(HBox statusItem) {
        // Poczekaj, potem fade out i usuń
        Timeline delay = new Timeline(new KeyFrame(SUCCESS_VISIBLE_DURATION, e -> {
            FadeTransition fade = new FadeTransition(FADE_DURATION, statusItem);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            fade.setOnFinished(event -> {
                messageContainer.getChildren().remove(statusItem);
                checkVisibility();
            });
            fade.play();
        }));
        delay.play();
    }
    
    /**
     * Usuwa status o podanej wiadomości.
     */
    private void removeStatusByMessage(String message) {
        messageContainer.getChildren().removeIf(node -> {
            if (node instanceof HBox && node.getUserData() != null) {
                return message.equals(node.getUserData());
            }
            return false;
        });
        checkVisibility();
    }
    
    /**
     * Sprawdza czy box powinien być widoczny.
     */
    private void checkVisibility() {
        boolean hasMessages = !messageContainer.getChildren().isEmpty();
        setVisible(hasMessages);
        setManaged(hasMessages);
    }
    
    /**
     * Czyści wszystkie wiadomości.
     */
    public void clear() {
        messageContainer.getChildren().clear();
        checkVisibility();
    }
}
