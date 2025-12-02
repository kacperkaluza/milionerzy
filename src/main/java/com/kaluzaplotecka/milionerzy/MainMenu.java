package com.kaluzaplotecka.milionerzy;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class MainMenu extends Application {

    private static final String BUTTON_STYLE = 
        "-fx-background-color: white; " +
        "-fx-border-color: black; " +
        "-fx-border-width: 1; " +
        "-fx-border-radius: 59; " +
        "-fx-background-radius: 59; " +
        "-fx-padding: 12 24; " +
        "-fx-font-size: 24px; " +
        "-fx-cursor: hand;";

    private static final String BUTTON_HOVER_STYLE = 
        "-fx-background-color: #f0f0f0; " +
        "-fx-border-color: black; " +
        "-fx-border-width: 1; " +
        "-fx-border-radius: 59; " +
        "-fx-background-radius: 59; " +
        "-fx-padding: 12 24; " +
        "-fx-font-size: 24px; " +
        "-fx-cursor: hand;";

    @Override
    public void start(Stage primaryStage) {
        // Tytuł gry
        Label titleLabel = new Label("Milionerzy Świętokrzyskiego");
        titleLabel.setFont(Font.font("Inter", FontWeight.LIGHT, 72));
        titleLabel.setStyle("-fx-text-fill: black;");

        // Przyciski menu
        Button createGameBtn = createMenuButton("Stwórz Grę");
        Button joinGameBtn = createMenuButton("Dołącz do gry");
        Button settingsBtn = createMenuButton("Ustawienia");
        Button loadGameBtn = createMenuButton("Wczytaj grę");
        Button authorsBtn = createMenuButton("Autorzy");
        Button exitBtn = createMenuButton("Wyjdź z gry");

        // Akcje przycisków
        createGameBtn.setOnAction(e -> onCreateGame());
        joinGameBtn.setOnAction(e -> onJoinGame());
        settingsBtn.setOnAction(e -> onSettings());
        loadGameBtn.setOnAction(e -> onLoadGame());
        authorsBtn.setOnAction(e -> onAuthors());
        exitBtn.setOnAction(e -> Platform.exit());

        // Kontener na przyciski
        VBox buttonsBox = new VBox(20);
        buttonsBox.setAlignment(Pos.CENTER);
        buttonsBox.getChildren().addAll(
            createGameBtn, 
            joinGameBtn, 
            settingsBtn, 
            loadGameBtn, 
            authorsBtn, 
            exitBtn
        );

        // Główny kontener
        VBox mainLayout = new VBox(80);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setPadding(new Insets(56, 74, 56, 74));
        mainLayout.setStyle("-fx-background-color: white;");
        mainLayout.getChildren().addAll(titleLabel, buttonsBox);

        Scene scene = new Scene(mainLayout, 1440, 900);
        primaryStage.setTitle("Milionerzy Świętokrzyskiego");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Button createMenuButton(String text) {
        Button button = new Button(text);
        button.setStyle(BUTTON_STYLE);
        button.setMinWidth(315);
        button.setOnMouseEntered(e -> button.setStyle(BUTTON_HOVER_STYLE));
        button.setOnMouseExited(e -> button.setStyle(BUTTON_STYLE));
        return button;
    }

    // === Handlery przycisków (do implementacji) ===

    private void onCreateGame() {
        System.out.println("Tworzenie nowej gry...");
        // TODO: Przejście do ekranu tworzenia gry
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