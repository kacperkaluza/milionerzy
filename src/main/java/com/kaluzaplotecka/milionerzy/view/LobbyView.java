package com.kaluzaplotecka.milionerzy.view;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.kaluzaplotecka.milionerzy.model.GameState;
import com.kaluzaplotecka.milionerzy.model.Player;
import com.kaluzaplotecka.milionerzy.network.GameMessage;
import com.kaluzaplotecka.milionerzy.network.NetworkManager;


/**
 * Widok lobby do tworzenia i dołączania do gry wieloosobowej.
 * Obsługuje tryb HOST (tworzenie pokoju) i CLIENT (dołączanie).
 */
public class LobbyView {

    private final Stage stage;
    private final boolean isHost;
    private final Runnable onBack;
    
    private NetworkManager networkManager;
    private String roomCode;
    private String playerName;
    private String playerId;
    private GameState loadedGameState; // Wczytany stan gry (do hostowania zapisu)
    private Thread connectionThread; // Thread for client connection
    
    // UI Components
    private Label playersCountLabel;
    private VBox playersList;
    private Button startGameBtn;
    private Label statusLabel;
    private TextArea chatArea;
    private TextField chatInput;
    
    // Lista graczy w lobby
    private final List<PlayerInfo> players = new ArrayList<>();
    
