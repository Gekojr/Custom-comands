package com.gekojr.customcomands.fabric;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import java.util.*;

public final class CustomComandsMod implements ModInitializer {
    private static final String DISCORD="https://discord.gg/mZRyBRzSA3";
    private final Map<UUID,Request> requests=new HashMap<>();
    private final Map<UUID,Trade> trades=new HashMap<>();
    private MinecraftServer server;
    @Override public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(s -> server=s);
        ServerLifecycleEvents.SERVER_STOPPING.register(s -> { for(var trade:new HashSet<>(trades.values()))trade.finish(false,"Server stopping."); requests.clear(); });
        ServerPlayConnectionEvents.DISCONNECT.register((handler,s) -> { var trade=trades.get(handler.player.getUUID()); if(trade!=null)trade.finish(false,"Trade cancelled: player disconnected."); requests.remove(handler.player.getUUID()); requests.values().removeIf(r -> r.sender.equals(handler.player.getUUID())); });
        ServerTickEvents.END_SERVER_TICK.register(s -> {
            long now=System.currentTimeMillis();
            requests.entrySet().removeIf(entry -> { if(now<entry.getValue().expiry)return false; var p=s.getPlayerList().getPlayer(entry.getKey()); if(p!=null)tell(p,"The trade request has expired."); return true; });
            for(var trade:new HashSet<>(trades.values()))if(!near(trade.first,trade.second) || !trade.first.isAlive() || !trade.second.isAlive())trade.finish(false,"Trade cancelled: players must remain alive and within 10 blocks.");
        });
        CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment) -> {
            dispatcher.register(Commands.literal("discord").executes(ctx -> { ctx.getSource().sendSystemMessage(label("DISCORD",net.minecraft.ChatFormatting.LIGHT_PURPLE,net.minecraft.ChatFormatting.DARK_PURPLE,DISCORD)); return 1; }));
            dispatcher.register(Commands.literal("annuncio").requires(src -> Access.allowed(src,"customcomands","annuncio",false))
                .then(Commands.argument("message",StringArgumentType.greedyString()).executes(ctx -> {
                    ctx.getSource().getServer().getPlayerList().broadcastSystemMessage(label("ANNOUNCEMENT",net.minecraft.ChatFormatting.GOLD,net.minecraft.ChatFormatting.DARK_GRAY,StringArgumentType.getString(ctx,"message")),false); return 1;
                })));
            dispatcher.register(Commands.literal("trade").executes(ctx -> { tell(ctx.getSource().getPlayerOrException(),"Usage: /trade <player> | /trade accept | /trade deny"); return 1; })
                .then(Commands.argument("player",StringArgumentType.word()).suggests((ctx,builder) -> { for(String name:ctx.getSource().getServer().getPlayerNames())if(name.toLowerCase().startsWith(builder.getRemainingLowerCase()))builder.suggest(name); builder.suggest("accept"); builder.suggest("deny"); return builder.buildFuture(); })
                    .executes(ctx -> command(ctx.getSource().getPlayerOrException(),StringArgumentType.getString(ctx,"player")))));
        });
    }
    private int command(ServerPlayer player,String arg) {
        if(arg.equalsIgnoreCase("deny")) {
            var request=requests.remove(player.getUUID()); if(request==null) { tell(player,"You have no pending trade request."); return 1; }
            var sender=server.getPlayerList().getPlayer(request.sender); if(sender!=null)tell(sender,player.getName().getString()+" denied your trade request."); tell(player,"Trade request denied."); return 1;
        }
        if(arg.equalsIgnoreCase("accept")) {
            var request=requests.remove(player.getUUID());
            if(request==null || request.expiry<=System.currentTimeMillis()) { tell(player,"You have no pending trade request."); return 1; }
            var other=server.getPlayerList().getPlayer(request.sender);
            if(other==null || !near(player,other)) { tell(player,"You must be within 10 blocks of the other player."); return 1; }
            if(trades.containsKey(player.getUUID()) || trades.containsKey(other.getUUID())) { tell(player,"One of you is already in a trade."); return 1; }
            var trade=new Trade(other,player); trades.put(player.getUUID(),trade); trades.put(other.getUUID(),trade); trade.open(); return 1;
        }
        var target=server.getPlayerList().getPlayerByName(arg);
        if(target==null) { tell(player,"That player is not online."); return 1; }
        if(player==target) { tell(player,"You cannot trade with yourself."); return 1; }
        if(!near(player,target)) { tell(player,"You must be within 10 blocks of that player to trade."); return 1; }
        if(trades.containsKey(player.getUUID()) || trades.containsKey(target.getUUID())) { tell(player,"One of you is already in a trade."); return 1; }
        if(requests.containsKey(target.getUUID())) { tell(player,"That player already has a pending trade request."); return 1; }
        requests.put(target.getUUID(),new Request(player.getUUID(),System.currentTimeMillis()+60_000));
        tell(player,"Trade request sent to "+target.getName().getString()+"."); tell(target,player.getName().getString()+" wants to trade. /trade accept or /trade deny"); return 1;
    }
    static boolean near(ServerPlayer a,ServerPlayer b) { return a.level()==b.level() && a.distanceToSqr(b)<=100; }
    static Component label(String name,net.minecraft.ChatFormatting color,net.minecraft.ChatFormatting separator,String message) { return Component.literal(name).withStyle(color,net.minecraft.ChatFormatting.BOLD).append(Component.literal(" » ").withStyle(separator).withStyle(style -> style.withBold(false))).append(Component.literal(message).withStyle(net.minecraft.ChatFormatting.WHITE).withStyle(style -> style.withBold(false))); }
    static void tell(ServerPlayer p,String text) { p.sendSystemMessage(Component.literal(text)); }
    record Request(UUID sender,long expiry) {}
    final class Trade {
        final ServerPlayer first,second; final SimpleContainer offers=new SimpleContainer(27);
        boolean firstConfirmed,secondConfirmed,finished;
        Trade(ServerPlayer first,ServerPlayer second) { this.first=first; this.second=second;
            for(int i=9;i<18;i++)offers.setItem(i,icon(Items.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.GRAY)," "));
            offers.setItem(11,icon(Items.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.LIME),"Confirm Trade")); offers.setItem(15,icon(Items.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.LIME),"Confirm Trade")); offers.setItem(13,icon(Items.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.RED),"Cancel Trade"));
        }
        void open() { open(first); open(second); }
        void open(ServerPlayer player) { player.openMenu(new SimpleMenuProvider((id,inventory,p) -> new TradeMenu(id,inventory,player),Component.literal("Trade"))); }
        void reset() { firstConfirmed=false; secondConfirmed=false; }
        void confirm(ServerPlayer player) {
            // A carried item has not yet been deposited in an offer slot.
            if(!first.containerMenu.getCarried().isEmpty() || !second.containerMenu.getCarried().isEmpty()) { tell(player,"Put down the item on your cursor before confirming."); return; }
            if(player==first)firstConfirmed=true; else secondConfirmed=true;
            if(firstConfirmed && secondConfirmed)server.execute(() -> { if(firstConfirmed && secondConfirmed)finish(true,"Trade completed!"); }); else tell(player,"Waiting for the other player's confirmation.");
        }
        void finish(boolean complete,String message) {
            if(finished)return; if(complete && (!near(first,second) || !first.isAlive() || !second.isAlive())) { complete=false; message="Trade cancelled: players moved or died."; } finished=true; trades.remove(first.getUUID(),this); trades.remove(second.getUUID(),this);
            for(int slot=0;slot<9;slot++)give(complete?second:first,offers.removeItemNoUpdate(slot));
            for(int slot=18;slot<27;slot++)give(complete?first:second,offers.removeItemNoUpdate(slot));
            first.closeContainer(); second.closeContainer(); tell(first,message); tell(second,message);
        }
        void give(ServerPlayer player,ItemStack item) { if(item.isEmpty())return; if(!player.getInventory().add(item) && !item.isEmpty())player.drop(item,false); }
        final class TradeMenu extends ChestMenu {
            final ServerPlayer owner;
            TradeMenu(int id,net.minecraft.world.entity.player.Inventory inventory,ServerPlayer owner) {
                super(MenuType.GENERIC_9x3,id,inventory,offers,3); this.owner=owner;
            }
            @Override public boolean stillValid(Player player) { return !finished && player==owner && near(first,second); }
            @Override public void clicked(int slot,int button,ContainerInput input,Player player) {
                if(finished || player!=owner)return;
                // Only ordinary pickup is permitted. Shift-click, drag, hotbar swap,
                // throw and collect-all must never bypass ownership of offer slots.
                if(input!=ContainerInput.PICKUP)return;
                if(slot==11 || slot==15) { if(button==0)confirm(owner); return; }
                if(slot==13) { server.execute(() -> finish(false,"Trade cancelled.")); return; }
                if(!TradePolicy.canInteract(owner==first,slot,slots.size(),input))return;
                reset(); super.clicked(slot,button,input,player);
                first.containerMenu.broadcastChanges(); second.containerMenu.broadcastChanges();
            }
            @Override public ItemStack quickMoveStack(Player p,int slot) { return ItemStack.EMPTY; }
            @Override public boolean canDragTo(Slot slot) { return false; }
            @Override public boolean canTakeItemForPickAll(ItemStack item,Slot slot) { return false; }
            @Override public void removed(Player p) { super.removed(p); if(!finished)server.execute(() -> finish(false,"Trade cancelled because a player closed the trade.")); }
        }
    }
    static ItemStack icon(Item item,String name) { var stack=new ItemStack(item); stack.set(DataComponents.CUSTOM_NAME,Component.literal(name)); return stack; }
}
