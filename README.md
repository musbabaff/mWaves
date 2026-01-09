<div align="center">

# 🌊 mWaves

**Advanced Wave Management System for Minecraft Servers**

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21+-green?style=for-the-badge&logo=minecraft&logoColor=white)](https://www.minecraft.net/)
[![Paper](https://img.shields.io/badge/Paper-1.21+-blue?style=for-the-badge)](https://papermc.io/)
[![Java](https://img.shields.io/badge/Java-21+-orange?style=for-the-badge&logo=openjdk&logoColor=white)](https://adoptium.net/)

---

**[📥 Download](https://github.com/musbabaff/mWaves/releases/latest)** • 
**[📖 Documentation](#-documentation)** • 
**[🐛 Bug Reports](https://github.com/musbabaff/mWaves/issues)** • 
**[💬 Support](https://musbabaff.com)**

</div>

---

## ✨ Key Features

<table>
<tr>
<td width="50%">

### 🎮 Gameplay Experience
- 🌊 **Infinite Waves** — Fully customizable mob wave progression.
- 🎲 **Spawn Logic** — Probability-based mob selection per wave.
- 🗺️ **Multi-Point Spawning** — Support for multiple world coordinates.
- ❤️ **Dynamic HP Scaling** — Custom health multipliers for mobs.
- 🔄 **Auto-Restart** — Optional automatic wave cycle looping.

</td>
<td width="50%">

### ⚙️ System Features
- 📊 **Dynamic BossBar** — High-fidelity progress tracking for players.
- 🔦 **Mob Highlighting** — Visual glow effects to find remaining mobs.
- 🎵 **Audio Events** — Immersive sounds for starts, completions, and deaths.
- 🏆 **Reward System** — Console-executed rewards upon wave completion.
- 📝 **PAPI Integration** — Robust placeholders for Scoreboards & TAB.

</td>
</tr>
</table>

---

## 📋 Requirements

| Component | Required Version | Essential |
|-----------|------------------|:---------:|
| **Minecraft / Paper** | 1.21.x+ | ✅ |
| **Java** | 21+ | ✅ |
| **MythicMobs** | 5.6.1+ | ✅ |
| **Project Lombok** | 1.18.30+ | 🛠️ (Build only) |
| **PlaceholderAPI** | 2.11.6+ | ❌ (Optional) |
| **ProtocolLib** | 5.4.0+ | ❌ (Optional) |

> **Note:** **ProtocolLib** is required specifically for private mob highlighting (ADMIN mode).

---

## 📥 Quick Installation

1. **Download** the latest JAR from [Releases](https://github.com/ImFriendlyy/mWaves/releases/latest).
2. **Drop** the file into your server's `plugins/` folder.
3. **Ensure** [MythicMobs](https://www.mythicmobs.net/) is installed.
4. **Restart** your server to generate configuration files.
5. **Configure** your waves in `plugins/mWaves/config.yml`.

---

## 📖 Documentation

### 🎯 Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/mwaves reload` | Reloads the configuration | `mwaves.admin` |
| `/mwaves start` | Starts the wave cycle | `mwaves.admin` |
| `/mwaves stop` | Stops all active waves | `mwaves.admin` |
| `/mwaves force_start <id>` | Jump to a specific wave | `mwaves.admin` |
| `/mwaves info` | View current wave statistics | `mwaves.user` |
| `/mwaves highlight` | Highlights remaining mobs | `mwaves.highlight` |

> **Alias:** You can also use `/mw` for all commands.

### 🔑 Permissions

- `mwaves.admin`: Access to all administrative controls.
- `mwaves.user`: Permission to use the info command.
- `mwaves.highlight`: Permission to trigger mob glowing effects.
- `mwaves.bossbar`: Visibility permission (if mode is restricted).

---

### 🔗 PlaceholderAPI (PAPI)

Integrate wave data into your HUD with these placeholders:

| Placeholder | Description |
|-------------|-------------|
| `%mwaves_wave%` | Current active wave number |
| `%mwaves_count%` | Number of remaining mobs in the wave |

---

## ⚙️ Wave Configuration Example

```yaml
# plugins/mWaves/config.yml

Waves:
  - count: 1                    # Wave ID
    mobs:                       # Mob list format: "MythicMobName:Chance"
      - "SkeletalKnight:70"
      - "DarkWizard:30"
    coordinates:                # Spawn points
      - "world,100,64,100"
      - "world,120,64,120"
    mobs-count: 20              # Total mobs to spawn
    exceptions: 15              # Delay (seconds) before next wave
    health-multiplier: 1.2      # 20% Extra HP for this wave
    title: "&c&lTHE INVASION BEGINS" # Custom BossBar title
    rewards:                    # Commands executed on completion
      - "give @a diamond 2"
      - "broadcast &aWave 1 Defeated!"