package com.kaluzaplotecka.milionerzy.model.tiles;

import com.kaluzaplotecka.milionerzy.model.GameState;
import com.kaluzaplotecka.milionerzy.model.Player;

/**
 * Reprezentuje pole "Kasa Społeczna" na planszy gry.
 * 
 * <p>Gdy gracz stanie na tym polu, losowana jest karta kasy społecznej,
 * która może przyznać bonus lub nałożyć karę finansową.
 * 
 * @see Tile
 * @see com.kaluzaplotecka.milionerzy.model.cards.EventCard
 */
public class CommunityChestTile extends Tile {
    
    /**
     * Tworzy nowe pole "Kasa Społeczna".
     *
     * @param position pozycja na planszy
     * @param name nazwa pola
     */
    public CommunityChestTile(int position, String name){
        super(position, name);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Na tym polu losowana jest karta kasy społecznej i wykonywane są jej efekty.
     */
    @Override
    public void onLand(GameState state, Player player){
        if (state == null || player == null) return;
        state.executeCommunityChestCardFor(player);
    }
}
