package com.kaluzaplotecka.milionerzy.model.tiles;

import com.kaluzaplotecka.milionerzy.model.GameState;
import com.kaluzaplotecka.milionerzy.model.Player;

/**
 * Reprezentuje pole "Szansa" na planszy gry.
 * 
 * <p>Gdy gracz stanie na tym polu, losowana jest karta szansy,
 * która może mieć różnorodne efekty (bonus pieniężny, kara, 
 * przeniesienie na inne pole itp.).
 * 
 * @see Tile
 * @see com.kaluzaplotecka.milionerzy.model.cards.EventCard
 */
public class ChanceTile extends Tile {
    
    /**
     * Tworzy nowe pole "Szansa".
     *
     * @param position pozycja na planszy
     * @param name nazwa pola
     */
    public ChanceTile(int position, String name){
        super(position, name);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Na tym polu losowana jest karta szansy i wykonywane są jej efekty.
     */
    @Override
    public void onLand(GameState state, Player player){
        if (state == null || player == null) return;
        state.executeChanceCardFor(player);
    }
}
