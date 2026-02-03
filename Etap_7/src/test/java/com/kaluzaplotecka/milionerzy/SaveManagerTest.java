package com.kaluzaplotecka.milionerzy;

import org.junit.jupiter.api.*;

import com.kaluzaplotecka.milionerzy.model.Board;
import com.kaluzaplotecka.milionerzy.model.GameState;
import com.kaluzaplotecka.milionerzy.model.Player;
import com.kaluzaplotecka.milionerzy.model.SaveManager;
import com.kaluzaplotecka.milionerzy.model.SaveManager.SaveInfo;
import com.kaluzaplotecka.milionerzy.model.tiles.PropertyTile;
import com.kaluzaplotecka.milionerzy.model.tiles.Tile;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Testy dla funkcjonalności zapisu i wczytywania gry.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SaveManagerTest {

    private static final String TEST_SAVE_NAME = "test_save_unit";
    
    private Board createTestBoard() {
        List<Tile> tiles = new ArrayList<>();
        tiles.add(new Tile(0, "START"));
        tiles.add(new PropertyTile(1, "Kielce", 100, 20));
        tiles.add(new PropertyTile(2, "Sandomierz", 120, 25));
        tiles.add(new PropertyTile(3, "Starachowice", 140, 30));
        tiles.add(new Tile(4, "Szansa"));
        tiles.add(new PropertyTile(5, "Ostrowiec", 160, 35));
        return new Board(tiles);
    }
    
    @Test
    @Order(1)
    @DisplayName("Zapisywanie stanu gry")
    void testSaveGame() throws IOException {
        // Given
        List<Player> players = new ArrayList<>();
        players.add(new Player("player-1", "Gracz1", 1500));
        players.add(new Player("player-2", "Gracz2", 1200));
        
        GameState gameState = new GameState(createTestBoard(), players);
        gameState.setRandom(new Random(42));
        
        // Wykonaj kilka ruchów
        gameState.getPlayers().get(0).moveTo(3);
        gameState.getPlayers().get(0).deductMoney(100);
        gameState.nextTurn();
        gameState.nextTurn();
        
        // When
        String filename = SaveManager.save(gameState, TEST_SAVE_NAME);
        
        // Then
        assertNotNull(filename);
        assertTrue(filename.contains(TEST_SAVE_NAME));
        assertTrue(filename.endsWith(".save"));
    }
    
    @Test
    @Order(2)
    @DisplayName("Wczytywanie zapisanej gry")
    void testLoadGame() throws IOException, ClassNotFoundException {
        // Given - zapis z poprzedniego testu
        List<SaveInfo> saves = SaveManager.listSaves();
        SaveInfo testSave = saves.stream()
            .filter(s -> s.getDisplayName().equals(TEST_SAVE_NAME))
            .findFirst()
            .orElse(null);
        
        assertNotNull(testSave, "Powinien istnieć zapis z poprzedniego testu");
        
        // When
        GameState loadedState = SaveManager.load(testSave.getFilename());
        
        // Then
        assertNotNull(loadedState);
        assertEquals(2, loadedState.getPlayers().size());
        
        Player player1 = loadedState.getPlayers().get(0);
        assertEquals("Gracz1", player1.getUsername());
        assertEquals(3, player1.getPosition());
        assertEquals(1400, player1.getMoney());
        
        assertEquals(1, loadedState.getRoundNumber());
    }
    
    @Test
    @Order(3)
    @DisplayName("Lista zapisów zawiera metadane")
    void testListSaves() {
        // When
        List<SaveInfo> saves = SaveManager.listSaves();
        
        // Then
        assertNotNull(saves);
        
        SaveInfo testSave = saves.stream()
            .filter(s -> s.getDisplayName().equals(TEST_SAVE_NAME))
            .findFirst()
            .orElse(null);
        
        assertNotNull(testSave);
        assertEquals(2, testSave.getPlayerCount());
        assertEquals(1, testSave.getRoundNumber());
        assertNotNull(testSave.getSavedAt());
    }
    
    @Test
    @Order(4)
    @DisplayName("Mapowanie ID graczy z lobby na wczytany stan")
    void testPlayerIdMapping() throws IOException, ClassNotFoundException {
        // Given - wczytany stan z poprzedniego testu
        List<SaveInfo> saves = SaveManager.listSaves();
        SaveInfo testSave = saves.stream()
            .filter(s -> s.getDisplayName().equals(TEST_SAVE_NAME))
            .findFirst()
            .orElse(null);
        
        assertNotNull(testSave);
        GameState loadedState = SaveManager.load(testSave.getFilename());
        
        // Nowi gracze z "lobby" z nowymi ID
        String newId1 = "lobby-new-1";
        String newId2 = "lobby-new-2";
        String newName1 = "NowyGracz1";
        String newName2 = "NowyGracz2";
        
        // When - symulacja mapowania jak w LobbyView.launchGame()
        List<Player> savedPlayers = loadedState.getPlayers();
        savedPlayers.get(0).setId(newId1);
        savedPlayers.get(0).setName(newName1);
        savedPlayers.get(1).setId(newId2);
        savedPlayers.get(1).setName(newName2);
        
        // Then - ID i nazwy zaktualizowane, ale pozycje i pieniądze zachowane
        Player p1 = loadedState.getPlayers().get(0);
        assertEquals(newId1, p1.getId());
        assertEquals(newName1, p1.getUsername());
        assertEquals(3, p1.getPosition(), "Pozycja powinna być zachowana z zapisu");
        assertEquals(1400, p1.getMoney(), "Pieniądze powinny być zachowane z zapisu");
        
        Player p2 = loadedState.getPlayers().get(1);
        assertEquals(newId2, p2.getId());
        assertEquals(newName2, p2.getUsername());
    }
    
    @Test
    @Order(5)
    @DisplayName("Aktualny gracz po wczytaniu i mapowaniu")
    void testCurrentPlayerAfterMapping() throws IOException, ClassNotFoundException {
        // Given
        List<SaveInfo> saves = SaveManager.listSaves();
        SaveInfo testSave = saves.stream()
            .filter(s -> s.getDisplayName().equals(TEST_SAVE_NAME))
            .findFirst()
            .orElse(null);
        
        assertNotNull(testSave);
        GameState loadedState = SaveManager.load(testSave.getFilename());
        
        // Zmień ID pierwszego gracza (hosta)
        String hostId = "new-host-id";
        loadedState.getPlayers().get(0).setId(hostId);
        
        // When - sprawdź czy getCurrentPlayer zwraca odpowiedniego gracza
        Player currentPlayer = loadedState.getCurrentPlayer();
        
        // Then
        assertNotNull(currentPlayer);
        // Gracz powinien mieć zaktualizowane ID jeśli to gracz 0
        if (loadedState.getPlayers().indexOf(currentPlayer) == 0) {
            assertEquals(hostId, currentPlayer.getId());
        }
    }
    
    @Test
    @Order(6)
    @DisplayName("Usuwanie zapisu")
    void testDeleteSave() {
        // Given
        List<SaveInfo> savesBefore = SaveManager.listSaves();
        SaveInfo testSave = savesBefore.stream()
            .filter(s -> s.getDisplayName().equals(TEST_SAVE_NAME))
            .findFirst()
            .orElse(null);
        
        assertNotNull(testSave);
        
        // When
        boolean deleted = SaveManager.delete(testSave.getFilename());
        
        // Then
        assertTrue(deleted);
        
        List<SaveInfo> savesAfter = SaveManager.listSaves();
        boolean stillExists = savesAfter.stream()
            .anyMatch(s -> s.getFilename().equals(testSave.getFilename()));
        assertFalse(stillExists, "Zapis powinien zostać usunięty");
    }
    
    @Test
    @Order(7)
    @DisplayName("Wczytywanie nieistniejącego pliku rzuca wyjątek")
    void testLoadNonExistentFile() {
        assertThrows(IOException.class, () -> {
            SaveManager.load("non_existent_file.save");
        });
    }
}
