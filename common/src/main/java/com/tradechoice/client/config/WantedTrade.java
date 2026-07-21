package com.tradechoice.client.config;

import java.util.Objects;

public class WantedTrade {

	private String profession;
	private String itemId;
	private String enchantmentId;
	private int enchantmentLevel;

	public WantedTrade() {
	}

	public WantedTrade(String profession, String itemId, String enchantmentId, int enchantmentLevel) {
		this.profession = profession;
		this.itemId = itemId;
		this.enchantmentId = enchantmentId;
		this.enchantmentLevel = enchantmentLevel;
	}

	public String getProfession() {
		return profession;
	}

	public String getItemId() {
		return itemId;
	}

	public String getEnchantmentId() {
		return enchantmentId;
	}

	public int getEnchantmentLevel() {
		return enchantmentLevel;
	}

	public boolean matchesProfession(String prof) {
		return profession != null && profession.equals(prof);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof WantedTrade that)) return false;
		return enchantmentLevel == that.enchantmentLevel
				&& Objects.equals(profession, that.profession)
				&& Objects.equals(itemId, that.itemId)
				&& Objects.equals(enchantmentId, that.enchantmentId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(profession, itemId, enchantmentId, enchantmentLevel);
	}
}
