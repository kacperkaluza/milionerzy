package com.kaluzaplotecka.milionerzy.view.components;

import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import com.kaluzaplotecka.milionerzy.model.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Komponent odpowiedzialny za rysowanie planszy i zarządzanie pionkami.
 */
public class BoardComponent extends StackPane {

    private static final double BOARD_SIZE = 600;
    private static final double CORNER_SIZE = 75;
    private static final double TILE_WIDTH = 50;
    private static final double TILE_HEIGHT = 75;

    // Tymczasowe przeniesienie definicji
    private static final String[][] BOARD_TILES = {
        // Dolna krawędź (od START w prawo)
        {"START", "corner"},
        {"Kielce\nCentrum", "property", "#8B4513", "60"},
        {"Szansa", "chance"},
        {"Kielce\nHerby", "property", "#8B4513", "60"},
        {"Podatek\nDochodowy", "tax"},
        {"Dworzec\nKielce", "railroad"},
        {"Sandomierz", "property", "#87CEEB", "100"},
        {"Szansa", "chance"},
        {"Ostrowiec\nŚw.", "property", "#87CEEB", "100"},
        {"Starachowice", "property", "#87CEEB", "120"},
        
        // Róg - Więzienie
        {"WIĘZIENIE", "corner"},
        
        // Lewa krawędź (od Więzienia w górę)
        {"Jędrzejów", "property", "#FF69B4", "140"},
        {"Elektrownia", "utility"},
        {"Busko-Zdrój", "property", "#FF69B4", "140"},
        {"Pińczów", "property", "#FF69B4", "160"},
        {"Dworzec\nSkarżysko", "railroad"},
        {"Końskie", "property", "#FFA500", "180"},
        {"Skarbonka", "chest"},
        {"Skarżysko\nKamienna", "property", "#FFA500", "180"},
        {"Suchedniów", "property", "#FFA500", "200"},
        
        // Róg - Darmowy Parking
        {"DARMOWY\nPARKING", "corner"},
        
        // Górna krawędź (od Parkingu w prawo)
        {"Chęciny", "property", "#FF0000", "220"},
        {"Szansa", "chance"},
        {"Piekoszów", "property", "#FF0000", "220"},
        {"Morawica", "property", "#FF0000", "240"},
        {"Dworzec\nStarachowice", "railroad"},
        {"Włoszczowa", "property", "#FFFF00", "260"},
        {"Wąchock", "property", "#FFFF00", "260"},
        {"Wodociągi", "utility"},
        {"Opatów", "property", "#FFFF00", "280"},
        
        // Róg - Idź do więzienia
        {"IDŹ DO\nWIĘZIENIA", "corner"},
        
        // Prawa krawędź (od Idź do więzienia w dół)
        {"Staszów", "property", "#008000", "300"},
        {"Zagnańsk", "property", "#008000", "300"},
        {"Skarbonka", "chest"},
        {"Łagów", "property", "#008000", "320"},
        {"Dworzec\nJędrzejów", "railroad"},
        {"Szansa", "chance"},
        {"Ciekoty", "property", "#00008B", "350"},
        {"Podatek\nOd luksusu", "tax"},
        {"Św. Katarzyna", "property", "#00008B", "400"}
    };

    private final Pane boardContainer;
    private final Pane playerLayer;
    private final Map<Player, Circle> playerPawns;
    private final List<Player> players;

    public BoardComponent(List<Player> players) {
        this.players = players;
        this.playerPawns = new HashMap<>();
        
        this.setAlignment(Pos.CENTER);
        
        // Kontener na planszę
        this.boardContainer = new Pane();
        boardContainer.setMaxSize(BOARD_SIZE, BOARD_SIZE);
        boardContainer.setMinSize(BOARD_SIZE, BOARD_SIZE);
        
        // Warstwa dla pionków
        this.playerLayer = new Pane();
        playerLayer.setPickOnBounds(false); // Pozwalamy klikać "przez" warstwę pionków
        playerLayer.setPrefSize(BOARD_SIZE, BOARD_SIZE);

        drawBoard();
        
        this.getChildren().addAll(boardContainer, playerLayer); 
        // Note: boardContainer contains background and tiles. playerLayer is overlay.
        // Wait, current structure in GameView put playerLayer INSIDE boardContainer.
        // Let's stick to that to ensure coordinates match parent.
        boardContainer.getChildren().add(playerLayer);
        
        // Wait, getChildren().addAll(boardContainer) is enough if playerLayer is inside boardContainer.
        this.getChildren().clear();
        this.getChildren().add(boardContainer);
        
        initializePawns();
    }
    
