package com.tradechoice.client;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.Optional;

public final class EnchantmentUtil {

	private EnchantmentUtil() {
	}

	public static EnchantmentInfo getFirstStoredEnchantment(ItemStack stack) {
		if (!stack.is(Items.ENCHANTED_BOOK)) return null;

		ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
		if (stored == null || stored.isEmpty()) return null;

		for (var entry : stored.entrySet()) {
			Holder<Enchantment> holder = entry.getKey();
			int level = entry.getIntValue();
			Optional<ResourceKey<Enchantment>> keyOpt = holder.unwrapKey();
			if (keyOpt.isPresent()) {
				String id = keyOpt.get().identifier().toString();
				return new EnchantmentInfo(id, level);
			}
		}
		return null;
	}

	public record EnchantmentInfo(String id, int level) {
	}
}
