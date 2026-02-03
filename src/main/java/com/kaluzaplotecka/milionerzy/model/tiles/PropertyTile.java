package com.kaluzaplotecka.milionerzy.model.tiles;


import com.kaluzaplotecka.milionerzy.events.GameEvent;
import com.kaluzaplotecka.milionerzy.model.GameState;
import com.kaluzaplotecka.milionerzy.model.Player;

/**
 * Reprezentuje pole nieruchomości na planszy gry.
 * 
 * <p>Nieruchomość może być kupiona przez gracza, który na niej stanie.
 * Właściciel pobiera czynsz od innych graczy, którzy staną na jego nieruchomości.
 * Czynsz rośnie wraz z liczbą wybudowanych domów.
 * 
 * <p>Główne funkcjonalności:
 * <ul>
 *   <li>Kupowanie nieruchomości</li>
 *   <li>Pobieranie czynszu</li>
 *   <li>Budowanie domów (zwiększa czynsz)</li>
 *   <li>Zastawianie w banku (hipoteka)</li>
 * </ul>
 * 
 * @see Tile
 * @see Player
 */
public class PropertyTile extends Tile {
    private static final long serialVersionUID = 1L;
    
    /** Nazwa miasta/nieruchomości. */
    String city;
    
    /** Cena zakupu nieruchomości. */
    int price;
    
    /** Bazowy czynsz (bez domów). */
    int baseRent;
    
    /** Właściciel nieruchomości lub {@code null} jeśli niczyja. */
    Player owner;
    
    /** Liczba wybudowanych domów (0-5, gdzie 5 = hotel). */
    int houses;
    
    /** Czy nieruchomość jest zastawiona (hipoteka). */
    boolean mortgaged;

    /**
     * Tworzy nową nieruchomość.
     *
     * @param position pozycja na planszy
     * @param city nazwa miasta/nieruchomości
     * @param price cena zakupu
     * @param baseRent bazowy czynsz
     */
    public PropertyTile(int position, String city, int price, int baseRent){
        super(position, city);
        this.city = city;
        this.price = price;
        this.baseRent = baseRent;
        this.owner = null;
        this.houses = 0;
        this.mortgaged = false;
    }

    /**
     * Zwraca nazwę miasta/nieruchomości.
     * @return nazwa miasta
     */
    public String getCity() { return city; }
    
    /**
     * Zwraca cenę zakupu nieruchomości.
     * @return cena zakupu
     */
    public int getPrice() { return price; }
    
    /**
     * Zwraca właściciela nieruchomości.
     * @return właściciel lub {@code null}
     */
    public Player getOwner() { return owner; }
    
    /**
     * Ustawia właściciela nieruchomości.
     * @param owner nowy właściciel
     */
    public void setOwner(Player owner) { this.owner = owner; }

    /**
     * Sprawdza, czy nieruchomość ma właściciela.
     * @return {@code true} jeśli nieruchomość ma właściciela
     */
    public boolean isOwned(){
        return owner != null;
    }

    /**
     * Próbuje kupić nieruchomość dla podanego gracza.
     * 
     * <p>Zakup powiedzie się tylko jeśli:
     * <ul>
     *   <li>Nieruchomość nie ma jeszcze właściciela</li>
     *   <li>Gracz ma wystarczającą ilość pieniędzy</li>
     * </ul>
     *
     * @param buyer gracz kupujący
     * @return {@code true} jeśli zakup się powiódł
     */
    public boolean buy(Player buyer){
        if (isOwned()) return false;
        if (buyer.getMoney() >= price){
            buyer.deductMoney(price);
            this.owner = buyer;
            buyer.addProperty(this);
            return true;
        }
        return false;
    }

    /**
     * Pobiera czynsz od najemcy i przekazuje właścicielowi.
     * 
     * <p>Jeśli najemca nie ma wystarczających środków, jego saldo staje się
     * ujemne, a właściciel otrzymuje tyle, ile najemca faktycznie miał przed
     * odjęciem czynszu.
     *
     * @param state stan gry (dla emitowania zdarzeń)
     * @param tenant gracz płacący czynsz
     * @return kwota czynszu
     */
    public int chargeRent(GameState state, Player tenant){
        if (!isOwned() || owner == null) return 0;
        int amount = calculateRent();
        boolean paid = tenant.deductMoney(amount);
        
        if (paid){
            owner.addMoney(amount);
            if (state != null) {
                state.fireEvent(new GameEvent(
                    GameEvent.Type.RENT_PAID,
                    tenant,
                    amount,
                    tenant.getUsername() + " płaci " + amount + " czynszu dla " + owner.getUsername()
                ));
            }
            return amount;
        } else {
            // Najemca nie miał pełnej kwoty - właściciel dostaje tyle, ile najemca miał
            owner.addMoney(amount + tenant.getMoney());
            
             if (state != null) {
                state.fireEvent(new GameEvent(
                    GameEvent.Type.RENT_PAID,
                    tenant,
                    amount,
                    tenant.getUsername() + " płaci " + amount + " czynszu (bankructwo)"
                ));
            }
            return amount;
        }
    }

    /**
     * Oblicza aktualny czynsz za nieruchomość.
     * 
     * <p>Czynsz = bazowy czynsz + (liczba domów * połowa bazowego czynszu)
     *
     * @return kwota czynszu
     */
    public int calculateRent(){
        return baseRent + houses * (baseRent/2);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Gdy gracz staje na nieruchomości:
     * <ul>
     *   <li>Jeśli nieruchomość jest niczyja - emitowane jest zdarzenie informujące o możliwości zakupu</li>
     *   <li>Jeśli nieruchomość należy do innego gracza - pobierany jest czynsz</li>
     *   <li>Jeśli nieruchomość należy do gracza, który na niej stanął - nic się nie dzieje</li>
     * </ul>
     */
    @Override
    public void onLand(GameState state, Player player){
        if (!isOwned()){
            state.fireEvent(new GameEvent(
                GameEvent.Type.PROPERTY_LANDED_NOT_OWNED,
                player,
                this,
                "Stanąłeś na: " + city
            ));
        } else if (owner != player){
            chargeRent(state, player);
            if (player.isBankrupt()){
                state.handleBankruptcy(player);
            }
        }
    }
}
