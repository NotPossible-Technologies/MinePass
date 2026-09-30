package io.github.duckycashy.minepass.update;

import io.github.duckycashy.minepass.MinePassPlugin;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UpdateChecker {

    private static final Pattern GITHUB_TAG =
            Pattern.compile(
                    "\"tag_name\"\\s*:\\s*\"([^\"]+)\""
            );

    private static final Pattern MODRINTH_VERSION =
            Pattern.compile(
                    "\"version_number\"\\s*:\\s*\"([^\"]+)\""
            );

    private final MinePassPlugin plugin;
    private final HttpClient client;

    public UpdateChecker(
            MinePassPlugin plugin
    ) {
        this.plugin = plugin;

        this.client =
                HttpClient.newBuilder()
                        .connectTimeout(
                                Duration.ofSeconds(5)
                        )
                        .build();
    }

    public CompletableFuture<UpdateResult> check() {
        if (!plugin.getConfig().getBoolean(
                "update-check.enabled",
                true
        )) {
            return CompletableFuture.completedFuture(
                    new UpdateResult(
                            "Update checking is disabled."
                    )
            );
        }

        return CompletableFuture.supplyAsync(() -> {
            String current =
                    plugin.getPluginMeta()
                            .getVersion();

            String github =
                    getGitHubVersion();

            String modrinth =
                    getModrinthVersion();

            String latest =
                    github != null
                            ? github
                            : modrinth;

            if (latest == null) {
                return new UpdateResult(
                        "Could not retrieve an update from GitHub or Modrinth."
                );
            }

            if (compareVersions(
                    latest,
                    current
            ) > 0) {
                return new UpdateResult(
                        "A newer MinePass release is available: "
                                + latest
                                + " (current: "
                                + current
                                + ")"
                );
            }

            return new UpdateResult(
                    "MinePass is up to date. Current version: "
                            + current
            );
        });
    }

    private String getGitHubVersion() {
        String repository =
                plugin.getConfig().getString(
                        "update-check.github-repository",
                        ""
                );

        if (repository.isBlank()) {
            return null;
        }

        String url =
                "https://api.github.com/repos/"
                        + repository
                        + "/releases/latest";

        String response =
                request(url);

        if (response == null) {
            return null;
        }

        Matcher matcher =
                GITHUB_TAG.matcher(response);

        if (!matcher.find()) {
            return null;
        }

        return normalizeVersion(
                matcher.group(1)
        );
    }

    private String getModrinthVersion() {
        String project =
                plugin.getConfig().getString(
                        "update-check.modrinth-project",
                        ""
                );

        if (project.isBlank()) {
            return null;
        }

        String url =
                "https://api.modrinth.com/v2/project/"
                        + project
                        + "/version";

        String response =
                request(url);

        if (response == null) {
            return null;
        }

        Matcher matcher =
                MODRINTH_VERSION.matcher(response);

        if (!matcher.find()) {
            return null;
        }

        return normalizeVersion(
                matcher.group(1)
        );
    }

    private String request(
            String url
    ) {
        try {
            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .timeout(
                                    Duration.ofSeconds(8)
                            )
                            .header(
                                    "User-Agent",
                                    "MinePass/"
                                            + plugin
                                            .getPluginMeta()
                                            .getVersion()
                            )
                            .GET()
                            .build();

            HttpResponse<String> response =
                    client.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (response.statusCode() != 200) {
                return null;
            }

            return response.body();

        } catch (
                IOException
                | InterruptedException exception
        ) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            return null;
        }
    }

    private String normalizeVersion(
            String version
    ) {
        if (version.startsWith("v")
                || version.startsWith("V")) {
            return version.substring(1);
        }

        return version;
    }

    private int compareVersions(
            String first,
            String second
    ) {
        String[] firstParts =
                normalizeVersion(first)
                        .split("\\.");

        String[] secondParts =
                normalizeVersion(second)
                        .split("\\.");

        int length =
                Math.max(
                        firstParts.length,
                        secondParts.length
                );

        for (int i = 0;
             i < length;
             i++) {

            int firstValue =
                    i < firstParts.length
                            ? parseVersionPart(
                            firstParts[i]
                    )
                            : 0;

            int secondValue =
                    i < secondParts.length
                            ? parseVersionPart(
                            secondParts[i]
                    )
                            : 0;

            if (firstValue != secondValue) {
                return Integer.compare(
                        firstValue,
                        secondValue
                );
            }
        }

        return 0;
    }

    private int parseVersionPart(
            String value
    ) {
        Matcher matcher =
                Pattern.compile(
                        "(\\d+)"
                ).matcher(value);

        if (!matcher.find()) {
            return 0;
        }

        return Integer.parseInt(
                matcher.group(1)
        );
    }

    public record UpdateResult(
            String message
    ) {
    }
}