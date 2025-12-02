package com.kaluzaplotecka.milionerzy;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MainMenu extends Application {

    private Stage primaryStage;

    private static final String BUTTON_STYLE = 
        "-fx-background-color: linear-gradient(to right, #667eea, #764ba2); " +
        "-fx-text-fill: white; " +
        "-fx-border-color: transparent; " +
        "-fx-border-radius: 30; " +
        "-fx-background-radius: 30; " +
        "-fx-padding: 15 40; " +
        "-fx-font-size: 22px; " +
        "-fx-font-weight: bold; " +
        "-fx-cursor: hand; " +
        "-fx-effect: dropshadow(gaussian, rgba(102, 126, 234, 0.5), 15, 0, 0, 5);";

    private static final String BUTTON_HOVER_STYLE = 
        "-fx-background-color: linear-gradient(to right, #764ba2, #667eea); " +
        "-fx-text-fill: white; " +
        "-fx-border-color: transparent; " +
        "-fx-border-radius: 30; " +
        "-fx-background-radius: 30; " +
        "-fx-padding: 15 40; " +
        "-fx-font-size: 22px; " +
        "-fx-font-weight: bold; " +
        "-fx-cursor: hand; " +
        "-fx-effect: dropshadow(gaussian, rgba(118, 75, 162, 0.7), 20, 0, 0, 8);";
    
    private static final String EXIT_BUTTON_STYLE = 
        "-fx-background-color: linear-gradient(to right, #ff6b6b, #ee5a5a); " +
        "-fx-text-fill: white; " +
        "-fx-border-color: transparent; " +
        "-fx-border-radius: 30; " +
        "-fx-background-radius: 30; " +
        "-fx-padding: 15 40; " +
        "-fx-font-size: 22px; " +
        "-fx-font-weight: bold; " +
        "-fx-cursor: hand; " +
        "-fx-effect: dropshadow(gaussian, rgba(255, 107, 107, 0.5), 15, 0, 0, 5);";
    
    private static final String EXIT_BUTTON_HOVER_STYLE = 
        "-fx-background-color: linear-gradient(to right, #ee5a5a, #ff6b6b); " +
        "-fx-text-fill: white; " +
        "-fx-border-color: transparent; " +
        "-fx-border-radius: 30; " +
        "-fx-background-radius: 30; " +
        "-fx-padding: 15 40; " +
        "-fx-font-size: 22px; " +
        "-fx-font-weight: bold; " +
        "-fx-cursor: hand; " +
        "-fx-effect: dropshadow(gaussian, rgba(238, 90, 90, 0.7), 20, 0, 0, 8);";

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        
        // Tytuł gry z efektem cienia
        Label titleLabel = new Label("Milionerzy Świętokrzyskiego");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 64));
        titleLabel.setStyle("-fx-text-fill: linear-gradient(to right, #667eea, #764ba2);");
        titleLabel.setTextFill(Color.web("#2d3436"));
        
        DropShadow titleShadow = new DropShadow();
        titleShadow.setColor(Color.rgb(102, 126, 234, 0.3));
        titleShadow.setRadius(20);
        titleShadow.setOffsetY(5);
        titleLabel.setEffect(titleShadow);
        
        // Podtytuł
        Label subtitleLabel = new Label("🎲 Gra planszowa 🎲");
        subtitleLabel.setFont(Font.font("System", FontWeight.NORMAL, 24));
        subtitleLabel.setTextFill(Color.web("#636e72"));

        // Przyciski menu z animacjami
        Button createGameBtn = createMenuButton("🎮  Stwórz Grę", false);
        Button joinGameBtn = createMenuButton("🌐  Dołącz do gry", false);
        Button settingsBtn = createMenuButton("⚙️  Ustawienia", false);
        Button loadGameBtn = createMenuButton("📂  Wczytaj grę", false);
        Button authorsBtn = createMenuButton("👥  Autorzy", false);
        Button exitBtn = createMenuButton("🚪  Wyjdź z gry", true);

        // Akcje przycisków
        createGameBtn.setOnAction(e -> onCreateGame());
        joinGameBtn.setOnAction(e -> onJoinGame());
        settingsBtn.setOnAction(e -> onSettings());
        loadGameBtn.setOnAction(e -> onLoadGame());
        authorsBtn.setOnAction(e -> onAuthors());
        exitBtn.setOnAction(e -> Platform.exit());

        // Kontener na tytuły
        VBox titleBox = new VBox(10);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.getChildren().addAll(titleLabel, subtitleLabel);

        // Kontener na przyciski
        VBox buttonsBox = new VBox(18);
        buttonsBox.setAlignment(Pos.CENTER);
        buttonsBox.getChildren().addAll(
            createGameBtn, 
            joinGameBtn, 
            settingsBtn, 
            loadGameBtn, 
            authorsBtn, 
            exitBtn
        );
        
        // Animacja wejścia dla przycisków
        int delay = 0;
        for (var node : buttonsBox.getChildren()) {
            node.setOpacity(0);
            node.setTranslateY(30);
            
            FadeTransition fade = new FadeTransition(Duration.millis(400), node);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.setDelay(Duration.millis(200 + delay));
            
            TranslateTransition translate = new TranslateTransition(Duration.millis(400), node);
            translate.setFromY(30);
            translate.setToY(0);
            translate.setDelay(Duration.millis(200 + delay));
            
            fade.play();
            translate.play();
            
            delay += 80;
        }

        // Główny kontener
        VBox mainLayout = new VBox(60);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setPadding(new Insets(60, 80, 60, 80));
        mainLayout.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #f5f7fa, #e4e8f0);"
        );
        mainLayout.getChildren().addAll(titleBox, buttonsBox);
        
        // Dekoracyjne elementy
        StackPane root = new StackPane();
        root.getChildren().add(mainLayout);
        
        // Dekoracyjne kółko w tle (lewy górny róg)
        Rectangle decorCircle1 = new Rectangle(300, 300);
        decorCircle1.setArcWidth(300);
        decorCircle1.setArcHeight(300);
        decorCircle1.setFill(Color.rgb(102, 126, 234, 0.1));
        decorCircle1.setTranslateX(-500);
        decorCircle1.setTranslateY(-300);
        
        // Dekoracyjne kółko w tle (prawy dolny róg)
        Rectangle decorCircle2 = new Rectangle(400, 400);
        decorCircle2.setArcWidth(400);
        decorCircle2.setArcHeight(400);
        decorCircle2.setFill(Color.rgb(118, 75, 162, 0.08));
        decorCircle2.setTranslateX(450);
        decorCircle2.setTranslateY(250);
        
        root.getChildren().addAll(decorCircle1, decorCircle2);
        decorCircle1.toBack();
        decorCircle2.toBack();
        mainLayout.toFront();

        Scene scene = new Scene(root, 1440, 900);
        this.primaryStage.setTitle("Milionerzy Świętokrzyskiego");
        this.primaryStage.setScene(scene);
        this.primaryStage.setMinWidth(1000);
        this.primaryStage.setMinHeight(700);
        this.primaryStage.show();
        
        // Animacja tytułu
        titleLabel.setOpacity(0);
        subtitleLabel.setOpacity(0);
        
        FadeTransition titleFade = new FadeTransition(Duration.millis(800), titleLabel);
        titleFade.setFromValue(0);
        titleFade.setToValue(1);
        titleFade.play();
        
        FadeTransition subtitleFade = new FadeTransition(Duration.millis(800), subtitleLabel);
        subtitleFade.setFromValue(0);
        subtitleFade.setToValue(1);
        subtitleFade.setDelay(Duration.millis(300));
        subtitleFade.play();
    }

    private Button createMenuButton(String text, boolean isExit) {
        Button button = new Button(text);
        
        String normalStyle = isExit ? EXIT_BUTTON_STYLE : BUTTON_STYLE;
        String hoverStyle = isExit ? EXIT_BUTTON_HOVER_STYLE : BUTTON_HOVER_STYLE;
        
        button.setStyle(normalStyle);
        button.setMinWidth(320);
        button.setMinHeight(55);
        
        // Hover animation
        button.setOnMouseEntered(e -> {
            button.setStyle(hoverStyle);
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), button);
            scale.setToX(1.05);
            scale.setToY(1.05);
            scale.play();
        });
        
        button.setOnMouseExited(e -> {
            button.setStyle(normalStyle);
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), button);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.play();
        });
        
        // Click animation
        button.setOnMousePressed(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(100), button);
            scale.setToX(0.95);
            scale.setToY(0.95);
            scale.play();
        });
        
        button.setOnMouseReleased(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(100), button);
            scale.setToX(1.05);
            scale.setToY(1.05);
            scale.play();
        });
        
        return button;
    }

    // === Handlery przycisków (do implementacji) ===

    private void onCreateGame() {
        System.out.println("Tworzenie nowej gry...");
        // Przejście do planszy gry
        GameBoardView boardView = new GameBoardView(primaryStage);
        boardView.show();
    }

    private void onJoinGame() {
        System.out.println("Dołączanie do gry...");
        // TODO: Przejście do ekranu dołączania
    }

    private void onSettings() {
        System.out.println("Ustawienia...");
        // TODO: Przejście do ekranu ustawień
    }

    private void onLoadGame() {
        System.out.println("Wczytywanie gry...");
        // TODO: Przejście do ekranu wczytywania
    }

    private void onAuthors() {
        System.out.println("Autorzy...");
        // TODO: Wyświetlenie informacji o autorach
    }
}