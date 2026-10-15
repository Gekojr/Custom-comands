package com.gekojr.customcomands.fabric;
import net.minecraft.world.inventory.ContainerInput;
/** Allowed offer interaction. All bulk operations are rejected before vanilla runs. */
final class TradePolicy {
    static boolean canInteract(boolean first,int slot,int size,ContainerInput input) {
        if(input!=ContainerInput.PICKUP)return false;
        if(slot==-999)return true;
        if(slot<0 || slot>=size)return false;
        return slot>=27 || (first?slot<9:slot>=18 && slot<27);
    }
}
