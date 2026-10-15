package com.gekojr.customcomands.fabric;
import net.minecraft.world.inventory.ContainerInput;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TradePolicyTest {
    @Test void everyBulkOperationIsRejectedOnBothSidesAndPlayerInventory() {
        for(var input:ContainerInput.values())if(input!=ContainerInput.PICKUP)for(boolean first:new boolean[]{true,false})for(int slot=-1;slot<63;slot++)assertFalse(TradePolicy.canInteract(first,slot,63,input));
    }
    @Test void opponentsOffersAndControlItemsCannotBeMoved() {
        for(int slot=0;slot<27;slot++) {
            assertEquals(slot<9,TradePolicy.canInteract(true,slot,63,ContainerInput.PICKUP));
            assertEquals(slot>=18,TradePolicy.canInteract(false,slot,63,ContainerInput.PICKUP));
        }
        assertFalse(TradePolicy.canInteract(true,63,63,ContainerInput.PICKUP));
        assertFalse(TradePolicy.canInteract(true,-1,63,ContainerInput.PICKUP));
        assertTrue(TradePolicy.canInteract(true,-999,63,ContainerInput.PICKUP));
    }
}
