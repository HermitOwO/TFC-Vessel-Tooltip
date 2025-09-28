package com.hermitowo.tfcvesseltooltip;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.common.component.mold.Vessel;
import net.dries007.tfc.common.items.VesselItem;
import net.dries007.tfc.common.recipes.HeatingRecipe;
import net.dries007.tfc.util.FluidAlloy;

@Mod(TFCVesselTooltip.MOD_ID)
@EventBusSubscriber(modid = TFCVesselTooltip.MOD_ID, value = Dist.CLIENT)
public class TFCVesselTooltip
{
    public static final String MOD_ID = "tfcvesseltooltip";

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onItemTooltip(ItemTooltipEvent event)
    {
        final ItemStack stack = event.getItemStack();
        final List<Component> text = event.getToolTip();
        if (stack.isEmpty())
            return;
        if (!(stack.getItem() instanceof VesselItem))
            return;

        Vessel vessel = Vessel.get(stack);
        vessel = vessel != null && vessel.isInventory() ? vessel : null;
        if (vessel == null)
            return;

        Map<Fluid, Integer> map = new HashMap<>();
        for (ItemStack item : vessel.contents())
        {
            final @Nullable HeatingRecipe recipe = HeatingRecipe.getRecipe(item);
            if (recipe == null)
                continue;

            final FluidStack fluid = recipe.assembleFluid(item);
            if (fluid.isEmpty())
                continue;

            map.computeIfPresent(fluid.getFluid(), (key, value) -> value + fluid.getAmount() * item.getCount());
            map.putIfAbsent(fluid.getFluid(), fluid.getAmount() * item.getCount());
        }

        if (!map.isEmpty())
        {
            text.add(Component.translatable("tfc.tooltip.small_vessel.contents").withStyle(ChatFormatting.DARK_GREEN));

            int total = map.values().stream().reduce(0, Integer::sum);
            for (Map.Entry<Fluid, Integer> entry : map.entrySet())
            {
                Fluid fluid = entry.getKey();
                int amount = entry.getValue();
                String percentage = String.format("%.1f", (float) amount / total * 100) + "%";
                text.add(Component.translatable("tfcvesseltooltip.tooltip.metal", amount, fluid.getFluidType().getDescription(), Component.literal(percentage).withStyle(ChatFormatting.GREEN)));
            }

            if (map.size() > 1)
            {
                text.add(Component.translatable("tfcvesseltooltip.tooltip.smelts_into").withStyle(ChatFormatting.DARK_GREEN));
                FluidAlloy alloy = FluidAlloy.empty();
                Vessel.ContainerInfo containerInfo = vessel.containerInfo();
                map.forEach((fluid, amount) -> alloy.fill(new FluidStack(fluid, amount), IFluidHandler.FluidAction.EXECUTE, containerInfo));
                text.add(Component.translatable("tfcvesseltooltip.tooltip.alloy", total, alloy.getResult().getHoverName()));
            }
        }
    }
}