    private void drawBoard() {
        // Tło planszy
        Rectangle boardBg = new Rectangle(BOARD_SIZE, BOARD_SIZE);
        boardBg.setFill(Color.web("#c8e6c9"));
        boardBg.setArcWidth(0);
        boardBg.setArcHeight(0);
        boardBg.setStroke(Color.web("#2e7d32"));
        boardBg.setStrokeWidth(3);
        
        // Efekt cienia
        DropShadow boardShadow = new DropShadow();
        boardShadow.setColor(Color.rgb(0, 0, 0, 0.3));
        boardShadow.setRadius(20);
        boardShadow.setOffsetY(8);
        boardBg.setEffect(boardShadow);
        
        boardContainer.getChildren().add(boardBg);
        
        // === RYSOWANIE PÓL ===
        
        // Róg START (prawy dolny)
        boardContainer.getChildren().add(
            createCornerTile(BOARD_SIZE - CORNER_SIZE, BOARD_SIZE - CORNER_SIZE, CORNER_SIZE, "START\n➡️", Color.web("#e8f5e9"))
        );
        
        // Dolna krawędź
        for (int i = 0; i < 9; i++) {
            double x = BOARD_SIZE - CORNER_SIZE - (i + 1) * TILE_WIDTH;
            double y = BOARD_SIZE - TILE_HEIGHT;
            String[] tileData = BOARD_TILES[i + 1];
            boardContainer.getChildren().add(createTile(x, y, TILE_WIDTH, TILE_HEIGHT, tileData, false));
        }
        
        // Róg WIĘZIENIE (lewy dolny)
        boardContainer.getChildren().add(
            createJailCorner(0, BOARD_SIZE - CORNER_SIZE, CORNER_SIZE)
        );
        
        // Lewa krawędź
        for (int i = 0; i < 9; i++) {
            double x = 0;
            double y = BOARD_SIZE - CORNER_SIZE - (i + 1) * TILE_WIDTH;
            String[] tileData = BOARD_TILES[11 + i];
            boardContainer.getChildren().add(createTile(x, y, TILE_HEIGHT, TILE_WIDTH, tileData, true));
        }
        
        // Róg DARMOWY PARKING (lewy górny)
        boardContainer.getChildren().add(
            createCornerTile(0, 0, CORNER_SIZE, "DARMOWY\nPARKING\n🅿️", Color.WHITE)
        );
        
        // Górna krawędź
        for (int i = 0; i < 9; i++) {
            double x = CORNER_SIZE + i * TILE_WIDTH;
            double y = 0;
            String[] tileData = BOARD_TILES[21 + i];
            boardContainer.getChildren().add(createTile(x, y, TILE_WIDTH, TILE_HEIGHT, tileData, false));
        }
        
        // Róg IDŹ DO WIĘZIENIA (prawy górny)
        boardContainer.getChildren().add(
            createCornerTile(BOARD_SIZE - CORNER_SIZE, 0, CORNER_SIZE, "IDŹ DO\nWIĘZIENIA\n👮", Color.web("#ffebee"))
        );
        
        // Prawa krawędź
        for (int i = 0; i < 9; i++) {
            double x = BOARD_SIZE - TILE_HEIGHT;
            double y = CORNER_SIZE + i * TILE_WIDTH;
            String[] tileData = BOARD_TILES[31 + i];
            boardContainer.getChildren().add(createTile(x, y, TILE_HEIGHT, TILE_WIDTH, tileData, true));
        }
        
        // Środkowa część planszy - logo/nazwa gry
        VBox centerContent = new VBox(10);
        centerContent.setAlignment(Pos.CENTER);
        centerContent.setLayoutX(CORNER_SIZE + 20);
        centerContent.setLayoutY(CORNER_SIZE + 20);
        centerContent.setPrefSize(BOARD_SIZE - 2 * CORNER_SIZE - 40, BOARD_SIZE - 2 * CORNER_SIZE - 40);
        
        Label gameLogo = new Label("MILIONERZY\nŚWIĘTOKRZYSKIEGO");
        gameLogo.setFont(Font.font("System", FontWeight.BOLD, 22));
        gameLogo.setTextFill(Color.web("#2e7d32"));
        gameLogo.setTextAlignment(TextAlignment.CENTER);
        gameLogo.setWrapText(true);
        
        centerContent.getChildren().add(gameLogo);
        boardContainer.getChildren().add(centerContent);
    }
    
