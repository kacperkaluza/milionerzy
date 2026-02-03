package com.kaluzaplotecka.milionerzy.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.kaluzaplotecka.milionerzy.model.tiles.PropertyTile;

/**
 * Reprezentuje gracza w grze Milionerzy (Monopoly).
 * 
 * <p>Klasa przechowuje wszystkie informacje o stanie gracza, w tym:
 * <ul>
 *   <li>Dane identyfikacyjne (id, nazwa użytkownika)</li>
 *   <li>Stan finansowy (ilość pieniędzy)</li>
 *   <li>Pozycję na planszy</li>
 *   <li>Posiadane nieruchomości</li>
 *   <li>Status więzienia</li>
 * </ul>
 * 
 * <p>Klasa implementuje {@link Serializable} dla wsparcia zapisu/odczytu gry
 * oraz synchronizacji sieciowej.
 * 
 * @see GameState
 * @see PropertyTile
 */
public class Player implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String id;
    private String username;
    private int money;
    private int position;
    private final List<PropertyTile> ownedProperties;
    private boolean inJail;
    private int jailTurns;

    /**
     * Tworzy nowego gracza z podanymi parametrami.
     *
     * @param id unikalny identyfikator gracza (np. UUID)
     * @param username wyświetlana nazwa gracza
     * @param startingMoney początkowa ilość pieniędzy
     */
    public Player(String id, String username, int startingMoney) {
        this.id = id;
        this.username = username;
        this.money = startingMoney;
        this.position = 0;
        this.ownedProperties = new ArrayList<>();
        this.inJail = false;
        this.jailTurns = 0;
    }

    /**
     * Zwraca unikalny identyfikator gracza.
     *
     * @return identyfikator gracza
     */
    public String getId() {
        return id;
    }
    
    /**
     * Ustawia identyfikator gracza.
     *
     * @param id nowy identyfikator
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Ustawia pozycję gracza na planszy.
     *
     * @param position nowa pozycja (indeks pola)
     */
    public void setPosition(int position) {
        this.position = position;
    }

    /**
     * Zwraca nazwę użytkownika (wyświetlaną nazwę gracza).
     *
     * @return nazwa użytkownika
     */
    public String getUsername() {
        return username;
    }
    
    /**
     * Ustawia nazwę użytkownika.
     *
     * @param name nowa nazwa
     */
    public void setName(String name) {
        this.username = name;
    }

    /**
     * Zwraca aktualną ilość pieniędzy gracza.
     *
     * @return ilość pieniędzy
     */
    public int getMoney() {
        return money;
    }

    /**
     * Zwraca aktualną pozycję gracza na planszy.
     *
     * @return indeks pola, na którym stoi gracz
     */
    public int getPosition() {
        return position;
    }
    
    /**
     * Zwraca kopię listy posiadanych nieruchomości.
     * 
     * <p>Zwracana jest kopia, aby chronić wewnętrzny stan gracza
     * przed nieautoryzowanymi modyfikacjami.
     *
     * @return kopia listy nieruchomości gracza
     */
    public List<PropertyTile> getOwnedProperties() {
        return new ArrayList<>(ownedProperties);
    }

    /**
     * Sprawdza, czy gracz jest w więzieniu.
     *
     * @return {@code true} jeśli gracz jest w więzieniu
     */
    public boolean isInJail() {
        return inJail;
    }
    
    /**
     * Zwraca liczbę tur spędzonych w więzieniu.
     *
     * @return liczba tur w więzieniu
     */
    public int getJailTurns() {
        return jailTurns;
    }

    /**
     * Dodaje pieniądze do konta gracza.
     *
     * @param amount kwota do dodania (może być ujemna)
     */
    public void addMoney(int amount) {
        this.money += amount;
    }

    /**
     * Odejmuje pieniądze z konta gracza.
     * 
     * <p>Metoda zawsze wykonuje odejmowanie, nawet jeśli saldo stanie się ujemne.
     * Ujemne saldo oznacza bankructwo gracza.
     *
     * @param amount kwota do odjęcia
     * @return {@code true} jeśli saldo po operacji jest nieujemne
     */
    public boolean deductMoney(int amount) {
        this.money -= amount;
        return this.money >= 0;
    }

    /**
     * Przesuwa gracza o podaną liczbę pól.
     * 
     * <p>Pozycja jest obliczana modulo rozmiar planszy (cyklicznie).
     *
     * @param steps liczba pól do przesunięcia
     * @param board plansza gry (potrzebna do obliczenia rozmiaru)
     */
    public void moveBy(int steps, Board board) {
        int newPos = (this.position + steps) % board.size();
        this.position = newPos;
    }

    /**
     * Przenosi gracza bezpośrednio na wskazaną pozycję.
     *
     * @param pos docelowa pozycja (indeks pola)
     */
    public void moveTo(int pos) {
        this.position = pos;
    }

    /**
     * Wysyła gracza do więzienia.
     * 
     * <p>Ustawia flagę więzienia i resetuje licznik tur.
     */
    public void goToJail() {
        this.inJail = true;
        this.jailTurns = 0;
    }

    /**
     * Zwalnia gracza z więzienia.
     * 
     * <p>Resetuje flagę więzienia i licznik tur.
     */
    public void releaseFromJail() {
        this.inJail = false;
        this.jailTurns = 0;
    }
    
    /**
     * Zwiększa licznik tur spędzonych w więzieniu.
     */
    public void incrementJailTurns() {
        this.jailTurns++;
    }

    /**
     * Dodaje nieruchomość do listy posiadanych przez gracza.
     * 
     * <p>Nieruchomość jest dodawana tylko jeśli gracz jej jeszcze nie posiada.
     *
     * @param p nieruchomość do dodania
     */
    public void addProperty(PropertyTile p) {
        if (!ownedProperties.contains(p)) {
            ownedProperties.add(p);
        }
    }

    /**
     * Usuwa nieruchomość z listy posiadanych przez gracza.
     *
     * @param p nieruchomość do usunięcia
     */
    public void removeProperty(PropertyTile p) {
        ownedProperties.remove(p);
    }

    /**
     * Sprawdza, czy gracz zbankrutował.
     * 
     * <p>Gracz jest bankrutem, gdy jego saldo jest ujemne.
     *
     * @return {@code true} jeśli gracz zbankrutował
     */
    public boolean isBankrupt() {
        return this.money < 0;
    }

    @Override
    public String toString() {
        return "Player{" +
                "username='" + username + '\'' +
                ", money=" + money +
                ", position=" + position +
                ", inJail=" + inJail +
                '}';
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Player player = (Player) o;

        return id != null ? id.equals(player.id) : player.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
