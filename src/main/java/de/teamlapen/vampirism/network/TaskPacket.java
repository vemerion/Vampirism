package de.teamlapen.vampirism.network;

import de.teamlapen.lib.network.IMessage;
import de.teamlapen.vampirism.VampirismMod;
import de.teamlapen.vampirism.player.TaskManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

public class TaskPacket implements IMessage {

    public static void encode(TaskPacket msg, PacketBuffer buffer) {
        buffer.writeVarInt(msg.containerId);
        buffer.writeVarInt(msg.completableTasks.size());
        buffer.writeVarInt(msg.completedRequirements.size());
        buffer.writeVarInt(msg.taskWrappers.size());
        msg.completableTasks.forEach((uuid, tasks) -> {
            buffer.writeUniqueId(uuid);
            buffer.writeVarInt(tasks.size());
            tasks.forEach(buffer::writeUniqueId);
        });
        msg.completedRequirements.forEach(((uuid, taskMapMap) -> {
            buffer.writeUniqueId(uuid);
            buffer.writeVarInt(taskMapMap.size());
            taskMapMap.forEach((task, data) -> {
                buffer.writeUniqueId(task);
                buffer.writeVarInt(data.size());
                data.forEach((loc, val) -> {
                    buffer.writeResourceLocation(loc);
                    buffer.writeVarInt(val);
                });
            });
        }));
        msg.taskWrappers.forEach((id, taskWrapper) -> taskWrapper.encode(buffer));
    }

    public static TaskPacket decode(PacketBuffer buffer) {
        int containerId = buffer.readVarInt();
        int completableSize = buffer.readVarInt();
        int statSize = buffer.readVarInt();
        int taskWrapperSIze = buffer.readVarInt();
        Map<UUID, Set<UUID>> completableTasks = new HashMap<>();
        for (int i = 0; i < completableSize; i++) {
            UUID uuid = buffer.readUniqueId();
            Set<UUID> task = new HashSet<>();
            int taskSize = buffer.readVarInt();
            for (int i1 = 0; i1 < taskSize; i1++) {
                task.add(buffer.readUniqueId());
            }
            completableTasks.put(uuid, task);
        }
        Map<UUID, Map<UUID, Map<ResourceLocation, Integer>>> completedRequirements = new HashMap<>();
        for (int i = 0; i < statSize; i++) {
            UUID uuid = buffer.readUniqueId();
            Map<UUID, Map<ResourceLocation, Integer>> taskRequirements = new HashMap<>();
            int taskRequirementSize = buffer.readVarInt();
            for (int i1 = 0; i1 < taskRequirementSize; i1++) {
                UUID task = buffer.readUniqueId();
                int requirementSize = buffer.readVarInt();
                Map<ResourceLocation, Integer> requirements = new HashMap<>();
                for (int i2 = 0; i2 < requirementSize; i2++) {
                    requirements.put(buffer.readResourceLocation(), buffer.readVarInt());
                }
                taskRequirements.put(task, requirements);
            }
            completedRequirements.put(uuid, taskRequirements);
        }
        Map<UUID, TaskManager.TaskWrapper> taskWrapper = new HashMap<>();
        for (int i = 0; i < taskWrapperSIze; i++) {
            TaskManager.TaskWrapper wrapper = TaskManager.TaskWrapper.decode(buffer);
            taskWrapper.put(wrapper.getId(), wrapper);
        }
        return new TaskPacket(containerId, taskWrapper, completableTasks, completedRequirements);
    }

    public static void handle(final TaskPacket msg, Supplier<NetworkEvent.Context> contextSupplier) {
        final NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> VampirismMod.proxy.handleTaskPacket(msg));
        ctx.setPacketHandled(true);
    }

    public final int containerId;
    public final Map<UUID, Map<UUID, Map<ResourceLocation, Integer>>> completedRequirements;
    public final Map<UUID, Set<UUID>> completableTasks;

    public final Map<UUID, TaskManager.TaskWrapper> taskWrappers;

    public TaskPacket(int containerId, Map<UUID, TaskManager.TaskWrapper> taskWrappers, Map<UUID, Set<UUID>> completableTasks, Map<UUID, Map<UUID, Map<ResourceLocation, Integer>>> completedRequirements) {
        this.containerId = containerId;
        this.taskWrappers = taskWrappers;
        this.completedRequirements = completedRequirements;
        this.completableTasks = completableTasks;
    }
}
