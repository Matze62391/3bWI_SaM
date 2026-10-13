package appeng.pack.gametest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.me.helpers.IGridConnectedBlockEntity;
import appeng.api.orientation.BlockOrientation;
import appeng.api.util.AEColor;
import appeng.blockentity.misc.InscriberBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEParts;
import appeng.core.definitions.BlockDefinition;
import appeng.server.testworld.Plot;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rearth.oritech.block.blocks.pipes.GenericPipeBlock;
import rearth.oritech.util.MultiblockMachineController;
import team.reborn.energy.api.EnergyStorage;

/**
 * Builds a 31x8x23 lab with an AE2 network and an Oritech machine hall, exports it as a Litematica schematic and a
 * vanilla structure file ({@code <game dir>/lab/}), and checks that it works: the generators power the AE2 network
 * through Oritech energy pipes, every terminal, pattern provider and import bus has a channel, the Oritech multiblocks
 * assemble and the import buses reach the machines.
 */
public class PackLabGameTest implements FabricClientGameTest {
    private static final Logger LOG = LoggerFactory.getLogger(PackLabGameTest.class);

    static final int SIZE_X = 31, SIZE_Y = 8, SIZE_Z = 23;

    /** Oritech machines in the hall: controller position (x = 25), facing and whether a core sits above it. */
    record Machine(String id, int z, Direction facing, boolean tall) {
    }

