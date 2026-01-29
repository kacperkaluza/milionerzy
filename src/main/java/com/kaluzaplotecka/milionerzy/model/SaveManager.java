package com.kaluzaplotecka.milionerzy.model;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Zarządza zapisem i wczytywaniem stanu gry.
 * Zapisy przechowywane są w ~/.milionerzy/saves/
 */
public class SaveManager {
    
    private static final String SAVE_DIR = System.getProperty("user.home") + "/.milionerzy/saves";
    private static final String SAVE_EXTENSION = ".save";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    
    /**
     * Reprezentuje metadane zapisu gry.
     */
    public static class SaveInfo {
        private final String filename;
        private final String displayName;
        private final LocalDateTime savedAt;
        private final int playerCount;
        private final int roundNumber;
        
        public SaveInfo(String filename, String displayName, LocalDateTime savedAt, int playerCount, int roundNumber) {
            this.filename = filename;
            this.displayName = displayName;
            this.savedAt = savedAt;
            this.playerCount = playerCount;
            this.roundNumber = roundNumber;
        }
        
        public String getFilename() { return filename; }
        public String getDisplayName() { return displayName; }
        public LocalDateTime getSavedAt() { return savedAt; }
        public int getPlayerCount() { return playerCount; }
        public int getRoundNumber() { return roundNumber; }
        
        @Override
        public String toString() {
            return String.format("%s - Runda %d (%d graczy) - %s", 
                displayName, roundNumber, playerCount, 
                savedAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        }
    }
    
    /**
     * Inicjalizuje katalog zapisów jeśli nie istnieje.
     */
    private static void ensureSaveDir() throws IOException {
        Path savePath = Paths.get(SAVE_DIR);
        if (!Files.exists(savePath)) {
            Files.createDirectories(savePath);
        }
    }
    
    /**
     * Zapisuje stan gry do pliku.
     * @param gameState stan gry do zapisania
     * @param saveName nazwa zapisu (opcjonalna, jeśli null generowana automatycznie)
     * @return nazwa pliku zapisu
     */
    public static String save(GameState gameState, String saveName) throws IOException {
        ensureSaveDir();
        
        String timestamp = LocalDateTime.now().format(DATE_FORMAT);
        String filename;
        
        if (saveName == null || saveName.trim().isEmpty()) {
            filename = "save_" + timestamp + SAVE_EXTENSION;
        } else {
            // Usuń niedozwolone znaki z nazwy
            String safeName = saveName.replaceAll("[^a-zA-Z0-9_-]", "_");
            filename = safeName + "_" + timestamp + SAVE_EXTENSION;
        }
        
        Path savePath = Paths.get(SAVE_DIR, filename);
        
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(savePath.toFile()))) {
            // Zapisz metadane
            oos.writeUTF(saveName != null ? saveName : "Zapis automatyczny");
            oos.writeObject(LocalDateTime.now());
            oos.writeInt(gameState.getPlayers().size());
            oos.writeInt(gameState.getRoundNumber());
            
            // Zapisz stan gry
            oos.writeObject(gameState);
        }
        
        System.out.println("Gra zapisana: " + savePath);
        return filename;
    }
    
    /**
     * Wczytuje stan gry z pliku.
     * @param filename nazwa pliku zapisu
     * @return wczytany stan gry
     */
    public static GameState load(String filename) throws IOException, ClassNotFoundException {
        Path savePath = Paths.get(SAVE_DIR, filename);
        
        if (!Files.exists(savePath)) {
            throw new FileNotFoundException("Zapis nie istnieje: " + filename);
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(savePath.toFile()))) {
            // Pomijamy metadane
            ois.readUTF(); // displayName
            ois.readObject(); // savedAt
            ois.readInt(); // playerCount
            ois.readInt(); // roundNumber
            
            // Wczytaj stan gry
            return (GameState) ois.readObject();
        }
    }
    
    /**
     * Zwraca listę dostępnych zapisów.
     * @return lista informacji o zapisach, posortowana od najnowszego
     */
    public static List<SaveInfo> listSaves() {
        List<SaveInfo> saves = new ArrayList<>();
        
        try {
            ensureSaveDir();
            
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(
                    Paths.get(SAVE_DIR), "*" + SAVE_EXTENSION)) {
                
                for (Path path : stream) {
                    try {
                        SaveInfo info = readSaveInfo(path);
                        if (info != null) {
                            saves.add(info);
                        }
                    } catch (Exception e) {
                        System.err.println("Błąd odczytu zapisu: " + path + " - " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Błąd listowania zapisów: " + e.getMessage());
        }
        
        // Sortuj od najnowszego
        saves.sort(Comparator.comparing(SaveInfo::getSavedAt).reversed());
        return saves;
    }
    
    /**
     * Odczytuje metadane zapisu bez wczytywania całego stanu gry.
     */
    private static SaveInfo readSaveInfo(Path path) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(path.toFile()))) {
            String displayName = ois.readUTF();
            LocalDateTime savedAt = (LocalDateTime) ois.readObject();
            int playerCount = ois.readInt();
            int roundNumber = ois.readInt();
            
            return new SaveInfo(path.getFileName().toString(), displayName, savedAt, playerCount, roundNumber);
        }
    }
    
    /**
     * Usuwa zapis gry.
     * @param filename nazwa pliku do usunięcia
     * @return true jeśli usunięto pomyślnie
     */
    public static boolean delete(String filename) {
        try {
            Path savePath = Paths.get(SAVE_DIR, filename);
            return Files.deleteIfExists(savePath);
        } catch (IOException e) {
            System.err.println("Błąd usuwania zapisu: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Szybki zapis (autosave).
     */
    public static String autoSave(GameState gameState) throws IOException {
        return save(gameState, "AutoSave");
    }
}
