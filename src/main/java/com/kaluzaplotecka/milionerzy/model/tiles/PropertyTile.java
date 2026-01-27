package com.kaluzaplotecka.milionerzy.model.tiles;


import com.kaluzaplotecka.milionerzy.events.GameEvent;
import com.kaluzaplotecka.milionerzy.model.GameState;
import com.kaluzaplotecka.milionerzy.model.Player;

public class PropertyTile extends Tile {
    private static final long serialVersionUID = 1L;
    String city;
    int price;
    int baseRent;
    Player owner;
    int houses;
    boolean mortgaged;

    public PropertyTile(int position, String city, int price, int baseRent){
        super(position, city);
        this.city = city;
        this.price = price;
        this.baseRent = baseRent;
        this.owner = null;
        this.houses = 0;
        this.mortgaged = false;
    }

    public String getCity() { return city; }
    public int getPrice() { return price; }
    public Player getOwner() { return owner; }
    public void setOwner(Player owner) { this.owner = owner; }

    public boolean isOwned(){
        return owner != null;
    }

    /** Attempt to buy property. Returns true if purchase succeeded. */
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

    /** Charge rent from tenant and transfer to owner. Returns amount charged. */
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
            // tenant couldn't fully pay — deduct remaining (money can go negative)
            // Currently simple logic: transfer whatever they have + debt? 
            // The original logic was: owner.addMoney(amount + tenant.getMoney()); 
            // Wait, if tenant has 100, rent is 150. deductMoney(150) returns false. 
            // Tenant money is now -50 (if deductMoney allows negative) OR remains 100 (if deductMoney is transactional)?
            // Player.deductMoney implementation: this.money -= amount; return this.money >= 0;
            // So money IS deducted even if it goes negative.
            // So tenant money is now e.g. -50.
            // Owner should receive full amount? Or just what tenant had?
            // "owner.addMoney(amount + tenant.getMoney())" -> if tenant has -50, amount 150. 
            // 150 + (-50) = 100. So owner gets 100 (original 100 of tenant). 
            // This logic seems to imply owner gets what tenant HAD.
            
            // Let's keep original logic for money math, just add event.
            owner.addMoney(amount + tenant.getMoney());
            
             if (state != null) {
                state.fireEvent(new GameEvent(
                    GameEvent.Type.RENT_PAID,
                    tenant,
                    amount, // Trigger event for full rent amount even if partial payment? Or just "Rent Paid"
                    tenant.getUsername() + " płaci " + amount + " czynszu (bankructwo?)"
                ));
            }
            return amount;
        }
    }

    public int calculateRent(){
        return baseRent + houses * (baseRent/2);
    }

    @Override
    public void onLand(GameState state, Player player){
        if (!isOwned()){
            // Fire event to notify UI that player landed on unowned property
            state.fireEvent(new GameEvent(
                GameEvent.Type.PROPERTY_LANDED_NOT_OWNED,
                player,
                this,
                "Stanąłeś na: " + city
            ));
            return;
        } else if (owner != player){
            chargeRent(state, player);
            if (player.isBankrupt()){
                state.handleBankruptcy(player);
            }
        }
    }
}
