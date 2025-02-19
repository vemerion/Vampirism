package de.teamlapen.vampirism.items;

import de.teamlapen.vampirism.VampirismMod;
import de.teamlapen.vampirism.blocks.AlchemicalFireBlock;
import de.teamlapen.vampirism.core.ModBlocks;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Simple placer item for alchemical fire. Also used in recipes
 *
 * @author maxanier
 */
public class AlchemicalFireItem extends VampirismItem {

    private static final String regName = "item_alchemical_fire";

    public AlchemicalFireItem() {
        super(regName, new Properties().group(VampirismMod.creativeTab));
    }


    @OnlyIn(Dist.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add(new TranslationTextComponent("item.vampirism.item_alchemical_fire.desc1").mergeStyle(TextFormatting.GRAY));
        tooltip.add(new TranslationTextComponent("item.vampirism.item_alchemical_fire.desc2").mergeStyle(TextFormatting.GRAY));
    }

    @Override
    public ActionResultType onItemUse(ItemUseContext ctx) {
        BlockPos pos = ctx.getPos().offset(ctx.getFace());

        if (ctx.getPlayer() != null && !ctx.getPlayer().canPlayerEdit(pos, ctx.getFace(), ctx.getItem())) {
            return ActionResultType.FAIL;
        } else {
            if (ctx.getWorld().isAirBlock(pos)) {
                ctx.getWorld().playSound(ctx.getPlayer(), pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1.0F, ctx.getPlayer().getRNG().nextFloat() * 0.4F + 0.8F);
                ctx.getWorld().setBlockState(pos, ModBlocks.alchemical_fire.getDefaultState().with(AlchemicalFireBlock.AGE, 15), 11);
            }

            return ActionResultType.SUCCESS;
        }
    }

}
