<p align="center">
<img src="https://nexusmc.store/cdn/shop/files/Lock.png?v=1734824084" alt="MinePass" width="15%"/>
</p>

<h1 align="center">MinePass</h1>

<p align="center">
MinePass is a Paper plugin that places a password gate in front of world entry. It uses Minecraft's native Dialog API during the connection configuration phase instead of waiting for a normal join event.
</p>

<p align="center">
<a href="https://modrinth.com/plugin/mpss"><img alt="Modrinth Downloads" src="https://img.shields.io/modrinth/dt/mpss?logo=modrinth&logoColor=white&label=Modrinth&color=00AF5C"></a>
<a href="https://modrinth.com/plugin/mpss/versions"><img alt="Supported Minecraft Versions" src="https://img.shields.io/badge/Available%20for-1.21.11%20%E2%80%93%2026.3-00AF5C"></a>
<a href="https://github.com/NotPossible-Technologies/MinePass/releases/latest"><img alt="GitHub Release" src="https://img.shields.io/badge/Release-V1.0.0-blue?style=flat&logo=github"></a>
</p>

> [!IMPORTANT]
> Official builds can be found in the links below.
>
> - **[Modrinth](https://modrinth.com/plugin/mpss)**
> - **[GitHub](https://github.com/NotPossible-Technologies/MinePass)**
> - **[Discord Server](https://discord.com/invite/)**
>
> Other platforms that are not listed here may contain unofficial, forked, or reuploaded builds. Unofficial builds may be malicious.

## Supported versions

MinePass targets:

- Paper 1.21.11
- Paper 26.1
- Paper 26.2
- Paper 26.3

The project uses the oldest supported Paper API as its compile target where possible, allowing the same plugin jar to run across compatible newer Paper versions.

## Project structure

```text
MinePass/
├── build.gradle.kts
├── gradle.properties
├── settings.gradle.kts
└── src/
    └── main/
        ├── java/
        │   └── io/github/duckycashy/minepass/
        │       ├── MinePassPlugin.java
        │       ├── command/
        │       │   └── MinePassCommand.java
        │       ├── connection/
        │       │   ├── ConnectionGate.java
        │       │   └── PasswordAuthenticator.java
        │       ├── dialog/
        │       │   └── PasswordDialog.java
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
