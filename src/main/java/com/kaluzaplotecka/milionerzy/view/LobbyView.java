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
import com.kaluzaplotecka.milionerzy.view.components.GameButton;
import com.kaluzaplotecka.milionerzy.view.utils.NetworkUtils;
import com.kaluzaplotecka.milionerzy.view.utils.UIConstants;
import com.kaluzaplotecka.milionerzy.view.utils.ViewFactory;

/**
 * Lobby view for creating and joining multiplayer games.
 */
public class LobbyView {

    private final Stage stage;
    private final boolean isHost;
    private final Runnable onBack;
    
    private NetworkManager networkManager;
    private String roomCode;
    private String playerName;
    private String playerId;
    private GameState loadedGameState;
    
    // UI Components
    private Label playersCountLabel;
    private VBox playersList;
    private GameButton startGameBtn; // Changed to GameButton
    private Label statusLabel;
    private TextArea chatArea;
    private TextField chatInput;
    
    // Lista graczy w lobby
    private final List<PlayerInfo> players = new ArrayList<>();
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
        Scene scene = ViewFactory.createStyledScene(root, 1440, 900);
        stage.setScene(scene);
        stage.setTitle(isHost ? "Milionerzy - Tworzenie Gry" : "Milionerzy - Lobby");
        
        initializeNetwork();
        playEntranceAnimation(root);
    }
    
    private VBox createMainLayout() {
        VBox root = new VBox();
        root.setStyle(UIConstants.BACKGROUND_GRADIENT);
        root.setPadding(new Insets(30));
        root.setSpacing(30);
        
        HBox header = createHeader();
        
        HBox content = new HBox(30);
        content.setAlignment(Pos.CENTER);
        VBox.setVgrow(content, Priority.ALWAYS);
        
        VBox playersCard = createPlayersCard();
        VBox infoSection = createInfoSection();
        
        content.getChildren().addAll(playersCard, infoSection);
        
        HBox footer = createFooter();
        
        root.getChildren().addAll(header, content, footer);
        
        return root;
    }
    
    private HBox createHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 20, 10, 20));
        
        GameButton backBtn = new GameButton("←", 24, 24, 24, this::handleBack);
        backBtn.setBorderWidth(4);
        backBtn.setBorderColor(UIConstants.PRIMARY_GRADIENT_START);
        backBtn.setTextColor(UIConstants.PRIMARY_GRADIENT_START);
        backBtn.setBorderRadius(100);
        backBtn.setColor("transparent");

        Label title = ViewFactory.createHeaderLabel(isHost ? "🎮 Tworzenie Gry" : "🌐 Lobby", 32);
        HBox.setMargin(title, new Insets(0, 0, 0, 30));
        
        statusLabel = new Label("⏳ Łączenie...");
        statusLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        statusLabel.setTextFill(Color.web(UIConstants.TEXT_SECONDARY));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        header.getChildren().addAll(backBtn, title, spacer, statusLabel);
        
        return header;
    }
    
    private VBox createPlayersCard() {
        VBox card = ViewFactory.createCard();
        card.setMinWidth(450);
        card.setMaxWidth(500);
        card.setPrefHeight(500);
        
        HBox cardHeader = new HBox();
        cardHeader.setAlignment(Pos.CENTER_LEFT);
        
        Label playersIcon = new Label("👥");
        playersIcon.setFont(Font.font(28));
        
        playersCountLabel = ViewFactory.createHeaderLabel("Gracze (1/4)", 24);
        HBox.setMargin(playersCountLabel, new Insets(0, 0, 0, 10));
        
        cardHeader.getChildren().addAll(playersIcon, playersCountLabel);
        
        Rectangle separator = new Rectangle();
        separator.setWidth(400);
        separator.setHeight(1);
        separator.setFill(Color.web("#e9ecef"));
        
        playersList = new VBox(12);
        playersList.setPadding(new Insets(10, 0, 0, 0));
        
        ScrollPane scrollPane = new ScrollPane(playersList);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        addPlayer(new PlayerInfo(playerId, playerName, isHost));
        
        card.getChildren().addAll(cardHeader, separator, scrollPane);
        return card;
    }
    
    private VBox createInfoSection() {
        VBox section = new VBox(30);
        section.setAlignment(Pos.TOP_CENTER);
        section.setMinWidth(400);
        
        VBox roomCodeCard = createRoomCodeCard();
        VBox chatCard = createChatCard();
        VBox.setVgrow(chatCard, Priority.ALWAYS);
        
        section.getChildren().addAll(roomCodeCard, chatCard);
        return section;
    }
    
    private VBox createRoomCodeCard() {
        VBox card = ViewFactory.createCard();
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(25));
        
        Label titleLabel = new Label("🔑 Kod Pokoju");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        titleLabel.setTextFill(Color.web(UIConstants.TEXT_SECONDARY));
        
        HBox codeBox = new HBox(15);
        codeBox.setAlignment(Pos.CENTER);
        codeBox.setStyle(
            "-fx-background-color: linear-gradient(to right, " + UIConstants.PRIMARY_GRADIENT_START + ", " + UIConstants.PRIMARY_GRADIENT_END + "); " +
            "-fx-background-radius: 12; " +
            "-fx-padding: 15 30;"
        );
        
        Label codeLabel = new Label(roomCode);
        codeLabel.setFont(Font.font("System", FontWeight.BOLD, 36));
        codeLabel.setTextFill(Color.WHITE);
        codeLabel.setStyle("-fx-letter-spacing: 5px;");
        GameButton copyBtn = new GameButton("📋", 24, 24, 24, this::copyRoomCode);
        copyBtn.setBorderRadius(10);
        copyBtn.setColor("#d2adff3d");

        codeBox.getChildren().addAll(codeLabel, copyBtn);
        
        Label hintLabel = new Label("Udostępnij kod znajomym!");
        hintLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        hintLabel.setTextFill(Color.web("#95a5a6"));
        
        card.getChildren().addAll(titleLabel, codeBox, hintLabel);
        
        if (isHost) {
            String ipAddress = NetworkUtils.getRealIpAddress();
            Label ipLabel = new Label("Twój Local IP: " + ipAddress);
            ipLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
            ipLabel.setTextFill(Color.web(UIConstants.TEXT_PRIMARY));
            ipLabel.setStyle("-fx-background-color: #dfe6e9; -fx-padding: 5 10; -fx-background-radius: 5;");
            
            VBox ipBox = new VBox(5);
            ipBox.setAlignment(Pos.CENTER);
            ipBox.getChildren().add(ipLabel);
            card.getChildren().add(ipBox);
        }
        
        return card;
    }
    
    private VBox createChatCard() {
        VBox card = ViewFactory.createCard();
        
        Label chatTitle = new Label("💬 Chat");
        chatTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        chatTitle.setTextFill(Color.web(UIConstants.TEXT_PRIMARY));
        
        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setStyle("-fx-control-inner-background: #f8f9fa; -fx-background-radius: 8;");
        chatArea.setPrefHeight(400);
        VBox.setVgrow(chatArea, Priority.ALWAYS);
        
        HBox chatInputBox = new HBox(10);
        chatInputBox.setAlignment(Pos.CENTER);
        
        chatInput = ViewFactory.createStyledTextField("Napisz wiadomość...");
        HBox.setHgrow(chatInput, Priority.ALWAYS);
        chatInput.setOnAction(e -> sendChatMessage());
        
        GameButton sendBtn = new GameButton("➤", 16, this::sendChatMessage);
        sendBtn.setPadding(10, 16, 10, 16); // Adjust padding as needed
        
        chatInputBox.getChildren().addAll(chatInput, sendBtn);
        card.getChildren().addAll(chatTitle, chatArea, chatInputBox);
        
        return card;
    }
    
    private HBox createFooter() {
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(20, 40, 20, 40));
        
        startGameBtn = new GameButton("🚀 Rozpocznij Grę", 200, 50, 16, () -> {
            if (isHost) startGame();
        });
        
        if (isHost) {
            startGameBtn.setDisabledStyle(true);
        } else {
            startGameBtn.setText("⏳ Oczekiwanie na hosta...");
            startGameBtn.setDisabledStyle(true);
        }
        
        footer.getChildren().add(startGameBtn);
        return footer;
    }
    
    // === NETWORK LOGIC (Unchanged mostly, just using utils where applicable) ===
    
    private void initializeNetwork() {
        networkManager = new NetworkManager(playerId);
        networkManager.setMessageHandler(this::handleMessage);
        
        networkManager.setConnectionHandler(status -> 
            Platform.runLater(() -> appendChat("🔔 System", status))
        );
        
        try {
            if (isHost) {
                networkManager.startHost();
                Platform.runLater(() -> {
                    statusLabel.setText("✅ Host aktywny • Port " + NetworkManager.DEFAULT_PORT);
                    statusLabel.setTextFill(Color.web(UIConstants.SUCCESS_COLOR));
                    updateStartButton();
                });
            } else {
                statusLabel.setText("⏳ Łączenie z " + (hostAddress != null ? hostAddress : "localhost") + "...");
                new Thread(() -> {
                    try {
                        String targetHost = (hostAddress != null && !hostAddress.isEmpty()) ? hostAddress : "localhost";
                        networkManager.connectToHost(targetHost, NetworkManager.DEFAULT_PORT, playerName);
                        Platform.runLater(() -> {
                            statusLabel.setText("✅ Połączono z hostem: " + targetHost);
                            statusLabel.setTextFill(Color.web(UIConstants.SUCCESS_COLOR));
                        });
                    } catch (IOException e) {
                        Platform.runLater(() -> {
                            statusLabel.setText("❌ Błąd: " + e.getMessage());
                            statusLabel.setTextFill(Color.web(UIConstants.ERROR_COLOR));
                        });
                    }
                }).start();
            }
        } catch (IOException e) {
            Platform.runLater(() -> {
                statusLabel.setText("❌ Błąd hosta: " + e.getMessage());
                statusLabel.setTextFill(Color.web(UIConstants.ERROR_COLOR));
            });
        }
    }
    
    private void handleMessage(GameMessage message) {
        Platform.runLater(() -> {
            switch (message.getType()) {
                case CONNECT -> {
                    String newPlayerName = (String) message.getPayload();
                    if (newPlayerName == null) newPlayerName = "Gracz " + message.getSenderId().substring(0, 4);
                    addPlayer(new PlayerInfo(message.getSenderId(), newPlayerName, false));
                    appendChat("🔔 System", newPlayerName + " dołączył do gry!");
                    updatePlayersCount();
                    if (isHost) broadcastPlayerList();
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
                    appendChat("🎮 System", "Gra rozpoczęta!");
                    launchGame();
                }
                case PLAYER_LIST -> {
                    @SuppressWarnings("unchecked")
                    List<PlayerInfo> playerList = (List<PlayerInfo>) message.getPayload();
                    if (playerList != null) {
                        players.clear();
                        playersList.getChildren().clear();
                        for (PlayerInfo p : playerList) addPlayer(p);
                        updatePlayersCount();
                    }
                }
                default -> {
                    // Ignore other message types
                }
            }
        });
    }
    
    private void broadcastPlayerList() {
        GameMessage msg = new GameMessage(GameMessage.MessageType.PLAYER_LIST, playerId, new ArrayList<>(players));
        networkManager.send(msg);
    }
    
    private void sendChatMessage() {
        String text = chatInput.getText().trim();
        if (text.isEmpty()) return;
        
        GameMessage msg = new GameMessage(GameMessage.MessageType.CHAT, playerId, text);
        msg.setBroadcast(true);
        networkManager.send(msg);
        
        appendChat(playerName, text);
        chatInput.clear();
    }
    
    private void appendChat(String sender, String message) {
        String timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        chatArea.appendText(String.format("[%s] %s: %s\n", timestamp, sender, message));
    }
    
    private void addPlayer(PlayerInfo player) {
        if (players.stream().anyMatch(p -> p.id.equals(player.id))) return;
        
        players.add(player);
        HBox playerRow = createPlayerRow(player);
        playersList.getChildren().add(playerRow);
        
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
        row.setStyle("-fx-background-color: " + (player.isHost ? "#1b1b1b" : "#222222") + "; -fx-background-radius: 10;");
        
        StackPane avatar = new StackPane();
        Circle avatarCircle = new Circle(20);
        avatarCircle.setFill(player.isHost ? Color.web(UIConstants.PRIMARY_GRADIENT_START) : Color.web("#" + player.id.substring(0, 6)));
        
        Label avatarLabel = new Label(player.name.substring(0, 1).toUpperCase());
        avatarLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        avatarLabel.setTextFill(Color.WHITE);
        
        avatar.getChildren().addAll(avatarCircle, avatarLabel);
        
        VBox nameBox = new VBox(2);
        Label nameLabel = new Label(player.name);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        nameLabel.setTextFill(Color.BLACK);
        
        Label roleLabel = new Label(player.isHost ? "👑 Host" : "🎮 Gracz");
        roleLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        roleLabel.setTextFill(Color.BLACK);
        
        nameBox.getChildren().addAll(nameLabel, roleLabel);
        HBox.setHgrow(nameBox, Priority.ALWAYS);
        
        row.getChildren().addAll(avatar, nameBox);
        row.setUserData(player.id);
        
        return row;
    }
    
    private void removePlayer(String playerId) {
        players.removeIf(p -> p.id.equals(playerId));
        playersList.getChildren().removeIf(node -> playerId.equals(node.getUserData()));
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
        startGameBtn.setDisabledStyle(!canStart);
    }
    
    private String getPlayerName(String playerId) {
        return players.stream().filter(p -> p.id.equals(playerId)).map(p -> p.name).findFirst().orElse("Gracz");
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
    
    private void handleBack() {
        if (networkManager != null) {
            GameMessage msg = new GameMessage(GameMessage.MessageType.DISCONNECT, playerId, playerName);
            msg.setBroadcast(true);
            networkManager.send(msg);
            networkManager.stop();
        }
        if (onBack != null) onBack.run();
    }
    
    private void startGame() {
        if (!isHost || players.size() < 2) return;
        
        appendChat("🎮 System", "Rozpoczynanie gry...");
        GameMessage msg = new GameMessage(GameMessage.MessageType.GAME_START, playerId, null);
        msg.setBroadcast(true);
        networkManager.send(msg);
        
        launchGame();
    }
    
    private void launchGame() {
        List<Player> gamePlayers = new ArrayList<>();
        for (PlayerInfo info : players) {
            gamePlayers.add(new Player(info.id, info.name, 1500));
        }
        
        Platform.runLater(() -> {
            GameView boardView;
            if (loadedGameState != null) {
                // ... same logic as before for loaded game ...
                List<Player> savedPlayers = loadedGameState.getPlayers();
                int minPlayers = Math.min(players.size(), savedPlayers.size());
                
                for (int i = 0; i < minPlayers; i++) {
                    Player savedPlayer = savedPlayers.get(i);
                    PlayerInfo lobbyPlayer = players.get(i);
                    savedPlayer.setId(lobbyPlayer.id);
                    savedPlayer.setName(lobbyPlayer.name);
                }
                while (savedPlayers.size() > players.size()) {
                    savedPlayers.remove(savedPlayers.size() - 1);
                }
                boardView = new GameView(stage, savedPlayers, networkManager, playerId);
                boardView.setGameState(loadedGameState);
            } else {
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