    public void initializePawns() {
        playerLayer.getChildren().clear();
        playerPawns.clear();
        
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            String colorHex = switch (i % 4) {
                case 0 -> "#667eea";
                case 1 -> "#e74c3c";
                case 2 -> "#27ae60";
                case 3 -> "#f39c12";
                default -> "black";
            };
            
            Circle pawn = new Circle(10);
            pawn.setFill(Color.web(colorHex));
            pawn.setStroke(Color.BLACK);
            pawn.setStrokeWidth(1);
            
            Point2D startPos = getTileCenter(p.getPosition());
            double offsetX = (i % 2 == 0 ? -5 : 5);
            double offsetY = (i < 2 ? -5 : 5);
            
            pawn.setTranslateX(startPos.getX() + offsetX);
            pawn.setTranslateY(startPos.getY() + offsetY);
            
            playerPawns.put(p, pawn);
            playerLayer.getChildren().add(pawn);
        }
    }
    
    public void refreshPawns(List<Player> currentPlayers, Map<String, Integer> oldPositions) {
        // Logika synchronizacji pionków (podobna do syncPawnsOnStateChange)
        // Jeśli lista graczy się zmieniła (np. ktoś zbankrutował/doszedł), musimy odświeżyć mapę
        
        // Uproszczona wersja: jeśli lista graczy się zmieniła całkowicie, reinicjalizacja
        // Jeśli tylko pozycje, to animacja
        
        // Na potrzeby tego zadania skopiujmy logikę syncPawnsOnStateChange,
        // ale dostosowaną do komponentu.
        
        for (int i = 0; i < currentPlayers.size(); i++) {
            Player p = currentPlayers.get(i);
            Circle existingPawn = null;
            
            // Szukaj pionka po ID gracza (bo obiekt gracza mógł zostać odtworzony z sieci/zapisu)
            for (var entry : playerPawns.entrySet()) {
                if (entry.getKey().getId().equals(p.getId())) {
                    existingPawn = entry.getValue();
                    playerPawns.remove(entry.getKey());
                    playerPawns.put(p, existingPawn); // Aktualizuj klucz na nowy obiekt gracza
                    break;
                }
            }
            
            if (existingPawn != null) {
                // Animuj
                Integer oldPos = oldPositions != null ? oldPositions.get(p.getId()) : null;
                int newPos = p.getPosition();
                
                if (oldPos != null && oldPos != newPos) {
                   animatePlayerMovement(p, oldPos, newPos);
                } else {
                   // Skok bez animacji (np. load game)
                   Point2D target = getTileCenter(newPos);
                   double offsetX = (i % 2 == 0 ? -5 : 5);
                   double offsetY = (i < 2 ? -5 : 5);
                   existingPawn.setTranslateX(target.getX() + offsetX);
                   existingPawn.setTranslateY(target.getY() + offsetY);
                }
            } else {
                // Stwórz nowy pionek
                // Powielona logika tworzenia - warto wydzielić do createPawn w przyszłości
                String colorHex = switch (i % 4) {
                    case 0 -> "#667eea";
                    case 1 -> "#e74c3c";
                    case 2 -> "#27ae60";
                    case 3 -> "#f39c12";
                    default -> "black";
                };
                
                Circle pawn = new Circle(10);
                pawn.setFill(Color.web(colorHex));
                pawn.setStroke(Color.BLACK);
                pawn.setStrokeWidth(1);
                
                Point2D startPos = getTileCenter(p.getPosition());
                double offsetX = (i % 2 == 0 ? -5 : 5);
                double offsetY = (i < 2 ? -5 : 5);
                
                pawn.setTranslateX(startPos.getX() + offsetX);
                pawn.setTranslateY(startPos.getY() + offsetY);
                
                playerPawns.put(p, pawn);
                playerLayer.getChildren().add(pawn);
            }
        }
    }
    
    public void animatePlayerMovement(Player player, int oldPos, int newPos) {
        Circle pawn = playerPawns.get(player);
        if (pawn == null) return;
        
        SequentialTransition seq = new SequentialTransition();
        int steps = newPos - oldPos;
        if (steps < 0) steps += 40;
        
        int pIndex = players.indexOf(player);
        double offsetX = (pIndex % 2 == 0 ? -5 : 5);
        double offsetY = (pIndex < 2 ? -5 : 5);
        
        int current = oldPos;
        for (int i = 0; i < steps; i++) {
            current = (current + 1) % 40;
            Point2D nextPoint = getTileCenter(current);
            
            TranslateTransition move = new TranslateTransition(Duration.millis(300), pawn);
            move.setToX(nextPoint.getX() + offsetX);
            move.setToY(nextPoint.getY() + offsetY);
            seq.getChildren().add(move);
        }
        seq.play();
    }

    private Point2D getTileCenter(int index) {
        index = index % 40;
        if (index < 0) index += 40;
        
        double x = 0;
        double y = 0;
        
        if (index == 0) { // START
            x = BOARD_SIZE - CORNER_SIZE / 2;
            y = BOARD_SIZE - CORNER_SIZE / 2;
        } else if (index < 10) { // Dolna
            double rightEdgeOfTiles = BOARD_SIZE - CORNER_SIZE;
            x = rightEdgeOfTiles - ((index - 1) * TILE_WIDTH) - TILE_WIDTH / 2.0;
            y = BOARD_SIZE - TILE_HEIGHT / 2.0;
        } else if (index == 10) { // WIĘZIENIE
            x = CORNER_SIZE / 2;
            y = BOARD_SIZE - CORNER_SIZE / 2;
        } else if (index < 20) { // Lewa
            int k = index - 10;
            x = TILE_HEIGHT / 2.0;
            double bottomEdgeOfTiles = BOARD_SIZE - CORNER_SIZE;
            y = bottomEdgeOfTiles - ((k - 1) * TILE_WIDTH) - TILE_WIDTH / 2.0;
        } else if (index == 20) { // PARKING
            x = CORNER_SIZE / 2;
            y = CORNER_SIZE / 2;
        } else if (index < 30) { // Górna
            int k = index - 20;
            double leftEdgeOfTiles = CORNER_SIZE;
            x = leftEdgeOfTiles + ((k - 1) * TILE_WIDTH) + TILE_WIDTH / 2.0;
            y = TILE_HEIGHT / 2.0;
        } else if (index == 30) { // IDŹ DO WIĘZIENIA
            x = BOARD_SIZE - CORNER_SIZE / 2;
            y = CORNER_SIZE / 2;
        } else { // Prawa
            int k = index - 30;
            x = BOARD_SIZE - TILE_HEIGHT / 2.0;
            double topEdgeOfTiles = CORNER_SIZE;
            y = topEdgeOfTiles + ((k - 1) * TILE_WIDTH) + TILE_WIDTH / 2.0;
        }
        
        return new Point2D(x, y);
    }
    
    // --- Helper Creation Methods (Private) ---
    
    // Copied from GameView.java
    private StackPane createTile(double x, double y, double width, double height, String[] tileData, boolean vertical) {
        StackPane tile = new StackPane();
        tile.setLayoutX(x);
        tile.setLayoutY(y);
        tile.setPrefSize(width, height);
        
        Rectangle bg = new Rectangle(width, height);
        bg.setFill(Color.WHITE);
        bg.setStroke(Color.BLACK);
        bg.setStrokeWidth(0.5);
        
        VBox content = new VBox(2);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(2));
        
        String type = tileData[1];
        
        if (type.equals("property")) {
            Rectangle colorBar = new Rectangle(width - 4, vertical ? 18 : 22);
            colorBar.setFill(Color.web(tileData[2]));
            colorBar.setStroke(Color.BLACK);
            colorBar.setStrokeWidth(0.5);
            
            Label name = new Label(tileData[0]);
            name.setFont(Font.font("System", FontWeight.NORMAL, vertical ? 6 : 7));
            name.setTextAlignment(TextAlignment.CENTER);
            name.setWrapText(true);
            name.setMaxWidth(width - 6);
            
            Label price = new Label(tileData[3] + " zł");
            price.setFont(Font.font("System", FontWeight.BOLD, 6));
            
            content.getChildren().addAll(colorBar, name, price);
        } else if (type.equals("chance")) {
            Label icon = new Label("⭐");
            icon.setFont(Font.font(24));
            Label name = new Label("SZANSA");
            name.setFont(Font.font("System", FontWeight.BOLD, 6));
            content.getChildren().addAll(icon, name);
        } else if (type.equals("chest")) {
            Label icon = new Label("💰");
            icon.setFont(Font.font(24));
            Label name = new Label("SKARBONKA");
            name.setFont(Font.font("System", FontWeight.BOLD, 6));
            content.getChildren().addAll(icon, name);
        } else if (type.equals("railroad")) {
            Label icon = new Label("🚂");
            icon.setFont(Font.font(20));
            Label name = new Label(tileData[0]);
            name.setFont(Font.font("System", FontWeight.NORMAL, 6));
            name.setTextAlignment(TextAlignment.CENTER);
            name.setWrapText(true);
            Label price = new Label("200 zł");
            price.setFont(Font.font("System", FontWeight.BOLD, 6));
            content.getChildren().addAll(icon, name, price);
        } else if (type.equals("utility")) {
            String icon = tileData[0].contains("Elektr") ? "💡" : "🚰";
            Label iconLabel = new Label(icon);
            iconLabel.setFont(Font.font(20));
            Label name = new Label(tileData[0]);
            name.setFont(Font.font("System", FontWeight.NORMAL, 6));
            name.setTextAlignment(TextAlignment.CENTER);
            Label price = new Label("150 zł");
            price.setFont(Font.font("System", FontWeight.BOLD, 6));
            content.getChildren().addAll(iconLabel, name, price);
        } else if (type.equals("tax")) {
            Label icon = new Label("💸");
            icon.setFont(Font.font(20));
            Label name = new Label(tileData[0]);
            name.setFont(Font.font("System", FontWeight.NORMAL, 6));
            name.setTextAlignment(TextAlignment.CENTER);
            name.setWrapText(true);
            content.getChildren().addAll(icon, name);
        }
        
        tile.getChildren().addAll(bg, content);
        return tile;
    }
    
    private StackPane createCornerTile(double x, double y, double size, String text, Color bgColor) {
        StackPane corner = new StackPane();
        corner.setLayoutX(x);
        corner.setLayoutY(y);
        corner.setPrefSize(size, size);
        
        Rectangle bg = new Rectangle(size, size);
        bg.setFill(bgColor);
        bg.setStroke(Color.BLACK);
        bg.setStrokeWidth(1);
        
        Label label = new Label(text);
        label.setFont(Font.font("System", FontWeight.BOLD, 10));
        label.setTextAlignment(TextAlignment.CENTER);
        label.setWrapText(true);
        
        corner.getChildren().addAll(bg, label);
        return corner;
    }
    
    private StackPane createJailCorner(double x, double y, double size) {
        StackPane corner = new StackPane();
        corner.setLayoutX(x);
        corner.setLayoutY(y);
        corner.setPrefSize(size, size);
        
        Rectangle bg = new Rectangle(size, size);
        bg.setFill(Color.WHITE);
        bg.setStroke(Color.BLACK);
        bg.setStrokeWidth(1);
        
        Rectangle jailArea = new Rectangle(size * 0.6, size * 0.6);
        jailArea.setFill(Color.web("#e5710c"));
        jailArea.setTranslateX(size * 0.15);
        jailArea.setTranslateY(-size * 0.15);
        
        Label jailLabel = new Label("W\nWIĘZIENIU");
        jailLabel.setFont(Font.font("System", FontWeight.BOLD, 8));
        jailLabel.setTextFill(Color.WHITE);
        jailLabel.setTextAlignment(TextAlignment.CENTER);
        jailLabel.setTranslateX(size * 0.15);
        jailLabel.setTranslateY(-size * 0.15);
        
        Label visitLabel = new Label("ODWIEDZAJĄCY");
        visitLabel.setFont(Font.font("System", FontWeight.NORMAL, 6));
        visitLabel.setTranslateX(-size * 0.2);
        visitLabel.setTranslateY(size * 0.3);
        
        corner.getChildren().addAll(bg, jailArea, jailLabel, visitLabel);
        return corner;
    }

    public static String[][] getBoardTiles() {
        return BOARD_TILES;
    }
}
