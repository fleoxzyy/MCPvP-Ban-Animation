![Banner](https://cdn.modrinth.com/data/cached_images/aeb6ad6b015e85bca1423fe12f8c0a3ead6a4dab.png)

<div align="center">

[![Platform](https://img.shields.io/badge/Platform-Paper%2FSpigot%2FFolia-blue?style=for-the-badge&logo=papermc&logoColor=white)](https://papermc.io/)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21%20--%2026.x-62B47A?style=for-the-badge&logo=minecraft&logoColor=white)](#requirements)
[![bStats Servers](https://img.shields.io/bstats/servers/34911?style=for-the-badge&logo=apachespark&logoColor=white&label=bStats%20Servers&labelColor=181717&color=00AF5C)](https://bstats.org/plugin/bukkit/MCPvPBanAnimation/34911)
[![bStats Players](https://img.shields.io/bstats/players/34911?style=for-the-badge&logo=minecraft&logoColor=white&label=bStats%20Players&labelColor=181717&color=5865F2)](https://bstats.org/plugin/bukkit/MCPvPBanAnimation/34911)
[![GitHub](https://img.shields.io/badge/Source-GitHub-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/fleoxzyy/MCPvP-Ban-Animation)
[![Discord](https://img.shields.io/badge/Community-Discord-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/5XEqzkSwza)
[![Modrinth](https://img.shields.io/badge/Download-Modrinth-00AF5C?style=for-the-badge&logo=modrinth&logoColor=white)](https://modrinth.com/plugin/mcpvp-ban-animation)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)](https://github.com/fleoxzyy/MCPvP-Ban-Animation/blob/main/LICENSE)

</div>

### Need a server?
Click the banner below and use code **FLEOX** for 30% off your first 2 billing cycles with Alyrix Cloud:

![Alyrix Cloud](https://cdn.modrinth.com/data/cached_images/ac975e14b2fbce61c7d0774b9cfd35be63df50ef.jpeg)

## [Alyrix Cloud](https://alyrixcloud.org?ref=ALYRIX)

---

MCPvPBanAnimation brings back the classic **MCPvP ban animation**. It isn't a ban plugin: it works on top of **the ban plugin you already use**, and your staff keep using their usual commands.

When a player is banned, the ban goes through **instantly** and the player is gone. Then everyone nearby sees:

- A frozen **replica** of the player, with their skin and gear, looking up with a floating `● {username} BANNED ●` label.
- A giant **enchanted mace** falling from the sky and smashing it into the ground.
- **Bones**, particles, a shockwave and a screen shake bursting out.

The whole thing is over in about 4 seconds, so it's quick enough for practice PvP.

## Requirements
- Spigot, Paper, Purpur or Folia, **1.21 or newer** (up to 26.x)
- Java 21+ (Java 25+ on 26.x)
- No dependencies

## Features

- **Bans are never delayed:** the plugin only watches bans. It never cancels, holds or re-runs them. If anything goes wrong, you lose the visuals, never the ban.
- **Works with any ban plugin:** LiteBans, AdvancedBan and LibertyBans are hooked directly for the exact staff, reason and duration. Vanilla, EssentialsX, LightBans, BanManager and every other ban plugin are detected through their ban commands.
- **Only bans animate:** bans, temp bans and IP bans play the animation. A normal `/kick` never does.
- **Real player model:** on 1.21.9+ the replica is a Mannequin with the player's skin. Older versions use an armor stand with the player's head and dyed armor.
- **Silent bans stay silent:** `/ban Steve -s` doesn't play the animation.
- **No chat spam:** the plugin never sends a ban message. Your ban plugin already does that.
- **Folia support:** everything runs on the region thread that owns the area.
- **One jar** for Spigot, Paper and Folia.
- **Leaves nothing behind:** every spawned entity is tagged and cleaned up, even after a crash.
- **Short config:** the MCPvP look is built in. You only set the timing and a few toggles.

## Commands

| Command | Description |
|---|---|
| `/mcpvpban test [player]` | Preview the animation at a player's position. Nobody is banned |
| `/mcpvpban reload` | Reload `config.yml` and `messages.yml` |
| `/mcpvpban info` | Show the version, server, replica type and connected ban plugins |

**Aliases:** `/mcpvpbananimation`, `/banimation`

## Permissions

| Permission | Description | Default |
|---|---|---|
| `mcpvpban.*` | All commands (no bypass included) | OP |
| `mcpvpban.test` | Use `/mcpvpban test` | OP |
| `mcpvpban.admin` | Use `/mcpvpban reload` and `/mcpvpban info` | OP |
| `mcpvpban.bypass` | Never play the animation for this player. Their bans still work normally | No one |

## Ban plugin support

| Plugin | Detected through |
|---|---|
| LiteBans | API (exact staff, reason, duration) |
| AdvancedBan | API (exact staff, reason, duration) |
| LibertyBans | API (exact staff, reason, duration) |
| Vanilla `/ban`, `/ban-ip` | Ban command |
| EssentialsX | Ban command |
| LightBans | Ban command |
| Any other ban plugin | Ban command |

Common ban commands are recognised automatically, also with a plugin prefix like `/anyplugin:ban`: `ban`, `tempban`, `ipban`, `banip`, `ban-ip`, `tempipban` and similar. If your ban plugin uses an unusual name, add it to `detection.extra-commands`.

## Config

<details>
<summary><b>Click to expand config.yml</b></summary>

```yaml
# ╔══════════════════════════════════════════════════════════════════╗
# ║                  MCPvPBanAnimation · config.yml                  ║
# ╚══════════════════════════════════════════════════════════════════╝
#
#  A player is banned → the ban goes through instantly → a replica of them is
#  smashed by a giant falling mace, and bones fly everywhere.
#  Only bans play it (ban, tempban, IP ban). A normal /kick never does.
#
#  Text uses MiniMessage. Legacy &c / &#RRGGBB codes also work.
#  Placeholders: {username} {reason} {staff} {duration}
#  Times are in ticks (20 ticks = 1 second).
#
#  /mcpvpban test    preview the animation on yourself
#  /mcpvpban reload  apply changes to this file and messages.yml

# Do not change. Used to upgrade this file automatically.
config-version: 4

# Log why each kick/command was or wasn't treated as a ban.
debug: false

# ── Animation ───────────────────────────────────────────────────────────
animation:
  # The mace appears this long after the ban...
  mace-spawn-delay: 4
  # ...and falls for this long. Defaults: impact at about 1.2 s, all gone after about 4 s.
  mace-fall-ticks: 20
  # How high above the replica the mace starts, in blocks.
  mace-height: 12.0
  # Enchantment shimmer on the mace.
  enchanted-mace: true
  # How far the replica looks up at the mace, in degrees (0 = straight ahead).
  look-up: 75
  # Shake the screen of nearby players on impact.
  camera-shake: true
  # auto = real player model on 1.21.9+ (Mannequin), armor stand on older versions.
  # Or force: mannequin / armorstand
  replica: auto
  # Floating text above the replica.
  label: "<red>● <dark_red><bold>{username} BANNED</bold> <red>●"

# ── Ban plugins ─────────────────────────────────────────────────────────
# Works with every ban plugin. Their ban commands (/ban, /tempban, /ipban, /banip, /tempipban...)
# are watched automatically, also namespaced ones like /anyplugin:ban.
# These plugins are hooked directly for exact staff / reason / duration:
# LiteBans, AdvancedBan, LibertyBans. You normally don't need to touch this.
#   API      the plugin's own events (most accurate)
#   COMMAND  watch its ban commands + the kick that follows
#   KICK     only count kicks the server itself reports as bans
#   OFF      never animate this plugin's bans
hooks:
  vanilla: COMMAND
  essentials: COMMAND
  litebans: API
  advancedban: API
  libertybans: API
  lightbans: COMMAND
  # Every other ban plugin (BanManager, MaxBans, ...).
  other: COMMAND

detection:
  # Don't animate silent bans (/ban Steve -s).
  skip-silent-bans: true
  # Only needed if your ban plugin uses an unusual command name, e.g. [ "punish", "gban" ].
  # The target player must be the first argument.
  extra-commands: []
  # Last resort for unsupported ban plugins: count a kick as a ban if its message says
  # "you have been banned". Off by default, a normal kick message could say that too.
  match-kick-message: false
```

</details>

## Building

Requires JDK 21+.

```bash
./gradlew shadowJar        # Linux / macOS
gradlew.bat shadowJar      # Windows
```

The jar is written to `build/libs/`.

## Statistics
<div align="center">

[![bStats](https://bstats.org/signatures/bukkit/MCPvPBanAnimation.svg)](https://bstats.org/plugin/bukkit/MCPvPBanAnimation/34911)

</div>

This plugin uses [bStats](https://bstats.org/) to collect anonymous usage stats (server count, platform, which ban plugin is used). You can turn it off in `plugins/bStats/config.yml`.
## Support

Found a bug or have a question? Ask on Discord or open an issue on GitHub.

[![Discord Banner](https://img.shields.io/badge/Join_Discord-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/5XEqzkSwza)
[![GitHub Banner](https://img.shields.io/badge/GitHub_Repository-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/fleoxzyy/MCPvP-Ban-Animation)

*Banned in style.*
