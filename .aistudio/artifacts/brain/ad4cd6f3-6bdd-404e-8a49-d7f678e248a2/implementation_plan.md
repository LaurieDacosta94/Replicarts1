# Crown & Dominion: Rise of Ages 3D

A full-scale 3D real-time strategy Android application inspired by *Rise of Nations*, combining territorial border warfare, eight-epoch historical progression, and deep economic management with a mobile-optimized Imperial Parchment & Bronze tactical interface, multi-tier Level of Detail (LOD) 3D rendering, deterministic object pooling, and an extensible Mod & Hook Engine.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following core product and architectural decisions incorporate your confirmed preferences from Phase 1 and define the blueprint for execution:

- **Historical & Civilization Scope**: Full 8-era progression (**Ancient, Classical, Medieval, Gunpowder, Enlightenment, Industrial, Modern, and Information Age**) across 8 asymmetric civilizations (*Romans, Greeks, Egyptians, Mongols, British, Japanese, Aztecs, and Americans*), complete with national borders, attrition damage, Wonders of the World, Rare Resource merchants, and Library research trees.
- **Matchmaking Architecture (Offline + Online)**: Dual-mode matchmaking hub supporting instant **Offline AI Skirmishes** (configurable AI personalities, map types, starting/ending epochs, and game rules) and **Online Cloud Lobbies** powered by Firebase Firestore and Google Sign-In via Jetpack Credential Manager.
- **Mod Manager & Deep Engine Hooks**: JSON Mod & Modpack bundles paired with an **In-App Visual Rule & Asset Hook Editor** and live interceptor pipeline capable of overriding unit stats, gathering formulas, border expansion math, epoch advancement costs, combat damage multipliers, and procedural 3D model/material palettes at runtime.
- **Visual Theme**: **Classic Imperial Parchment & Bronze Strategy Frame** featuring burnished bronze bezels, warm cartographic parchment surfaces, gold-leaf accents, and custom locally bundled Google Fonts (*Cinzel* for imperial display headers, *Plus Jakarta Sans* for crisp tactical readouts, and *JetBrains Mono* for resource tickers, console logs, and API documentation).

---

## 1. Overview & Core Concept

- **What It Does**: Delivers a complete 3D real-time strategy experience engineered specifically for Android touchscreens. Players found cities to expand national borders, harvest six core resources (**Food, Timber, Wealth, Metal, Oil, and Knowledge**), research four Library technology tracks (**Military, Civic, Commerce, Science**), construct monumental Wonders, and command combined-arms armies from Bronze Age phalanxes to Modern main battle tanks and stealth bombers.
- **Target Audience / Persona**: Strategy enthusiasts, modders, and competitive mobile RTS players who want deep macro-strategy, transparent performance telemetry (LOD & object pool controls), and desktop-grade customization without UI clutter on mobile screens.
- **Key Value**: Solves the two classic bottlenecks of mobile RTS games—**viewport crowding** and **frame-time spikes**—by uniting select HUD panels into collapsible imperial docks, automating repetitive micro-tasks (citizen and military automation toggles), and driving the 3D battlefield with distance-based LOD switching and pre-allocated object pools.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Command Citadel & Matchmaking Hub**:
   - Launch into the Imperial Command Screen with instant access to **Quick Skirmish**, **Offline Custom Lobby**, **Online Cloud Matchmaking** (with Google Sign-In status & real-time Firestore room browser), **Spectator Theatre**, **Mod & Modpack Studio**, and **API & Diagnostics Exporter**.
2. **Tactical 3D Battlefield & Touch Ergonomics**:
   - **Single-Finger Drag**: Smooth inertial camera panning across the 3D terrain.
   - **Pinch & Two-Finger Twist**: Continuous zoom from close-up LOD0 unit inspection out to LOD3 strategic cartographic overview, with adjustable pitch and yaw.
   - **Tap / Double-Tap / Marquee Toggle**: Tap to select individual units or cities; double-tap to select all on-screen units of the same class; toggle the **Marquee Box Selection** button to drag selection rectangles over armies.
   - **Contextual Orders**: Tap terrain to move, tap enemy units/cities to attack or besiege, or tap resource nodes with Citizens selected to construct Camps, Farms, Mines, or Oil Wells.
