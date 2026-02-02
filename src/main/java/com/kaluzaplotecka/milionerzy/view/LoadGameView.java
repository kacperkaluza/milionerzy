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
import com.kaluzaplotecka.milionerzy.view.components.GameButton;
import com.kaluzaplotecka.milionerzy.view.utils.UIConstants;
import com.kaluzaplotecka.milionerzy.view.utils.ViewFactory;

import java.util.List;

public class LoadGameView {
    private final Stage stage;
    private final Runnable onBack;
    private ListView<SaveInfo> savesList;

    public LoadGameView(Stage stage, Runnable onBack) {
        this.stage = stage;
        this.onBack = onBack;
    }

    public void show() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle(UIConstants.BACKGROUND_GRADIENT);

        Label title = new Label("📂 Wczytaj Grę");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: " + UIConstants.TEXT_PRIMARY + ";");

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
        
        GameButton loadBtn = new GameButton("▶️ Gra lokalna", this::loadSelectedGame);
        loadBtn.setGradient("#27ae60", "#2ecc71");
        loadBtn.setDisabledStyle(true);
        
        GameButton hostBtn = new GameButton("🌐 Hostuj grę", this::hostSelectedGame);
        hostBtn.setDisabledStyle(true);
        
        GameButton deleteBtn = new GameButton("🗑️ Usuń", this::deleteSelectedSave);
        deleteBtn.setGradient("#e74c3c", "#c0392b");
        deleteBtn.setDisabledStyle(true);
        
        GameButton refreshBtn = new GameButton("🔄 Odśwież", this::refreshSavesList);
        
        GameButton backBtn = new GameButton("↩️ Powrót", onBack);
        
        // Aktywuj przyciski po wybraniu zapisu
        savesList.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            boolean hasSelection = newVal != null;
            loadBtn.setDisabledStyle(!hasSelection);
            hostBtn.setDisabledStyle(!hasSelection);
            deleteBtn.setDisabledStyle(!hasSelection);
        });
        
        buttonsBox.getChildren().addAll(loadBtn, hostBtn, deleteBtn, refreshBtn, backBtn);
        
        // Informacja gdy brak zapisów
        Label noSavesLabel = new Label("Brak zapisanych gier");
        noSavesLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #7f8c8d;");
        noSavesLabel.setVisible(false);
        
        VBox.setVgrow(savesList, Priority.ALWAYS);
        root.getChildren().addAll(title, savesList, noSavesLabel, buttonsBox);

        Scene scene = ViewFactory.createStyledScene(root, 1440, 900);
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
            
            GameView gameBoard = new GameView(
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
    
    private void hostSelectedGame() {
        SaveInfo selected = savesList.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        
        // Dialog z nazwą gracza-hosta
        TextInputDialog dialog = new TextInputDialog("Host");
        dialog.setTitle("Hostuj grę");
        dialog.setHeaderText("Podaj swoją nazwę gracza");
        dialog.setContentText("Nazwa:");
        
        dialog.showAndWait().ifPresent(playerName -> {
            if (playerName.trim().isEmpty()) return;
            
            try {
                GameState loadedState = SaveManager.load(selected.getFilename());
                
                // Otwórz lobby jako host z wczytanym stanem
                LobbyView lobby = new LobbyView(
                    stage,
                    playerName.trim(),
                    loadedState,  // przekazujemy wczytany stan
                    () -> show()  // callback powrotu
                );
                lobby.show();
                
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Błąd");
                alert.setHeaderText("Nie udało się wczytać gry");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            }
        });
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
