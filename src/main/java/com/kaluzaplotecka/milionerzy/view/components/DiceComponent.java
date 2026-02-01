package com.kaluzaplotecka.milionerzy.view.components;

import javafx.animation.KeyFrame;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.Random;
import java.util.function.Consumer;

public class DiceComponent extends VBox {

    private static class DiceView {
        final StackPane stackPane;
        final Label label;
        
        DiceView(StackPane stackPane, Label label) {
            this.stackPane = stackPane;
            this.label = label;
        }
    }

    private final StackPane[] diceStacks = new StackPane[2];
    private final Label[] diceLabels = new Label[2];
    private final Button rollButton;
    private final Button saveButton;
    private final Random random = new Random();
    
    private Runnable onRollAction;
    private Consumer<String> onSaveAction;
    private Runnable onAnimationFinished;

    public DiceComponent() {
        setAlignment(Pos.CENTER);
        setSpacing(20);
        setMaxSize(280, 200);
        
        setStyle(
            "-fx-background-color: #edf0e7;" +
            "-fx-background-radius: 15;" +
            "-fx-padding: 25;"
        );
        
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.15));
        shadow.setRadius(15);
        shadow.setOffsetY(5);
        setEffect(shadow);
        
        HBox diceBox = new HBox(20);
        diceBox.setAlignment(Pos.CENTER);
        
        for (int i = 0; i < 2; i++) {
            DiceView diceView = createDice(1);
            diceStacks[i] = diceView.stackPane;
            diceLabels[i] = diceView.label;
            diceBox.getChildren().add(diceView.stackPane);
        }
        
        rollButton = new Button("🎲  Losuj");
        styleButton(rollButton, "#3498db", "#2980b9");
        rollButton.setOnAction(e -> {
            if (onRollAction != null) onRollAction.run();
        });
        
        saveButton = new Button("💾  Zapisz grę");
        styleButton(saveButton, "#27ae60", "#219a52");
        saveButton.setStyle(saveButton.getStyle() + "-fx-font-size: 14px; -fx-padding: 8 20;");
        saveButton.setOnAction(e -> showSaveDialog());
        
        getChildren().addAll(diceBox, rollButton, saveButton);
    }
    
    private DiceView createDice(int value) {
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
        return new DiceView(diceStack, valueLabel);
    }
    
    private void styleButton(Button btn, String color, String hoverColor) {
        String baseStyle = 
            "-fx-background-color: " + color + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 18px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 12 35;" +
            "-fx-background-radius: 25;" +
            "-fx-cursor: hand;";
            
        String hoverStyle = 
            "-fx-background-color: " + hoverColor + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 18px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 12 35;" +
            "-fx-background-radius: 25;" +
            "-fx-cursor: hand;";
            
        btn.setStyle(baseStyle);
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(baseStyle));
    }
    
    public void setOnRoll(Runnable param) {
        this.onRollAction = param;
    }
    
    public void setOnSave(Consumer<String> onSave) {
        this.onSaveAction = onSave;
    }
    
    public void setOnAnimationFinished(Runnable param) {
        this.onAnimationFinished = param;
    }

    public void setRollButtonState(boolean enabled, String text) {
        rollButton.setDisable(!enabled);
        rollButton.setText(text);
        rollButton.setOpacity(enabled ? 1.0 : 0.5);
    }
    
    public void animateDiceRoll(int sum) {
        for (int i = 0; i < diceStacks.length; i++) {
            StackPane diceStack = diceStacks[i];
            Label diceLabel = diceLabels[i];
            
            RotateTransition rotate = new RotateTransition(Duration.millis(500), diceStack);
            rotate.setByAngle(360);
            rotate.setCycleCount(2);
            
            if (i == diceStacks.length - 1) {
                rotate.setOnFinished(e -> {
                    if (onAnimationFinished != null) onAnimationFinished.run();
                });
            }
            rotate.play();
            
            Timeline timeline = new Timeline();
            for (int k = 0; k < 10; k++) {
                KeyFrame keyFrame = new KeyFrame(Duration.millis(k * 80), e -> 
                    diceLabel.setText(getDiceSymbol(random.nextInt(6) + 1)));
                timeline.getKeyFrames().add(keyFrame);
            }
            
            // Valid dice sums are 2-12; clamp the input sum to this range
            int clampedSum = Math.max(2, Math.min(12, sum));
            int dice1;
            int dice2;
            if (clampedSum <= 7) {
                // For sums 2-7, choose 1 and (sum - 1), both within 1-6
                dice1 = 1;
                dice2 = clampedSum - 1;
            } else {
                // For sums 8-12, choose 6 and (sum - 6), both within 1-6
                dice1 = 6;
                dice2 = clampedSum - 6;
            }
            final int finalValue = (i == 0) ? dice1 : dice2;
            timeline.setOnFinished(e -> diceLabel.setText(getDiceSymbol(finalValue)));
            timeline.play();
        }
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
    
    private void showSaveDialog() {
         TextInputDialog dialog = new TextInputDialog("Moja gra");
         dialog.setTitle("Zapisz grę");
         dialog.setHeaderText("Podaj nazwę zapisu");
         dialog.setContentText("Nazwa:");
         dialog.showAndWait().ifPresent(saveName -> {
             if (onSaveAction != null) {
                 onSaveAction.accept(saveName);
             }
         });
    }
}
