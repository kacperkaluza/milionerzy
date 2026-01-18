package com.kaluzaplotecka.milionerzy.view;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoadGameView {
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

    public LoadGameView(Stage stage, Runnable onBack) {
        this.stage = stage;
        this.onBack = onBack;
    }

    public void show() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #f5f7fa, #e4e8f0);");

        Label title = new Label("Wczytaj Grę");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #2d3436;");

        ListView<String> gamesList = new ListView<>();
        gamesList.getItems().addAll("Gra 1 - 2026-01-18", "Gra 2 - 2025-12-24", "AutoSave");
        gamesList.setMaxWidth(400);
        gamesList.setMaxHeight(300);
        gamesList.setStyle("-fx-control-inner-background: white; -fx-background-radius: 10;");

        Button backBtn = new Button("Powrót");
        backBtn.setStyle(BUTTON_STYLE);
        backBtn.setOnAction(e -> onBack.run());

        root.getChildren().addAll(title, gamesList, backBtn);

        Scene scene = new Scene(root, 1440, 900);
        stage.setScene(scene);
        stage.setTitle("Milionerzy - Wczytaj Grę");
    }
}
