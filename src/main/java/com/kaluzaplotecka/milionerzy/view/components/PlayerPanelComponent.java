package com.kaluzaplotecka.milionerzy.view.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import com.kaluzaplotecka.milionerzy.model.Player;

public class PlayerPanelComponent extends VBox {

    private final Label nameLabel;
    private final Label moneyLabel;
    private final Label propertiesLabel;
    private final Circle avatar;

    public PlayerPanelComponent(String name, int money, String accentColor) {
        setAlignment(Pos.CENTER);
        setPadding(new Insets(10));
        setMinWidth(150);
        setMaxWidth(150);
        
        setStyle(
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
        setEffect(shadow);
        
        avatar = new Circle(20);
        avatar.setFill(Color.web(accentColor));
        
        Label avatarIcon = new Label("👤");
        avatarIcon.setFont(Font.font(16));
        StackPane avatarPane = new StackPane(avatar, avatarIcon);
        
        nameLabel = new Label(name);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        nameLabel.setTextFill(Color.web("#2d3436"));
        nameLabel.setId("nameLabel_" + name.replaceAll("\\s+", "_"));
        
        HBox moneyBox = new HBox(3);
        moneyBox.setAlignment(Pos.CENTER);
        moneyLabel = new Label(String.format("%,d", money));
        moneyLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        moneyLabel.setTextFill(Color.web("#27ae60"));
        moneyLabel.setId("moneyLabel_" + name.replaceAll("\\s+", "_"));
        
        Label currencyLabel = new Label("zł");
        currencyLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        currencyLabel.setTextFill(Color.web("#636e72"));
        moneyBox.getChildren().addAll(moneyLabel, currencyLabel);
        
        propertiesLabel = new Label("🏠 0 nieruchomości");
        propertiesLabel.setFont(Font.font("System", FontWeight.NORMAL, 10));
        propertiesLabel.setTextFill(Color.web("#636e72"));
        propertiesLabel.setId("propertiesLabel_" + name.replaceAll("\\s+", "_"));
        
        getChildren().addAll(avatarPane, nameLabel, moneyBox, propertiesLabel);
    }
    
    public void update(Player p) {
        moneyLabel.setText(String.format("%,d", p.getMoney()));
        propertiesLabel.setText("🏠 " + p.getOwnedProperties().size() + " nieruchomości");
    }
}
