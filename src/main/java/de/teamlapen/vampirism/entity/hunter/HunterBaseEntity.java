package de.teamlapen.vampirism.entity.hunter;

import de.teamlapen.vampirism.advancements.VampireActionTrigger;
import de.teamlapen.vampirism.api.entity.hunter.IHunterMob;
import de.teamlapen.vampirism.core.ModAdvancements;
import de.teamlapen.vampirism.core.ModEffects;
import de.teamlapen.vampirism.entity.VampirismEntity;
import de.teamlapen.vampirism.util.Helper;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Util;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Base class for all vampire hunter
 */
@SuppressWarnings("EntityConstructor")
public abstract class HunterBaseEntity extends VampirismEntity implements IHunterMob, INPC/*mainly for JourneyMap*/ {

    public static boolean spawnPredicateHunter(EntityType<? extends HunterBaseEntity> entityType, IWorld world, SpawnReason spawnReason, BlockPos blockPos, Random random) {
        return world.getDifficulty() != Difficulty.PEACEFUL && spawnPredicateCanSpawn(entityType, world, spawnReason, blockPos, random);
    }

    private final boolean countAsMonster;
    protected final int MOVE_TO_RESTRICT_PRIO = 3;

    public HunterBaseEntity(EntityType<? extends HunterBaseEntity> type, World world, boolean countAsMonster) {
        super(type, world);
        this.countAsMonster = countAsMonster;
    }

    @Override
    public LivingEntity getRepresentingEntity() {
        return this;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new SwimGoal(this));
    }

    @Override
    public EntityClassification getClassification(boolean forSpawnCount) {
        if (forSpawnCount && countAsMonster) {
            return EntityClassification.MONSTER;
        }
        return super.getClassification(forSpawnCount);
    }

    /**
     * Tries to cure sanguinare and if successful sends a message.
     *
     * @return If player was cured
     */
    protected boolean tryCureSanguinare(PlayerEntity entity) {
        if (!this.world.isRemote && entity.isPotionActive(ModEffects.sanguinare)) {
            entity.removePotionEffect(ModEffects.sanguinare);
            entity.sendMessage(new TranslationTextComponent("text.vampirism.hunter.cured_sanguinare"), Util.DUMMY_UUID);
            return true;
        }
        return false;
    }

    public void makeCampHunter(BlockPos pos) {
        super.setHome(new AxisAlignedBB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1).grow(10));
        this.setMoveTowardsRestriction(MOVE_TO_RESTRICT_PRIO, true);
    }

    @Override
    public void onDeath(DamageSource cause) {
        super.onDeath(cause);
        if (cause.getTrueSource() instanceof ServerPlayerEntity && Helper.isVampire(((PlayerEntity) cause.getTrueSource())) && this.getActivePotionEffect(ModEffects.freeze) != null) {
            ModAdvancements.TRIGGER_VAMPIRE_ACTION.trigger(((ServerPlayerEntity) cause.getTrueSource()), VampireActionTrigger.Action.KILL_FROZEN_HUNTER);
        }
    }
}