    static final List<Machine> MACHINES = List.of(
            new Machine("oritech:pulverizer", 6, Direction.WEST, false),
            new Machine("oritech:powered_furnace", 8, Direction.WEST, true),
            new Machine("oritech:foundry", 11, Direction.WEST, true),
            new Machine("oritech:assembler", 14, Direction.WEST, true),
            new Machine("oritech:centrifuge", 16, Direction.WEST, true),
            new Machine("oritech:fragment_forge", 17, Direction.EAST, true));

    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().adjustSettings(settings -> {
            settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
            settings.getNormalPresetList().stream()
                    .filter(entry -> entry.preset() != null && entry.preset().is(WorldPresets.FLAT))
                    .findFirst().ifPresent(settings::setWorldType);
        }).create()) {
            var server = world.getServer();
            server.runCommand("gamerule advance_time false");
            server.runCommand("gamerule advance_weather false");
            server.runCommand("gamerule fall_damage false");
            server.runCommand("gamerule send_command_feedback false");
            server.runCommand("time set noon");
            context.waitTicks(20);

            var origin = server.computeOnServer(s -> s.getPlayerList().getPlayers().getFirst().blockPosition()
                    .offset(4, -1, 4));
            server.runOnServer(s -> buildLab(s, origin));
            context.waitTicks(40);

            var exportDir = FabricLoader.getInstance().getGameDir().resolve("lab");
            server.runOnServer(s -> export(s.overworld(), origin, exportDir));
            LOG.info("Lab exported to {}", exportDir);
            if (FabricLoader.getInstance().isModLoaded("litematica")) {
                String mismatch = compareWithLitematica(context, server, exportDir, origin);
                if (mismatch != null) {
                    throw new AssertionError("Litematica reads the schematic differently: " + mismatch);
                }
            }

            checkLab(context, server, origin);
            screenshots(context, server, origin);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Building

    static void buildLab(MinecraftServer server, BlockPos o) {
        var level = server.overworld();
        var player = server.getPlayerList().getPlayers().getFirst();

        // clear the area and lay the shell
        for (var pos : BlockPos.betweenClosed(o, o.offset(SIZE_X - 1, SIZE_Y + 2, SIZE_Z - 1))) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        for (int x = 0; x < SIZE_X; x++) {
            for (int z = 0; z < SIZE_Z; z++) {
                boolean aisle = x >= 14 && x <= 16 && z >= 6;
                boolean border = z == 5 && x > 0 && x < SIZE_X - 1;
                set(level, o, x, 0, z, aisle ? AEBlocks.SMOOTH_QUARTZ_BLOCK.block().defaultBlockState()
                        : border ? AEBlocks.SKY_STONE_SMALL_BRICK.block().defaultBlockState()
                        : AEBlocks.SMOOTH_SKY_STONE_BLOCK.block().defaultBlockState());

                boolean light = x % 4 == 3 && z % 4 == 3 && x < SIZE_X - 1 && z < SIZE_Z - 1;
                set(level, o, x, 7, z, light ? AEBlocks.QUARTZ_VIBRANT_GLASS.block().defaultBlockState()
                        : block("oritech:iron_plating"));
            }
        }
        for (int y = 1; y <= 6; y++) {
            for (int x = 0; x < SIZE_X; x++) {
                wall(level, o, x, y, 0, x % 6 == 0);
                wall(level, o, x, y, SIZE_Z - 1, x % 6 == 0);
            }
            for (int z = 1; z < SIZE_Z - 1; z++) {
                wall(level, o, 0, y, z, z % 6 == 0);
                wall(level, o, SIZE_X - 1, y, z, z % 6 == 0);
            }
        }
        // windows in the front wall, entrance in its middle
        for (int x = 1; x < SIZE_X - 1; x++) {
            for (int y = 2; y <= 4; y++) {
                if (x % 6 != 0 && (x < 13 || x > 17)) {
                    set(level, o, x, y, SIZE_Z - 1, AEBlocks.QUARTZ_GLASS.block().defaultBlockState());
                }
            }
        }
        for (int x = 14; x <= 16; x++) {
            for (int y = 1; y <= 3; y++) {
                set(level, o, x, y, SIZE_Z - 1, Blocks.AIR.defaultBlockState());
            }
        }
        // terminal desk, pillars between the autocrafting and processing columns
        for (int x = 12; x <= 18; x++) {
            set(level, o, x, 1, 5, AEBlocks.SMOOTH_QUARTZ_BLOCK.block().defaultBlockState());
        }
        for (int z : new int[] { 7, 9, 11, 14, 16, 18 }) {
            for (int y = 1; y <= 3; y++) {
                set(level, o, 2, y, z, AEBlocks.QUARTZ_PILLAR.block().defaultBlockState());
            }
        }

        buildNetwork().build(level, player, o);
        buildOritech(level, o);
    }

    private static void wall(ServerLevel level, BlockPos o, int x, int y, int z, boolean pillar) {
        set(level, o, x, y, z, block(pillar || y == 6 ? "oritech:carbon_plating" : "oritech:iron_plating"));
    }

    /** The AE2 network, built with AE2's own test plot builder. */
    static Plot buildNetwork() {
        var plot = new Plot(Identifier.fromNamespaceAndPath("ae2_fabric_pack", "lab"));

        // ME core: controller column, drives on both sides, energy acceptor and dense energy cells at the ends
        plot.block("15 [1,3] 2", AEBlocks.CONTROLLER);
        plot.blockEntity("14 [1,3] 2", AEBlocks.DRIVE, drive -> BlockOrientation.get(Direction.SOUTH).setOn(drive));
        plot.blockEntity("16 [1,3] 2", AEBlocks.DRIVE, drive -> BlockOrientation.get(Direction.SOUTH).setOn(drive));
        plot.block("13 1 2", AEBlocks.ENERGY_ACCEPTOR);
        plot.block("13 [2,3] 2", AEBlocks.DENSE_ENERGY_CELL);
        plot.block("17 [1,3] 2", AEBlocks.DENSE_ENERGY_CELL);

        // terminal console in front of the controller
        plot.cable("15 2 [3,4]", AEParts.SMART_CABLE);
        plot.cable("12 2 5", AEParts.SMART_CABLE);
        plot.cable("13 2 5", AEParts.SMART_CABLE).part(Direction.SOUTH, AEParts.TERMINAL);
        plot.cable("14 2 5", AEParts.SMART_CABLE).part(Direction.SOUTH, AEParts.PATTERN_ENCODING_TERMINAL);
        plot.cable("15 2 5", AEParts.SMART_CABLE).part(Direction.SOUTH, AEParts.CRAFTING_TERMINAL);
        plot.cable("16 2 5", AEParts.SMART_CABLE).part(Direction.SOUTH, AEParts.PATTERN_ACCESS_TERMINAL);
        plot.cable("17 2 5", AEParts.SMART_CABLE).part(Direction.SOUTH, AEParts.STORAGE_MONITOR);
        plot.cable("18 2 5", AEParts.SMART_CABLE);

        // two dense trunks along the ceiling, each from its own controller face (a face carries 32 channels):
        // from the back of the controller to the west wall, from the front to the machine hall
        plot.cable("15 [3,5] 1", AEParts.SMART_DENSE_CABLE);
        plot.cable("[4,14] 5 1", AEParts.SMART_DENSE_CABLE);
        plot.cable("4 [1,4] 1", AEParts.SMART_DENSE_CABLE);
        plot.cable("4 1 [2,4]", AEParts.SMART_DENSE_CABLE);
        plot.cable("3 1 [4,19]", AEParts.SMART_DENSE_CABLE);
        plot.cable("15 [3,4] 3", AEParts.SMART_DENSE_CABLE);
        plot.cable("[15,27] 5 3", AEParts.SMART_DENSE_CABLE);
        plot.cable("27 [1,4] 3", AEParts.SMART_DENSE_CABLE);

        // crafting CPU in the north-west corner: accelerators below, 64k storage above, monitor facing the room
        plot.block("[1,3] 1 [1,3]", AEBlocks.CRAFTING_ACCELERATOR);
        plot.block("[1,2] 2 [1,3]", AEBlocks.CRAFTING_STORAGE_64K);
        plot.block("3 2 1", AEBlocks.CRAFTING_STORAGE_64K);
        plot.block("3 2 3", AEBlocks.CRAFTING_STORAGE_64K);
        plot.blockEntity("3 2 2", AEBlocks.CRAFTING_MONITOR, monitor -> BlockOrientation.get(Direction.EAST).setOn(monitor));

        // autocrafting wall: pattern providers with molecular assemblers behind them
        for (int z : new int[] { 6, 8, 10 }) {
            plot.block("2 [1,3] " + z, AEBlocks.PATTERN_PROVIDER);
            plot.block("1 [1,3] " + z, AEBlocks.MOLECULAR_ASSEMBLER);
        }
        // processing: inscribers that export their results back into their pattern provider
        for (int z : new int[] { 13, 15, 17, 19 }) {
            plot.block("2 1 " + z, AEBlocks.PATTERN_PROVIDER);
            plot.blockEntity("1 1 " + z, AEBlocks.INSCRIBER, inscriber -> {
                BlockOrientation.get(Direction.EAST).setOn(inscriber);
                ((InscriberBlockEntity) inscriber).getConfigManager().putSetting(Settings.AUTO_EXPORT, YesNo.YES);
            });
        }

        // machine hall: orange line with pattern providers, light blue line with import buses (two lines, because a
        // normal cable carries 8 channels)
        for (int z = 3; z <= 17; z++) {
            final int mz = z;
            var machine = MACHINES.stream().filter(m -> m.z() == mz).findFirst();
            var lower = plot.cable("26 1 " + z, AEParts.SMART_CABLE, AEColor.ORANGE);
            var upper = plot.cable("26 2 " + z, AEParts.SMART_CABLE, AEColor.LIGHT_BLUE);
            if (machine.isPresent()) {
                lower.part(Direction.WEST, AEParts.PATTERN_PROVIDER);
                if (machine.get().tall()) {
                    upper.part(Direction.WEST, AEParts.IMPORT_BUS);
                }
            }
        }
        // the pulverizer is one block high: its import bus sits on top of it
        plot.cable("25 2 6", AEParts.SMART_CABLE, AEColor.LIGHT_BLUE).part(Direction.DOWN, AEParts.IMPORT_BUS);
        return plot;
    }

    /** Oritech: machines with their cores (not assembled), coal generators and energy pipes in the floor. */
    static void buildOritech(ServerLevel level, BlockPos o) {
        for (var machine : MACHINES) {
            var pos = o.offset(25, 1, machine.z());
            level.setBlock(pos, block(machine.id()).setValue(BlockStateProperties.HORIZONTAL_FACING, machine.facing()),
                    Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof MultiblockMachineController controller) {
                for (var core = controller.getNextMissingCore(); core != null; core = controller.getNextMissingCore()) {
                    level.setBlock(core, block("oritech:machine_core_1"), Block.UPDATE_ALL);
                }
            }
        }
        for (int x = 19; x <= 22; x++) {
            set(level, o, x, 1, 1, block("oritech:basic_generator").setValue(BlockStateProperties.HORIZONTAL_FACING,
                    Direction.SOUTH));
        }
        // energy pipes in the floor: generators -> AE2 energy acceptor and along the machine row
        var pipes = new ArrayList<BlockPos>();
        pipes.add(new BlockPos(13, 0, 2));
        for (int x = 13; x <= 25; x++) {
            pipes.add(new BlockPos(x, 0, 1));
        }
        for (int z = 2; z <= 17; z++) {
            pipes.add(new BlockPos(25, 0, z));
        }
        var pipe = (GenericPipeBlock) BuiltInRegistries.BLOCK.getValue(Identifier.parse("oritech:energy_pipe"));
        for (var rel : pipes) {
            var pos = o.offset(rel);
            level.setBlock(pos, pipe.addConnectionStates(pipe.defaultBlockState(), level, pos, true), Block.UPDATE_ALL);
        }
    }

    private static void set(ServerLevel level, BlockPos o, int x, int y, int z, BlockState state) {
        level.setBlock(o.offset(x, y, z), state, Block.UPDATE_ALL);
    }

    private static BlockState block(String id) {
        return BuiltInRegistries.BLOCK.getOptional(Identifier.parse(id))
                .orElseThrow(() -> new AssertionError("Unknown block " + id)).defaultBlockState();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Export

    static void export(ServerLevel level, BlockPos origin, Path dir) {
        try {
            Files.createDirectories(dir);
            NbtIo.writeCompressed(litematic(level, origin), dir.resolve("ae2_oritech_lab.litematic"));

            var template = new StructureTemplate();
            template.fillFromWorld(level, origin, new BlockPos(SIZE_X, SIZE_Y, SIZE_Z), false, List.of());
            template.setAuthor("AE2 Fabric Pack");
            NbtIo.writeCompressed(template.save(new CompoundTag()), dir.resolve("ae2_oritech_lab.nbt"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** Litematica's format (version 7): one region, a block state palette and bit-packed block states. */
    static CompoundTag litematic(ServerLevel level, BlockPos origin) {
        var palette = new LinkedHashMap<BlockState, Integer>();
        palette.put(Blocks.AIR.defaultBlockState(), 0);
        int volume = SIZE_X * SIZE_Y * SIZE_Z;
        var indices = new int[volume];
        var blockEntities = new ListTag();
        int nonAir = 0;
        for (int y = 0; y < SIZE_Y; y++) {
            for (int z = 0; z < SIZE_Z; z++) {
                for (int x = 0; x < SIZE_X; x++) {
                    var pos = origin.offset(x, y, z);
                    var state = level.getBlockState(pos);
                    if (!state.isAir()) {
                        nonAir++;
                    }
                    indices[(y * SIZE_Z + z) * SIZE_X + x] = palette.computeIfAbsent(state, s -> palette.size());
                    var be = level.getBlockEntity(pos);
                    if (be != null) {
                        var tag = be.saveWithFullMetadata(level.registryAccess());
                        tag.putInt("x", x);
                        tag.putInt("y", y);
                        tag.putInt("z", z);
                        blockEntities.add(tag);
                    }
                }
            }
        }

        int bits = Math.max(2, 32 - Integer.numberOfLeadingZeros(palette.size() - 1));
        long mask = (1L << bits) - 1;
        var packed = new long[(int) ((volume * (long) bits + 63) / 64)];
        for (int i = 0; i < volume; i++) {
            long start = (long) i * bits;
            int startIndex = (int) (start >> 6);
            int endIndex = (int) (((long) (i + 1) * bits - 1) >> 6);
            int startBit = (int) (start & 63);
            long value = indices[i] & mask;
            packed[startIndex] = packed[startIndex] & ~(mask << startBit) | value << startBit;
            if (startIndex != endIndex) {
                int endBits = 64 - startBit;
                int remaining = bits - endBits;
                packed[endIndex] = packed[endIndex] >>> remaining << remaining | value >> endBits;
            }
        }

        var paletteTag = new ListTag();
        for (var state : palette.keySet()) {
            paletteTag.add(NbtUtils.writeBlockState(state));
        }
        var region = new CompoundTag();
        region.put("Position", vec(0, 0, 0));
        region.put("Size", vec(SIZE_X, SIZE_Y, SIZE_Z));
        region.put("BlockStatePalette", paletteTag);
        region.put("BlockStates", new LongArrayTag(packed));
        region.put("TileEntities", blockEntities);
        region.put("Entities", new ListTag());
        region.put("PendingBlockTicks", new ListTag());
        region.put("PendingFluidTicks", new ListTag());
        var regions = new CompoundTag();
        regions.put("Lab", region);

        long now = System.currentTimeMillis();
        var metadata = new CompoundTag();
        metadata.putString("Name", "AE2 + Oritech Lab");
        metadata.putString("Author", "AE2 Fabric Pack");
        metadata.putString("Description", "ME core, autocrafting, inscribers and an Oritech machine hall");
        metadata.putInt("RegionCount", 1);
        metadata.putLong("TotalVolume", volume);
        metadata.putLong("TotalBlocks", nonAir);
        metadata.putLong("TimeCreated", now);
        metadata.putLong("TimeModified", now);
        metadata.put("EnclosingSize", vec(SIZE_X, SIZE_Y, SIZE_Z));

        var root = new CompoundTag();
        root.putInt("MinecraftDataVersion", SharedConstants.getCurrentVersion().dataVersion().version());
        root.putInt("Version", 7);
        root.putInt("SubVersion", 1);
        root.put("Metadata", metadata);
        root.put("Regions", regions);
        LOG.info("Litematic: {} block states in the palette, {} blocks, {} block entities", palette.size(), nonAir,
                blockEntities.size());
        return root;
    }

    private static CompoundTag vec(int x, int y, int z) {
        var tag = new CompoundTag();
        tag.putInt("x", x);
        tag.putInt("y", y);
        tag.putInt("z", z);
        return tag;
    }

    /** Loads the schematic with Litematica itself and compares every block state with the built lab. */
    static String compareWithLitematica(ClientGameTestContext context, TestServerContext server, Path dir, BlockPos o) {
        Object loaded = context.computeOnClient(mc -> {
            try {
                var schematicClass = Class.forName("fi.dy.masa.litematica.schematic.LitematicaSchematic");
                var schematic = schematicClass.getMethod("createFromFile", Path.class, String.class)
                        .invoke(null, dir, "ae2_oritech_lab.litematic");
                if (schematic == null) {
                    return "not loaded";
                }
                var container = schematicClass.getMethod("getSubRegionContainer", String.class).invoke(schematic, "Lab");
                var get = container.getClass().getMethod("get", int.class, int.class, int.class);
                var blockEntities = (Map<?, ?>) schematicClass.getMethod("getBlockEntityMapForRegion", String.class)
                        .invoke(schematic, "Lab");
                LOG.info("Litematica loaded the schematic with {} block entities", blockEntities.size());
                var states = new BlockState[SIZE_X * SIZE_Y * SIZE_Z];
                for (int y = 0; y < SIZE_Y; y++) {
                    for (int z = 0; z < SIZE_Z; z++) {
                        for (int x = 0; x < SIZE_X; x++) {
                            states[(y * SIZE_Z + z) * SIZE_X + x] = (BlockState) get.invoke(container, x, y, z);
                        }
                    }
                }
                return states;
            } catch (ReflectiveOperationException e) {
                return e.toString();
            }
        });
        if (!(loaded instanceof BlockState[] states)) {
            return String.valueOf(loaded);
        }
        String mismatch = server.computeOnServer(s -> {
            var level = s.overworld();
            for (int y = 0; y < SIZE_Y; y++) {
                for (int z = 0; z < SIZE_Z; z++) {
                    for (int x = 0; x < SIZE_X; x++) {
                        var expected = level.getBlockState(o.offset(x, y, z));
                        var actual = states[(y * SIZE_Z + z) * SIZE_X + x];
                        if (!expected.equals(actual)) {
                            return x + "," + y + "," + z + ": " + actual + " instead of " + expected;
                        }
                    }
                }
            }
            return null;
        });
        LOG.info("Litematica's blocks are {}", mismatch == null ? "identical to the lab" : "different: " + mismatch);
        return mismatch;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Checks

    static void checkLab(ClientGameTestContext context, TestServerContext server, BlockPos o) {
        var failures = new ArrayList<String>();

        // assemble the Oritech multiblocks (players right-click the controllers)
        var notAssembled = server.computeOnServer(s -> {
            var level = s.overworld();
            var result = new ArrayList<String>();
            for (var machine : MACHINES) {
                var pos = o.offset(25, 1, machine.z());
                if (level.getBlockEntity(pos) instanceof MultiblockMachineController controller
                        && !controller.initMultiblock(level.getBlockState(pos))) {
                    result.add(machine.id());
                }
            }
            return result;
        });
        if (!notAssembled.isEmpty()) {
            failures.add("multiblocks not assembled: " + notAssembled);
        }

        // fuel the generators
        long coal = server.computeOnServer(s -> {
            long inserted = 0;
            for (int x = 19; x <= 22; x++) {
                var storage = ItemStorage.SIDED.find(s.overworld(), o.offset(x, 1, 1), Direction.UP);
                if (storage == null) continue;
                try (var tx = Transaction.openOuter()) {
                    inserted += storage.insert(ItemVariant.of(Items.COAL_BLOCK), 16, tx);
                    tx.commit();
                }
            }
            return inserted;
        });
        LOG.info("Fed {} coal blocks into the generators", coal);
        if (coal == 0) {
            failures.add("generators accept coal");
        }

        // the AE2 network comes online from the generators and every device gets a channel
        String network = null;
        for (int i = 0; i < 60 && network == null; i++) {
            context.waitTicks(20);
            network = server.computeOnServer(s -> {
                var level = s.overworld();
                if (!(level.getBlockEntity(o.offset(15, 1, 2)) instanceof IGridConnectedBlockEntity controller)) {
                    return "no controller";
                }
                var grid = controller.getMainNode().getGrid();
                if (grid == null || !grid.getEnergyService().isNetworkPowered()) {
                    return null;
                }
                int nodes = 0, inactive = 0;
                var inactiveNames = new ArrayList<String>();
                for (var node : grid.getNodes()) {
                    nodes++;
                    if (!node.isActive()) {
                        inactive++;
                        var owner = node.getOwner();
                        inactiveNames.add(owner instanceof net.minecraft.world.level.block.entity.BlockEntity be
                                ? owner.getClass().getSimpleName() + "@" + be.getBlockPos().subtract(o).toShortString()
                                : owner.getClass().getSimpleName());
                    }
                }
                return "nodes=" + nodes + " inactive=" + inactive + " " + inactiveNames + " stored="
                        + (long) grid.getEnergyService().getStoredPower();
            });
        }
        LOG.info("AE2 network: {}", network);
        if (network == null || !network.contains("inactive=0 ")) {
            failures.add("AE2 network powered with all devices active (" + network + ")");
        }

        // the import buses reach the machines, and the machines get energy through the pipes
        String machines = server.computeOnServer(s -> {
            var level = s.overworld();
            var sb = new StringBuilder();
            for (var machine : MACHINES) {
                var pos = o.offset(25, 1, machine.z());
                var output = machine.tall() ? ItemStorage.SIDED.find(level, pos.above(), Direction.EAST)
                        : ItemStorage.SIDED.find(level, pos, Direction.UP);
                var energy = EnergyStorage.SIDED.find(level, pos, Direction.DOWN);
                sb.append(machine.id()).append(" output=").append(output != null).append(" energy=")
                        .append(energy == null ? "none" : energy.getAmount()).append("; ");
            }
            return sb.toString();
        });
        LOG.info("Machines: {}", machines);
        if (machines.contains("output=false") || machines.contains("energy=none") || machines.contains("energy=0;")) {
            failures.add("machines reachable and powered (" + machines + ")");
        }

        if (!failures.isEmpty()) {
            throw new AssertionError("Lab checks failed: " + failures);
        }
        LOG.info("Lab checks passed");
    }

    // ------------------------------------------------------------------------------------------------------------
    // Screenshots

    static void screenshots(ClientGameTestContext context, TestServerContext server, BlockPos o) {
        server.runOnServer(s -> {
            var player = s.getPlayerList().getPlayers().getFirst();
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        });
        context.runOnClient(mc -> { if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle(); });
        shot(context, server, o, 15.5, 3, 34, 180, 8, "lab-front");
        shot(context, server, o, 15.5, 2.5, 20, 180, 12, "lab-me-core");
        shot(context, server, o, 3.5, 4.5, 20.5, -150, 22, "lab-overview");
        shot(context, server, o, 18.5, 2.5, 12.5, -90, 10, "lab-machine-hall");
        shot(context, server, o, 10.5, 2.5, 12.5, 90, 12, "lab-autocrafting");
        server.runOnServer(s -> {
            for (var pos : BlockPos.betweenClosed(o.offset(0, 7, 0), o.offset(SIZE_X - 1, 7, SIZE_Z - 1))) {
                s.overworld().setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        });
        shot(context, server, o, 15.5, 30, 38, 180, 50, "lab-aerial");
        context.runOnClient(mc -> { if (mc.gui.hud.isHidden()) mc.gui.hud.toggle(); });
    }

    private static void shot(ClientGameTestContext context, TestServerContext server, BlockPos o, double x, double y,
            double z, float yaw, float pitch, String name) {
        server.runCommand("tp @p %s %s %s %s %s".formatted(o.getX() + x, o.getY() + y, o.getZ() + z, yaw, pitch));
        context.waitTicks(40);
        context.takeScreenshot(name);
    }
}
