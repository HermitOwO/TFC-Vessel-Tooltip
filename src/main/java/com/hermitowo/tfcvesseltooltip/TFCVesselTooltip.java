package com.hermitowo.tfcvesseltooltip;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Mod;

import net.dries007.tfc.client.ClientHelpers;
import net.dries007.tfc.common.capabilities.VesselLike;
import net.dries007.tfc.common.items.VesselItem;
import net.dries007.tfc.common.recipes.HeatingRecipe;
import net.dries007.tfc.common.recipes.inventory.ItemStackInventory;
import net.dries007.tfc.util.Alloy;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.Metal;

@Mod(TFCVesselTooltip.MOD_ID)
@Mod.EventBusSubscriber(modid = TFCVesselTooltip.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class TFCVesselTooltip
{
    public static final String MOD_ID = "tfcvesseltooltip";

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onItemTooltip(ItemTooltipEvent event)
    {
        final ItemStack stack = event.getItemStack();
        final List<Component> text = event.getToolTip();
        if (!stack.isEmpty())
        {
            if (stack.getItem() instanceof VesselItem)
            {
                VesselLike vessel = VesselLike.get(stack);
                vessel = vessel != null && vessel.mode() == VesselLike.Mode.INVENTORY ? vessel : null;
                if (vessel != null)
                {
                    Map<Metal, Integer> map = new HashMap<>();
                    for (ItemStack item : Helpers.iterate(vessel))
                    {
                        final ItemStackInventory inventory = new ItemStackInventory(item);
                        final HeatingRecipe recipe = HeatingRecipe.getRecipe(inventory);
                        if (recipe == null)
                            continue;

                        final FluidStack fluid = recipe.assembleFluid(inventory);
                        if (fluid.isEmpty())
                            continue;

                        final Metal metal = Metal.get(fluid.getFluid());
                        if (metal == null)
                            continue;

                        map.computeIfPresent(metal, (key, value) -> value + fluid.getAmount() * item.getCount());
                        map.putIfAbsent(metal, fluid.getAmount() * item.getCount());
                    }

                    if (!map.isEmpty())
                    {
                        text.add(Helpers.translatable("tfc.tooltip.small_vessel.contents").withStyle(ChatFormatting.DARK_GREEN));

                        Alloy alloy = new Alloy();
                        int total = map.values().stream().reduce(0, Integer::sum);
                        for (Map.Entry<Metal, Integer> entry : map.entrySet())
                        {
                            Metal metal = entry.getKey();
                            int amount = entry.getValue();
                            String percentage = String.format("%.1f", (float) amount / total * 100) + "%";
                            text.add(Helpers.translatable("tfcvesseltooltip.tooltip.metal", amount, Helpers.translatable(metal.getTranslationKey()), Helpers.literal(percentage).withStyle(ChatFormatting.GREEN)));

                            alloy.add(metal, amount, false);
                        }

                        if (map.size() > 1)
                        {
                            text.add(Helpers.translatable("tfcvesseltooltip.tooltip.smelts_into").withStyle(ChatFormatting.DARK_GREEN));
                            Metal result = alloy.getResult(ClientHelpers.getLevelOrThrow());
                            text.add(Helpers.translatable("tfcvesseltooltip.tooltip.alloy", total, Helpers.translatable(result.getTranslationKey())));
                        }
                    }
                }
            }
        }
    }
}
