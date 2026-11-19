# Aegis Showcase (Fabric 1.21.11, Yarn, Java 21)

Client-side showcase mod: animated dark ClickGUI plus AimAssist, TriggerBot, Hitbox and ShieldBreaker.
Intended for private/local worlds. Do not use on servers whose rules prohibit it.

## Build / run
```
gradle wrapper --gradle-version 9.2.0   # once, to generate gradlew + wrapper jar (or open in IntelliJ)
./gradlew runClient
./gradlew build                         # jar in build/libs
```
Versions: Minecraft 1.21.11, Yarn `1.21.11+build.3`, Loader 0.18.4, Fabric API `0.141.3+1.21.11`, Loom `1.14-SNAPSHOT`.

Open the GUI with **RIGHT SHIFT** in a world. Right-click a card (or click `>`) to expand settings.
Slider: drag, double-click or right-click to reset. Keybind: click, press a key; ESC/DELETE unbinds.

## Layout
```
client/   AegisClient (entrypoint) + mixin/{MinecraftClientMixin, EntityMixin}
module/   Module, ModuleManager, Category, combat/ render/ player/ movement/
setting/  Setting, SliderSetting, ToggleSetting, ModeSetting
gui/      ClickGuiScreen, Theme
util/     RenderUtil, MathUtil, CombatUtil, Rand, Time, KeyNames
```

## Design notes / deviations from the spec
- **Per-frame hook**: `MinecraftClientMixin` injects at the head of `MinecraftClient.render`, giving modules a frame
  delta. AimAssist rotates via `Entity#changeLookDirection` (what the mouse uses), so vanilla camera interpolation
  stays consistent at any FPS.
- **Scissor**: uses `DrawContext.enableScissor/disableScissor` (GUI-scale coordinates). `RenderSystem.enableScissor`
  no longer exists in the 1.21.6+ GUI renderer.
- **Input**: the GUI polls mouse buttons/keys from GLFW each frame (edge detected); only the mouse wheel overrides
  `Screen`. This avoids the `Click`/`KeyInput` event signatures introduced in 1.21.9. Keybinds are polled per frame too,
  so quick taps are never missed between ticks.
- **Hitbox**: implemented by adding to `Entity#getTargetingMargin` (what `ProjectileUtil.raycast` uses for the crosshair
  target) on the client thread only. This expands the box in all directions, not only horizontally.
- **Smart Air Crits**: "falling" is `velocity.y < 0` rather than `fallDistance > 0`, to avoid depending on that field's
  type/visibility across versions.
- **AimAssist FOV** is the full cone (half-angle = FOV/2). Aim point is the target's upper torso.
- **ShieldBreaker** waits at least one client tick after the hotbar swap so the server has the new slot before the hit.
  TriggerBot yields while ShieldBreaker is executing, so its shield pause never stalls a stun.
- `HudRenderCallback` is deprecated in newer Fabric API but still works; used for the Module List / Target HUD.

## If a build or launch error appears
This project was written without access to the 1.21.11 jars, so it has not been compiled. Likely spots, in order:
1. `./gradlew genSources` and check names used in: `DrawContext.getMatrices()` (expects `Matrix3x2fStack` with
   `pushMatrix/popMatrix/translate/scale`), `PlayerInventory.getSelectedSlot/setSelectedSlot`,
   `MinecraftClient.getWindow().getHandle()`, `Entity.changeLookDirection`.
2. Mixin targets (fail loudly at launch): `MinecraftClient.render(boolean)` and `Entity.getTargetingMargin()`.