3. **Interactive Stock Resource Map-Tracking**:
   - Clicking any of the six stock resource counters (**Food, Timber, Wealth, Metal, Oil, Knowledge**) in the top Imperial Resource Ribbon activates **Resource Map-Tracking**:
     - Highlights all active gathering structures, assigned citizens, trade caravans, and unexploited rare resource deposits for that resource with pulsing 3D golden light pillars and floating yield badges in the 3D viewport.
     - Pulses matching radar rings on the 3D Minimap.
     - Repeated taps smoothly cycle the camera focus across each active harvesting site while displaying a compact breakdown of gather rates and idle capacity.
4. **Collapsible & Combined HUD Containers**:
   - Every HUD container includes a dedicated bronze **Hide / Collapse Chevron (`_`)** and can be minimized to a slim edge tab, leaving up to 95% of the 3D battlefield unobstructed.
   - Select panels are thoughtfully combined into unified multi-mode containers:
     - **Top Imperial Resource & Epoch Ribbon** (Collapsible): Combines Stock Resource counters (with click-to-track), Age Progression badge, National Pop/Commerce Cap, and Quick Automation Toggles.
     - **Left Combined Empire, Library & Diplomacy Dock** (Collapsible): Unifies Library Tech Tree (Military, Civic, Commerce, Science tracks), Empire Modifiers/Wonders, and Diplomacy/Tribute into a single tabbed slide-out drawer.
     - **Bottom Combined Selection Inspector & Production Command Deck** (Collapsible): Merges unit/building stat inspection, multi-unit formation grid, building construction catalog, and training queue into one ergonomic bottom dock.
     - **Right Combined Tactical Radar, Automation & Engine Drawer** (Collapsible): Combines the interactive Minimap, Citizen/Military Automation switches, Graphics/LOD sliders, Live Engine Console (with **Copy Console Logs** button), and quick **Fullscreen Toggle**.
5. **Citizen & Military Automation Suite**:
   - **Citizen Automation Toggles**: *Auto-Assign Idle Citizens* (routes idle workers to the lowest-ratio resource or nearest construction site), *Auto-Queue Citizens at Cities*, *Auto-Rebuild Destroyed Farms/Mines*, and *Auto-Establish Caravans*.
   - **Military Automation Toggles**: *Auto-Military Production* (continuous balanced army recruitment), *Auto-Stance Policy* (Aggressive, Defensive, Hold Ground), *Auto-Scout Fog of War*, and *Auto-Retreat Wounded Units* back inside friendly national borders to heal and avoid enemy attrition.
6. **Dedicated Spectator Mode**:
   - Accessible directly from the Matchmaking Hub or mid-match pause menu.
   - Unlocks **Fog of War Toggle** (All Visible vs. Specific Nation POV), **Nation Perspective Switcher**, **Playback Speed Controls (0.25x, 0.5x, 1x, 2x, 4x)**, **AI vs. AI Simulation Mode**, and a collapsible **Live Telemetry Graph Overlay** comparing resource income, military power, territory percentage, and technology progress across all nations.
7. **Mod Manager, Modpack Studio & Complete API Export Tool**:
   - **Visual Hook & Rule Editor**: Create and edit individual mods or multi-mod **Modpacks** with load-order priority, conflict alerts, and live sliders/fields for all core hooks (`onCalculateGatherRate`, `onUnitSpawn`, `onCombatDamage`, `onBorderRadiusCalculate`, `onTechCostEvaluate`, `onWonderEffectApply`, `onAssetMeshOverride`).
   - **Import / Export Pipeline**: Export any Mod or combined Modpack as a portable JSON bundle (to clipboard, file download, or share sheet) and import external JSON modpacks with schema validation.
   - **Complete API Export Tool**: One-click generator that exports the complete **Crown & Dominion Engine Hook & Scripting API Reference** (all hook endpoints, data structures, nation/unit/building/tech IDs, default balance tables, and current active mod overrides) as a comprehensive JSON specification or Markdown developer guide.

