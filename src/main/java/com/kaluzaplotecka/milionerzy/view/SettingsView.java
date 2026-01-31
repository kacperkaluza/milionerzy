package com.kaluzaplotecka.milionerzy.view;

import com.kaluzaplotecka.milionerzy.manager.SoundManager;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SettingsView {
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

    public SettingsView(Stage stage, Runnable onBack) {
        this.stage = stage;
        this.onBack = onBack;
    }

    public void show() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #f5f7fa, #e4e8f0);");

        Label title = new Label("Ustawienia");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #2d3436;");

        // Głośność
        VBox volumeBox = new VBox(10);
        volumeBox.setAlignment(Pos.CENTER);
        Label volumeLabel = new Label("Głośność");
        volumeLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #636e72;");
        Slider volumeSlider = new Slider(0, 100, 50);
        volumeSlider.setMaxWidth(300);
        volumeBox.getChildren().addAll(volumeLabel, volumeSlider);

        // Initialize slider and add listener
        SoundManager soundManager = SoundManager.getInstance();
        volumeSlider.setValue(soundManager.getVolume() * 100);
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            soundManager.setVolume(newVal.doubleValue() / 100.0);
        });

        Button backBtn = new Button("Powrót");
        backBtn.setStyle(BUTTON_STYLE);
        backBtn.setOnAction(e -> onBack.run());

        root.getChildren().addAll(title, volumeBox, backBtn);

        Scene scene = new Scene(root, 1440, 900);
        stage.setScene(scene);
        stage.setTitle("Milionerzy - Ustawienia");
    }
}
