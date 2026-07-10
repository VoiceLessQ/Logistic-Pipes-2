# Porting to NeoForge 1.21.1

Working notes for moving Logistic Pipes 2 from NeoForge 1.20.1 to 1.21.1. Local
branch `port/1.21.1`, cut from `1.20.1`. Not pushed.

## Why this branch exists

The old `1.21.1` branch on the remote is a 42-commit Architectury scaffold that
never received the real port. It shares no history with `1.20.1` (zero common
commits). `1.20.1` is the actual codebase: 4134 commits, near complete. So the
port starts from there, not from the scaffold. The scaffold stays on the remote,
untouched.

## Decisions

- NeoForge only, single project, same shape as 1.20.1. Fabric can come later as
  its own phase on a working 1.21 build.
- Target Minecraft 1.21.1, NeoForge 21.1.x.
- ModDevGradle instead of NeoGradle 7. Kotlin 2.x. No SRG reobf: 1.20.2+ runs
  Mojmap at runtime, so the hand-rolled TinyRemapper step gets deleted.
- Local only for now. Nothing pushed, remote left alone.

## Check versions before touching the build

Pin these against live sources, do not trust memory:
- NeoForge 21.1.x patch, ModDevGradle version, Kotlin for Forge version for 1.21.1
- capability handles (EnergyStorage / ItemHandler / FluidHandler, BLOCK variants)
- FluidStack changes (moved to data components in 1.20.5+, not just a rename)

## Phases

- [ ] 0. Branch and toolchain. gradle.properties versions, NeoGradle to
      ModDevGradle, delete the reobf/TinyRemapper block in build.gradle, mods.toml
      to neoforge.mods.toml.
- [ ] 1. Namespace rename. `net.minecraftforge.*` to `net.neoforged.*`: 295
      imports across 118 Java and 10 Kotlin files. `@OnlyIn`/`Dist`: 110 sites.
      Mechanical, but not every subpackage maps 1:1.
- [ ] 2. Capabilities (the hard one). LazyOptional is gone. Remove the
      getCapability overrides on the pipe and the RF provider, write a
      RegisterCapabilitiesEvent handler (there is none today), rewrite PowerProxy
      and the ~10 consumer lookups. Wire the two half-finished block entities.
- [ ] 3. Networking bridge. SimpleChannel to RegisterPayloadsEvent /
      PayloadRegistrar, LPPacketPayload to CustomPacketPayload plus StreamCodec.
      Keep the index-based packet IDs and the copy/release framing exactly. The
      174 packet classes do not change.
- [ ] 4. Events and fluids. Event bus split, TickEvent reshape,
      MissingMappingsEvent has no equivalent (drop it, there is no 1.12 upgrade
      path anyway). Fluids package rename plus the FluidStack component changes.
- [ ] 5. Verify. compileJava and compileKotlin on JDK 21, runClient, cold-load
      rejoin test, dedicated server launch.

## Build

JDK 21 only (Kotlin 1.9 dies on newer JDKs; 2.x still wants a sane JDK here):

    JAVA_HOME=<jdk21> ./gradlew "-Dorg.gradle.java.home=<jdk21>" \
      -Pkotlin.compiler.execution.strategy=in-process --no-daemon \
      compileJava compileKotlin

## Gotchas found while mapping the surface

- The capability wiring does not actually exist yet on 1.20.1. Four javadocs and
  a comment in LPRegistries claim it is wired, but capabilities are exposed only
  through per-block-entity getCapability overrides on the pipe and the RF
  provider. The power junction and crafting table expose accessors
  (`getEnergyInterface`, `getInvWrapper`) that nothing currently reads. Phase 2
  is the moment to wire them for real.
- LPPacketPayload is already half converted. Its comments mention
  CustomPacketPayload and RegisterPayloadsEvent, but the code still uses the
  1.20.1 messageBuilder model. Phase 3 finishes what was started.
- Packet IDs are the packet's index in a list sorted by class name, with null
  gaps kept on purpose. Do not switch to a success counter: it would shift every
  later ID and desync client and server.
- One item capability outlier: `FluidIdentifier` reads FLUID_HANDLER_ITEM off an
  ItemStack, so it moves to the ItemCapability path, not the block path used
  everywhere else.
