package de.teamlapen.vampirism.inventory.container;

import de.teamlapen.lib.lib.inventory.InventoryContainer;
import de.teamlapen.vampirism.VampirismMod;
import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.api.entity.player.task.ITaskInstance;
import de.teamlapen.vampirism.api.entity.player.task.TaskRequirement;
import de.teamlapen.vampirism.api.items.IRefinementItem;
import de.teamlapen.vampirism.core.ModContainer;
import de.teamlapen.vampirism.entity.factions.FactionPlayerHandler;
import de.teamlapen.vampirism.network.TaskActionPacket;
import de.teamlapen.vampirism.player.TaskManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class VampirismContainer extends InventoryContainer implements TaskContainer {

    private static final SelectorInfo[] SELECTOR_INFOS = new SelectorInfo[]{
            new SelectorInfo(stack -> stack.getItem() instanceof IRefinementItem && ((IRefinementItem) stack.getItem()).getSlotType() == IRefinementItem.AccessorySlotType.AMULET, 58, 8),
            new SelectorInfo(stack -> stack.getItem() instanceof IRefinementItem && ((IRefinementItem) stack.getItem()).getSlotType() == IRefinementItem.AccessorySlotType.RING, 58, 26),
            new SelectorInfo(stack -> stack.getItem() instanceof IRefinementItem && ((IRefinementItem) stack.getItem()).getSlotType() == IRefinementItem.AccessorySlotType.OBI_BELT, 58, 44)};

    public Map<UUID, TaskManager.TaskWrapper> taskWrapper = new HashMap<>();
    public Map<UUID, Set<UUID>> completableTasks = new HashMap<>();
    public Map<UUID, Map<UUID, Map<ResourceLocation, Integer>>> completedRequirements = new HashMap<>();

    private final IFactionPlayer<?> factionPlayer;
    private final TextFormatting factionColor;

    private Runnable listener;

    private final NonNullList<ItemStack> refinementStacks = NonNullList.withSize(3, ItemStack.EMPTY);

    public VampirismContainer(int id, @Nonnull PlayerInventory playerInventory) {
        super(ModContainer.vampirism, id, playerInventory, IWorldPosCallable.DUMMY, new Inventory(3), RemovingSelectorSlot::new, SELECTOR_INFOS);
        this.factionPlayer = FactionPlayerHandler.get(playerInventory.player).getCurrentFactionPlayer().orElseThrow(() -> new IllegalStateException("Opening vampirism container without faction"));
        this.factionColor = factionPlayer.getFaction().getChatColor();
        this.addPlayerSlots(playerInventory, 37, 124);
        ItemStack[] sets = this.factionPlayer.getSkillHandler().createRefinementItems();
        for (int i = 0; i < sets.length; i++) {
            if (sets[i] != null) {
                this.refinementStacks.set(i, sets[i]);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void init(@Nonnull Map<UUID, TaskManager.TaskWrapper> taskWrapper, @Nonnull Map<UUID, Set<UUID>> completableTasks, @Nonnull Map<UUID, Map<UUID, Map<ResourceLocation, Integer>>> completedRequirements) {
        this.taskWrapper = taskWrapper;
        this.completedRequirements = completedRequirements;
        this.completableTasks = completableTasks;
        if (this.listener != null) {
            this.listener.run();
        }
    }

    public void setRefinement(int slot, @Nonnull ItemStack stack) {
        this.factionPlayer.getSkillHandler().equipRefinementItem(stack);
        this.refinementStacks.set(slot, stack);
    }

    public NonNullList<ItemStack> getRefinementStacks() {
        return refinementStacks;
    }

    public Collection<ITaskInstance> getTaskInfos() {
        return this.taskWrapper.values().stream().flatMap(t -> t.getTaskInstances().stream().filter(ITaskInstance::isAccepted)).collect(Collectors.toList());
    }

    @Override
    public void setReloadListener(@Nullable Runnable listener) {
        this.listener = listener;
    }


    @Override
    public boolean isTaskNotAccepted(@Nonnull ITaskInstance taskInfo) {
        return false;
    }

    @Override
    public boolean canCompleteTask(@Nonnull ITaskInstance taskInfo) {
        return this.completableTasks.containsKey(taskInfo.getTaskBoard()) && this.completableTasks.get(taskInfo.getTaskBoard()).contains(taskInfo.getId()) && this.factionPlayer.getRepresentingPlayer().world.getGameTime() < taskInfo.getTaskTimeStamp();
    }

    @Override
    public void pressButton(@Nonnull ITaskInstance taskInfo) {
        VampirismMod.dispatcher.sendToServer(new TaskActionPacket(taskInfo.getId(), taskInfo.getTaskBoard(), buttonAction(taskInfo)));
        this.taskWrapper.get(taskInfo.getTaskBoard()).removeTask(taskInfo, true);
        if (this.listener != null) {
            this.listener.run();
        }
    }

    @Override
    public TaskAction buttonAction(@Nonnull ITaskInstance taskInfo) {
        return taskInfo.isUnique() || this.factionPlayer.getRepresentingPlayer().world.getGameTime() < taskInfo.getTaskTimeStamp() ? TaskContainer.TaskAction.ABORT : TaskAction.REMOVE;
    }

    @Override
    public boolean isCompleted(@Nonnull ITaskInstance item) {
        return false;
    }

    @Override
    public TextFormatting getFactionColor() {
        return this.factionColor;
    }

    @Override
    public boolean areRequirementsCompleted(@Nonnull ITaskInstance taskInfo, @Nonnull TaskRequirement.Type type) {
        if (this.completedRequirements != null) {
            if (this.completedRequirements.containsKey(taskInfo.getTaskBoard()) && this.completedRequirements.get(taskInfo.getTaskBoard()).containsKey(taskInfo.getId())) {
                Map<ResourceLocation, Integer> data = this.completedRequirements.get(taskInfo.getTaskBoard()).get(taskInfo.getId());
                for (TaskRequirement.Requirement<?> requirement : taskInfo.getTask().getRequirement().requirements().get(type)) {
                    if (!data.containsKey(requirement.getId()) || data.get(requirement.getId()) < requirement.getAmount(this.factionPlayer)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public int getRequirementStatus(@Nonnull ITaskInstance taskInfo, @Nonnull TaskRequirement.Requirement<?> requirement) {
        assert this.completedRequirements != null;
        if (this.completedRequirements.containsKey(taskInfo.getTaskBoard())) {
            return this.completedRequirements.get(taskInfo.getTaskBoard()).get(taskInfo.getId()).get(requirement.getId());
        } else {
            return requirement.getAmount(this.factionPlayer);
        }
    }

    @Override
    public boolean isRequirementCompleted(@Nonnull ITaskInstance taskInfo, @Nonnull TaskRequirement.Requirement<?> requirement) {
        if (this.completedRequirements != null) {
            if (this.completedRequirements.containsKey(taskInfo.getTaskBoard()) && this.completedRequirements.get(taskInfo.getTaskBoard()).containsKey(taskInfo.getId())) {
                Map<ResourceLocation, Integer> data = this.completedRequirements.get(taskInfo.getTaskBoard()).get(taskInfo.getId());
                return data.containsKey(requirement.getId()) && data.get(requirement.getId()) >= requirement.getAmount(this.factionPlayer);
            }
        }
        return false;
    }

    private static class RemovingSelectorSlot extends SelectorSlot {

        public RemovingSelectorSlot(IInventory inventoryIn, int index, SelectorInfo info, Consumer<IInventory> refreshInvFunc, Function<Integer, Boolean> activeFunc) {
            super(inventoryIn, index, info, refreshInvFunc, activeFunc);
        }

        @Override
        public void putStack(@Nonnull ItemStack stack) {
            if (!stack.isEmpty()) {
                ((VampirismContainer) this.getContainer()).setRefinement(this.getSlotIndex(), stack);
            }
        }
    }

}
