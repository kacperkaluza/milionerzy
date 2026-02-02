package com.kaluzaplotecka.milionerzy.view;

import com.kaluzaplotecka.milionerzy.manager.SoundManager;
import com.kaluzaplotecka.milionerzy.view.components.GameButton;
import com.kaluzaplotecka.milionerzy.view.utils.UIConstants;
import com.kaluzaplotecka.milionerzy.view.utils.ViewFactory;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SettingsView {
    private final Stage stage;
    private final Runnable onBack;

    public SettingsView(Stage stage, Runnable onBack) {
        this.stage = stage;
        this.onBack = onBack;
    }

    public void show() {
        VBox root = new VBox(20);
        root.setStyle(UIConstants.BACKGROUND_GRADIENT);
        root.setPadding(new Insets(30));
        root.setSpacing(30);

        HBox header = createHeader();

        VBox volumeBox = new VBox(10);
        volumeBox.setAlignment(Pos.CENTER);
        Label volumeLabel = ViewFactory.createHeaderLabel("Głośność", 18);
        Slider volumeSlider = new Slider(0, 100, 50);
        volumeSlider.setMaxWidth(300);
        volumeBox.getChildren().addAll(volumeLabel, volumeSlider);

        SoundManager soundManager = SoundManager.getInstance();
        volumeSlider.setValue(soundManager.getVolume() * 100);
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            soundManager.setVolume(newVal.doubleValue() / 100.0);
        });

        root.getChildren().addAll(header, volumeBox);

        Scene scene = new Scene(root, 1440, 900);
        stage.setScene(scene);
        stage.setTitle("Milionerzy - Ustawienia");
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

        Label title = ViewFactory.createHeaderLabel("Ustawienia", 32);
        HBox.setMargin(title, new Insets(0, 0, 0, 30));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        header.getChildren().addAll(backBtn, title, spacer);
        
        return header;
    }

    private void handleBack() {
        onBack.run();
    }
}
