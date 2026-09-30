package io.github.duckycashy.minepass.dialog;

import io.github.duckycashy.minepass.MinePassPlugin;
import io.github.duckycashy.minepass.model.PasswordEntry;
import io.github.duckycashy.minepass.storage.PasswordStore;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public final class PasswordDialog {

    private static final String INPUT_KEY = "minepass_password";

    private final MinePassPlugin plugin;
    private final PasswordStore passwordStore;

    public PasswordDialog(
            MinePassPlugin plugin,
            PasswordStore passwordStore
    ) {
        this.plugin = plugin;
        this.passwordStore = passwordStore;
    }

    public void show(
            UUID uuid,
            Audience audience,
            CompletableFuture<Boolean> result
    ) {
        AtomicInteger attempts = new AtomicInteger(0);

        showAttempt(
                uuid,
                audience,
                result,
                attempts
        );
    }

    private void showAttempt(
            UUID uuid,
            Audience audience,
            CompletableFuture<Boolean> result,
            AtomicInteger attempts
    ) {
        if (result.isDone()) {
            return;
        }

        int maximumAttempts = plugin.getConfig().getInt(
                "maximum-login-attempts",
                3
        );

        int remaining = Math.max(
                0,
                maximumAttempts - attempts.get()
        );

        Component body = Component.text(
                        "Enter the server password to continue.",
                        NamedTextColor.GRAY
                )
                .append(Component.newline())
                .append(
                        Component.text(
                                "Attempts remaining: " + remaining,
                                NamedTextColor.DARK_GRAY
                        )
                );

        DialogInput passwordInput = DialogInput.text(
                        INPUT_KEY,
                        Component.text(
                                "Password",
                                NamedTextColor.GRAY
                        )
                )
                .labelVisible(true)
                .maxLength(
                        plugin.getConfig().getInt(
                                "maximum-password-length",
                                128
                        )
                )
                .width(300)
                .build();

        ActionButton authenticateButton = ActionButton.create(
                Component.text("Authenticate"),
                Component.text("Submit the password."),
                150,
                DialogAction.customClick(
                        (response, callbackAudience) ->
                                handlePassword(
                                        uuid,
                                        callbackAudience,
                                        response,
                                        result,
                                        attempts
                                ),
                        net.kyori.adventure.text.event.ClickCallback.Options
                                .builder()
                                .uses(1)
                                .lifetime(Duration.ofMinutes(2))
                                .build()
                )
        );

        ActionButton cancelButton = ActionButton.create(
                Component.text("Cancel"),
                Component.text("Disconnect from the server."),
                150,
                DialogAction.customClick(
                        (response, callbackAudience) ->
                                result.complete(false),
                        net.kyori.adventure.text.event.ClickCallback.Options
                                .builder()
                                .uses(1)
                                .lifetime(Duration.ofMinutes(2))
                                .build()
                )
        );

        Dialog dialog = Dialog.create(builder ->
                builder.empty()
                        .base(
                                DialogBase.builder(
                                                Component.text("MinePass")
                                        )
                                        .canCloseWithEscape(false)
                                        .body(
                                                List.of(
                                                        DialogBody.plainMessage(body)
                                                )
                                        )
                                        .inputs(
                                                List.of(passwordInput)
                                        )
                                        .build()
                        )
                        .type(
                                DialogType.confirmation(
                                        authenticateButton,
                                        cancelButton
                                )
                        )
        );

        audience.showDialog(dialog);
    }

    private void handlePassword(
            UUID uuid,
            Audience audience,
            DialogResponseView response,
            CompletableFuture<Boolean> result,
            AtomicInteger attempts
    ) {
        if (result.isDone()) {
            return;
        }

        String password = response.getText(INPUT_KEY);

        if (password == null) {
            password = "";
        }

        PasswordEntry entry = passwordStore.getPassword(password);

        if (entry != null && !entry.isExpired()) {
            if (plugin.getConfig().getBoolean(
                    "auto-whitelist",
                    true
            )) {
                passwordStore.addAutoWhitelist(uuid);
            }

            result.complete(true);
            return;
        }

        int attempt = attempts.incrementAndGet();

        int maximumAttempts = plugin.getConfig().getInt(
                "maximum-login-attempts",
                3
        );

        if (attempt >= maximumAttempts) {
            result.complete(false);
            return;
        }

        audience.sendMessage(
                Component.text(
                        "Incorrect password.",
                        NamedTextColor.RED
                )
        );

        showAttempt(
                uuid,
                audience,
                result,
                attempts
        );
    }
}