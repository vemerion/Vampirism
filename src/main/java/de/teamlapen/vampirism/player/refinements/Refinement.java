package de.teamlapen.vampirism.player.refinements;

import de.teamlapen.vampirism.api.entity.player.refinement.IRefinement;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.BiFunction;

public class Refinement extends ForgeRegistryEntry<IRefinement> implements IRefinement {

    private final Attribute attribute;
    private final BiFunction<UUID, Double, AttributeModifier> modifier;
    private final UUID uuid;
    private final double baseValue;
    private boolean detrimental = false;
    private IFormattableTextComponent description;

    public Refinement(Attribute attribute, UUID uuid, double baseValue, BiFunction<UUID, Double, AttributeModifier> modifier) {
        this.attribute = attribute;
        this.modifier = modifier;
        this.uuid = uuid;
        this.baseValue = baseValue;
    }

    public Refinement() {
        this.attribute = null;
        this.modifier = null;
        this.baseValue = 0;
        this.uuid = null;
    }

    /**
     * Set when refinement actually makes things worse instead of better
     * @return this
     */
    public Refinement setDetrimental(){
        this.detrimental = true;
        return this;
    }

    @Override
    public AttributeModifier createAttributeModifier(UUID uuid, double value) {
        return this.modifier==null?null:this.modifier.apply(uuid, value);
    }

    @Override
    public double getModifierValue() {
        return this.baseValue;
    }

    @Nullable
    @Override
    public UUID getUUID() {
        return this.uuid;
    }

    @Nullable
    @Override
    public Attribute getAttribute() {
        return this.attribute;
    }

    @Nonnull
    @Override
    public ITextComponent getDescription() {
        if(description==null){
            description=new TranslationTextComponent("refinement." + getRegistryName().getNamespace() + "." + getRegistryName().getPath() + ".desc");
            if(detrimental)description.mergeStyle(TextFormatting.RED);
        }
        return description;
    }
}
