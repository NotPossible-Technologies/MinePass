package io.github.duckycashy.minepass.storage;

import io.github.duckycashy.minepass.MinePassPlugin;
import io.github.duckycashy.minepass.model.PasswordEntry;
import io.github.duckycashy.minepass.model.PasswordType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PasswordStore {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final char[] INITIAL_PASSWORD_CHARACTERS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
                    .toCharArray();

    private static final int INITIAL_PASSWORD_LENGTH = 16;

    private final MinePassPlugin plugin;
    private final File file;

    private final Map<String, PasswordEntry> passwords =
            new LinkedHashMap<>();

    private final List<UUID> autoWhitelistedPlayers =
            new ArrayList<>();

    private boolean serverLocked;

    public PasswordStore(MinePassPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(
                plugin.getDataFolder(),
                "data.yml"
        );
    }

    public synchronized void load() {
        passwords.clear();
        autoWhitelistedPlayers.clear();

        if (!file.exists()) {
            save();
            return;
        }

        YamlConfiguration data =
                YamlConfiguration.loadConfiguration(file);

        serverLocked =
                data.getBoolean("server-locked", false);

        for (String value :
                data.getStringList("auto-whitelist")) {

            try {
                autoWhitelistedPlayers.add(
                        UUID.fromString(value)
                );
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning(
                        "Ignoring invalid UUID in auto-whitelist: "
                                + value
                );
            }
        }

        ConfigurationSection section =
                data.getConfigurationSection("passwords");

        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection entry =
                    section.getConfigurationSection(key);

            if (entry == null) {
                continue;
            }

            String password =
                    entry.getString("password");

            if (password == null || password.isEmpty()) {
                continue;
            }

            PasswordType type;

            try {
                type = PasswordType.valueOf(
                        entry.getString(
                                "type",
                                "PERMANENT"
                        ).toUpperCase()
                );
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning(
                        "Ignoring password with invalid type: "
                                + password
                );
                continue;
            }

            long createdAt =
                    entry.getLong(
                            "created-at",
                            System.currentTimeMillis()
                    );

            Long expiresAt =
                    entry.contains("expires-at")
                            ? entry.getLong("expires-at")
                            : null;

            PasswordEntry passwordEntry =
                    new PasswordEntry(
                            password,
                            type,
                            createdAt,
                            expiresAt
                    );

            if (!passwordEntry.isExpired()) {
                passwords.put(
                        password,
                        passwordEntry
                );
            }
        }
    }

    public synchronized void save() {
        if (!plugin.getDataFolder().exists()
                && !plugin.getDataFolder().mkdirs()) {

            plugin.getLogger().severe(
                    "Could not create plugin data directory."
            );

            return;
        }

        YamlConfiguration data =
                new YamlConfiguration();

        data.set(
                "server-locked",
                serverLocked
        );

        data.set(
                "auto-whitelist",
                autoWhitelistedPlayers.stream()
                        .map(UUID::toString)
                        .toList()
        );

        int index = 0;

        for (PasswordEntry entry : passwords.values()) {
            String path =
                    "passwords." + index++;

            data.set(
                    path + ".password",
                    entry.getPassword()
            );

            data.set(
                    path + ".type",
                    entry.getType().name()
            );

            data.set(
                    path + ".created-at",
                    entry.getCreatedAt()
            );

            if (entry.getExpiresAt() != null) {
                data.set(
                        path + ".expires-at",
                        entry.getExpiresAt()
                );
            }
        }

        try {
            data.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe(
                    "Could not save data.yml: "
                            + exception.getMessage()
            );
        }
    }

    public synchronized boolean hasPasswords() {
        removeExpiredInternal();
        return !passwords.isEmpty();
    }

    public synchronized PasswordEntry createInitialPassword() {
        removeExpiredInternal();

        if (!passwords.isEmpty()) {
            return null;
        }

        String password;

        do {
            password = generateInitialPassword();
        } while (passwords.containsKey(password));

        PasswordEntry entry =
                new PasswordEntry(
                        password,
                        PasswordType.PERMANENT,
                        System.currentTimeMillis(),
                        null
                );

        passwords.put(
                password,
                entry
        );

        save();

        return entry;
    }

    private String generateInitialPassword() {
        StringBuilder password =
                new StringBuilder(INITIAL_PASSWORD_LENGTH);

        for (int i = 0; i < INITIAL_PASSWORD_LENGTH; i++) {
            password.append(
                    INITIAL_PASSWORD_CHARACTERS[
                            SECURE_RANDOM.nextInt(
                                    INITIAL_PASSWORD_CHARACTERS.length
                            )
                            ]
            );
        }

        return password.toString();
    }

    public synchronized boolean addPassword(
            PasswordEntry entry
    ) {
        removeExpiredInternal();

        if (passwords.containsKey(
                entry.getPassword()
        )) {
            return false;
        }

        passwords.put(
                entry.getPassword(),
                entry
        );

        save();

        return true;
    }

    public synchronized boolean removePassword(
            String password
    ) {
        removeExpiredInternal();

        if (passwords.remove(password) == null) {
            return false;
        }

        save();

        return true;
    }

    public synchronized PasswordEntry getPassword(
            String password
    ) {
        removeExpiredInternal();
        return passwords.get(password);
    }

    public synchronized Collection<PasswordEntry> getPasswords() {
        removeExpiredInternal();
        return List.copyOf(passwords.values());
    }

    public synchronized int removeExpired() {
        int removed = removeExpiredInternal();

        if (removed > 0) {
            save();
        }

        return removed;
    }

    private int removeExpiredInternal() {
        int removed = 0;

        var iterator =
                passwords.entrySet().iterator();

        while (iterator.hasNext()) {
            PasswordEntry entry =
                    iterator.next().getValue();

            if (entry.isExpired()) {
                iterator.remove();
                removed++;
            }
        }

        return removed;
    }

    public synchronized boolean isServerLocked() {
        return serverLocked;
    }

    public synchronized void setServerLocked(
            boolean serverLocked
    ) {
        this.serverLocked = serverLocked;
        save();
    }

    public synchronized boolean isAutoWhitelisted(
            UUID uuid
    ) {
        return autoWhitelistedPlayers.contains(uuid);
    }

    public synchronized void addAutoWhitelist(
            UUID uuid
    ) {
        if (!autoWhitelistedPlayers.contains(uuid)) {
            autoWhitelistedPlayers.add(uuid);
            save();
        }
    }

    public synchronized List<UUID> getAutoWhitelistedPlayers() {
        return List.copyOf(
                autoWhitelistedPlayers
        );
    }
}