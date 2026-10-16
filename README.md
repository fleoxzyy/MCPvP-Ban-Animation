# MCPvPBanAnimation

The classic **MCPvP-style ban animation** for Spigot, Paper and Folia.

When a player is banned:

1. They are **killed** on the spot.
2. A frozen **replica** of them appears where they died, with a floating `● {username} BANNED ●` label.
3. A giant **mace** falls from the sky and smashes the replica.
4. **Bones** and **particles** burst out.
5. The **real ban** goes through.

It is designed to work with whatever ban plugin your server already uses.

> **Status:** in development. See [Roadmap](#roadmap) for what is finished.

---

## Support

One jar runs on every supported platform.

| Platform | Minecraft | Java |
|---|---|---|
| Spigot | 1.21 – 1.21.x | 21+ |
| Spigot | 26.1 – 26.3 | 25+ |
| Paper | 1.21 – 1.21.x | 21+ |
| Paper | 26.1 – 26.3 | 25+ |
| Folia | 1.21 – 1.21.x | 21+ |
| Folia | 26.1 – 26.3 | 25+ |

The plugin is compiled to Java 21 bytecode against the 1.21 API. Newer features (such as the
Mannequin entity on 1.21.9+) are detected at runtime.

## Installation

1. Download `MCPvPBanAnimation-<version>.jar` (or [build it yourself](#building)).
2. Put it in your server's `plugins/` folder.
3. Restart the server. Avoid `/reload`.
4. Edit `plugins/MCPvPBanAnimation/config.yml` and `messages.yml`, then run `/mcpvpban reload`.

## Commands & Permissions

| Command | Permission | Description |
|---|---|---|
| `/mcpvpban test <player>` | `mcpvpban.test` | Play the animation only (no kill, no ban) |
| `/mcpvpban ban <player> [duration] [reason]` | `mcpvpban.ban` | Built-in ban through the vanilla ban list |
| `/mcpvpban reload` | `mcpvpban.admin` | Reload the config |
| `/mcpvpban info` | `mcpvpban.admin` | Platform, Minecraft version, Java version, active hooks |
| — | `mcpvpban.bypass` | This player is never animated or killed |

Aliases: `/mcpvpbananimation`, `/banimation`.

## Ban plugin compatibility

| Plugin | Default mode | How |
|---|---|---|
| Vanilla `/ban`, `/minecraft:ban` | `COMMAND` | Command interception + kick fallback |
| EssentialsX | `COMMAND` | Uses the vanilla ban list: command + kick |
| LiteBans | `API` | LiteBans event API |
| AdvancedBan | `API` | `PunishmentEvent` |
| LibertyBans | `API` | `PunishEvent` |
| LightBans | `COMMAND` | Command + kick |
| Anything else (web panels, APIs) | `KICK` | Ban-kick detection |

Set the mode per plugin in `config.yml` under `hooks:`. The options are `COMMAND | KICK | API | OFF`.

## Configuration

All text uses [MiniMessage](https://docs.advntr.dev/minimessage/format.html). Legacy `&c` and
`&#RRGGBB` codes also work. On Spigot, MiniMessage is converted to legacy colors automatically.
Placeholders: `{username}`, `{reason}`, `{staff}`, `{duration}`. Times are in ticks (20 = 1 second).

| Section | What it controls |
|---|---|
| `animation` | Mace spawn delay, fall duration, when the ban is released, cleanup, hard timeout |
| `kill` | Kill on/off, item/XP drops, death message |
| `spectator` | Camera distance, height and angle while the player watches |
| `replica` | `auto` / `mannequin` / `armorstand`, equipment, label text, bob/pulse, squash |
| `mace` | Material, drop height, scale, spin, smoke trail |
| `impact` | Sounds and particles (by `minecraft:` key), shockwave ring, camera shake |
| `bones` | Count, skulls, lifetime, fade, physics (gravity, drag, bounce, spin) |
| `interception` | Ban commands to intercept, silent flags, kick reason patterns |
| `hooks` | Detection mode per ban plugin |

Sounds and particles are looked up by namespaced key. Anything missing on your server version
is skipped with a single warning, never an error.

## Safety

- **A ban is never lost.** If anything fails (an error, a chunk unload, the player quitting,
  the server stopping), the ban is carried out right away. A hard timeout forces it as a last resort.
- Every spawned entity is tagged and non-persistent. Leftovers are removed when chunks load.

## Building

Requires JDK 21 or newer (Gradle downloads a JDK 21 toolchain if needed).

```bash
./gradlew shadowJar        # Linux / macOS
gradlew.bat shadowJar      # Windows
```

The output is `build/libs/MCPvPBanAnimation-<version>.jar`. The version is `1.1.2-<git short hash>`
(`1.1.2-nogit` outside a git repository).

## Roadmap

- [x] Gradle project, plugin.yml, config loading
- [x] Platform layer (Spigot / Paper / Folia schedulers, text, teleports, feature detection)
- [x] Animation engine (timeline, falling mace, impact, bones)
- [ ] Replica (Mannequin / ArmorStand) + label
- [ ] Kill + spectator camera
- [ ] Command interceptor
- [ ] Kick interceptor
- [ ] LiteBans / AdvancedBan / LibertyBans hooks
- [ ] Commands and tab completion
- [ ] Test matrix

## License

[MIT](LICENSE) © 2026 fleoraticlyyy
