package com.kaluzaplotecka.milionerzy.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import com.kaluzaplotecka.milionerzy.model.GameState;
import com.kaluzaplotecka.milionerzy.model.SaveManager;
import com.kaluzaplotecka.milionerzy.model.SaveManager.SaveInfo;

import java.util.List;

public class LoadGameView {
    private final Stage stage;
    private final Runnable onBack;
    private ListView<SaveInfo> savesList;

    private static final String BUTTON_STYLE = 
        "-fx-background-color: linear-gradient(to right, #667eea, #764ba2); " +
        "-fx-text-fill: white; " +
        "-fx-border-radius: 30; " +
        "-fx-background-radius: 30; " +
        "-fx-padding: 10 30; " +
        "-fx-font-size: 16px; " +
        "-fx-font-weight: bold; " +
        "-fx-cursor: hand;";
    
    private static final String BUTTON_GREEN_STYLE = 
        "-fx-background-color: linear-gradient(to right, #27ae60, #2ecc71); " +
        "-fx-text-fill: white; " +
        "-fx-border-radius: 30; " +
        "-fx-background-radius: 30; " +
        "-fx-padding: 10 30; " +
        "-fx-font-size: 16px; " +
        "-fx-font-weight: bold; " +
        "-fx-cursor: hand;";
    
    private static final String BUTTON_RED_STYLE = 
        "-fx-background-color: linear-gradient(to right, #e74c3c, #c0392b); " +
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
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #f5f7fa, #e4e8f0);");

        Label title = new Label("📂 Wczytaj Grę");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #2d3436;");

        // Lista zapisów
        savesList = new ListView<>();
        savesList.setMaxWidth(600);
        savesList.setMaxHeight(400);
        savesList.setStyle("-fx-control-inner-background: white; -fx-background-radius: 10;");
        
        // Ładuj zapisy
        refreshSavesList();
        
        // Ustaw wyświetlanie
        savesList.setCellFactory(lv -> new ListCell<SaveInfo>() {
            @Override
            protected void updateItem(SaveInfo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toString());
                    setStyle("-fx-font-size: 14px; -fx-padding: 10;");
                }
            }
        });
        
        // Przyciski akcji
        HBox buttonsBox = new HBox(15);
        buttonsBox.setAlignment(Pos.CENTER);
        
        Button loadBtn = new Button("▶️ Wczytaj");
        loadBtn.setStyle(BUTTON_GREEN_STYLE);
        loadBtn.setOnAction(e -> loadSelectedGame());
        loadBtn.setDisable(true);
        
        Button deleteBtn = new Button("🗑️ Usuń");
        deleteBtn.setStyle(BUTTON_RED_STYLE);
        deleteBtn.setOnAction(e -> deleteSelectedSave());
        deleteBtn.setDisable(true);
        
        Button refreshBtn = new Button("🔄 Odśwież");
        refreshBtn.setStyle(BUTTON_STYLE);
        refreshBtn.setOnAction(e -> refreshSavesList());
        
        Button backBtn = new Button("↩️ Powrót");
        backBtn.setStyle(BUTTON_STYLE);
        backBtn.setOnAction(e -> onBack.run());
        
        // Aktywuj przyciski po wybraniu zapisu
        savesList.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            boolean hasSelection = newVal != null;
            loadBtn.setDisable(!hasSelection);
            deleteBtn.setDisable(!hasSelection);
        });
        
        buttonsBox.getChildren().addAll(loadBtn, deleteBtn, refreshBtn, backBtn);
        
        // Informacja gdy brak zapisów
        Label noSavesLabel = new Label("Brak zapisanych gier");
        noSavesLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #7f8c8d;");
        noSavesLabel.setVisible(false);
        
        VBox.setVgrow(savesList, Priority.ALWAYS);
        root.getChildren().addAll(title, savesList, noSavesLabel, buttonsBox);

        Scene scene = new Scene(root, 1440, 900);
        stage.setScene(scene);
        stage.setTitle("Milionerzy - Wczytaj Grę");
    }
    
    private void refreshSavesList() {
        List<SaveInfo> saves = SaveManager.listSaves();
        savesList.getItems().clear();
        savesList.getItems().addAll(saves);
    }
    
    private void loadSelectedGame() {
        SaveInfo selected = savesList.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        
        try {
            GameState loadedState = SaveManager.load(selected.getFilename());
            
            // Uruchom grę z wczytanym stanem
            // Gracz lokalny = pierwszy gracz z listy
            String localPlayerId = loadedState.getPlayers().get(0).getId();
            
            GameBoardView gameBoard = new GameBoardView(
                stage, 
                loadedState.getPlayers(), 
                null, // brak NetworkManager - gra lokalna
                localPlayerId
            );
            
            // Ustaw wczytany stan
            gameBoard.setGameState(loadedState);
            gameBoard.show();
            
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Błąd wczytywania");
            alert.setHeaderText("Nie udało się wczytać gry");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
    
    private void deleteSelectedSave() {
        SaveInfo selected = savesList.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Potwierdź usunięcie");
        confirm.setHeaderText("Czy na pewno chcesz usunąć ten zapis?");
        confirm.setContentText(selected.getDisplayName());
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (SaveManager.delete(selected.getFilename())) {
                    refreshSavesList();
                } else {
                    Alert error = new Alert(Alert.AlertType.ERROR);
                    error.setTitle("Błąd");
                    error.setContentText("Nie udało się usunąć zapisu.");
                    error.showAndWait();
                }
            }
        });
    }
}
