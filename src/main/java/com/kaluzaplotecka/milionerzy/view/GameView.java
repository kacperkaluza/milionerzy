package com.kaluzaplotecka.milionerzy.view;

import com.kaluzaplotecka.milionerzy.manager.SoundManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;

import com.kaluzaplotecka.milionerzy.events.GameEvent;
import com.kaluzaplotecka.milionerzy.events.GameEventListener;
import com.kaluzaplotecka.milionerzy.model.Board;
import com.kaluzaplotecka.milionerzy.model.GameState;
import com.kaluzaplotecka.milionerzy.model.Player;
import com.kaluzaplotecka.milionerzy.network.GameMessage;
import com.kaluzaplotecka.milionerzy.network.NetworkManager;
import com.kaluzaplotecka.milionerzy.model.SaveManager;
import com.kaluzaplotecka.milionerzy.model.tiles.ChanceTile;
import com.kaluzaplotecka.milionerzy.model.tiles.CommunityChestTile;
import com.kaluzaplotecka.milionerzy.model.tiles.PropertyTile;
import com.kaluzaplotecka.milionerzy.model.tiles.Tile;
import com.kaluzaplotecka.milionerzy.view.components.AuctionComponent;
import com.kaluzaplotecka.milionerzy.view.components.BoardComponent;
import com.kaluzaplotecka.milionerzy.view.components.DiceComponent;
import com.kaluzaplotecka.milionerzy.view.components.PlayerPanelComponent;

import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

public class GameView implements GameEventListener {
    
    private Stage stage;
    private final List<Player> players;
    private PlayerPanelComponent[] playerPanels;
    private GameState gameState;
    private NetworkManager networkManager;
    private String playerId;
    private AuctionComponent auctionView;
    private NetworkStatusBox networkStatusBox;
    
    private BoardComponent boardComponent;
    private DiceComponent diceComponent;

    public GameView(Stage stage, List<Player> players, NetworkManager networkManager, String playerId) {
        this.stage = stage;
        this.players = players;
        this.networkManager = networkManager;
        this.playerId = playerId;
        
        this.playerPanels = new PlayerPanelComponent[4];
        
        this.boardComponent = new BoardComponent(players);
        this.diceComponent = new DiceComponent();
        
        setupDiceComponent();
        
        if (networkManager != null) {
            setupNetworkListeners();
            this.networkStatusBox = new NetworkStatusBox(networkManager);
        }
        
        // Initialize AuctionComponent
        this.auctionView = new AuctionComponent();
        setupAuctionCallbacks();

        // Initialize GameState
        if (networkManager == null || networkManager.getMode() == NetworkManager.Mode.HOST) {
            this.gameState = new GameState(createBoardModel(), players);
            this.gameState.addEventListener(this);
        }
        // Client waits for sync
    }
    
    private void setupDiceComponent() {
        diceComponent.setOnRoll(this::rollDice);
        diceComponent.setOnSave(this::saveGame);
        diceComponent.setOnAnimationFinished(this::updateRollButtonState);
    }

    private void setupAuctionCallbacks() {
        auctionView.setOnBid(amount -> {
            if (networkManager != null) {
                networkManager.send(new GameMessage(GameMessage.MessageType.AUCTION_BID, playerId, amount));
            } else if (gameState != null) {
                Player p = gameState.getPlayers().stream()
                    .filter(pl -> pl.getId().equals(playerId))
                    .findFirst()
                    .orElse(null);
                if (p != null) {
                    gameState.placeBid(p, amount);
                }
            }
        });
        
        auctionView.setOnPass(() -> {
            if (networkManager != null) {
                networkManager.send(new GameMessage(GameMessage.MessageType.AUCTION_PASS, playerId));
            } else if (gameState != null) {
                gameState.passAuction();
            }
        });
    }
    
