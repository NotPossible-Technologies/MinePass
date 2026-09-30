package io.github.duckycashy.minepass.command;

import io.github.duckycashy.minepass.MinePassPlugin;
import io.github.duckycashy.minepass.model.PasswordEntry;
import io.github.duckycashy.minepass.model.PasswordType;
import io.github.duckycashy.minepass.storage.PasswordStore;
import io.github.duckycashy.minepass.update.UpdateChecker;
import io.github.duckycashy.minepass.util.DurationParser;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

public final class MinePassCommand
        implements CommandExecutor, TabCompleter {

    private final MinePassPlugin plugin;
    private final PasswordStore passwordStore;
    private final UpdateChecker updateChecker;

    public MinePassCommand(
            MinePassPlugin plugin,
            PasswordStore passwordStore,
            UpdateChecker updateChecker
    ) {
        this.plugin = plugin;
        this.passwordStore = passwordStore;
        this.updateChecker = updateChecker;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!sender.hasPermission(
                "minepass.admin"
        )) {
            sender.sendMessage(
                    ChatColor.RED
                            + "You do not have permission to use MinePass."
            );
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list" -> list(sender);
            case "create" -> create(sender, args);
            case "delete" -> delete(sender, args);
            case "lockserver" -> lockServer(sender);
            case "unlockserver" -> unlockServer(sender);
            case "update" -> update(sender);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void list(CommandSender sender) {
        var passwords =
                passwordStore.getPasswords();

        sender.sendMessage(
                ChatColor.GOLD
                        + "MinePass passwords ("
                        + passwords.size()
                        + "):"
        );

        if (passwords.isEmpty()) {
            sender.sendMessage(
                    ChatColor.GRAY
                            + "No passwords have been created."
            );
        } else {
            for (PasswordEntry entry : passwords) {
                String expiration =
                        entry.getExpiresAt() == null
                                ? "Never"
                                : formatExpiration(
                                entry.getExpiresAt()
                        );

                sender.sendMessage(
                        ChatColor.GRAY
                                + "- "
                                + ChatColor.WHITE
                                + entry.getPassword()
                                + ChatColor.GRAY
                                + " ["
                                + entry.getType().name()
                                + "] expires: "
                                + expiration
                );
            }
        }

        sender.sendMessage(
                ChatColor.GRAY
                        + "Server locked: "
                        + ChatColor.WHITE
                        + passwordStore.isServerLocked()
        );
    }

    private void create(
            CommandSender sender,
            String[] args
    ) {
        if (args.length < 3) {
            sender.sendMessage(
                    ChatColor.RED
                            + "Usage: /mp create <Perm|Temp> <password>"
            );
            return;
        }

        PasswordType type;

        try {
            type = switch (
                    args[1].toLowerCase()
                    ) {
                case "perm", "permanent" ->
                        PasswordType.PERMANENT;

                case "temp", "temporary" ->
                        PasswordType.TEMPORARY;

                default ->
                        throw new IllegalArgumentException();
            };

        } catch (IllegalArgumentException exception) {
            sender.sendMessage(
                    ChatColor.RED
                            + "Password type must be Perm or Temp."
            );
            return;
        }

        String password =
                joinArguments(args, 2).trim();

        if (password.isEmpty()) {
            sender.sendMessage(
                    ChatColor.RED
                            + "Password cannot be empty."
            );
            return;
        }

        int maximumLength =
                plugin.getConfig().getInt(
                        "maximum-password-length",
                        128
                );

        if (password.length() > maximumLength) {
            sender.sendMessage(
                    ChatColor.RED
                            + "Password is too long. Maximum length: "
                            + maximumLength
            );
            return;
        }

        long createdAt =
                System.currentTimeMillis();

        Long expiresAt = null;

        if (type == PasswordType.TEMPORARY) {
            String configuredDuration =
                    plugin.getConfig().getString(
                            "temporary-password-duration",
                            "1h"
                    );

            try {
                Duration duration =
                        DurationParser.parse(
                                configuredDuration
                        );

                expiresAt =
                        createdAt
                                + duration.toMillis();

            } catch (IllegalArgumentException exception) {
                sender.sendMessage(
                        ChatColor.RED
                                + "The configured temporary-password-duration is invalid."
                );
                return;
            }
        }

        PasswordEntry entry =
                new PasswordEntry(
                        password,
                        type,
                        createdAt,
                        expiresAt
                );

        if (!passwordStore.addPassword(entry)) {
            sender.sendMessage(
                    ChatColor.RED
                            + "That password already exists."
            );
            return;
        }

        sender.sendMessage(
                ChatColor.GREEN
                        + "Password created successfully."
        );
    }

    private void delete(
            CommandSender sender,
            String[] args
    ) {
        if (args.length < 2) {
            sender.sendMessage(
                    ChatColor.RED
                            + "Usage: /mp delete <password>"
            );
            return;
        }

        String password =
                joinArguments(args, 1).trim();

        if (!passwordStore.removePassword(
                password
        )) {
            sender.sendMessage(
                    ChatColor.RED
                            + "Password not found."
            );
            return;
        }

        sender.sendMessage(
                ChatColor.GREEN
                        + "Password deleted."
        );
    }

    private void lockServer(
            CommandSender sender
    ) {
        if (passwordStore.isServerLocked()) {
            sender.sendMessage(
                    ChatColor.YELLOW
                            + "The server is already locked."
            );
            return;
        }

        passwordStore.setServerLocked(true);

        sender.sendMessage(
                ChatColor.RED
                        + "MinePass server lockdown enabled."
        );
    }

    private void unlockServer(
            CommandSender sender
    ) {
        if (!passwordStore.isServerLocked()) {
            sender.sendMessage(
                    ChatColor.YELLOW
                            + "The server is already unlocked."
            );
            return;
        }

        passwordStore.setServerLocked(false);

        sender.sendMessage(
                ChatColor.GREEN
                        + "MinePass server lockdown disabled."
        );
    }

    private void update(
            CommandSender sender
    ) {
        sender.sendMessage(
                ChatColor.GRAY
                        + "Checking GitHub and Modrinth for updates..."
        );

        updateChecker.check()
                .thenAccept(result ->
                        sender.sendMessage(
                                result.message()
                        )
                );
    }

    private String formatExpiration(
            Long expiresAt
    ) {
        long remaining =
                expiresAt
                        - System.currentTimeMillis();

        if (remaining <= 0) {
            return "Expired";
        }

        Duration duration =
                Duration.ofMillis(remaining);

        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();

        if (days > 0) {
            return days + "d " + hours + "h";
        }

        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }

        return minutes + "m";
    }

    private String joinArguments(
            String[] args,
            int start
    ) {
        StringBuilder builder =
                new StringBuilder();

        for (int i = start;
             i < args.length;
             i++) {

            if (i > start) {
                builder.append(' ');
            }

            builder.append(args[i]);
        }

        return builder.toString();
    }

    private void sendUsage(
            CommandSender sender
    ) {
        sender.sendMessage(
                ChatColor.GOLD
                        + "MinePass commands:"
        );

        sender.sendMessage(
                ChatColor.GRAY
                        + "/mp list"
        );

        sender.sendMessage(
                ChatColor.GRAY
                        + "/mp create <Perm|Temp> <password>"
        );

        sender.sendMessage(
                ChatColor.GRAY
                        + "/mp delete <password>"
        );

        sender.sendMessage(
                ChatColor.GRAY
                        + "/mp lockserver"
        );

        sender.sendMessage(
                ChatColor.GRAY
                        + "/mp unlockserver"
        );

        sender.sendMessage(
                ChatColor.GRAY
                        + "/mp update"
        );
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {
        if (args.length == 1) {
            List<String> commands = List.of(
                    "list",
                    "create",
                    "delete",
                    "lockserver",
                    "unlockserver",
                    "update"
            );

            String input =
                    args[0].toLowerCase();

            return commands.stream()
                    .filter(value ->
                            value.startsWith(input))
                    .toList();
        }

        if (args.length == 2
                && args[0].equalsIgnoreCase(
                "create"
        )) {
            return List.of(
                    "Perm",
                    "Temp"
            );
        }

        return Collections.emptyList();
    }
}