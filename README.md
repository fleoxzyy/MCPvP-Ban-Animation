# MCPvPBanAnimation

The classic **MCPvP-style ban animation** for Spigot, Paper and Folia. It's an animation only, not a
ban plugin: it plugs into **whatever ban plugin you already use**.

When a player is banned, the ban goes through **instantly**. Then everyone nearby sees:

1. A frozen **replica** of the player, wearing their skin and gear, with a floating `● {username} BANNED ●` label.
2. A giant **mace** falling from the sky and smashing it into the ground.
3. **Bones**, particles, a shockwave and a screen shake bursting out.

There's nothing to set up. Your staff keep using their usual ban commands.

> **Status:** in development. See the [roadmap](#roadmap).

---

## Highlights

- **Bans are never delayed.** The plugin only watches bans. It never cancels, holds or re-runs them.
  If anything goes wrong, you lose the visuals, never the ban.
- **Only bans animate.** Bans, temp bans and IP bans play the animation. A normal `/kick` never does.
- **One jar** for Spigot, Paper and Folia, 1.21 through 26.x.
- **Real player model** on 1.21.9+ (Mannequin). Older versions use an armor stand with the player's head.
- **Simple config.** The MCPvP look is built in. You only set the timing, a few toggles and the messages.
- **Leaves nothing behind.** Every spawned entity is tagged and removed, even after a crash.

## Supported servers

| Platform | Minecraft | Java |
|---|---|---|
| Spigot / Paper / Folia | 1.21 – 1.21.x | 21+ |
| Spigot / Paper / Folia | 26.1 – 26.3 | 25+ |

## Installation

1. Download `MCPvPBanAnimation-<version>.jar`, or [build it yourself](#building).
2. Drop it into `plugins/` and restart the server. Avoid `/reload`.
3. Run `/mcpvpban test` in-game to preview the animation on yourself.
4. Adjust `plugins/MCPvPBanAnimation/config.yml` and `messages.yml`, then run `/mcpvpban reload`.

## Commands & permissions

| Command | Permission | Description |
|---|---|---|
| `/mcpvpban test [player]` | `mcpvpban.test` | Preview the animation at a player's position. Nobody is banned or touched |
| `/mcpvpban reload` | `mcpvpban.admin` | Reload `config.yml` and `messages.yml` |
| `/mcpvpban info` | `mcpvpban.admin` | Platform, Minecraft / Java version, active hooks |
| — | `mcpvpban.bypass` | Never animate this player. Their bans still work normally |

Aliases: `/mcpvpbananimation`, `/banimation`. All permissions except `bypass` default to op.

## Ban plugin support

| Plugin | Default mode | How bans are detected |
|---|---|---|
| LiteBans | `API` | LiteBans event API |
| AdvancedBan | `API` | `PunishmentEvent` |
| LibertyBans | `API` | `PostPunishEvent` |
| Vanilla `/ban`, `/ban-ip` | `COMMAND` | Ban command + ban kick |
| EssentialsX | `COMMAND` | Ban command + ban kick |
| LightBans | `COMMAND` | Ban command + ban kick |
| **Any other ban plugin** (BanManager, MaxBans, …) | `COMMAND` | Ban command + ban kick |

Every common ban command is recognised automatically: `ban`, `tempban`, `ipban`, `banip`, `ban-ip`,
`tempipban` and similar, also namespaced (`/anyplugin:ban`). If your plugin uses an unusual name,
add it to `detection.extra-commands`. Each plugin's mode can be changed under `hooks:` in `config.yml`
(`API`, `COMMAND`, `KICK` or `OFF`).

**When does a kick count as a ban?** Only if a ban command for that player was just run, a ban
plugin reported the ban, the server reports the kick as a ban (Paper/Folia), or the player is on the
server's ban list. Matching the kick message text is an opt-in last resort (`match-kick-message`),
for bans made from a web panel or GUI.

## Configuration

`config.yml` is kept short on purpose. The sounds, particles, bone physics and mace look are the
fixed MCPvP style, built into the plugin. What you can change:

| Section | What it controls |
|---|---|
| `animation` | Mace delay, fall time, drop height, enchant shimmer, look-up angle, camera shake, replica type, label text |
| `hooks` | Detection mode per ban plugin (`API` / `COMMAND` / `KICK` / `OFF`) |
| `detection` | Skip silent bans, extra ban commands, opt-in kick-message matching |

Chat texts live in `messages.yml`. The plugin never sends a ban message to chat, because your ban
plugin already announces bans.

- **Text** uses [MiniMessage](https://docs.advntr.dev/minimessage/format.html). Legacy `&c` and
  `&#RRGGBB` codes also work. On Spigot, the text is converted to legacy colors automatically.
- **Placeholders:** `{username}`, `{reason}`, `{staff}`, `{duration}`.
- **Times** are in ticks (20 ticks = 1 second).
- **Upgrades:** when a new version changes the file, it is rewritten in the new layout with your
  values kept (also for renamed options) and a backup of the old file. A file with broken YAML is
  renamed to `.broken` and replaced with the defaults.

## Building

Requires JDK 21+.

```bash
./gradlew shadowJar        # Linux / macOS
gradlew.bat shadowJar      # Windows
```

The jar is written to `build/libs/MCPvPBanAnimation-<version>.jar`. The version is
`1.1.2-<git short hash>`, or `1.1.2-nogit` outside a git repository.

## Roadmap

- [x] Gradle project, plugin.yml, config loading
- [x] Platform layer (Spigot / Paper / Folia schedulers, text, feature detection)
- [x] Animation engine (timeline, falling mace, impact, bones)
- [x] Replica (Mannequin / armor stand) + label
- [x] Instant-ban flow (snapshots, snapshot cache, duplicate guard)
- [x] Ban command detection
- [x] Ban kick detection
- [x] LiteBans / AdvancedBan / LibertyBans hooks
- [x] Commands (`test`, `reload`, `info`) with tab completion
- [ ] Test on every platform and version

## License

[MIT](LICENSE) © 2026 fleoraticlyyy