    private void setupNetworkListeners() {
        if (networkManager == null) return;
        
        networkManager.setMessageHandler(msg -> {
            Platform.runLater(() -> {
                if (msg.getType() == GameMessage.MessageType.GAME_STATE_SYNC) {
                    if (msg.getPayload() instanceof GameState newState) {
                        System.out.println("Otrzymano synchronizację stanu gry!");
                        
                        Map<String, Integer> oldPositions = new HashMap<>();
                        for (Player p : this.players) {
                            oldPositions.put(p.getId(), p.getPosition());
                        }
                        
                        this.gameState = newState;
                        this.players.clear();
                        this.players.addAll(this.gameState.getPlayers());
                        
                        this.gameState.addEventListener(this);
                        
                        // Update components
                        boardComponent.refreshPawns(this.players, oldPositions);
                        
                        refreshBoard(); 
                        updateRollButtonState();
                    }
                } else if (msg.getType() == GameMessage.MessageType.DICE_RESULT) {
                    if (msg.getPayload() instanceof Integer val) {
                        diceComponent.animateDiceRoll(val);
                    }
                } else if (msg.getType() == GameMessage.MessageType.PROPERTY_OFFER) {
                    if (!networkManager.getMode().equals(NetworkManager.Mode.HOST)) {
                        String senderId = msg.getSenderId();
                        if (playerId.equals(senderId)) {
                            Object payload = msg.getPayload();
                            if (payload instanceof PropertyTile tile) {
                                showPropertyPurchaseDialog(tile);
                            }
                        }
                    }
                } else if (msg.getType() == GameMessage.MessageType.MOVE) {
                    handleMove(msg);
                } else if (networkManager.getMode().equals(NetworkManager.Mode.HOST) && gameState != null) {
                     gameState.processNetworkMessage(msg, true, networkManager);
                }
            });
        });
    }

    private void handleMove(GameMessage msg) {
        String senderId = msg.getSenderId();
        if (networkManager.getMode() == NetworkManager.Mode.HOST && senderId.equals(this.playerId)) {
            return;
        }

        Player player = players.stream()
            .filter(p -> p.getId().equals(senderId))
            .findFirst()
            .orElse(null);
        if (player != null) {
            int newPos = (int) msg.getPayload();
            int oldPos = player.getPosition();
            player.setPosition(newPos);
            boardComponent.animatePlayerMovement(player, oldPos, newPos);
        }
    }
    
    private void showPropertyPurchaseDialog(PropertyTile tile) {
         Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
         alert.setTitle("Kupno Nieruchomości");
         alert.setHeaderText("Czy chcesz kupić " + tile.getCity() + "?");
         alert.setContentText("Cena: " + tile.getPrice() + " zł");
         
         ButtonType buyButton = new ButtonType("Kup");
         ButtonType passButton = new ButtonType("Licytuj");
         
         alert.getButtonTypes().setAll(buyButton, passButton);
         
         Optional<ButtonType> result = alert.showAndWait();
         if (result.isPresent() && result.get() == buyButton) {
             if (networkManager != null) {
                 networkManager.send(new GameMessage(GameMessage.MessageType.BUY_PROPERTY, playerId));
             } else if (gameState != null) {
                 // Local game: directly update game state
                 Player currentPlayer = gameState.getPlayers().stream()
                     .filter(p -> p.getId().equals(playerId))
                     .findFirst()
                     .orElse(null);
                 if (currentPlayer != null) {
                     gameState.buyProperty(currentPlayer, tile);
                 }
             }
         } else {
             if (networkManager != null) {
                 networkManager.send(new GameMessage(GameMessage.MessageType.DECLINE_PURCHASE, playerId));
             } else if (gameState != null) {
                 // Local game: start auction
                 gameState.startAuction(tile);
             }
         }
    }


    private void refreshBoard() {
        if (gameState == null) {
            // In client mode, gameState can be null before the first synchronization.
            // In that case, there is nothing to refresh yet.
            return;
        }
        for (Player p : gameState.getPlayers()) {
             // Using ID or Index. Players list in GameView is kept in sync with GameState.
             // Assuming players list order matches panels order (as created in createScene)
             int index = players.indexOf(p);
             if (index == -1) {
                 // Try by ID
                 for(int i=0; i<players.size(); i++) {
                     if (players.get(i).getId().equals(p.getId())) {
                         index = i; 
                         break;
                     }
                 }
             }

             if (index >= 0 && index < playerPanels.length && playerPanels[index] != null) {
                 playerPanels[index].update(p);
             }
        }
    }
    
    public Scene createScene() {
        HBox root = new HBox(10);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #88bde7, #dbebea);");
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(10));
        
        String[] colors = {"#667eea", "#e74c3c", "#27ae60", "#f39c12"};
        
        for (int i = 0; i < 4; i++) {
            if (i < players.size()) {
                Player p = players.get(i);
                playerPanels[i] = new PlayerPanelComponent(p.getUsername(), p.getMoney(), colors[i%colors.length]);
            } else {
                playerPanels[i] = null;
            }
        }

