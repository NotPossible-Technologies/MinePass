# MinePass

MinePass is a Paper plugin that places a password gate in front of world entry. It uses Minecraft's native Dialog API during the connection configuration phase instead of waiting for a normal join event.

## Supported versions

MinePass targets:

- Paper 1.21.11
- Paper 26.1
- Paper 26.2
- Paper 26.3

The source is compiled against the Paper 1.21.11 API so the same jar can target the supported range.

## Project structure

```text
MinePass/
├── build.gradle
├── gradle.properties
├── settings.gradle
└── src/
    └── main/
        ├── java/
        │   └── io/github/duckycashy/minepass/
        │       ├── MinePassPlugin.java
        │       ├── command/
        │       │   └── MinePassCommand.java
        │       ├── connection/
        │       │   └── ConnectionGate.java
        │       ├── model/
        │       │   ├── PasswordEntry.java
        │       │   └── PasswordType.java
        │       ├── storage/
        │       │   └── PasswordStore.java
        │       ├── update/
        │       │   └── UpdateChecker.java
        │       └── util/
        │           └── DurationParser.java
        └── resources/
            ├── config.yml
            └── plugin.yml
```

## Build

Requirements:

- JDK 21
- Gradle
- Internet access for the Paper API dependency

Run:

```text
gradle build
```

The plugin jar is written to:

```text
build/libs/
```

## Connection flow

MinePass listens to `AsyncPlayerConnectionConfigureEvent`. Paper exposes this event after connection configuration and before the player enters the world.

When a connection arrives:

1. MinePass checks the server lock state.
2. A native Minecraft Dialog is shown.
3. The connection remains in the configuration phase while the response is pending.
4. The submitted password is checked against the in-memory password store.
5. A valid password allows the configuration event to finish.
6. An invalid password consumes an attempt.
7. Cancellation, timeout, disconnect, or too many failed attempts closes the connection.

The Dialog action uses a custom click identifier and reads the submitted value through `DialogResponseView`.

## Password storage

Passwords are kept in memory for fast connection checks and persisted to `data.yml`.

Temporary passwords store an expiration timestamp. Expired entries are removed during normal lookups and by the periodic cleanup task.

The store writes through a dedicated single-thread executor so routine password changes do not block the server thread on disk I/O.

## Commands

```text
/mp list
/mp create <Perm|Temp> <password>
/mp delete <password>
/mp lockserver
/mp unlockserver
/mp update
```

The `minepass.admin` permission defaults to operators.

## Update checking

`/mp update` checks:

- GitHub Releases
- Modrinth project versions

The configured sources are controlled by `config.yml`.

```yaml
update-check:
  enabled: true
  github-repository: DuckyCashy/MinePass
  modrinth-project: minepass
```

The checker only reports release information. It does not download or replace the plugin jar.

## Configuration

```yaml
temporary-password-duration: 1h
maximum-password-length: 128
login-timeout-seconds: 60
maximum-login-attempts: 3
```

Temporary password durations use:

```text
30s
15m
2h
7d
```

## Native Dialog limitation

Minecraft's native text dialog input does not expose a password-masking option in the Paper API used by this project. MinePass therefore uses a normal text input for the password field.

## Version policy

The project intentionally uses the oldest supported Paper API as its compile target. This keeps the runtime dependency surface small while allowing the plugin to run on newer supported Paper API lines where those APIs remain compatible.
