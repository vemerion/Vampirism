package de.teamlapen.vampirism.core;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.entity.factions.IFaction;
import de.teamlapen.vampirism.potion.*;
import de.teamlapen.vampirism.util.REFERENCE;
import de.teamlapen.vampirism.util.SRGNAMES;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.potion.*;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.ObjectHolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static de.teamlapen.lib.lib.util.UtilLib.getNull;

/**
 * Handles all potion registrations and reference.
 */
@ObjectHolder(REFERENCE.MODID)
public class ModEffects {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final Effect sanguinare = getNull();
    public static final Effect thirst = getNull();
    public static final Effect saturation = getNull();
    public static final Effect sunscreen = getNull();
    public static final Effect disguise_as_vampire = getNull();
    public static final Effect fire_protection = getNull();
    public static final Effect garlic = getNull();
    public static final Effect poison = getNull();
    public static final Effect freeze = getNull();
    public static final Effect neonatal = getNull();
    public static final Effect oblivion = getNull();
    public static final Effect armor_regeneration = getNull();
    public static final Effect bad_omen_hunter = getNull();
    public static final Effect bad_omen_vampire = getNull();

    private static Effect modded_night_vision;  //Substituted version
    private static Effect vanilla_night_vision; //Vanilla night vision instance


    static void registerEffects(IForgeRegistry<Effect> registry) {
        vanilla_night_vision = Effects.NIGHT_VISION;
        registry.register(new VampirismNightVisionPotion());
        registry.register(new PotionThirst("thirst", EffectType.HARMFUL, 859494));
        registry.register(new PotionSanguinare("sanguinare", EffectType.NEUTRAL, 0x6A0888));
        registry.register(new VampirismEffect("saturation", EffectType.BENEFICIAL, 0xDCFF00));
        registry.register(new VampirismEffect("sunscreen", EffectType.BENEFICIAL, 0xFFF100).addAttributesModifier(ModAttributes.sundamage, "9dc9420c-3e5e-41c7-9ba4-ff70e9dc69fc", -0.5, AttributeModifier.Operation.MULTIPLY_TOTAL));
        registry.register(new VampirismEffect("fire_protection", EffectType.BENEFICIAL, 14981690));
        registry.register(new VampirismEffect("disguise_as_vampire", EffectType.NEUTRAL, 0x999900));
        registry.register(new VampirismEffect("garlic", EffectType.HARMFUL, 0xFFFFFF));
        registry.register(new PotionPoison("poison", 0x4E9331));
        registry.register(new PotionFreeze("freeze"));
        registry.register(new VampirismEffect("neonatal", EffectType.NEUTRAL, 0xFFBBBB).addAttributesModifier(Attributes.ATTACK_DAMAGE, "377d132d-d091-43b2-8a8f-b940f9bc894c",  -0.15, AttributeModifier.Operation.MULTIPLY_TOTAL).addAttributesModifier(Attributes.MOVEMENT_SPEED, "ad6d7def-46e2-485f-afba-39252767f114", -0.15, AttributeModifier.Operation.MULTIPLY_TOTAL));
        registry.register(new OblivionEffect("oblivion", EffectType.NEUTRAL, 0x4E9331));
        registry.register(new VampirismEffect("armor_regeneration", EffectType.NEUTRAL, 0xD17642));
        registry.register(new BadOmen(REFERENCE.MODID, REFERENCE.HUNTER_PLAYER_KEY) {
            @Override
            public IFaction<?> getFaction() {
                return VReference.HUNTER_FACTION;
            }
        });
        registry.register(new BadOmen(REFERENCE.MODID, REFERENCE.VAMPIRE_PLAYER_KEY) {
            @Override
            public IFaction<?> getFaction() {
                return VReference.VAMPIRE_FACTION;
            }
        });
    }

    static void fixNightVisionEffecTypes() {
        /*We have to fix the vanilla night vision potion types as they are created using the vanilla night vision potion before it can be replaced
        There are two options:
        1) Substitute the potion types too
            Less hacky
            If the vanilla duration or other things change it is less likely to be noticed (in development)
            Issue with JEI/Bug
            Annoying Forge warning
        2) Update the potion object in these potion types using reflection
            Reflection :/

        Using 2) for now
        */
        LOGGER.info("Fixing vanilla night vision potion types");
        try {
            for (EffectInstance effect : Potions.NIGHT_VISION.getEffects()) {
                if (effect.getPotion().equals(vanilla_night_vision)) { //If still referring to vanilla potion replace
                    ObfuscationReflectionHelper.setPrivateValue(EffectInstance.class, effect, modded_night_vision, SRGNAMES.EffectInstance_potion);
                }
            }
            for (EffectInstance effect : Potions.LONG_NIGHT_VISION.getEffects()) {
                if (effect.getPotion().equals(vanilla_night_vision)) {
                    ObfuscationReflectionHelper.setPrivateValue(EffectInstance.class, effect, modded_night_vision, SRGNAMES.EffectInstance_potion);
                }
            }
        } catch (ObfuscationReflectionHelper.UnableToAccessFieldException e) {
            LOGGER.error("Unable to modify vanilla night vision types. Potion tileInventory and more might not work", e);
        }
    }

    static boolean checkNightVision() {
        if (!(Effects.NIGHT_VISION instanceof VampirismNightVisionPotion)) {
            LOGGER.warn("Vampirism was not able to register it's night vision potion");
            return false;
        }
        return true;
    }
}
