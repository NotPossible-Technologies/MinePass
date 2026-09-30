package io.github.duckycashy.minepass;

import io.github.duckycashy.minepass.command.MinePassCommand;
import io.github.duckycashy.minepass.connection.ConnectionGate;
import io.github.duckycashy.minepass.storage.PasswordStore;
import io.github.duckycashy.minepass.update.UpdateChecker;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class MinePassPlugin extends JavaPlugin {

    private PasswordStore passwordStore;
    private ConnectionGate connectionGate;
    private UpdateChecker updateChecker;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        passwordStore = new PasswordStore(this);
        passwordStore.load();

        connectionGate = new ConnectionGate(
                this,
                passwordStore
        );

        getServer().getPluginManager().registerEvents(
                connectionGate,
                this
        );

        updateChecker = new UpdateChecker(this);

        MinePassCommand command = new MinePassCommand(
                this,
                passwordStore,
                updateChecker
        );

        PluginCommand pluginCommand = getCommand("mp");

        if (pluginCommand == null) {
            getLogger().severe(
                    "The /mp command is missing from plugin.yml."
            );
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);

        if (getConfig().getBoolean(
                "update-check.enabled",
                true
        )) {
            updateChecker.check();
        }

        printBanner();
    }

    @Override
    public void onDisable() {
        if (passwordStore != null) {
            passwordStore.save();
        }
    }

    private void printBanner() {
        getLogger().info("");
        getLogger().info(
                "   _______    ________  ________  ________  ________  ________  ________  ________"
        );
        getLogger().info(
                "  ╱       ╲╲ ╱        ╲╱    ╱   ╲╱        ╲╱        ╲╱        ╲╱        ╲╱        ╲"
        );
        getLogger().info(
                " ╱        ╱╱_╱       ╱╱         ╱         ╱         ╱         ╱        _╱        _╱"
        );
        getLogger().info(
                "╱         ╱╱         ╱         ╱        _╱╱      __╱         ╱-        ╱-        ╱"
        );
        getLogger().info(
                "╲__╱__╱__╱ ╲________╱╲__╱_____╱╲________╱╲╲_____╱  ╲___╱____╱╲________╱╲________╱"
        );
        getLogger().info("");
        getLogger().info("Thanks for using MinePass!");
        getLogger().info(
                "GitHub: https://github.com/NotPossible-Technologies/MinePass"
        );
        getLogger().info(
                "Modrinth: https://modrinth.com/plugin/mpss"
        );
        getLogger().info("");
    }
}