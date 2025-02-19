package de.teamlapen.vampirism.network;

import de.teamlapen.lib.network.IMessage;
import de.teamlapen.vampirism.VampirismMod;
import de.teamlapen.vampirism.inventory.container.TaskContainer;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class TaskActionPacket implements IMessage {

    public final UUID task;
    public final UUID entityId;
    public final TaskContainer.TaskAction action;

    public TaskActionPacket(UUID task, UUID entityId, TaskContainer.TaskAction action) {
        this.task = task;
        this.entityId = entityId;
        this.action = action;
    }

    static void encode(TaskActionPacket msg, PacketBuffer buf) {
        buf.writeUniqueId(msg.task);
        buf.writeUniqueId(msg.entityId);
        buf.writeVarInt(msg.action.ordinal());
    }

    static TaskActionPacket decode(PacketBuffer buf) {
        return new TaskActionPacket(buf.readUniqueId(), buf.readUniqueId(), TaskContainer.TaskAction.values()[buf.readVarInt()]);
    }

    public static void handle(final TaskActionPacket msg, Supplier<NetworkEvent.Context> contextSupplier) {
        final NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> VampirismMod.proxy.handleTaskActionPacket(msg, ctx.getSender()));
        ctx.setPacketHandled(true);
    }
}