### Visual Identity & Theme

- **Aesthetic Direction**: **Classic Imperial Parchment & Bronze Strategy Frame**—evoking grand historical atlases, illuminated military treatises, and forged bronze astrolabes while maintaining crisp readability and zero visual clutter.
- **Color Palette & Mood**:
  - `--background` (Deep Obsidian Slate Canvas): `#14110E`
  - `--surface-parchment` (Warm Cartographic Vellum): `#F3E8D2` (Light cards) / `#241E18` (Dark Tactical Parchment HUD containers with 88% alpha)
  - `--primary` (Imperial Burnished Gold): `#D4AF37`
  - `--secondary` (Forged Corinthian Bronze): `#8C6239`
  - `--accent-crimson` (Imperial Seal & Combat Alert): `#A62626`
  - `--accent-emerald` (Fertile Territory & Gather Highlight): `#2E8B57`
  - `--accent-azure` (Knowledge & Scientific Aura): `#2B6CB0`
  - `--text-primary` (Illuminated Ivory): `#FBF6EE`
  - `--text-secondary` (Aged Brass Muted): `#C4B293`
- **Typography & Hierarchy**:
  - **Display & Epoch Headers**: *Cinzel* (bundled locally via `font-util`) for authoritative classical titles, Age banners, and Wonder announcements.
  - **UI Controls & Tactical Labels**: *Plus Jakarta Sans* for high-legibility unit names, tooltips, and collapsible dock tabs at compact mobile sizes.
  - **Resource Tickers, Console Logs & API Schemas**: *JetBrains Mono* for tabular resource counters (`+42/m`), LOD/Pool telemetry metrics, console logs, and JSON mod editors.
- **Interactive Feedback & Motion**:
  - Smooth spring transitions (`spring(dampingRatio = 0.8f, stiffness = 400f)`) when collapsing or expanding any HUD container.
  - Pulsing golden targeting reticles and animated 3D beacon shafts when clicking a stock resource to track active gathering locations on the map.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Custom 3D OpenGL ES / Hardware-Accelerated Isometric-Perspective Engine with 4-Tier LOD & Pre-Allocated Object Pools**
  - *Chosen Approach*: Build a dedicated 3D projection, frustum-culled terrain chunk renderer, and procedural 3D mesh/impostor pipeline featuring **4 configurable LOD tiers** (`LOD0` High-Detail Multi-Part 3D Mesh, `LOD1` Simplified Low-Poly Mesh, `LOD2` Shaded Billboard/Octahedral Proxy, `LOD3` Strategic Cartographic Emblem) and deterministic **Object Pools** for Units, Projectiles, Particle Emitters, Selection Rings, and Floating Combat Text.
  - *Why*: Guarantees butter-smooth 60 FPS performance during massive 8-nation late-game battles while giving players real-time sliders to tune LOD switch distances (`15m–250m`) and inspect live pool reuse vs. allocation counts.
  - *Alternatives Considered*: Unpooled per-frame object instantiation or fixed-zoom 2D sprites, which cause garbage-collection stutter and lack 3D camera freedom.

- **Decision 2: Combined & Fully Collapsible HUD Architecture**
  - *Chosen Approach*: Consolidate secondary panels into 4 edge-hugging docks where **every single UI container** features an independent collapse/expand toggle (`isCollapsed`), plus a master "Collapse All / Minimal HUD" shortcut and a **Fullscreen Toggle** (`WindowInsetsControllerCompat` immersive sticky mode).
  - *Why*: Mobile screens cannot afford six separate floating windows. Combining related workflows (e.g., Library Research + Empire Modifiers, Selection Info + Production Queue) and allowing instant collapse maximizes tactical visibility.

- **Decision 3: Deep Event-Driven Mod & Hook Pipeline + Complete API Exporter**
  - *Chosen Approach*: Route all simulation calculations (resource gathering rates, unit max HP/attack/range/speed, national border push radius, attrition tick damage, tech research costs, AI build weights, and 3D unit color/scale/mesh overrides) through a central `ModHookRegistry`.
  - *Why*: Ensures mods and modpacks can genuinely alter any core game mechanic in real time—either via imported JSON modpacks or the in-app Visual Rule Editor—without recompiling the APK.