        VBox leftPlayers = new VBox(20);
        leftPlayers.setAlignment(Pos.CENTER);
        if (playerPanels[0] != null) leftPlayers.getChildren().add(playerPanels[0]);
        if (playerPanels[2] != null) leftPlayers.getChildren().add(playerPanels[2]);
        
        VBox rightPlayers = new VBox(20);
        rightPlayers.setAlignment(Pos.CENTER);
        if (playerPanels[1] != null) rightPlayers.getChildren().add(playerPanels[1]);
        Button pauseBtn = createPauseButton();
        rightPlayers.getChildren().add(pauseBtn);
        if (playerPanels[3] != null) rightPlayers.getChildren().add(playerPanels[3]);
        
        StackPane centerPane = new StackPane();
        centerPane.getChildren().addAll(boardComponent, diceComponent); 
        
        if (auctionView != null) {
             centerPane.getChildren().add(auctionView);
        }
        
        root.getChildren().addAll(leftPlayers, centerPane, rightPlayers);
        
        StackPane rootWrapper = new StackPane();
        rootWrapper.getChildren().add(root);
        
        if (networkStatusBox != null) {
            StackPane.setAlignment(networkStatusBox, Pos.TOP_RIGHT);
            StackPane.setMargin(networkStatusBox, new Insets(15));
            rootWrapper.getChildren().add(networkStatusBox);
        }
        
