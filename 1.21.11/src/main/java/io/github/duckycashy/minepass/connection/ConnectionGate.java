package io.github.duckycashy.minepass.connection;

import io.github.duckycashy.minepass.dialog.PasswordDialog;
import io.github.duckycashy.minepass.MinePassPlugin;
import io.github.duckycashy.minepass.storage.PasswordStore;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class ConnectionGate implements Listener {

    private final MinePassPlugin plugin;
    private final PasswordStore passwordStore;
    private final PasswordDialog passwordDialog;

    public ConnectionGate(
            MinePassPlugin plugin,
            PasswordStore passwordStore
    ) {
        this.plugin = plugin;
        this.passwordStore = passwordStore;
        this.passwordDialog =
                new PasswordDialog(
                        plugin,
                        passwordStore
                );
    }

    @EventHandler
    public void onConfigure(
            AsyncPlayerConnectionConfigureEvent event
    ) {
        PlayerConfigurationConnection connection =
                event.getConnection();

        UUID uuid =
                connection.getProfile().getId();

        if (uuid == null) {
            connection.disconnect(
                    Component.text(
                            "MinePass could not identify your account."
                    )
            );
            return;
        }

        if (passwordStore.isServerLocked()) {
            connection.disconnect(
                    Component.text(
                            "This server is currently locked."
                    )
            );
            return;
        }

        String username =
                connection.getProfile().getName();

        if (isWhitelisted(uuid, username)) {
            return;
        }

        CompletableFuture<Boolean> authentication =
                new CompletableFuture<>();

        passwordDialog.show(
                uuid,
                connection.getAudience(),
                authentication
        );

        long timeoutSeconds =
                plugin.getConfig().getLong(
                        "login-timeout-seconds",
                        60
                );

        try {
            boolean authenticated =
                    authentication.get(
                            timeoutSeconds,
                            TimeUnit.SECONDS
                    );

            if (!authenticated) {
                connection.disconnect(
                        Component.text(
                                "MinePass authentication failed."
                        )
                );
            }

        } catch (TimeoutException exception) {
            connection.disconnect(
                    Component.text(
                            "MinePass authentication timed out."
                    )
            );

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            connection.disconnect(
                    Component.text(
                            "MinePass authentication was interrupted."
                    )
            );

        } catch (Exception exception) {
            plugin.getLogger().warning(
                    "Authentication failed for "
                            + uuid
                            + ": "
                            + exception.getMessage()
            );

            connection.disconnect(
                    Component.text(
                            "MinePass authentication failed."
                    )
            );
        }
    }

    private boolean isWhitelisted(
            UUID uuid,
            String username
    ) {
        if (passwordStore.isAutoWhitelisted(uuid)) {
            return true;
        }

        if (plugin.getConfig().getBoolean(
                "operator-bypass",
                true
        )) {
            if (Bukkit.getOfflinePlayer(uuid).isOp()) {
                return true;
            }
        }

        List<String> uuidWhitelist =
                plugin.getConfig().getStringList(
                        "whitelist.uuids"
                );

        for (String value : uuidWhitelist) {
            if (value.equalsIgnoreCase(
                    uuid.toString()
            )) {
                return true;
            }
        }

        if (username != null) {
            List<String> userWhitelist =
                    plugin.getConfig().getStringList(
                            "whitelist.users"
                    );

            for (String value : userWhitelist) {
                if (value.equalsIgnoreCase(
                        username
                )) {
                    return true;
                }
            }
        }

        return false;
    }
}