- **Decision 4: Hybrid Offline + Online Matchmaking with Dual Persistence**
  - *Chosen Approach*: Support immediate offline skirmishes and local Modpack/Save persistence via **Room Database**, while enabling **Online Cloud Matchmaking Lobbies** and shared Modpack publishing via **Firebase Firestore** authenticated with **Google Sign-In (`CredentialManager`)**.
  - *Why*: Respects the user's choice for *both* offline and online matchmaking—players can jump straight into offline AI battles anytime without a network connection, or sign in with Google to host/join live cloud lobbies.

---

## 4. Technical Architecture & Data Strategy

### Architecture & Component Diagram

```
┌────────────────────────────────────────────────────────────────────────────────────┐
│                       CROWN & DOMINION: 3D RTS APPLICATION                         │
├────────────────────────────────────────────────────────────────────────────────────┤
│  PRESENTATION LAYER (Jetpack Compose + Imperial Parchment & Bronze Theme)          │
│  ┌──────────────────────┐  ┌──────────────────────────┐  ┌──────────────────────┐  │
│  │ Command Citadel Hub  │  │ 3D Battlefield Viewport  │  │ Mod & API Studio     │  │
│  │ • Offline Skirmish   │  │ • Multi-Touch Camera     │  │ • Modpack Manager    │  │
│  │ • Online Lobbies     │  │ • 4-Tier LOD Renderer    │  │ • Visual Hook Editor │  │
│  │ • Spectator Setup    │  │ • Resource Map-Tracker   │  │ • Complete API Export│  │
│  └──────────┬───────────┘  └────────────┬─────────────┘  └──────────┬───────────┘  │
│             │                           │                           │              │
│  ┌──────────▼───────────────────────────▼───────────────────────────▼───────────┐  │
│  │ COLLAPSIBLE & COMBINED HUD CONTAINERS (Every Container Has Collapse Toggle)  │  │
│  │ [Top Resource & Age Bar] [Left Empire/Tech Dock] [Bottom Command Deck]       │  │
│  │ [Right Radar, Automation, LOD Graphics, Console Log Copy & Fullscreen Dock]  │  │
│  └──────────────────────────────────────┬───────────────────────────────────────┘  │
├─────────────────────────────────────────┼──────────────────────────────────────────┤
│  SIMULATION, AUTOMATION & HOOK ENGINE   │                                          │
│  ┌─────────────────────────┐  ┌─────────▼────────────┐  ┌───────────────────────┐  │
│  │ Deterministic ECS Loop  │  │ ModHookRegistry      │  │ ObjectPoolManager     │  │
│  │ • 8 Ages & 8 Nations    │◄─┤ • Rule Interceptors  │  │ • UnitEntityPool      │  │
│  │ • Borders & Attrition   │  │ • Stat & Tech Hooks  │  │ • ProjectilePool      │  │
│  │ • Citizen/Mil Auto AI   │  │ • Asset/Mesh Hooks   │  │ • Particle & TextPool │  │
│  └────────────┬────────────┘  └─────────┬────────────┘  └───────────────────────┘  │
├───────────────┼─────────────────────────┼──────────────────────────────────────────┤
│  DATA & PERSISTENCE LAYER               │                                          │
│  ┌────────────▼─────────────────────────▼────────────┐  ┌───────────────────────┐  │
│  │ Local Persistence (Room Database + JSON I/O)      │  │ Cloud Sync (Firestore)│  │
│  │ • Saved Mods, Modpacks, Custom Rules & Skirmishes │  │ • Online Lobbies      │  │
│  │ • Engine Console Log RingBuffer & API Spec Builder│  │ • Google Sign-In Auth │  │
│  └───────────────────────────────────────────────────┘  └───────────────────────┘  │
└────────────────────────────────────────────────────────────────────────────────────┘
```

### Data Model & Core Entities

