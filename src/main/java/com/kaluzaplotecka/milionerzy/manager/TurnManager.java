package com.kaluzaplotecka.milionerzy.manager;

import com.kaluzaplotecka.milionerzy.model.Player;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Zarządza kolejnością tur i przebiegiem rund w grze.
 * 
 * <p>Klasa odpowiada za:
 * <ul>
 *   <li>Śledzenie aktualnego gracza</li>
 *   <li>Przechodzenie do następnej tury</li>
 *   <li>Zliczanie rund</li>
 *   <li>Obsługę rzutu kostką (flaga hasRolled)</li>
 *   <li>Usuwanie graczy (np. po bankructwie)</li>
 * </ul>
 * 
 * <p><b>Uwaga:</b> Klasa nie jest thread-safe. W grze sieciowej wymagana jest
 * zewnętrzna synchronizacja.
 * 
 * @see com.kaluzaplotecka.milionerzy.model.GameState
 * @see Player
 */
public class TurnManager implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /** Lista graczy w grze. */
    private List<Player> players;
    
    /** Indeks aktualnego gracza. */
    private int currentPlayerIndex;
    
    /** Numer aktualnej rundy. */
    private int roundNumber;
    
    /** Flaga określająca, czy aktualny gracz już rzucał kostką. */
    private boolean hasRolled;

    /**
     * Tworzy nowy menedżer tur z podaną listą graczy.
     * 
     * @param players lista graczy (nie może być null; może być pusta dla scenariuszy 
     *                awaryjnych jak uszkodzone pliki zapisu)
     * @throws IllegalArgumentException jeśli players jest null
     */
    public TurnManager(List<Player> players) {
        if (players == null) {
            throw new IllegalArgumentException("Players list cannot be null");
        }
        this.players = new ArrayList<>(players);
        this.currentPlayerIndex = 0;
        this.roundNumber = 0;
        this.hasRolled = false;
    }

    /**
     * Zwraca aktualnego gracza.
     *
     * @return aktualny gracz lub {@code null} jeśli lista graczy jest pusta
     */
    public Player getCurrentPlayer() {
        if (players.isEmpty()) return null;
        return players.get(currentPlayerIndex);
    }
    
    /**
     * Zwraca kopię listy graczy.
     *
     * @return kopia listy graczy
     */
    public List<Player> getPlayers() {
        return new ArrayList<>(players);
    }

    /**
     * Przechodzi do następnej tury.
     * 
     * <p>Resetuje flagę rzutu kostką i przesuwa wskaźnik na następnego gracza.
     * Gdy wskaźnik wraca do pierwszego gracza, zwiększa numer rundy.
     * 
     * @return {@code true} jeśli rozpoczęła się nowa runda
     */
    public boolean nextTurn() {
        if (players.isEmpty()) return false;
        hasRolled = false;
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        if (currentPlayerIndex == 0) {
            roundNumber++;
            return true;
        }
        return false;
    }

    /**
     * Zwraca numer aktualnej rundy.
     *
     * @return numer rundy
     */
    public int getRoundNumber() {
        return roundNumber;
    }

    /**
     * Sprawdza, czy aktualny gracz już rzucał kostką w tej turze.
     *
     * @return {@code true} jeśli gracz już rzucał
     */
    public boolean hasRolled() {
        return hasRolled;
    }

    /**
     * Ustawia flagę rzutu kostką.
     *
     * @param hasRolled nowa wartość flagi
     */
    public void setHasRolled(boolean hasRolled) {
        this.hasRolled = hasRolled;
    }
    
    /**
     * Ustawia numer rundy.
     * 
     * <p>Używane przez GameState przy migracji starych zapisów.
     *
     * @param roundNumber numer rundy do ustawienia
     */
    public void setRoundNumber(int roundNumber) {
        this.roundNumber = roundNumber;
    }
    
    /**
     * Ustawia aktualnego gracza na podstawie jego ID.
     * 
     * <p>Używane przez klientów do synchronizacji stanu tury
     * przy odbieraniu zmian tury od hosta.
     *
     * @param playerId ID gracza do ustawienia jako aktualny
     * @return {@code true} jeśli gracz został znaleziony i ustawiony
     */
    public boolean setCurrentPlayerById(String playerId) {
        if (playerId == null) return false;
        for (int i = 0; i < players.size(); i++) {
            if (playerId.equals(players.get(i).getId())) {
                currentPlayerIndex = i;
                return true;
            }
        }
        return false;
    }
    
    /**
     * Usuwa gracza z gry.
     * 
     * <p>Automatycznie dostosowuje indeks aktualnego gracza,
     * jeśli usunięty gracz był przed lub na pozycji aktualnego.
     *
     * @param p gracz do usunięcia
     */
    public void removePlayer(Player p) {
        int removedIndex = players.indexOf(p);
        if (removedIndex >= 0) {
            players.remove(removedIndex);
            if (removedIndex <= currentPlayerIndex && currentPlayerIndex > 0) {
                currentPlayerIndex--;
            }
            if (players.isEmpty()) {
                currentPlayerIndex = 0;
            } else {
                currentPlayerIndex = currentPlayerIndex % players.size();
            }
        }
    }
    
    /**
     * Sprawdza, czy gra się zakończyła.
     * 
     * <p>Gra kończy się, gdy pozostał maksymalnie jeden gracz.
     *
     * @return {@code true} jeśli gra się zakończyła
     */
    public boolean isGameOver() {
        return players.size() <= 1;
    }
    
    /**
     * Zwraca zwycięzcę gry.
     *
     * @return zwycięzca (ostatni pozostały gracz) lub {@code null}
     */
    public Player getWinner() {
        if (players.isEmpty()) return null;
        return players.get(0);
    }
}
