package com.kaluzaplotecka.milionerzy.view;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Widok informacji o autorach gry.
 * 
 * <p>Wyświetla listę twórców projektu.
 * 
 */
public class AuthorsView {
    private final Stage stage;
    private final Runnable onBack;

    private static final String BUTTON_STYLE = 
        "-fx-background-color: linear-gradient(to right, #667eea, #764ba2); " +
        "-fx-text-fill: white; " +
        "-fx-border-radius: 30; " +
        "-fx-background-radius: 30; " +
        "-fx-padding: 10 30; " +
        "-fx-font-size: 16px; " +
        "-fx-font-weight: bold; " +
        "-fx-cursor: hand;";

    public AuthorsView(Stage stage, Runnable onBack) {
        this.stage = stage;
        this.onBack = onBack;
    }

    public void show() {
        VBox root = new VBox(30);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #f5f7fa, #e4e8f0);");

        Label title = new Label("Autorzy");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #2d3436;");

        VBox contentBox = new VBox(15);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setStyle("-fx-background-color: white; -fx-padding: 30; -fx-background-radius: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 5);");
        contentBox.setMaxWidth(500);

        Label authorsLabel = new Label("Zespół Projektowy");
        authorsLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #667eea;");
        
        Label namesLabel = new Label("Kacper Kałuża\nJulia Płotecka");
        namesLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #2d3436; -fx-text-alignment: center;");
        
        contentBox.getChildren().addAll(authorsLabel, namesLabel);

        Button backBtn = new Button("Powrót");
        backBtn.setStyle(BUTTON_STYLE);
        backBtn.setOnAction(e -> onBack.run());

        root.getChildren().addAll(title, contentBox, backBtn);

        Scene scene = new Scene(root, 1440, 900);
        stage.setScene(scene);
        stage.setTitle("Milionerzy - Autorzy");
    }
}
