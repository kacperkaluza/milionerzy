package com.kaluzaplotecka.milionerzy;

public class PropertyTile extends Tile {
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
    public int chargeRent(Player tenant){
        if (!isOwned() || owner == null) return 0;
        int amount = calculateRent();
        boolean paid = tenant.deductMoney(amount);
        if (paid){
            owner.addMoney(amount);
            return amount;
        } else {
            // tenant couldn't fully pay — deduct remaining (money can go negative)
            owner.addMoney(amount + tenant.getMoney());
            return amount;
        }
    }

    public int calculateRent(){
        return baseRent + houses * (baseRent/2);
    }

    @Override
    public void onLand(GameState state, Player player){
        if (!isOwned()){
            // auto-buy if player can afford (simple rule for now)
            buy(player);
        } else if (owner != player){
            chargeRent(player);
            if (player.isBankrupt()){
                // simple bankruptcy handling: transfer properties back to bank
                state.handleBankruptcy(player);
            }
        }
    }
}
