package com.kaluzaplotecka.milionerzy.model.tiles;

import java.io.Serializable;

import com.kaluzaplotecka.milionerzy.model.GameState;
import com.kaluzaplotecka.milionerzy.model.Player;

/**
 * Bazowa klasa reprezentująca pole na planszy gry.
 * 
 * <p>Klasa stanowi podstawę hierarchii pól. Konkretne typy pól
 * (nieruchomości, szansa, kasa społeczna, więzienie itp.) dziedziczą
 * po tej klasie i nadpisują metodę {@link #onLand(GameState, Player)}.
 * 
 * <p>Klasa implementuje {@link Serializable} dla wsparcia zapisu/odczytu gry.
 * 
 * @see PropertyTile
 * @see ChanceTile
 * @see CommunityChestTile
 */
public class Tile implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /** Pozycja pola na planszy (indeks). */
    int position;
    
    /** Nazwa wyświetlana pola. */
    String name;

    /**
     * Tworzy nowe pole z podaną pozycją i nazwą.
     *
     * @param position indeks pola na planszy
     * @param name nazwa wyświetlana pola
     */
    public Tile(int position, String name){
        this.position = position;
        this.name = name;
    }

    /**
     * Zwraca pozycję pola na planszy.
     *
     * @return indeks pola
     */
    public int getPosition(){ return position; }

    /**
     * Wywoływane gdy gracz staje na tym polu.
     * 
     * <p>Domyślna implementacja nie wykonuje żadnej akcji.
     * Podklasy powinny nadpisać tę metodę, aby zdefiniować
     * specyficzne zachowanie (np. pobieranie czynszu, losowanie karty).
     *
     * @param state aktualny stan gry
     * @param player gracz, który stanął na polu
     */
    public void onLand(GameState state, Player player){
        // Domyślnie: brak akcji
    }
}
