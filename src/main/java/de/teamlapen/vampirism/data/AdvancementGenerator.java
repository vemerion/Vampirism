package de.teamlapen.vampirism.data;

import com.google.common.collect.ImmutableList;
import de.teamlapen.vampirism.advancements.*;
import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.core.*;
import de.teamlapen.vampirism.entity.minion.management.MinionTasks;
import de.teamlapen.vampirism.util.REFERENCE;
import net.minecraft.advancements.*;
import net.minecraft.advancements.criterion.*;
import net.minecraft.data.AdvancementProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class AdvancementGenerator extends AdvancementProvider {
    public AdvancementGenerator(DataGenerator generatorIn) {
        super(generatorIn);
        MainAdvancements main = new MainAdvancements();
        this.advancements = ImmutableList.of(main, new HunterAdvancements(main::getRoot), new VampireAdvancements(main::getRoot), new MinionAdvancements(main::getRoot));
    }

    private static class HunterAdvancements implements Consumer<Consumer<Advancement>> {

        private final Supplier<Advancement> root;

        public HunterAdvancements(Supplier<Advancement> root) {
            this.root = root;
        }

        @Override
        public void accept(Consumer<Advancement> consumer) {
            Advancement become_hunter = Advancement.Builder.builder()
                    .withDisplay(ModItems.item_garlic, new TranslationTextComponent("advancement.vampirism.become_hunter"), new TranslationTextComponent("advancement.vampirism.become_hunter.desc"), null, FrameType.TASK, true, false, false)
                    .withParent(root.get())
                    .withCriterion("main", TriggerFaction.builder(VReference.HUNTER_FACTION, 1))
                    .register(consumer, REFERENCE.MODID + ":hunter/become_hunter");
            Advancement stake = Advancement.Builder.builder()
                    .withDisplay(ModItems.stake, new TranslationTextComponent("advancement.vampirism.stake"), new TranslationTextComponent("advancement.vampirism.stake.desc"), null, FrameType.CHALLENGE, true, true, true)
                    .withParent(become_hunter)
                    .withCriterion("flower", HunterActionTrigger.builder(HunterActionTrigger.Action.STAKE))
                    .withRewards(AdvancementRewards.Builder.experience(100))
                    .register(consumer, REFERENCE.MODID + ":hunter/stake");
            Advancement betrayal = Advancement.Builder.builder()
                    .withDisplay(ModItems.human_heart, new TranslationTextComponent("advancement.vampirism.betrayal"), new TranslationTextComponent("advancement.vampirism.betrayal.desc"), null, FrameType.TASK, true, true, true)
                    .withParent(become_hunter)
                    .withCriterion("kill", KilledTrigger.Instance.playerKilledEntity(EntityPredicate.Builder.create().type(ModTags.Entities.HUNTER)))
                    .withCriterion("faction", TriggerFaction.builder(VReference.HUNTER_FACTION, 1))
                    .register(consumer, REFERENCE.MODID + ":hunter/betrayal");
            Advancement max_level = Advancement.Builder.builder()
                    .withDisplay(ModItems.item_garlic, new TranslationTextComponent("advancement.vampirism.max_level_hunter"), new TranslationTextComponent("advancement.vampirism.max_level_hunter.desc"), null, FrameType.GOAL, true, true, true)
                    .withParent(stake)
                    .withCriterion("level", TriggerFaction.builder(VReference.HUNTER_FACTION, 14))
                    .withRewards(AdvancementRewards.Builder.experience(100))
                    .register(consumer, REFERENCE.MODID + ":hunter/max_level");
            Advancement technology = Advancement.Builder.builder()
                    .withDisplay(ModItems.basic_tech_crossbow, new TranslationTextComponent("advancement.vampirism.technology"), new TranslationTextComponent("advancement.vampirism.technology.desc"), null, FrameType.TASK, true, true, true)
                    .withParent(become_hunter)
                    .withCriterion("basic", InventoryChangeTrigger.Instance.forItems(ModItems.basic_tech_crossbow))
                    .withCriterion("advanced", InventoryChangeTrigger.Instance.forItems(ModItems.enhanced_tech_crossbow))
                    .withRequirementsStrategy(IRequirementsStrategy.AND)
                    .register(consumer, REFERENCE.MODID + ":hunter/technology");
            Advancement max_lord = Advancement.Builder.builder()
                    .withDisplay(ModItems.hunter_minion_upgrade_special, new TranslationTextComponent("advancement.vampirism.max_lord_hunter"), new TranslationTextComponent("advancement.vampirism.max_lord_hunter"), null, FrameType.CHALLENGE, true, true, true)
                    .withParent(max_level)
                    .withCriterion("level", TriggerFaction.lord(VReference.HUNTER_FACTION, 5))
                    .register(consumer, REFERENCE.MODID + ":hunter/max_lord");
            Advancement cure_vampire = Advancement.Builder.builder()
                    .withDisplay(ModItems.cure_apple, new TranslationTextComponent("advancement.vampirism.cure_vampire_villager"), new TranslationTextComponent("advancement.vampirism.cure_vampire_villager"),null , FrameType.TASK, true, true, true )
                    .withParent(become_hunter)
                    .withCriterion("cure", CuredVampireVillagerTrigger.Instance.any())
                    .register(consumer, REFERENCE.MODID + ":hunter/cure_vampire_villager");
        }
    }

    private static class MainAdvancements implements Consumer<Consumer<Advancement>> {
        Advancement root;

        @Override
        public void accept(Consumer<Advancement> consumer) {
            root = Advancement.Builder.builder()
                    .withDisplay(ModItems.vampire_fang, new TranslationTextComponent("itemGroup.vampirism"), new TranslationTextComponent("advancement.vampirism.desc"), new ResourceLocation(REFERENCE.MODID, "textures/block/castle_block_dark_brick.png"), FrameType.TASK, false, false, false)
                    .withCriterion("main", InventoryChangeTrigger.Instance.forItems(ModItems.vampire_fang))
                    .withCriterion("second", InventoryChangeTrigger.Instance.forItems(ModItems.item_garlic))
                    .withRequirementsStrategy(IRequirementsStrategy.OR)
                    .register(consumer, REFERENCE.MODID + ":main/root");
            Advancement vampire_forest = Advancement.Builder.builder()
                    .withDisplay(Items.OAK_LOG, new TranslationTextComponent("advancement.vampirism.vampire_forest"), new TranslationTextComponent("advancement.vampirism.vampire_forest.desc"), null, FrameType.TASK, true, true, true)
                    .withParent(root)
                    .withCriterion("main", PositionTrigger.Instance.forLocation(LocationPredicate.forBiome(ModBiomes.VAMPIRE_FOREST_KEY)))
                    .withCriterion("second", PositionTrigger.Instance.forLocation(LocationPredicate.forBiome(ModBiomes.VAMPIRE_FOREST_HILLS_KEY)))
                    .withRequirementsStrategy(IRequirementsStrategy.OR)
                    .register(consumer, REFERENCE.MODID + ":main/vampire_forest");
            Advancement ancient_knowledge = Advancement.Builder.builder()
                    .withDisplay(ModItems.vampire_book, new TranslationTextComponent("advancement.vampirism.ancient_knowledge"), new TranslationTextComponent("advancement.vampirism.ancient_knowledge.desc"), null, FrameType.TASK, true, true, true)
                    .withParent(vampire_forest)
                    .withCriterion("blood_container", InventoryChangeTrigger.Instance.forItems(ModItems.vampire_book))
                    .register(consumer, REFERENCE.MODID + ":main/ancient_knowledge");
            Advancement regicide = Advancement.Builder.builder()
                    .withDisplay(ModItems.pure_blood_0, new TranslationTextComponent("advancement.vampirism.regicide"), new TranslationTextComponent("advancement.vampirism.regicide.desc"), null, FrameType.CHALLENGE, true, true, true)
                    .withParent(vampire_forest)
                    .withCriterion("main", KilledTrigger.Instance.playerKilledEntity(EntityPredicate.Builder.create().type(ModEntities.vampire_baron)))
                    .register(consumer, REFERENCE.MODID + ":main/regicide");
        }

        public Advancement getRoot() {
            return root;
        }

    }

    private static class VampireAdvancements implements Consumer<Consumer<Advancement>> {

        private final Supplier<Advancement> root;

        public VampireAdvancements(Supplier<Advancement> root) {
            this.root = root;
        }

        @Override
        public void accept(Consumer<Advancement> consumer) {
            Advancement become_vampire = Advancement.Builder.builder()
                    .withDisplay(ModItems.vampire_fang, new TranslationTextComponent("advancement.vampirism.become_vampire"), new TranslationTextComponent("advancement.vampirism.become_vampire.desc"), null, FrameType.TASK, true, false, false)
                    .withParent(root.get())
                    .withCriterion("main", TriggerFaction.builder(VReference.VAMPIRE_FACTION, 1))
                    .register(consumer, REFERENCE.MODID + ":vampire/become_vampire");
            Advancement bat = Advancement.Builder.builder()
                    .withDisplay(Items.FEATHER, new TranslationTextComponent("advancement.vampirism.bat"), new TranslationTextComponent("advancement.vampirism.bat.desc"), null, FrameType.TASK, true, true, true)
                    .withParent(become_vampire)
                    .withCriterion("action", VampireActionTrigger.builder(VampireActionTrigger.Action.BAT))
                    .register(consumer, REFERENCE.MODID + ":vampire/bat");
            Advancement first_blood = Advancement.Builder.builder()
                    .withDisplay(ModItems.blood_bottle, new TranslationTextComponent("advancement.vampirism.sucking_blood"), new TranslationTextComponent("advancement.vampirism.sucking_blood.desc"), null, FrameType.TASK, true, true, true)
                    .withParent(become_vampire)
                    .withCriterion("flower", VampireActionTrigger.builder(VampireActionTrigger.Action.SUCK_BLOOD))
                    .register(consumer, REFERENCE.MODID + ":vampire/first_blood");
            Advancement blood_cult = Advancement.Builder.builder()
                    .withDisplay(ModBlocks.altar_infusion, new TranslationTextComponent("advancement.vampirism.blood_cult"), new TranslationTextComponent("advancement.vampirism.blood_cult.desc"), null, FrameType.TASK, true, true, true)
                    .withParent(become_vampire)
                    .withCriterion("flower", VampireActionTrigger.builder(VampireActionTrigger.Action.PERFORM_RITUAL_INFUSION))
                    .register(consumer, REFERENCE.MODID + ":vampire/blood_cult");
            Advancement extra_storage = Advancement.Builder.builder()
                    .withDisplay(ModBlocks.blood_container, new TranslationTextComponent("advancement.vampirism.extra_storage"), new TranslationTextComponent("advancement.vampirism.extra_storage.desc"), null, FrameType.TASK, true, true, true)
                    .withParent(first_blood)
                    .withCriterion("blood_container", InventoryChangeTrigger.Instance.forItems(ModBlocks.blood_container))
                    .register(consumer, REFERENCE.MODID + ":vampire/extra_storage");
            Advancement max_level = Advancement.Builder.builder()
                    .withDisplay(ModItems.vampire_fang, new TranslationTextComponent("advancement.vampirism.max_level_vampire"), new TranslationTextComponent("advancement.vampirism.max_level_vampire.desc"), null, FrameType.GOAL, true, true, true)
                    .withParent(bat)
                    .withCriterion("level", TriggerFaction.builder(VReference.VAMPIRE_FACTION, 14))
                    .withRewards(AdvancementRewards.Builder.experience(100))
                    .register(consumer, REFERENCE.MODID + ":vampire/max_level");
            Advancement sniped = Advancement.Builder.builder()
                    .withDisplay(Items.ARROW, new TranslationTextComponent("advancement.vampirism.sniped"), new TranslationTextComponent("advancement.vampirism.sniped"), null, FrameType.TASK, true, true, true)
                    .withParent(bat)
                    .withCriterion("flower", VampireActionTrigger.builder(VampireActionTrigger.Action.SNIPED_IN_BAT))
                    .register(consumer, REFERENCE.MODID + ":vampire/sniped");
            CompoundNBT potion = new ItemStack(Items.POTION).serializeNBT();
            potion.putString("Potion", "minecraft:poison");
            Advancement yuck = Advancement.Builder.builder()
                    .withDisplay(new DisplayInfo(ItemStack.read(potion), new TranslationTextComponent("advancement.vampirism.yuck"), new TranslationTextComponent("advancement.vampirism.yuck"), null, FrameType.TASK, true, true, true))
                    .withParent(first_blood)
                    .withCriterion("flower", VampireActionTrigger.builder(VampireActionTrigger.Action.POISONOUS_BITE))
                    .register(consumer, REFERENCE.MODID + ":vampire/yuck");
            Advancement freeze_kill = Advancement.Builder.builder()
                    .withDisplay(new DisplayInfo(new ItemStack(Items.CLOCK), new TranslationTextComponent("advancement.vampirism.freeze_kill"), new TranslationTextComponent("advancement.vampirism.freeze_kill.desc"), null, FrameType.TASK, true, true, true))
                    .withParent(blood_cult)
                    .withCriterion("kill", VampireActionTrigger.builder(VampireActionTrigger.Action.KILL_FROZEN_HUNTER))
                    .register(consumer, REFERENCE.MODID + ":vampire/freeze_kill");
            Advancement max_lord = Advancement.Builder.builder()
                    .withDisplay(ModItems.vampire_minion_upgrade_special, new TranslationTextComponent("advancement.vampirism.max_lord_vampire"), new TranslationTextComponent("advancement.vampirism.max_lord_vampire"), null, FrameType.CHALLENGE, true, true, true)
                    .withParent(max_level)
                    .withCriterion("level", TriggerFaction.lord(VReference.VAMPIRE_FACTION, 5))
                    .register(consumer, REFERENCE.MODID + ":vampire/max_lord");

        }
    }

    private static class MinionAdvancements implements Consumer<Consumer<Advancement>> {

        private final Supplier<Advancement> root;

        public MinionAdvancements(Supplier<Advancement> root) {
            this.root = root;
        }

        @Override
        public void accept(Consumer<Advancement> consumer) {
            Advancement become_lord = Advancement.Builder.builder()
                    .withDisplay(Items.PAPER, new TranslationTextComponent("advancement.vampirism.become_lord"), new TranslationTextComponent("advancement.vampirism.become_lord"), null, FrameType.TASK, true, true, true)
                    .withParent(root.get())
                    .withCriterion("level", TriggerFaction.lord(null, 1))
                    .register(consumer, REFERENCE.MODID + ":minion/become_lord");
            Advancement collect_blood = Advancement.Builder.builder()
                    .withDisplay(ModItems.blood_bottle, new TranslationTextComponent("advancement.vampirism.collect_blood"), new TranslationTextComponent("advancement.vampirism.collect_blood.desc"), null, FrameType.TASK, true, true, true)
                    .withParent(become_lord)
                    .withCriterion("task", MinionTaskTrigger.tasks(MinionTasks.collect_blood))
                    .register(consumer, REFERENCE.MODID + ":minion/collect_blood");
            Advancement collect_hunter_items = Advancement.Builder.builder()
                    .withDisplay(Items.GOLD_NUGGET, new TranslationTextComponent("advancement.vampirism.collect_hunter_items"), new TranslationTextComponent("advancement.vampirism.collect_hunter_items.desc"), null, FrameType.TASK, true, true, true)
                    .withParent(become_lord)
                    .withCriterion("task", MinionTaskTrigger.tasks(MinionTasks.collect_hunter_items))
                    .register(consumer, REFERENCE.MODID + ":minion/collect_hunter_items");
            Advancement protect_lord = Advancement.Builder.builder()
                    .withDisplay(Items.SHIELD, new TranslationTextComponent("advancement.vampirism.protect_lord"), new TranslationTextComponent("advancement.vampirism.protect_lord.desc"), null, FrameType.TASK, true, true, true)
                    .withParent(become_lord)
                    .withCriterion("task", MinionTaskTrigger.tasks(MinionTasks.protect_lord))
                    .register(consumer, REFERENCE.MODID + ":minion/protect_lord");
            Advancement defend_area = Advancement.Builder.builder()
                    .withDisplay(Items.SHIELD, new TranslationTextComponent("advancement.vampirism.defend_area"), new TranslationTextComponent("advancement.vampirism.defend_area.desc"), null, FrameType.TASK, true, true, true)
                    .withParent(become_lord)
                    .withCriterion("task", MinionTaskTrigger.tasks(MinionTasks.defend_area))
                    .register(consumer, REFERENCE.MODID + ":minion/defend_area");
        }
    }

}
