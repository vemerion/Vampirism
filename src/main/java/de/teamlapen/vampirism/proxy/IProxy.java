package de.teamlapen.vampirism.proxy;

import de.teamlapen.lib.lib.util.IInitListener;
import de.teamlapen.vampirism.network.*;
import de.teamlapen.vampirism.player.skills.SkillTree;
import de.teamlapen.vampirism.tileentity.GarlicBeaconTileEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Proxy interface
 */
public interface IProxy extends IInitListener {

    default void displayNameSwordScreen(ItemStack stack) {
    }

    default void displayRevertBackScreen() {
    }

    default void displayGarlicBeaconScreen(GarlicBeaconTileEntity tile, ITextComponent title){
    }

    @Nullable
    PlayerEntity getClientPlayer();

    @Nullable
    Entity getMouseOverEntity();

    /**
     * Shows a DBNO state with the given death message if the passed player is the client player
     */
    default void showDBNOScreen(PlayerEntity player, @Nullable ITextComponent deathMessage){}

    default float getRenderPartialTick() {
        return 1F;
    }

    /**
     * Only call after client_load/server started
     *
     * @param client Request the client version. Server ignores this
     * @return The appropriate skill tree
     */
    SkillTree getSkillTree(boolean client);

    default void handleBloodValuePacket(BloodValuePacket msg) {
    }

    default void handleSkillTreePacket(SkillTreePacket msg) {
    }

    default void handleVampireBookPacket(OpenVampireBookPacket msg) {
    }

    default void handleAppearancePacket(PlayerEntity player, AppearancePacket msg) {
    }

    default void handlePlayEventPacket(PlayEventPacket msg) {
    }

    default void handleTaskActionPacket(TaskActionPacket msg, PlayerEntity playerEntity) {
    }

    default void handleTaskStatusPacket(TaskStatusPacket msg) {
    }

    default void handleTaskPacket(TaskPacket msg) {
    }

    void renderScreenFullColor(int ticksOn, int ticksOff, int color);

    /**
     * Handle client side only sleep things
     */
    void handleSleepClient(PlayerEntity player);

    default void handleRequestMinionSelect(RequestMinionSelectPacket.Action action, List<Pair<Integer, ITextComponent>> minions) {
    }

    default void resetSkillScreenCache() {
    }

    default void handleActionBindingPacket(ActionBindingPacket msg, PlayerEntity playerEntity) {
    }

    default void handleUpdateMultiBossInfoPacket(UpdateMultiBossInfoPacket msg){
    }

}
