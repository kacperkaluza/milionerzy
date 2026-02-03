package com.kaluzaplotecka.milionerzy.model;

import java.io.Serializable;
import java.util.List;

import com.kaluzaplotecka.milionerzy.model.tiles.Tile;

/**
 * Reprezentuje planszę gry Milionerzy (Monopoly).
 * 
 * <p>Plansza zawiera listę pól ({@link Tile}), po których poruszają się gracze.
 * Wspiera cykliczne przechodzenie po planszy (po ostatnim polu następuje pierwsze).
 * 
 * <p>Klasa implementuje {@link Serializable} dla wsparcia zapisu/odczytu gry.
 * 
 * @see Tile
 * @see GameState
 */
public class Board implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /** Lista pól na planszy. */
    List<Tile> tiles;

    /**
     * Tworzy nową planszę z podaną listą pól.
     *
     * @param tiles lista pól planszy
     */
    public Board(List<Tile> tiles){
        this.tiles = tiles;
    }

    /**
     * Zwraca pole na podanej pozycji.
     * 
     * <p>Pozycja jest obliczana cyklicznie (modulo rozmiar planszy),
     * więc zarówno ujemne jak i zbyt duże wartości są obsługiwane poprawnie.
     *
     * @param position pozycja pola (może być poza zakresem)
     * @return pole na obliczonej pozycji lub {@code null} jeśli plansza jest pusta
     */
    public Tile getTile(int position){
        if (tiles == null || tiles.isEmpty()) return null;
        int p = ((position % tiles.size()) + tiles.size()) % tiles.size();
        return tiles.get(p);
    }

    /**
     * Zwraca liczbę pól na planszy.
     *
     * @return liczba pól lub 0 jeśli plansza jest pusta
     */
    public int size(){
        return tiles == null ? 0 : tiles.size();
    }

    /**
     * Zwraca listę wszystkich pól na planszy.
     * 
     * <p><b>Uwaga:</b> Zwracana jest oryginalna lista (nie kopia).
     *
     * @return lista pól
     */
    public List<Tile> getTiles() {
        return tiles;
    }
}