1. **World & Simulation State (`GameWorldState`)**:
   - **6 Core Resources**: `FOOD`, `TIMBER`, `WEALTH`, `METAL`, `OIL`, `KNOWLEDGE` (each storing current stock, commerce cap, per-minute gather rate, active gatherer count, and list of active harvesting node IDs for instant map-tracking).
   - **8 Historical Ages**: `ANCIENT`, `CLASSICAL`, `MEDIEVAL`, `GUNPOWDER`, `ENLIGHTENMENT`, `INDUSTRIAL`, `MODERN`, `INFORMATION`.
   - **4 Library Research Tracks**: `MILITARY`, `CIVIC`, `COMMERCE`, `SCIENCE` (Levels 1–8 per track, unlocking units, border push bonuses, commerce caps, and Age transitions).
   - **National Borders & Territory Grid**: Dynamic Voronoi/distance-field influence grid computed from Cities, Forts, and Civic level, applying real-time supply attrition to unallied enemy units inside national borders unless supplied by a Supply Wagon.
2. **3D LOD & Object Pool Entities**:
   - `PooledUnitEntity`, `PooledProjectile`, `PooledParticle`, `PooledCombatText`: Pre-allocated arrays with `acquire()` and `release()` lifecycle hooks, zero runtime allocation during combat, and real-time pool utilization metrics.
   - `GraphicsConfig`: User-adjustable distances (`lod0MaxDistance`, `lod1MaxDistance`, `lod2MaxDistance`), terrain mesh subdivision, shadow toggle, particle limit, and resolution scale.
3. **Mod, Modpack & Deep Hook Specification (`ModDefinition`, `ModPackBundle`)**:
   - Supports atomic rule overrides (`gatherRateMultiplier`, `borderRadiusBase`, `attritionDamagePerSec`, `popCapLimit`, `wonderBuildTimeMultiplier`), entity stat patches (any unit, building, or wonder), custom unit archetypes, custom tech costs, and 3D procedural mesh/color overrides.
4. **Online & Offline Matchmaking Entities**:
   - `MatchLobby` (Firestore document & local skirmish config): Host UID, display name, map seed, biome (`Mediterranean`, `Continental`, `Arid Mesa`, `Archipelago`, `Highland Passes`), starting/ending Age, game speed, active Modpack checksum, player slots, and match status.

### Interactive Component & State Mapping

- **Stock Resource Click -> Map-Tracking Handler**:
  - Clicking any resource pill (`Food`, `Timber`, `Wealth`, `Metal`, `Oil`, `Knowledge`) invokes `viewModel.trackResourceLocations(resourceType)`.
  - Filters all active player harvesting structures and gatherers for that resource, sets `activeTrackedResource = resourceType`, spawns 3D golden locator beams on those coordinates, highlights their minimap icons, opens a compact collapsible **Resource Tracking Banner** showing total sites and workers, and smoothly interpolates the 3D camera target to each site in sequence on subsequent taps.
- **Hide / Collapse Container Toggles**:
  - Managed via `HudVisibilityState` storing independent booleans for `topResourceRibbonExpanded`, `leftEmpireDockExpanded`, `bottomCommandDeckExpanded`, `rightTacticalDrawerExpanded`, `resourceTrackerBannerExpanded`, and `spectatorGraphExpanded`, plus a master toggle to collapse/restore all panels at once.
- **Fullscreen Mode Toggle**:
  - Uses `WindowCompat.getInsetsController(window, view)` to dynamically hide/show `WindowInsetsCompat.Type.systemBars()` with `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`, synchronized with `isFullscreen` state in the HUD.
- **Copy Console Logs & Complete API Export**:
  - `EngineLogger` captures structured timestamped events (`[SIM]`, `[LOD]`, `[POOL]`, `[HOOK]`, `[AI]`, `[NET]`). Clicking **Copy Console Logs** copies the formatted log buffer via `ClipboardManager` and displays a confirmation toast/snackbar.
  - Clicking **Export Complete API** synthesizes the full engine documentation, hook signatures, JSON schemas, and live entity registries into a downloadable/shareable JSON & Markdown package and copies it to clipboard on demand.
