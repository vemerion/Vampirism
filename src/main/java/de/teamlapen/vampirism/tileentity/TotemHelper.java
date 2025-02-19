package de.teamlapen.vampirism.tileentity;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import de.teamlapen.lib.VampLib;
import de.teamlapen.lib.lib.util.UtilLib;
import de.teamlapen.vampirism.api.entity.factions.IFaction;
import de.teamlapen.vampirism.config.VampirismConfig;
import de.teamlapen.vampirism.world.FactionPointOfInterestType;
import de.teamlapen.vampirism.world.VampirismWorld;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.village.PointOfInterest;
import net.minecraft.village.PointOfInterestManager;
import net.minecraft.village.PointOfInterestType;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.server.ServerWorld;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TotemHelper {
    public static final int MIN_HOMES = 4;
    public static final int MIN_WORKSTATIONS = 2;
    public static final int MIN_VILLAGER = 4;

    private static final Logger LOGGER = LogManager.getLogger();


    /**
     * saves the position of a {@link PointOfInterest} to the related village totem position
     */
    private static final Map<RegistryKey<World>, Map<BlockPos, BlockPos>> totemPositions = Maps.newHashMap();

    /**
     * saves the {@link PointOfInterest}s for every village totem
     */
    private static final Map<RegistryKey<World>, Map<BlockPos, Set<PointOfInterest>>> poiSets = Maps.newHashMap();

    /**
     * TODO 1.17 remove
     * use {@link VampirismWorld} directly
     * adds a vampire village
     *
     * @param dimension dimension of the village totem
     * @param pos       position of the village totem
     * @param box       bounding box of the village
     */
    @Deprecated
    public static void addVampireVillage(RegistryKey<World> dimension, BlockPos pos, AxisAlignedBB box) {
        World w =VampLib.proxy.getWorldFromKey(dimension);
        if(w!=null){
            VampirismWorld.getOpt(w).ifPresent(vw -> vw.updateArtificialFogBoundingBox(pos,box));
        }
    }

    /**
     * TODO 1.17 remove
     * use {@link VampirismWorld} directly
     * removes a vampire village
     *
     * @param dimension dimension of the village totem
     * @param pos       position of the village totem
     */
    @Deprecated
    public static void removeVampireVillage(RegistryKey<World> dimension, BlockPos pos) {
        World w =VampLib.proxy.getWorldFromKey(dimension);
        if(w!=null){
            VampirismWorld.getOpt(w).ifPresent(vw -> vw.updateArtificialFogBoundingBox(pos,null));
        }
    }

    /**
     * TODO 1.17 remove
     * Use {@link VampirismWorld#isInsideArtificialVampireFogArea(BlockPos)}
     * checks if the position is in a vampire village
     *
     * @param dimension dimension of the pos
     * @param blockPos  pos to check
     * @return true if in a vampire controlled village otherwise false
     */
    @Deprecated
    public static boolean isInsideVampireAreaCached(RegistryKey<World> dimension, BlockPos blockPos) {
        World w =VampLib.proxy.getWorldFromKey(dimension);
        if(w!=null){
            return VampirismWorld.getOpt(w).map(vw -> vw.isInsideArtificialVampireFogArea(blockPos)).orElse(false);
        }
        return false;
    }

    /**
     * add a totem
     *
     * @param world world of the totem
     * @param pois points that may belong to the totem
     * @param totemPos position of the totem
     * @return false if no {@link PointOfInterest} belongs to the totem
     */
    public static boolean addTotem(ServerWorld world, Set<PointOfInterest> pois, BlockPos totemPos) {
        BlockPos conflict = null;
        Map<BlockPos, BlockPos> totemPositions = TotemHelper.totemPositions.computeIfAbsent(world.getDimensionKey(), key -> new HashMap<>());
        for (PointOfInterest poi : pois) {
            if (totemPositions.containsKey(poi.getPos()) && !totemPositions.get(poi.getPos()).equals(totemPos)) {
                conflict = totemPositions.get(poi.getPos());
                break;
            }
        }
        if (conflict != null) {
            handleTotemConflict(pois, world, totemPos, conflict);
        }
        if (pois.isEmpty()) {
            return false;
        }
        for (PointOfInterest pointOfInterest : pois) {
            totemPositions.put(pointOfInterest.getPos(), totemPos);
        }
        totemPositions.put(totemPos, totemPos);
        Map<BlockPos, Set<PointOfInterest>> poiSets = TotemHelper.poiSets.computeIfAbsent(world.getDimensionKey(), key -> new HashMap<>());
        if (poiSets.containsKey(totemPos)) {
            poiSets.get(totemPos).forEach(poi -> {
                if (!pois.contains(poi)) {
                    totemPositions.remove(poi.getPos());
                }
            });
        }
        poiSets.put(totemPos, pois);
        return !pois.isEmpty();
    }

    /**
     * removes {@link PointOfInterest} from the given set if another totem has more right to control them
     *
     * @param pois        {@link PointOfInterest} collection which is disputed
     * @param world       world of the totem
     * @param totem       position of the totem
     * @param conflicting position of the conflicting totem
     */
    private static void handleTotemConflict(Set<PointOfInterest> pois, ServerWorld world, BlockPos totem, BlockPos conflicting) {

        TotemTileEntity totem1 = ((TotemTileEntity) world.getTileEntity(totem));
        TotemTileEntity totem2 = ((TotemTileEntity) world.getTileEntity(conflicting));

        if (totem2 == null) {
            return;
        }

        boolean ignoreOtherTotem = true;

        //noinspection ConstantConditions
        if (totem1.getControllingFaction() != totem2.getControllingFaction()) { //both keep their pois
            ignoreOtherTotem = false;
        }

        if (totem1.getCapturingFaction() != null || totem2.getCapturingFaction() != null) { //both keep their pois
            ignoreOtherTotem = false;
        }

        StructureStart<?> structure1 = UtilLib.getStructureStartAt(world,totem,Structure.VILLAGE);
        StructureStart<?> structure2 = UtilLib.getStructureStartAt(world,conflicting,Structure.VILLAGE);

        if ((structure1== StructureStart.DUMMY || !structure1.isValid()) && (structure2 != StructureStart.DUMMY && structure2.isValid())) { //the first totem wins the POIs if located in natural village, other looses then
            ignoreOtherTotem = false;
        }

        if (totem2.getSize() >= totem1.getSize()) { //bigger village gets the pois, other looses them
            ignoreOtherTotem = false;
        }

        if (!ignoreOtherTotem) {
            pois.removeIf(poi -> !totem.equals(totemPositions.get(world.getDimensionKey()).get(poi.getPos())));
        }
    }

    /**
     * removes the poi references to the totem
     *
     * @param pois        the related {@link PointOfInterest}s
     * @param pos         the position of the totem
     * @param removeTotem if the totem poi should be removed too
     */
    public static void removeTotem(RegistryKey<World> dimension, Collection<PointOfInterest> pois, BlockPos pos, boolean removeTotem) { //TODO 1.17 change RegistryKey<World> dimension -> World world
        Map<BlockPos, BlockPos> totemPositions = TotemHelper.totemPositions.computeIfAbsent(dimension, key -> new HashMap<>());
        pois.forEach(pointOfInterest -> totemPositions.remove(pointOfInterest.getPos(), pos));
        if (removeTotem) {
            totemPositions.remove(pos);
        }
    }

    /**
     * @see #removeTotem(RegistryKey, Collection, BlockPos, boolean)
     */
    @Deprecated
    public static void removeTotem(Collection<PointOfInterest> pois, BlockPos pos, boolean removeTotem) { //TODO 1.17 remove
        removeTotem(World.OVERWORLD, pois, pos, removeTotem);
    }

    /**
     * @see #removeTotem(RegistryKey, Collection, BlockPos, boolean)
     */
    @Deprecated
    public static void removeTotem(Collection<PointOfInterest> pois, BlockPos pos) { //TODO 1.17 remove
        removeTotem(pois, pos, true);
    }

    /**
     * gets a totem position of a {@link PointOfInterest} if it exists
     *
     * @param pois collection of {@link PointOfInterest} to search for a totem position
     * @return the registered totem position or {@code null} if no totem exists
     */
    @Nonnull
    public static Optional<BlockPos> getTotemPosition(RegistryKey<World> dimension, Collection<PointOfInterest> pois) { //TODO 1.17 change RegistryKey<World> dimension -> World world
        Map<BlockPos, BlockPos> totemPositions = TotemHelper.totemPositions.computeIfAbsent(dimension, key -> new HashMap<>());
        for (PointOfInterest pointOfInterest : pois) {
            if (totemPositions.containsKey(pointOfInterest.getPos())) {
                return Optional.of(totemPositions.get(pointOfInterest.getPos()));
            }
        }
        return Optional.empty();
    }

    /**
     * @see #getTotemPosition(RegistryKey, Collection)
     */
    @Deprecated
    @Nonnull
    public static Optional<BlockPos> getTotemPosition(Collection<PointOfInterest> pois) { //TODO 1.17 remove
        return getTotemPosition(World.OVERWORLD, pois);
    }

    /**
     * gets the saved totem position for a related {@link PointOfInterest}
     *
     * @param pos position of the {@link PointOfInterest}
     * @return the blockpos of the totem or {@code null} if there is no registered totem position for the {@link PointOfInterest}
     */
    @Nullable
    public static BlockPos getTotemPosition(RegistryKey<World> world, BlockPos pos) { //TODO 1.17 change RegistryKey<World> dimension -> World world
        if (totemPositions.containsKey(world)) {
            return totemPositions.get(world).get(pos);
        }
        return null;
    }

    /**
     * @see #getTotemPosition(RegistryKey, BlockPos)
     */
    @Deprecated
    @Nullable
    public static BlockPos getTotemPosition(BlockPos pos) { //TODO 1.17 remove
        return getTotemPosition(World.OVERWORLD, pos);
    }

    @Nonnull
    public static Optional<BlockPos> getTotemPosNearPos(ServerWorld world, BlockPos pos) {
        Collection<PointOfInterest> points = world.getPointOfInterestManager().func_219146_b(p -> true, pos, 25, PointOfInterestManager.Status.ANY).collect(Collectors.toList());
        if (!points.isEmpty()) {
            return getTotemPosition(world.getDimensionKey(), points);
        }
        return Optional.empty();
    }

    @Nonnull
    public static Optional<TotemTileEntity> getTotemNearPos(ServerWorld world, BlockPos posSource, boolean mustBeLoaded) {
        Optional<BlockPos> posOpt = getTotemPosNearPos(world, posSource);
        if (mustBeLoaded) {
            posOpt = posOpt.filter(pos -> world.getChunkProvider().isChunkLoaded(new ChunkPos(pos)));
        }
        return posOpt.map(pos -> {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TotemTileEntity) {
                return ((TotemTileEntity) tile);
            } else {
                return null;
            }
        });
    }

    /**
     * forces a village totem to a specific faction
     *
     * @param faction the forced faction
     * @param player  the player that requests the faction
     * @return the feedback for the player
     */
    public static ITextComponent forceFactionCommand(IFaction<?> faction, ServerPlayerEntity player) {
        Map<BlockPos, BlockPos> totemPositions = TotemHelper.totemPositions.computeIfAbsent(player.getEntityWorld().getDimensionKey(), key -> new HashMap<>());
        List<PointOfInterest> pointOfInterests = ((ServerWorld) player.getEntityWorld()).getPointOfInterestManager().func_219146_b(point -> true, player.getPosition(), 25, PointOfInterestManager.Status.ANY).sorted(Comparator.comparingInt(point -> (int) (point.getPos()).distanceSq(player.getPosition()))).collect(Collectors.toList());
        if (pointOfInterests.stream().noneMatch(point -> totemPositions.containsKey(point.getPos()))) {
            return new TranslationTextComponent("command.vampirism.test.village.no_village");
        }
        TileEntity te = player.getEntityWorld().getTileEntity(totemPositions.get(pointOfInterests.get(0).getPos()));
        if (!(te instanceof TotemTileEntity)) {
            LOGGER.warn("TileEntity at {} is no TotemTileEntity", totemPositions.get(pointOfInterests.get(0).getPos()));
            return new StringTextComponent("");
        }
        TotemTileEntity tile = (TotemTileEntity) te;
        tile.setForcedFaction(faction);
        return new TranslationTextComponent("command.vampirism.test.village.success", faction == null ? "none" : faction.getName());
    }

    /**
     * gets all {@link PointOfInterest} points for a village totem to consider them as part of the village
     *
     * @param world world in which to search
     * @param pos   position of the village totem to start searching
     * @return a set of all related {@link PointOfInterest} points
     */
    public static Set<PointOfInterest> getVillagePointsOfInterest(ServerWorld world, BlockPos pos) {
        PointOfInterestManager manager = world.getPointOfInterestManager();
        Set<PointOfInterest> finished = Sets.newHashSet();
        Set<PointOfInterest> points = manager.func_219146_b(type -> !(type instanceof FactionPointOfInterestType), pos, 50, PointOfInterestManager.Status.ANY).collect(Collectors.toSet());
        while (!points.isEmpty()) {
            List<Stream<PointOfInterest>> list = points.stream().map(pointOfInterest -> manager.func_219146_b(type -> !(type instanceof FactionPointOfInterestType), pointOfInterest.getPos(), 40, PointOfInterestManager.Status.ANY)).collect(Collectors.toList());
            points.clear();
            list.forEach(stream -> stream.forEach(point -> {
                if (!finished.contains(point)) {
                    if (point.getPos().withinDistance(pos, VampirismConfig.BALANCE.viMaxTotemRadius.get())) {
                        points.add(point);
                    }
                }
                finished.add(point);
            }));
        }
        return finished;
    }

    /**
     * use {@link #isVillage(Set, ServerWorld, BlockPos, boolean)}
     * <p>
     *
     * flag & 1 != 0 :
     * <p>
     *   - enough homes
     * <p>
     * flag & 2 != 0 :
     * <p>
     *   - enough work stations
     * <p>
     * flag & 4 != 0 :
     * <p>
     *   - enough villager
     * <p>
     *
     * @param stats          the output of {@link #getVillageStats(Set, World)}
     * @param hasInteraction if the village is influenced by a faction
     *
     * @return flag which requirements are met
     */
    public static int isVillage(Map<Integer, Integer> stats, boolean hasInteraction) {
        int status = 0;
        if (stats.get(1) >= MIN_HOMES) {
            status += 1;
        }
        if (stats.get(2) >= MIN_WORKSTATIONS) {
            status += 2;
        }
        if (hasInteraction || stats.get(4) >= MIN_VILLAGER) {
            status += 4;
        }
        return status;
    }

    /**
     * checks if the given  {@link PointOfInterest} Set can be interpreted as village
     * <p>
     * <p>
     * flag & 1 != 0 :
     * <p>
     * - enough homes
     * <p>
     * flag & 2 != 0 :
     * <p>
     * - enough work stations
     * <p>
     * flag & 4 != 0 :
     * <p>
     * - enough villager
     * <p>
     *
     * @param pointOfInterests the output of {@link #getVillageStats(Set, World)}
     * @param world            the world of the point of interests
     * @param hasInteraction   if the village is influenced by a faction
     * @return flag which requirements are met
     */
    public static int isVillage(Set<PointOfInterest> pointOfInterests, ServerWorld world, BlockPos totemPos, boolean hasInteraction) {
        if (UtilLib.getStructureStartAt(world,totemPos, Structure.VILLAGE) != StructureStart.DUMMY) {
            return 7;
        }
        return isVillage(getVillageStats(pointOfInterests, world), hasInteraction);
    }

    /**
     * searches the given {@link PointOfInterest} set for village qualifying data
     *
     * @param pointOfInterests a {@link PointOfInterest} set to check for a village
     * @param world            world of the point of interests
     * @return map containing village related data
     */
    public static Map<Integer, Integer> getVillageStats(Set<PointOfInterest> pointOfInterests, World world) {
        Map<PointOfInterestType, Long> poiTCounts = pointOfInterests.stream().map(PointOfInterest::getType).collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        AxisAlignedBB area = getAABBAroundPOIs(pointOfInterests);
        return new HashMap<Integer, Integer>() {{
            put(1, poiTCounts.getOrDefault(PointOfInterestType.HOME, 0L).intValue());
            put(2, ((int) poiTCounts.entrySet().stream().filter(entry -> entry.getKey() != PointOfInterestType.HOME).mapToLong(Entry::getValue).sum()));
            put(4, area == null ? 0 : world.getEntitiesWithinAABB(VillagerEntity.class, area).size());
        }};
    }



    /**
     * creates a bounding box for the given {@link PointOfInterest}s
     *
     * @throws NoSuchElementException if poi is empty
     */
    @Nullable
    public static AxisAlignedBB getAABBAroundPOIs(@Nonnull Set<PointOfInterest> pois) {
        return pois.stream().map(poi -> new AxisAlignedBB(poi.getPos()).grow(25)).reduce(AxisAlignedBB::union).orElse(null);
    }

    public static void ringBell(World world, @Nonnull PlayerEntity player) {
        if (!world.isRemote) {
            Optional<TotemTileEntity> tile = getTotemNearPos(((ServerWorld) world), player.getPosition(), false);
            tile.ifPresent(s -> s.ringBell(player));
        }
    }
}
