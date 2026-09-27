package com.gekojr.customcomands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CustomComands extends JavaPlugin implements CommandExecutor, Listener {

    private static final String DISCORD = "https://discord.gg/mZRyBRzSA3";

    private final Map<UUID, TradeRequest> requests = new HashMap<>();
    private final Map<UUID, TradeSession> activeTrades = new HashMap<>();

    @Override
    public void onEnable() {
        getCommand("discord").setExecutor(this);
        getCommand("annuncio").setExecutor(this);
        getCommand("trade").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("CustomComands enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("CustomComands disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("discord")) {
            Component discordMessage = Component.text()
                .append(Component.text("DISCORD", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .append(Component.text(" » ", NamedTextColor.DARK_PURPLE))
                .append(Component.text(DISCORD, NamedTextColor.WHITE))
                .build();

            sender.sendMessage(discordMessage);
            return true;
        }

        if (command.getName().equalsIgnoreCase("annuncio")) {
            if (!sender.hasPermission("customcomands.annuncio")) {
                sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
                return true;
            }

            if (args.length == 0) {
                sender.sendMessage(Component.text("Usage: /annuncio <message>", NamedTextColor.RED));
                return true;
            }

            String message = String.join(" ", args);

            Component announcement = Component.text()
                .append(Component.text("ANNOUNCEMENT", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(" » ", NamedTextColor.DARK_GRAY))
                .append(Component.text(message, NamedTextColor.WHITE))
                .build();

            getServer().sendMessage(announcement);
            return true;
        }

        if (command.getName().equalsIgnoreCase("trade")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Only players can use /trade.", NamedTextColor.RED));
                return true;
            }

            if (args.length == 0) {
                player.sendMessage(Component.text("Usage: /trade <player> | /trade accept | /trade deny", NamedTextColor.RED));
                return true;
            }

            if (args[0].equalsIgnoreCase("accept")) {
                return acceptTrade(player);
            }

            if (args[0].equalsIgnoreCase("deny")) {
                return denyTrade(player);
            }

            if (args.length == 1) {
                Player target = Bukkit.getPlayerExact(args[0]);

                if (target == null || !target.isOnline()) {
                    player.sendMessage(Component.text("That player is not online.", NamedTextColor.RED));
                    return true;
                }

                if (target.equals(player)) {
                    player.sendMessage(Component.text("You cannot trade with yourself.", NamedTextColor.RED));
                    return true;
                }

                if (activeTrades.containsKey(player.getUniqueId()) || activeTrades.containsKey(target.getUniqueId())) {
                    player.sendMessage(Component.text("One of you is already in a trade.", NamedTextColor.RED));
                    return true;
                }

                if (requests.containsKey(target.getUniqueId())) {
                    player.sendMessage(Component.text("That player already has a pending trade request.", NamedTextColor.RED));
                    return true;
                }

                requests.put(target.getUniqueId(), new TradeRequest(player.getUniqueId()));

                player.sendMessage(Component.text("Trade request sent to " + target.getName() + ".", NamedTextColor.GREEN));
                target.sendMessage(Component.text()
                    .append(Component.text("TRADE", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                    .append(Component.text(" » ", NamedTextColor.DARK_PURPLE))
                    .append(Component.text(player.getName() + " wants to trade with you. ", NamedTextColor.WHITE))
                    .append(Component.text("/trade accept", NamedTextColor.GREEN, TextDecoration.BOLD))
                    .append(Component.text(" or ", NamedTextColor.WHITE))
                    .append(Component.text("/trade deny", NamedTextColor.RED, TextDecoration.BOLD))
                    .build());

                Bukkit.getScheduler().runTaskLater(this, () -> {
                    TradeRequest request = requests.get(target.getUniqueId());
                    if (request != null && request.requester.equals(player.getUniqueId())) {
                        requests.remove(target.getUniqueId());
                        if (target.isOnline()) {
                            target.sendMessage(Component.text("The trade request has expired.", NamedTextColor.RED));
                        }
                    }
                }, 20L * 60L);

                return true;
            }

            player.sendMessage(Component.text("Usage: /trade <player> | /trade accept | /trade deny", NamedTextColor.RED));
            return true;
        }

        return false;
    }

    private boolean acceptTrade(Player target) {
        TradeRequest request = requests.remove(target.getUniqueId());

        if (request == null) {
            target.sendMessage(Component.text("You have no pending trade request.", NamedTextColor.RED));
            return true;
        }

        Player requester = Bukkit.getPlayer(request.requester);
        if (requester == null || !requester.isOnline()) {
            target.sendMessage(Component.text("The player who sent the request is no longer online.", NamedTextColor.RED));
            return true;
        }

        if (activeTrades.containsKey(requester.getUniqueId()) || activeTrades.containsKey(target.getUniqueId())) {
            target.sendMessage(Component.text("One of you is already in a trade.", NamedTextColor.RED));
            return true;
        }

        TradeSession session = new TradeSession(requester, target);
        activeTrades.put(requester.getUniqueId(), session);
        activeTrades.put(target.getUniqueId(), session);

        session.open();
        return true;
    }

    private boolean denyTrade(Player target) {
        TradeRequest request = requests.remove(target.getUniqueId());

        if (request == null) {
            target.sendMessage(Component.text("You have no pending trade request.", NamedTextColor.RED));
            return true;
        }

        Player requester = Bukkit.getPlayer(request.requester);
        target.sendMessage(Component.text("Trade request denied.", NamedTextColor.RED));

        if (requester != null && requester.isOnline()) {
            requester.sendMessage(Component.text(target.getName() + " denied your trade request.", NamedTextColor.RED));
        }

        return true;
    }

    private void cancelTrade(TradeSession session, String message) {
        activeTrades.remove(session.first.getUniqueId());
        activeTrades.remove(session.second.getUniqueId());

        returnItems(session.inventory, 0, 9, session.first);
        returnItems(session.inventory, 18, 27, session.second);

        session.first.closeInventory();
        session.second.closeInventory();

        session.first.sendMessage(Component.text(message, NamedTextColor.RED));
        session.second.sendMessage(Component.text(message, NamedTextColor.RED));
    }

    private void returnItems(Inventory inventory, int from, int to, Player player) {
        for (int slot = from; slot < to; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                for (ItemStack remaining : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), remaining);
                }
                inventory.setItem(slot, null);
            }
        }
    }

    @EventHandler
    public void onTradeClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!(event.getView().getTopInventory().getHolder() instanceof TradeHolder holder)) {
            return;
        }

        TradeSession session = holder.session;
        if (!activeTrades.containsKey(player.getUniqueId()) || activeTrades.get(player.getUniqueId()) != session) {
            event.setCancelled(true);
            return;
        }

        int rawSlot = event.getRawSlot();

        if (rawSlot >= event.getView().getTopInventory().getSize()) {
            return;
        }

        event.setCancelled(true);

        if (rawSlot == 11) {
            session.confirm(player);
            return;
        }

        if (rawSlot == 15) {
            session.confirm(player);
            return;
        }

        if (rawSlot == 22) {
            cancelTrade(session, "Trade cancelled.");
            return;
        }

        boolean firstSide = rawSlot >= 0 && rawSlot <= 8;
        boolean secondSide = rawSlot >= 18 && rawSlot <= 26;

        if ((player.equals(session.first) && firstSide) || (player.equals(session.second) && secondSide)) {
            event.setCancelled(false);
            session.resetConfirmation();
        }
    }

    @EventHandler
    public void onTradeClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        if (!(event.getView().getTopInventory().getHolder() instanceof TradeHolder holder)) {
            return;
        }

        TradeSession session = holder.session;
        if (!activeTrades.containsKey(player.getUniqueId())) {
            return;
        }

        Bukkit.getScheduler().runTask(this, () -> {
            if (activeTrades.containsKey(player.getUniqueId())) {
                cancelTrade(session, "Trade cancelled because a player closed the trade.");
            }
        });
    }

    private final class TradeSession {
        private final Player first;
        private final Player second;
        private final Inventory inventory;
        private boolean firstConfirmed;
        private boolean secondConfirmed;

        private TradeSession(Player first, Player second) {
            this.first = first;
            this.second = second;
            this.inventory = Bukkit.createInventory(new TradeHolder(this), 27, Component.text("Trade"));
            setup();
        }

        private void setup() {
            for (int slot = 9; slot <= 17; slot++) {
                inventory.setItem(slot, createGlass(Material.GRAY_STAINED_GLASS_PANE, " "));
            }

            inventory.setItem(11, createGlass(Material.LIME_STAINED_GLASS_PANE, "Confirm Trade"));
            inventory.setItem(15, createGlass(Material.LIME_STAINED_GLASS_PANE, "Confirm Trade"));
            inventory.setItem(22, createGlass(Material.RED_STAINED_GLASS_PANE, "Cancel Trade"));
        }

        private ItemStack createGlass(Material material, String name) {
            ItemStack item = new ItemStack(material);
            item.editMeta(meta -> meta.displayName(Component.text(name, NamedTextColor.WHITE)));
            return item;
        }

        private void open() {
            first.openInventory(inventory);
            second.openInventory(inventory);

            first.sendMessage(Component.text("Trade started with " + second.getName() + ".", NamedTextColor.GREEN));
            second.sendMessage(Component.text("Trade started with " + first.getName() + ".", NamedTextColor.GREEN));
        }

        private void resetConfirmation() {
            firstConfirmed = false;
            secondConfirmed = false;
        }

        private void confirm(Player player) {
            if (player.equals(first)) {
                firstConfirmed = true;
            } else if (player.equals(second)) {
                secondConfirmed = true;
            }

            if (firstConfirmed && secondConfirmed) {
                complete();
            } else {
                player.sendMessage(Component.text("You confirmed the trade. Waiting for the other player.", NamedTextColor.YELLOW));
            }
        }

        private void complete() {
            activeTrades.remove(first.getUniqueId());
            activeTrades.remove(second.getUniqueId());

            for (int slot = 0; slot <= 8; slot++) {
                ItemStack item = inventory.getItem(slot);
                if (item != null && item.getType() != Material.AIR) {
                    giveItem(second, item);
                    inventory.setItem(slot, null);
                }
            }

            for (int slot = 18; slot <= 26; slot++) {
                ItemStack item = inventory.getItem(slot);
                if (item != null && item.getType() != Material.AIR) {
                    giveItem(first, item);
                    inventory.setItem(slot, null);
                }
            }

            first.closeInventory();
            second.closeInventory();

            first.sendMessage(Component.text("Trade completed with " + second.getName() + "!", NamedTextColor.GREEN));
            second.sendMessage(Component.text("Trade completed with " + first.getName() + "!", NamedTextColor.GREEN));
        }

        private void giveItem(Player player, ItemStack item) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            for (ItemStack remaining : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), remaining);
            }
        }
    }

    private static final class TradeRequest {
        private final UUID requester;

        private TradeRequest(UUID requester) {
            this.requester = requester;
        }
    }

    private static final class TradeHolder implements InventoryHolder {
        private final TradeSession session;

        private TradeHolder(TradeSession session) {
            this.session = session;
        }

        @Override
        public Inventory getInventory() {
            return session.inventory;
        }
    }
}
