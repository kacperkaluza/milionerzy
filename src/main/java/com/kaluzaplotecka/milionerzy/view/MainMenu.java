package com.kaluzaplotecka.milionerzy.view;

import com.kaluzaplotecka.milionerzy.view.components.GameButton;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MainMenu extends Application {

    private Stage primaryStage;

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
        int buttonWidth = 200;
        int buttonHeight = 60;
        int fontSize = 24;
        GameButton createGameBtn = new GameButton("Graj", buttonWidth, buttonHeight, fontSize, () ->onCreateGame());
        GameButton joinGameBtn = new GameButton("Dołącz do gry", buttonWidth, buttonHeight, fontSize, () ->onJoinGame());
        GameButton settingsBtn = new GameButton("Ustawienia", buttonWidth, buttonHeight, fontSize, () ->onSettings());
        GameButton loadGameBtn = new GameButton("Wczytaj grę", buttonWidth, buttonHeight, fontSize, () ->onLoadGame());
        GameButton authorsBtn = new GameButton("Autorzy", buttonWidth, buttonHeight, fontSize, () ->onAuthors());
        GameButton exitBtn = new GameButton("Wyjdź z gry", buttonWidth, buttonHeight, fontSize, () -> Platform.exit());
        
        exitBtn.setGradient("#ff6b6b", "#ff4757");
        
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
        
        mainLayout.toFront();

        Scene scene = new Scene(mainLayout, 1440, 900);
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
    
    private void onCreateGame() {
        System.out.println("Tworzenie nowej gry...");
        
        // Dialog wprowadzania nazwy gracza
        TextInputDialog dialog = new TextInputDialog("Gracz");
        dialog.setTitle("Stwórz Grę");
        dialog.setHeaderText("Wprowadź swoją nazwę gracza");
        dialog.setContentText("Nazwa:");
        
        // Stylizacja dialogu
        dialog.getDialogPane().setStyle(
            "-fx-background-color: white; " +
            "-fx-font-size: 14px;"
        );
        
        dialog.showAndWait().ifPresent(playerName -> {
            if (!playerName.trim().isEmpty()) {
                LobbyView lobby = new LobbyView(
                    primaryStage, 
                    playerName.trim(), 
                    () -> start(primaryStage)  // Callback powrotu
                );
                lobby.show();
            }
        });
    }

    private void onJoinGame() {
        System.out.println("Dołączanie do gry...");
        
        // Dialog wprowadzania nazwy i kodu pokoju
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Dołącz do Gry");
        dialog.setHeaderText("Wprowadź dane do dołączenia");
        
        ButtonType joinButtonType = new ButtonType("Dołącz", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(joinButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 100, 10, 10));
        
        TextField playerNameField = new TextField();
        playerNameField.setPromptText("Twoja nazwa");
        TextField roomCodeField = new TextField();
        roomCodeField.setPromptText("Kod pokoju (np. ABC123)");
        TextField hostAddressField = new TextField("localhost");
        hostAddressField.setPromptText("Adres hosta");
        
        grid.add(new Label("Nazwa gracza:"), 0, 0);
        grid.add(playerNameField, 1, 0);
        grid.add(new Label("Kod pokoju:"), 0, 1);
        grid.add(roomCodeField, 1, 1);
        grid.add(new Label("Adres hosta:"), 0, 2);
        grid.add(hostAddressField, 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == joinButtonType) {
                return new String[]{
                    playerNameField.getText().trim(),
                    roomCodeField.getText().trim().toUpperCase(),
                    hostAddressField.getText().trim()
                };
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(result -> {
            if (!result[0].isEmpty() && !result[1].isEmpty()) {
                LobbyView lobby = new LobbyView(
                    primaryStage,
                    result[0],      // playerName
                    result[1],      // roomCode
                    result[2],      // hostAddress
                    () -> start(primaryStage)
                );
                lobby.show();
            }
        });
    }

    private void onSettings() {
        System.out.println("Ustawienia...");
        new SettingsView(primaryStage, () -> start(primaryStage)).show();
    }

    private void onLoadGame() {
        System.out.println("Wczytywanie gry...");
        new LoadGameView(primaryStage, () -> start(primaryStage)).show();
    }

    private void onAuthors() {
        System.out.println("Autorzy...");
        new AuthorsView(primaryStage, () -> start(primaryStage)).show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        System.exit(0);
    }
}