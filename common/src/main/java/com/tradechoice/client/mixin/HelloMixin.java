package com.tradechoice.client.mixin;

import com.tradechoice.TradeChoiceMod;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantScreen.class)
public abstract class HelloMixin {
    @Inject(method = "init()V", at = @At("RETURN"))
    private void tradechoice$hello(CallbackInfo ci) {
        TradeChoiceMod.LOGGER.info("hello from mixin");
    }
}
