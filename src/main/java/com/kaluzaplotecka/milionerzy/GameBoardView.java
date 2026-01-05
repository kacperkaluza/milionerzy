package com.kaluzaplotecka.milionerzy;

import javafx.animation.KeyFrame;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;
import java.util.Random;

public class GameBoardView {
    
    private Stage stage;
    private final List<Player> players;
    private Label[] diceLabels;
    private StackPane[] diceStacks;
    private Button rollButton;
    private Random random = new Random();
    private VBox[] playerPanels;
    
    // Nazwy pól świętokrzyskich
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
        {"Bodzentyn", "property", "#FF0000", "220"},
        {"Nowa Słupia", "property", "#FF0000", "240"},
        {"Dworzec\nOstrowiec", "railroad"},
        {"Chmielnik", "property", "#FFFF00", "260"},
        {"Daleszyce", "property", "#FFFF00", "260"},
        {"Wodociągi", "utility"},
        {"Staszów", "property", "#FFFF00", "280"},
        
        // Róg - Idź do Więzienia
        {"IDŹ DO\nWIĘZIENIA", "corner"},
        
        // Prawa krawędź (od "Idź do więzienia" w dół)
        {"Włoszczowa", "property", "#00FF00", "300"},
        {"Kazimierza\nWielka", "property", "#00FF00", "300"},
        {"Skarbonka", "chest"},
        {"Miechów", "property", "#00FF00", "320"},
        {"Dworzec\nBusko", "railroad"},
        {"Szansa", "chance"},
        {"Łysogóry", "property", "#0000FF", "350"},
        {"Podatek\nod luksusu", "tax"},
        {"Góra\nŚwiętokrzyska", "property", "#0000FF", "400"}
    };
    
    public GameBoardView(Stage stage, List<Player> players) {
        this.stage = stage;
        this.players = players;
        this.diceLabels = new Label[2];
        this.diceStacks = new StackPane[2];
        this.playerPanels = new VBox[4];
    }
    
    public Scene createScene() {
        // Główny kontener z gradientem
        HBox root = new HBox(10);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #88bde7, #dbebea);");
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(10));
        
        // Default colors for up to 4 players
        String[] colors = {"#667eea", "#e74c3c", "#27ae60", "#f39c12"};
        
        // Create panels for present players
        for (int i = 0; i < 4; i++) {
            if (i < players.size()) {
                Player p = players.get(i);
                playerPanels[i] = createPlayerPanel(p.getUsername(), p.getMoney(), colors[i%colors.length]);
            } else {
                // Invisible placeholder or null? 
                // Let's create an invisible/empty panel to maintain layout if needed,
                // or just handle it in the layout addition below.
                playerPanels[i] = null;
            }
        }

        // Lewa kolumna - gracze 1 i 3
        VBox leftPlayers = new VBox(20);
        leftPlayers.setAlignment(Pos.CENTER);
        if (playerPanels[0] != null) leftPlayers.getChildren().add(playerPanels[0]);
        if (playerPanels[2] != null) leftPlayers.getChildren().add(playerPanels[2]);
        
        // Prawa kolumna - gracze 2 i 4 + przycisk pauzy
        VBox rightPlayers = new VBox(20);
        rightPlayers.setAlignment(Pos.CENTER);
        
        if (playerPanels[1] != null) rightPlayers.getChildren().add(playerPanels[1]);
        Button pauseBtn = createPauseButton();
        rightPlayers.getChildren().add(pauseBtn); // Pause button always present
        if (playerPanels[3] != null) rightPlayers.getChildren().add(playerPanels[3]);
        
        // Plansza centralna
        Pane boardPane = createBoard();
        
        // Kostki i przycisk w środku planszy
        VBox diceArea = createDiceArea();
        
        // StackPane na planszę + kostki
        StackPane centerPane = new StackPane();
        centerPane.getChildren().addAll(boardPane, diceArea);
        
        root.getChildren().addAll(leftPlayers, centerPane, rightPlayers);
        
        Scene scene = new Scene(root, 1100, 750);
        return scene;
    }
    
    private static final double BOARD_SIZE = 600;
    private static final double CORNER_SIZE = 75;
    private static final double TILE_WIDTH = 50;
    private static final double TILE_HEIGHT = 75;
    
    private Pane createBoard() {
        // Kontener na planszę
        Pane boardContainer = new Pane();
        boardContainer.setMaxSize(BOARD_SIZE, BOARD_SIZE);
        boardContainer.setMinSize(BOARD_SIZE, BOARD_SIZE);
        
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
        
        // === DOLNA KRAWĘDŹ (START -> prawo) ===
        // Róg START (prawy dolny)
        boardContainer.getChildren().add(
            createCornerTile(BOARD_SIZE - CORNER_SIZE, BOARD_SIZE - CORNER_SIZE, CORNER_SIZE, "START\n➡️", Color.web("#e8f5e9"))
        );
        
        // Dolna krawędź (od prawej do lewej, pomijając rogi)
        for (int i = 0; i < 9; i++) {
            double x = BOARD_SIZE - CORNER_SIZE - (i + 1) * TILE_WIDTH;
            double y = BOARD_SIZE - TILE_HEIGHT;
            String[] tileData = BOARD_TILES[9 - i];
            boardContainer.getChildren().add(createTile(x, y, TILE_WIDTH, TILE_HEIGHT, tileData, false));
        }
        
        // Róg WIĘZIENIE (lewy dolny)
        boardContainer.getChildren().add(
            createJailCorner(0, BOARD_SIZE - CORNER_SIZE, CORNER_SIZE)
        );
        
        // === LEWA KRAWĘDŹ (Więzienie -> góra) ===
        for (int i = 0; i < 9; i++) {
            double x = 0;
            double y = BOARD_SIZE - CORNER_SIZE - (i + 1) * TILE_WIDTH;
            String[] tileData = BOARD_TILES[11 + i];
            boardContainer.getChildren().add(createTile(x, y - (TILE_HEIGHT - TILE_WIDTH), TILE_HEIGHT, TILE_WIDTH, tileData, true));
        }
        
        // Róg DARMOWY PARKING (lewy górny)
        boardContainer.getChildren().add(
            createCornerTile(0, 0, CORNER_SIZE, "DARMOWY\nPARKING\n🅿️", Color.WHITE)
        );
        
        // === GÓRNA KRAWĘDŹ (Parking -> prawo) ===
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
        
        // === PRAWA KRAWĘDŹ (Idź do więzienia -> dół) ===
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
        
        // Centrowanie planszy
        StackPane wrapper = new StackPane(boardContainer);
        wrapper.setAlignment(Pos.CENTER);
        
        return wrapper;
    }
    
    private StackPane createTile(double x, double y, double width, double height, String[] tileData, boolean vertical) {
        StackPane tile = new StackPane();
        tile.setLayoutX(x);
        tile.setLayoutY(y);
        tile.setPrefSize(width, height);
        
        // Tło pola
        Rectangle bg = new Rectangle(width, height);
        bg.setFill(Color.WHITE);
        bg.setStroke(Color.BLACK);
        bg.setStrokeWidth(0.5);
        
        VBox content = new VBox(2);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(2));
        
        String type = tileData[1];
        
        if (type.equals("property")) {
            // Pasek koloru nieruchomości
            Rectangle colorBar = new Rectangle(width - 4, vertical ? 18 : 22);
            colorBar.setFill(Color.web(tileData[2]));
            colorBar.setStroke(Color.BLACK);
            colorBar.setStrokeWidth(0.5);
            
            VBox propertyContent = new VBox(2);
            propertyContent.setAlignment(Pos.CENTER);
            
            // Nazwa
            Label name = new Label(tileData[0]);
            name.setFont(Font.font("System", FontWeight.NORMAL, vertical ? 6 : 7));
            name.setTextAlignment(TextAlignment.CENTER);
            name.setWrapText(true);
            name.setMaxWidth(width - 6);
            
            // Cena
            Label price = new Label(tileData[3] + " zł");
            price.setFont(Font.font("System", FontWeight.BOLD, 6));
            
            if (vertical) {
                content.getChildren().addAll(colorBar, name, price);
            } else {
                content.getChildren().addAll(colorBar, name, price);
            }
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
        
        // Pomarańczowy obszar "W WIĘZIENIU"
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
    
    private VBox createPlayerPanel(String name, int money, String accentColor) {
        VBox panel = new VBox(5);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(10));
        panel.setMinWidth(150);
        panel.setMaxWidth(150);
        panel.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 10;" +
            "-fx-border-radius: 10;" +
            "-fx-border-color: " + accentColor + ";" +
            "-fx-border-width: 3;"
        );
        
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.2));
        shadow.setRadius(8);
        shadow.setOffsetY(2);
        panel.setEffect(shadow);
        
        // Avatar gracza
        Circle avatar = new Circle(20);
        avatar.setFill(Color.web(accentColor));
        
        Label avatarIcon = new Label("👤");
        avatarIcon.setFont(Font.font(16));
        StackPane avatarPane = new StackPane(avatar, avatarIcon);
        
        // Nazwa gracza
        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        nameLabel.setTextFill(Color.web("#2d3436"));
        
        // Pieniądze
        HBox moneyBox = new HBox(3);
        moneyBox.setAlignment(Pos.CENTER);
        Label moneyLabel = new Label(String.format("%,d", money));
        moneyLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        moneyLabel.setTextFill(Color.web("#27ae60"));
        Label currencyLabel = new Label("zł");
        currencyLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        currencyLabel.setTextFill(Color.web("#636e72"));
        moneyBox.getChildren().addAll(moneyLabel, currencyLabel);
        
        // Nieruchomości
        Label propertiesLabel = new Label("🏠 0 nieruchomości");
        propertiesLabel.setFont(Font.font("System", FontWeight.NORMAL, 10));
        propertiesLabel.setTextFill(Color.web("#636e72"));
        
        panel.getChildren().addAll(avatarPane, nameLabel, moneyBox, propertiesLabel);
        return panel;
    }
    
    private VBox createDiceArea() {
        VBox diceArea = new VBox(20);
        diceArea.setAlignment(Pos.CENTER);
        diceArea.setMaxSize(280, 200);
        
        // Tło dla kostek
        diceArea.setStyle(
            "-fx-background-color: #edf0e7;" +
            "-fx-background-radius: 15;" +
            "-fx-padding: 25;"
        );
        
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.15));
        shadow.setRadius(15);
        shadow.setOffsetY(5);
        diceArea.setEffect(shadow);
        
        // Kostki
        HBox diceBox = new HBox(20);
        diceBox.setAlignment(Pos.CENTER);
        
        for (int i = 0; i < 2; i++) {
            StackPane dice = createDice(1);
            diceStacks[i] = dice;
            diceLabels[i] = (Label) ((StackPane) dice.getChildren().get(0)).getChildren().get(1);
            diceBox.getChildren().add(dice);
        }
        
        // Przycisk rzutu
        rollButton = new Button("🎲  Losuj");
        rollButton.setStyle(
            "-fx-background-color: #3498db;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 18px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 12 35;" +
            "-fx-background-radius: 25;" +
            "-fx-cursor: hand;"
        );
        
        rollButton.setOnMouseEntered(e -> rollButton.setStyle(
            "-fx-background-color: #2980b9;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 18px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 12 35;" +
            "-fx-background-radius: 25;" +
            "-fx-cursor: hand;"
        ));
        
        rollButton.setOnMouseExited(e -> rollButton.setStyle(
            "-fx-background-color: #3498db;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 18px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 12 35;" +
            "-fx-background-radius: 25;" +
            "-fx-cursor: hand;"
        ));
        
        rollButton.setOnAction(e -> rollDice());
        
        diceArea.getChildren().addAll(diceBox, rollButton);
        return diceArea;
    }
    
    private StackPane createDice(int value) {
        StackPane diceStack = new StackPane();
        
        Rectangle dice = new Rectangle(80, 80);
        dice.setFill(Color.WHITE);
        dice.setArcWidth(20);
        dice.setArcHeight(20);
        dice.setStroke(Color.web("#bdc3c7"));
        dice.setStrokeWidth(2);
        
        DropShadow diceShadow = new DropShadow();
        diceShadow.setColor(Color.rgb(0, 0, 0, 0.2));
        diceShadow.setRadius(8);
        diceShadow.setOffsetY(3);
        dice.setEffect(diceShadow);
        
        Label valueLabel = new Label(getDiceSymbol(value));
        valueLabel.setFont(Font.font("System", FontWeight.BOLD, 40));
        valueLabel.setTextFill(Color.web("#2d3436"));
        
        StackPane dicePane = new StackPane(dice, valueLabel);
        diceStack.getChildren().add(dicePane);
        
        return diceStack;
    }
    
    private String getDiceSymbol(int value) {
        return switch (value) {
            case 1 -> "⚀";
            case 2 -> "⚁";
            case 3 -> "⚂";
            case 4 -> "⚃";
            case 5 -> "⚄";
            case 6 -> "⚅";
            default -> "⚀";
        };
    }
    
    private void rollDice() {
        if (rollButton != null) {
            rollButton.setDisable(true);
        }
        
        // Animacja rzutu kostkami
        for (int i = 0; i < diceStacks.length; i++) {
            StackPane diceStack = diceStacks[i];
            Label diceLabel = diceLabels[i];
            
            RotateTransition rotate = new RotateTransition(Duration.millis(500), diceStack);
            rotate.setByAngle(360);
            rotate.setCycleCount(2);
            
            // Re-enable button after the last dice animation finishes
            if (i == diceStacks.length - 1) {
                rotate.setOnFinished(e -> {
                    if (rollButton != null) {
                        rollButton.setDisable(false);
                    }
                });
            }
            
            rotate.play();
            
            // Animacja zmiany wartości
            Timeline timeline = new Timeline();
            for (int k = 0; k < 10; k++) {
                KeyFrame keyFrame = new KeyFrame(Duration.millis(k * 80), e -> {
                    diceLabel.setText(getDiceSymbol(random.nextInt(6) + 1));
                });
                timeline.getKeyFrames().add(keyFrame);
            }
            
            timeline.setOnFinished(e -> {
                int finalValue = random.nextInt(6) + 1;
                diceLabel.setText(getDiceSymbol(finalValue));
            });
            
            timeline.play();
        }
    }
    
    private Button createPauseButton() {
        Button pauseBtn = new Button("⏸");
        pauseBtn.setStyle(
            "-fx-background-color: rgba(255,255,255,0.9);" +
            "-fx-font-size: 28px;" +
            "-fx-padding: 10;" +
            "-fx-background-radius: 50;" +
            "-fx-cursor: hand;"
        );
        
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.2));
        shadow.setRadius(10);
        pauseBtn.setEffect(shadow);
        
        pauseBtn.setOnAction(e -> {
            // Powrót do menu
            MainMenu mainMenu = new MainMenu();
            try {
                mainMenu.start(stage);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        
        return pauseBtn;
    }
    
    public void show() {
        Scene scene = createScene();
        stage.setScene(scene);
        stage.setTitle("Milionerzy Świętokrzyskiego - Gra");
        stage.setResizable(false); // Blokujemy resize
        stage.show();
    }
}
