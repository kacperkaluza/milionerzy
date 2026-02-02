package com.kaluzaplotecka.milionerzy.view.utils;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

public class ViewFactory {
    
    private ViewFactory() {} // Prevent instantiation

    public static Scene createStyledScene(Parent root, int width, int height) {
        return new Scene(root, width, height);
    }

    public static VBox createCard() {
        VBox card = new VBox(20);
        card.setStyle(UIConstants.CARD_STYLE);
        card.setPadding(new Insets(30));
        return card;
    }

    public static TextField createStyledTextField(String prompt) {
        TextField textField = new TextField();
        textField.setPromptText(prompt);
        textField.setStyle(UIConstants.INPUT_STYLE);
        textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            textField.setStyle(newVal ? UIConstants.INPUT_FOCUS_STYLE : UIConstants.INPUT_STYLE);
        });
        return textField;
    }
    
    public static Label createHeaderLabel(String text, int fontSize) {
        Label label = new Label(text);
        label.setFont(Font.font("System", FontWeight.BOLD, fontSize));
        label.setTextFill(Color.web(UIConstants.TEXT_PRIMARY));
        return label;
    }
}
