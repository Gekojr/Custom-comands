package com.gekojr.customcomands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class CustomComands extends JavaPlugin implements CommandExecutor {

    private static final String DISCORD = "https://discord.gg/mZRyBRzSA3";

    @Override
    public void onEnable() {
        getCommand("discord").setExecutor(this);
        getCommand("annuncio").setExecutor(this);
        getLogger().info("CustomComands attivo.");
    }

    @Override
    public void onDisable() {
        getLogger().info("CustomComands disattivato.");
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
                sender.sendMessage(Component.text("Non hai il permesso per usare questo comando.", NamedTextColor.RED));
                return true;
            }

            if (args.length == 0) {
                sender.sendMessage(Component.text("Uso: /annuncio <messaggio>", NamedTextColor.RED));
                return true;
            }

            String message = String.join(" ", args);

            Component announcement = Component.text()
                .append(Component.text("ANNUNCIO", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(" » ", NamedTextColor.DARK_GRAY))
                .append(Component.text(message, NamedTextColor.WHITE))
                .build();

            getServer().sendMessage(announcement);
            return true;
        }

        return false;
    }
}