    // Style
    private static final String CARD_STYLE = 
        "-fx-background-color: white; " +
        "-fx-background-radius: 16; " +
        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 20, 0, 0, 5);";
    
    private static final String PRIMARY_BUTTON_STYLE = 
        "-fx-background-color: linear-gradient(to right, #667eea, #764ba2); " +
        "-fx-text-fill: white; " +
        "-fx-border-radius: 12; " +
        "-fx-background-radius: 12; " +
        "-fx-padding: 12 30; " +
        "-fx-font-size: 16px; " +
        "-fx-font-weight: bold; " +
        "-fx-cursor: hand;";
    
    private static final String PRIMARY_BUTTON_HOVER_STYLE = 
        "-fx-background-color: linear-gradient(to right, #764ba2, #667eea); " +
        "-fx-text-fill: white; " +
        "-fx-border-radius: 12; " +
        "-fx-background-radius: 12; " +
        "-fx-padding: 12 30; " +
        "-fx-font-size: 16px; " +
        "-fx-font-weight: bold; " +
        "-fx-cursor: hand;";
    
    private static final String DISABLED_BUTTON_STYLE = 
        "-fx-background-color: #bdc3c7; " +
        "-fx-text-fill: white; " +
        "-fx-border-radius: 12; " +
        "-fx-background-radius: 12; " +
        "-fx-padding: 12 30; " +
        "-fx-font-size: 16px; " +
        "-fx-font-weight: bold;";
    
    private static final String SECONDARY_BUTTON_STYLE = 
        "-fx-background-color: transparent; " +
        "-fx-text-fill: #667eea; " +
        "-fx-border-color: #667eea; " +
        "-fx-border-width: 2; " +
        "-fx-border-radius: 12; " +
        "-fx-background-radius: 12; " +
        "-fx-padding: 10 24; " +
        "-fx-font-size: 14px; " +
        "-fx-font-weight: bold; " +
        "-fx-cursor: hand;";
    
    private static final String INPUT_STYLE = 
        "-fx-background-color: #f8f9fa; " +
        "-fx-border-color: #e9ecef; " +
        "-fx-border-width: 2; " +
        "-fx-border-radius: 8; " +
        "-fx-background-radius: 8; " +
        "-fx-padding: 10 15; " +
        "-fx-font-size: 14px;";
    
    private static final String INPUT_FOCUS_STYLE = 
        "-fx-background-color: white; " +
        "-fx-border-color: #667eea; " +
        "-fx-border-width: 2; " +
        "-fx-border-radius: 8; " +
        "-fx-background-radius: 8; " +
        "-fx-padding: 10 15; " +
        "-fx-font-size: 14px;";


    private String hostAddress; // Added field to store host address

    /**
     * Konstruktor dla trybu HOST (tworzenie pokoju).
     */
    public LobbyView(Stage stage, String playerName, Runnable onBack) {
        this.stage = stage;
        this.isHost = true;
        this.playerName = playerName;
        this.onBack = onBack;
        this.playerId = UUID.randomUUID().toString().substring(0, 8);
        this.roomCode = generateRoomCode();
    }
    
    /**
     * Konstruktor dla trybu CLIENT (dołączanie).
     */
    public LobbyView(Stage stage, String playerName, String roomCode, String hostAddress, Runnable onBack) {
        this.stage = stage;
        this.isHost = false;
        this.playerName = playerName;
        this.roomCode = roomCode;
        this.hostAddress = hostAddress; // Store the provided host address
        this.onBack = onBack;
        this.playerId = UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * Konstruktor dla trybu HOST z wczytanym stanem gry (hostowanie zapisanej gry).
     */
    public LobbyView(Stage stage, String playerName, GameState loadedState, Runnable onBack) {
        this.stage = stage;
        this.isHost = true;
        this.playerName = playerName;
        this.onBack = onBack;
        this.playerId = UUID.randomUUID().toString().substring(0, 8);
        this.roomCode = generateRoomCode();
        this.loadedGameState = loadedState;
    }
    
    public void show() {
        VBox root = createMainLayout();
        
        Scene scene = new Scene(root, 1440, 900);
        stage.setScene(scene);
        stage.setTitle(isHost ? "Milionerzy - Tworzenie Gry" : "Milionerzy - Lobby");
        
        // Inicjalizacja sieci
        initializeNetwork();
        
        // Animacja wejścia
        playEntranceAnimation(root);
    }
    
    private VBox createMainLayout() {
        VBox root = new VBox();
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #f5f7fa, #e4e8f0);");
        root.setPadding(new Insets(30));
        
        // Nagłówek z przyciskiem powrotu
        HBox header = createHeader();
        
        // Główna zawartość
        HBox content = new HBox(40);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(40, 60, 40, 60));
        VBox.setVgrow(content, Priority.ALWAYS);
        
        // Lewa strona - lista graczy
        VBox playersCard = createPlayersCard();
        
        // Prawa strona - informacje o pokoju i chat
        VBox infoSection = createInfoSection();
        
        content.getChildren().addAll(playersCard, infoSection);
        
        // Footer z przyciskiem rozpoczęcia gry
        HBox footer = createFooter();
        
        root.getChildren().addAll(header, content, footer);
        
        // Dekoracje w tle
        addDecorations(root);
        
        return root;
    }
    
    private HBox createHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 20, 10, 20));
        
        // Przycisk powrotu
        Button backBtn = new Button("← Powrót");
        backBtn.setStyle(SECONDARY_BUTTON_STYLE);
        backBtn.setOnMouseEntered(e -> backBtn.setStyle(SECONDARY_BUTTON_STYLE + "-fx-background-color: rgba(102, 126, 234, 0.1);"));
        backBtn.setOnMouseExited(e -> backBtn.setStyle(SECONDARY_BUTTON_STYLE));
        backBtn.setOnAction(e -> handleBack());
        
        // Tytuł
        Label title = new Label(isHost ? "🎮 Tworzenie Gry" : "🌐 Lobby");
        title.setFont(Font.font("System", FontWeight.BOLD, 32));
        title.setTextFill(Color.web("#2d3436"));
        HBox.setMargin(title, new Insets(0, 0, 0, 30));
        
        // Status połączenia
        statusLabel = new Label("⏳ Łączenie...");
        statusLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        statusLabel.setTextFill(Color.web("#636e72"));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        header.getChildren().addAll(backBtn, title, spacer, statusLabel);
        
        return header;
    }
    
    private VBox createPlayersCard() {
        VBox card = new VBox(20);
        card.setStyle(CARD_STYLE);
        card.setPadding(new Insets(30));
        card.setMinWidth(450);
        card.setMaxWidth(500);
        card.setPrefHeight(500);
        
        // Nagłówek karty
        HBox cardHeader = new HBox();
        cardHeader.setAlignment(Pos.CENTER_LEFT);
        
        Label playersIcon = new Label("👥");
        playersIcon.setFont(Font.font(28));
        
        playersCountLabel = new Label("Gracze (1/4)");
        playersCountLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        playersCountLabel.setTextFill(Color.web("#2d3436"));
        HBox.setMargin(playersCountLabel, new Insets(0, 0, 0, 10));
        
        cardHeader.getChildren().addAll(playersIcon, playersCountLabel);
        
        // Separator
        Rectangle separator = new Rectangle();
        separator.setWidth(400);
        separator.setHeight(1);
        separator.setFill(Color.web("#e9ecef"));
        
        // Lista graczy
        playersList = new VBox(12);
        playersList.setPadding(new Insets(10, 0, 0, 0));
        
        ScrollPane scrollPane = new ScrollPane(playersList);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        // Dodaj siebie jako pierwszego gracza
        addPlayer(new PlayerInfo(playerId, playerName, isHost));
        
        card.getChildren().addAll(cardHeader, separator, scrollPane);
        
        return card;
    }
    
    private VBox createInfoSection() {
        VBox section = new VBox(30);
        section.setAlignment(Pos.TOP_CENTER);
        section.setMinWidth(400);
        
        // Karta z kodem pokoju
        VBox roomCodeCard = createRoomCodeCard();
        
        // Karta z chatem
        VBox chatCard = createChatCard();
        VBox.setVgrow(chatCard, Priority.ALWAYS);
        
        section.getChildren().addAll(roomCodeCard, chatCard);
        
        return section;
    }
    
    private VBox createRoomCodeCard() {
        VBox card = new VBox(15);
        card.setStyle(CARD_STYLE);
        card.setPadding(new Insets(25));
        card.setAlignment(Pos.CENTER);
        
        Label titleLabel = new Label("🔑 Kod Pokoju");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        titleLabel.setTextFill(Color.web("#636e72"));
        
        // Kod pokoju z możliwością kopiowania
        HBox codeBox = new HBox(15);
        codeBox.setAlignment(Pos.CENTER);
        codeBox.setStyle(
            "-fx-background-color: linear-gradient(to right, #667eea, #764ba2); " +
            "-fx-background-radius: 12; " +
            "-fx-padding: 15 30;"
        );
        
        Label codeLabel = new Label(roomCode);
        codeLabel.setFont(Font.font("System", FontWeight.BOLD, 36));
        codeLabel.setTextFill(Color.WHITE);
        codeLabel.setStyle("-fx-letter-spacing: 5px;");
        
        Button copyBtn = new Button("📋");
        copyBtn.setStyle(
            "-fx-background-color: rgba(255,255,255,0.2); " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 8; " +
            "-fx-cursor: hand;"
        );
        copyBtn.setOnAction(e -> copyRoomCode());
        copyBtn.setOnMouseEntered(e -> copyBtn.setStyle(
            "-fx-background-color: rgba(255,255,255,0.4); " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 8; " +
            "-fx-cursor: hand;"
        ));
        copyBtn.setOnMouseExited(e -> copyBtn.setStyle(
            "-fx-background-color: rgba(255,255,255,0.2); " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 8; " +
            "-fx-cursor: hand;"
        ));
        
        codeBox.getChildren().addAll(codeLabel, copyBtn);
        
        Label hintLabel = new Label("Udostępnij kod znajomym!");
        hintLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        hintLabel.setTextFill(Color.web("#95a5a6"));
        
        card.getChildren().addAll(titleLabel, codeBox, hintLabel);

        // Wyświetlanie IP hosta (tylko dla hosta)
        if (isHost) {
            String ipAddress = getRealIpAddress();
            
            Label ipLabel = new Label("Twój Local IP: " + ipAddress);
            ipLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
            ipLabel.setTextFill(Color.web("#2d3436"));
            ipLabel.setStyle("-fx-background-color: #dfe6e9; -fx-padding: 5 10; -fx-background-radius: 5;");
            
            // Add tooltip explaining what this is
            Tooltip tooltip = new Tooltip("Podaj ten adres innym graczom, aby mogli dołączyć (muszą być w tej samej sieci Wi-Fi/LAN)");
            ipLabel.setTooltip(tooltip);
            
            VBox ipBox = new VBox(5);
            ipBox.setAlignment(Pos.CENTER);
            ipBox.getChildren().add(ipLabel);
            
            card.getChildren().add(ipBox);
        }
        
        return card;
    }
    
    private VBox createChatCard() {
        VBox card = new VBox(15);
        card.setStyle(CARD_STYLE);
        card.setPadding(new Insets(20));
        
        Label chatTitle = new Label("💬 Chat");
        chatTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        chatTitle.setTextFill(Color.web("#2d3436"));
        
        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setStyle(
            "-fx-control-inner-background: #f8f9fa; " +
            "-fx-font-size: 13px; " +
            "-fx-background-radius: 8; " +
            "-fx-border-radius: 8;"
        );
        chatArea.setPrefHeight(200);
        VBox.setVgrow(chatArea, Priority.ALWAYS);
        
        // Input do chatu
        HBox chatInputBox = new HBox(10);
        chatInputBox.setAlignment(Pos.CENTER);
        
        chatInput = new TextField();
        chatInput.setPromptText("Napisz wiadomość...");
        chatInput.setStyle(INPUT_STYLE);
        chatInput.focusedProperty().addListener((obs, oldVal, newVal) -> {
            chatInput.setStyle(newVal ? INPUT_FOCUS_STYLE : INPUT_STYLE);
        });
        HBox.setHgrow(chatInput, Priority.ALWAYS);
        chatInput.setOnAction(e -> sendChatMessage());
        
        Button sendBtn = new Button("➤");
        sendBtn.setStyle(PRIMARY_BUTTON_STYLE);
        sendBtn.setOnAction(e -> sendChatMessage());
        
        chatInputBox.getChildren().addAll(chatInput, sendBtn);
        
        card.getChildren().addAll(chatTitle, chatArea, chatInputBox);
        
        return card;
    }
    
    private HBox createFooter() {
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(20, 40, 20, 40));
        
        startGameBtn = new Button("🚀 Rozpocznij Grę");
        startGameBtn.setStyle(DISABLED_BUTTON_STYLE);
        startGameBtn.setMinWidth(200);
        startGameBtn.setMinHeight(50);
        startGameBtn.setDisable(true);
        
        if (isHost) {
            startGameBtn.setOnAction(e -> startGame());
        } else {
            startGameBtn.setText("⏳ Oczekiwanie na hosta...");
        }
        
        footer.getChildren().add(startGameBtn);
        
        return footer;
    }
    
    private void addDecorations(Pane root) {
        // Dekoracyjne koła w tle
        Circle deco1 = new Circle(150, Color.rgb(102, 126, 234, 0.08));
        deco1.setTranslateX(-100);
        deco1.setTranslateY(100);
        
        Circle deco2 = new Circle(200, Color.rgb(118, 75, 162, 0.06));
        deco2.setTranslateX(1300);
        deco2.setTranslateY(700);
        
        // Dodaj na spód
        if (root instanceof StackPane) {
            ((StackPane) root).getChildren().addAll(0, List.of(deco1, deco2));
        }
    }
    
    // === LOGIKA SIECIOWA ===
    
    private void initializeNetwork() {
        networkManager = new NetworkManager(playerId);
        
        // Handler wiadomości
        networkManager.setMessageHandler(this::handleMessage);
        
        // Handler połączeń
        networkManager.setConnectionHandler(status -> {
            Platform.runLater(() -> {
                appendChat("🔔 System", status);
            });
        });
        
        try {
            if (isHost) {
                networkManager.startHost();
                Platform.runLater(() -> {
                    statusLabel.setText("✅ Host aktywny • Port " + NetworkManager.DEFAULT_PORT);
                    statusLabel.setTextFill(Color.web("#27ae60"));
                    updateStartButton();
                });
            } else {
                // Dla klienta - łączenie z hostem w osobnym wątku, aby nie blokować UI
                statusLabel.setText("⏳ Łączenie z " + (hostAddress != null ? hostAddress : "localhost") + "...");
                
                new Thread(() -> {
                    try {
                        String targetHost = (hostAddress != null && !hostAddress.isEmpty()) ? hostAddress : "localhost";
                        networkManager.connectToHost(targetHost, NetworkManager.DEFAULT_PORT, playerName);
                        Platform.runLater(() -> {
                            statusLabel.setText("✅ Połączono z hostem: " + targetHost);
                            statusLabel.setTextFill(Color.web("#27ae60"));
                        });
                    } catch (IOException e) {
                        Platform.runLater(() -> {
                            statusLabel.setText("❌ Błąd: " + e.getMessage());
                            statusLabel.setTextFill(Color.web("#e74c3c"));
                        });
                    }
                }).start();
            }
        } catch (IOException e) {
            Platform.runLater(() -> {
                statusLabel.setText("❌ Błąd hosta: " + e.getMessage());
                statusLabel.setTextFill(Color.web("#e74c3c"));
            });
        }
    }
    
    private void handleMessage(GameMessage message) {
        Platform.runLater(() -> {
            switch (message.getType()) {
                case CONNECT -> {
                    // Nowy gracz dołączył
                    String newPlayerName = (String) message.getPayload();
                    if (newPlayerName == null) newPlayerName = "Gracz " + message.getSenderId().substring(0, 4);
                    addPlayer(new PlayerInfo(message.getSenderId(), newPlayerName, false));
                    appendChat("🔔 System", newPlayerName + " dołączył do gry!");
                    updatePlayersCount();
                    
                    // Host wysyła listę graczy nowemu graczowi
                    if (isHost) {
                        broadcastPlayerList();
                    }
                }
                case DISCONNECT -> {
                    removePlayer(message.getSenderId());
                    appendChat("🔔 System", "Gracz opuścił lobby");
                    updatePlayersCount();
                }
                case CHAT -> {
                    String chatContent = (String) message.getPayload();
                    String senderName = getPlayerName(message.getSenderId());
                    appendChat(senderName, chatContent);
                }
                case GAME_START -> {
                    // Host rozpoczął grę
                    appendChat("🎮 System", "Gra rozpoczęta!");
                    launchGame();
                }
                case PLAYER_LIST -> {
                    // Aktualizacja listy graczy (dla klientów)
                    @SuppressWarnings("unchecked")
                    List<PlayerInfo> playerList = (List<PlayerInfo>) message.getPayload();
                    if (playerList != null) {
                        players.clear();
                        playersList.getChildren().clear();
                        for (PlayerInfo p : playerList) {
                            addPlayer(p);
                        }
                        updatePlayersCount();
                    }
                }
                default -> {
                    // Inne wiadomości
                }
            }
        });
    }
    
    private void broadcastPlayerList() {
        GameMessage msg = new GameMessage(
            GameMessage.MessageType.PLAYER_LIST,
            playerId,
            new ArrayList<>(players)
        );
        networkManager.send(msg);
    }
    
    private void sendChatMessage() {
        String text = chatInput.getText().trim();
        if (text.isEmpty()) return;
        
        GameMessage msg = new GameMessage(
            GameMessage.MessageType.CHAT,
            playerId,
            text
        );
        msg.setBroadcast(true);
        networkManager.send(msg);
        
        appendChat(playerName, text);
        chatInput.clear();
    }
    
    private void appendChat(String sender, String message) {
        String timestamp = java.time.LocalTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("HH:mm")
        );
        chatArea.appendText(String.format("[%s] %s: %s\n", timestamp, sender, message));
    }
    
    // === UI HELPERS ===
    
    private void addPlayer(PlayerInfo player) {
        // Sprawdź czy gracz już istnieje
        if (players.stream().anyMatch(p -> p.id.equals(player.id))) {
            return;
        }
        
        players.add(player);
        
        HBox playerRow = createPlayerRow(player);
        playersList.getChildren().add(playerRow);
        
        // Animacja dodania
        playerRow.setOpacity(0);
        playerRow.setTranslateX(-20);
        
        FadeTransition fade = new FadeTransition(Duration.millis(300), playerRow);
        fade.setToValue(1);
        
        TranslateTransition slide = new TranslateTransition(Duration.millis(300), playerRow);
        slide.setToX(0);
        
        fade.play();
        slide.play();
        
        updatePlayersCount();
        updateStartButton();
    }
    
    private HBox createPlayerRow(PlayerInfo player) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 15, 12, 15));
        row.setStyle(
            "-fx-background-color: " + (player.isHost ? "#f0f4ff" : "#f8f9fa") + "; " +
            "-fx-background-radius: 10;"
        );
        
        // Avatar
        StackPane avatar = new StackPane();
        Circle avatarCircle = new Circle(20);
        avatarCircle.setFill(player.isHost 
            ? Color.web("#667eea") 
            : Color.web("#" + player.id.substring(0, 6)));
        
        Label avatarLabel = new Label(player.name.substring(0, 1).toUpperCase());
        avatarLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        avatarLabel.setTextFill(Color.WHITE);
        
        avatar.getChildren().addAll(avatarCircle, avatarLabel);
        
        // Nazwa
        VBox nameBox = new VBox(2);
        Label nameLabel = new Label(player.name);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        nameLabel.setStyle("-fx-text-fill: black;");
        
        Label roleLabel = new Label(player.isHost ? "👑 Host" : "🎮 Gracz");
        roleLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        roleLabel.setStyle("-fx-text-fill: black;");
        
        nameBox.getChildren().addAll(nameLabel, roleLabel);
        HBox.setHgrow(nameBox, Priority.ALWAYS);
        
        row.getChildren().addAll(avatar, nameBox);
        row.setUserData(player.id);
        
        return row;
    }
    
    private void removePlayer(String playerId) {
        players.removeIf(p -> p.id.equals(playerId));
        playersList.getChildren().removeIf(node -> 
            playerId.equals(node.getUserData())
        );
        updatePlayersCount();
        updateStartButton();
    }
    
    private void updatePlayersCount() {
        if (playersCountLabel != null) {
            playersCountLabel.setText("Gracze (" + players.size() + "/4)");
        }
    }
    
    private void updateStartButton() {
        if (!isHost || startGameBtn == null) return;
        
        boolean canStart = players.size() >= 2;
        startGameBtn.setDisable(!canStart);
        startGameBtn.setStyle(canStart ? PRIMARY_BUTTON_STYLE : DISABLED_BUTTON_STYLE);
        
        if (canStart) {
            startGameBtn.setOnMouseEntered(e -> startGameBtn.setStyle(PRIMARY_BUTTON_HOVER_STYLE));
            startGameBtn.setOnMouseExited(e -> startGameBtn.setStyle(PRIMARY_BUTTON_STYLE));
        }
    }
    
    private String getPlayerName(String playerId) {
        return players.stream()
            .filter(p -> p.id.equals(playerId))
            .map(p -> p.name)
            .findFirst()
            .orElse("Gracz");
    }
    
    private void copyRoomCode() {
        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(roomCode);
        clipboard.setContent(content);
        
        appendChat("🔔 System", "Kod pokoju skopiowany do schowka!");
    }
    
    private String generateRoomCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }
    
    // === AKCJE ===
    
    private void handleBack() {
        if (networkManager != null) {
            // Wyślij wiadomość o rozłączeniu
            GameMessage msg = new GameMessage(
                GameMessage.MessageType.DISCONNECT,
                playerId,
                playerName
            );
            msg.setBroadcast(true);
            networkManager.send(msg);
            networkManager.stop();
        }
        
        if (onBack != null) {
            onBack.run();
        }
    }
    
    private void startGame() {
        if (!isHost || players.size() < 2) return;
        
        appendChat("🎮 System", "Rozpoczynanie gry...");
        
        // Wyślij wiadomość o rozpoczęciu gry
        GameMessage msg = new GameMessage(
            GameMessage.MessageType.GAME_START,
            playerId,
            null
        );
        msg.setBroadcast(true);
        networkManager.send(msg);
        
        // Uruchom grę lokalnie
        launchGame();
    }
    
    private void launchGame() {
        // Konwersja listy graczy
        List<Player> gamePlayers = new ArrayList<>();
        for (PlayerInfo info : players) {
            gamePlayers.add(new Player(info.id, info.name, 1500));
        }

        // Przejście do widoku gry
        Platform.runLater(() -> {
            GameView boardView;
            
            if (loadedGameState != null) {
                // Mapowanie nowych graczy z lobby na graczy z wczytanego stanu
                // Aktualizujemy ID i nazwy graczy w zapisanym stanie
                List<Player> savedPlayers = loadedGameState.getPlayers();
                int minPlayers = Math.min(players.size(), savedPlayers.size());
                
                for (int i = 0; i < minPlayers; i++) {
                    Player savedPlayer = savedPlayers.get(i);
                    PlayerInfo lobbyPlayer = players.get(i);
                    
                    // Aktualizuj ID i nazwę gracza z zapisu na nowe z lobby
                    savedPlayer.setId(lobbyPlayer.id);
                    savedPlayer.setName(lobbyPlayer.name);
                }
                
                // Usuń nadmiarowych graczy jeśli w lobby jest mniej osób
                while (savedPlayers.size() > players.size()) {
                    savedPlayers.remove(savedPlayers.size() - 1);
                }
                
                // Użyj graczy z wczytanego stanu (z zaktualizowanymi ID)
                boardView = new GameView(stage, savedPlayers, networkManager, playerId);
                boardView.setGameState(loadedGameState);
            } else {
                // Normalna gra - nowi gracze
                boardView = new GameView(stage, gamePlayers, networkManager, playerId);
            }
            
            boardView.show();
        });
    }
    
    private void playEntranceAnimation(VBox root) {
        root.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(400), root);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }
    
    private String getRealIpAddress() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface iface = interfaces.nextElement();
                // Skip loopback and down interfaces
                if (iface.isLoopback() || !iface.isUp()) continue;
                
                java.util.Enumeration<java.net.InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress addr = addresses.nextElement();
                    // We want IPv4 and not loopback
                    if (addr instanceof java.net.Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
            // Fallback
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "Nieznane (sprawdź ustawienia sieci)";
        }
    }
    
    // === KLASA WEWNĘTRZNA ===
    
    public static class PlayerInfo implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        
        public final String id;
        public final String name;
        public final boolean isHost;
        
        public PlayerInfo(String id, String name, boolean isHost) {
            this.id = id;
            this.name = name;
            this.isHost = isHost;
        }
    }
}