        Scene scene = new Scene(rootWrapper, 1100, 750);
        return scene;
    }

    private void saveGame(String saveName) {
         try {
             String filename = SaveManager.save(gameState, saveName);
             Alert alert = new Alert(Alert.AlertType.INFORMATION);
             alert.setTitle("Zapis gry");
             alert.setHeaderText(null);
             alert.setContentText("Gra została zapisana pomyślnie!\n\nPlik: " + filename);
             alert.showAndWait();
         } catch (Exception e) {
             Alert alert = new Alert(Alert.AlertType.ERROR);
             alert.setTitle("Błąd zapisu");
             alert.setHeaderText("Nie udało się zapisać gry");
             alert.setContentText(e.getMessage());
             alert.showAndWait();
         }
    }

    private void rollDice() {
        if (diceComponent != null) diceComponent.setRollButtonState(false, "Losowanie...");
        if (networkManager != null && networkManager.getMode() == NetworkManager.Mode.CLIENT) {
            networkManager.send(new GameMessage(GameMessage.MessageType.ROLL_DICE, playerId));
        } else {
            gameState.moveCurrentPlayer();
        }
    }

    private Button createPauseButton() {
        Button pauseBtn = new Button("⏸");
        pauseBtn.setStyle("-fx-background-color: rgba(255,255,255,0.9); -fx-font-size: 28px; -fx-padding: 10; -fx-background-radius: 50; -fx-cursor: hand;");
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.2));
        shadow.setRadius(10);
        pauseBtn.setEffect(shadow);
        pauseBtn.setOnAction(e -> showPauseDialog());
        return pauseBtn;
    }

    private void showPauseDialog() {
        Alert pauseDialog = new Alert(Alert.AlertType.NONE);
        pauseDialog.setTitle("Pauza");
        pauseDialog.setHeaderText("Gra wstrzymana");
        pauseDialog.setContentText("Wybierz co chcesz zrobić:");
        ButtonType resumeButton = new ButtonType("Powrót do gry");
        ButtonType menuButton = new ButtonType("Menu Główne");
        pauseDialog.getButtonTypes().setAll(resumeButton, menuButton);
        pauseDialog.showAndWait().ifPresent(response -> {
            if (response == menuButton) {
                if (networkManager != null) {
                    GameMessage msg = new GameMessage(GameMessage.MessageType.DISCONNECT, playerId, "Player left game");
                    msg.setBroadcast(true);
                    networkManager.send(msg);
                    networkManager.stop();
                }
                MainMenu mainMenu = new MainMenu();
                try { mainMenu.start(stage); } catch (Exception ex) { ex.printStackTrace(); }
            }
        });
    }

    public void show() {
        Scene scene = createScene();
        stage.setScene(scene);
        stage.setTitle("Milionerzy Świętokrzyskiego - Gra");
        stage.setResizable(false);
        stage.show();
    }

    public void setGameState(GameState loadedState) {
        this.gameState = loadedState;
        this.players.clear();
        this.players.addAll(loadedState.getPlayers());
        loadedState.addEventListener(this);
        // Also refresh components
        boardComponent.refreshPawns(this.players, null);
    }
    
    private Board createBoardModel() {
        String[][] boardTiles = BoardComponent.getBoardTiles();
        List<Tile> tiles = new ArrayList<>();
        
        tiles.add(new Tile(0, "START"));
        for (int i = 1; i <= 9; i++) tiles.add(createTileFromData(i, boardTiles[i]));
        tiles.add(new Tile(10, "WIĘZIENIE"));
        for (int i = 11; i <= 19; i++) tiles.add(createTileFromData(i, boardTiles[i]));
        tiles.add(new Tile(20, "DARMOWY PARKING"));
        for (int i = 21; i <= 29; i++) tiles.add(createTileFromData(i, boardTiles[i]));
        tiles.add(new Tile(30, "IDŹ DO WIĘZIENIA"));
        for (int i = 31; i <= 39; i++) tiles.add(createTileFromData(i, boardTiles[i]));
        
        return new Board(tiles);
    }
    
    private Tile createTileFromData(int pos, String[] data) {
        String name = data[0].replace("\n", " ");
        String type = data[1];
        switch (type) {
            case "property":
                int price;
                try {
                    price = Integer.parseInt(data[3]);
                } catch (NumberFormatException e) {
                    System.err.println("Warning: Invalid price format for property '" + name + "' at position " + pos + ". Using default value 100.");
                    price = 100; // Default property price
                }
                return new PropertyTile(pos, name, price, price / 10);
            case "chance": return new ChanceTile(pos, name);
            case "chest": return new CommunityChestTile(pos, name);
            case "railroad": return new PropertyTile(pos, name, 200, 25);
            case "utility": return new PropertyTile(pos, name, 150, 20);
            default: return new Tile(pos, name);
        }
    }

    @Override
    public void onGameEvent(GameEvent event) {
        switch (event.getType()) {
            case DICE_ROLLED -> {
                int val = (int) event.getData();
                 Platform.runLater(() -> {
                     SoundManager.getInstance().playSound("dice.mp3");
                     diceComponent.animateDiceRoll(val);
                 });
            }
            case PLAYER_MOVED -> {
                Player p = (Player) event.getSource();
                int currentPos = p.getPosition();
                int steps = (int) event.getData();
                int tempOldPos = (currentPos - steps);
                if (tempOldPos < 0) tempOldPos += 40;
                final int oldPos = tempOldPos;
                Platform.runLater(() -> boardComponent.animatePlayerMovement(p, oldPos, currentPos));
            }
            case AUCTION_STARTED -> {
                if (event.getData() instanceof com.kaluzaplotecka.milionerzy.model.Auction auction) {
                     Platform.runLater(() -> auctionView.show(auction));
                }
            }
            case AUCTION_BID -> {
                if (gameState.getCurrentAuction() != null) {
                     Platform.runLater(() -> auctionView.updateAuction(gameState.getCurrentAuction()));
                }
            }
            case AUCTION_ENDED -> {
                 Platform.runLater(() -> {
                     refreshBoard();
                     new Timeline(new KeyFrame(Duration.seconds(2), ae -> auctionView.hide())).play();
                 });
            }
            case PROPERTY_LANDED_NOT_OWNED -> {
                Player p = (Player) event.getSource();
                PropertyTile tile = (PropertyTile) event.getData();
                Platform.runLater(() -> {
                    if (p.getId().equals(playerId)) {
                        showPropertyPurchaseDialog(tile);
                    } 
                });
            }
            case PROPERTY_BOUGHT, MONEY_CHANGED, RENT_PAID, TRADE_ACCEPTED -> {
                 Platform.runLater(() -> refreshBoard());
            }
            default -> {}
        }
        
        if (event.getType() == GameEvent.Type.TURN_STARTED) {
             Platform.runLater(this::updateRollButtonState);
        }
    }

    private void updateRollButtonState() {
        if (diceComponent == null || gameState == null) return;
        boolean isMyTurn = false;
        Player current = gameState.getCurrentPlayer();
        
        if (networkManager != null && (networkManager.getMode() == NetworkManager.Mode.CLIENT || networkManager.getMode() == NetworkManager.Mode.HOST)) {
             if (current != null && current.getId().equals(playerId)) isMyTurn = true;
        } else {
             isMyTurn = true; // Local
        }
        
        if (isMyTurn) {
            diceComponent.setRollButtonState(true, "🎲  Losuj");
        } else {
            String name = current != null ? current.getUsername() : "";
            diceComponent.setRollButtonState(false, "Tura: " + name);
        }
    }
}
