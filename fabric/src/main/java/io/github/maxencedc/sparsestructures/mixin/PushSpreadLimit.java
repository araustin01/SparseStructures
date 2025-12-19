package io.github.maxencedc.sparsestructures.mixin;

import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(RandomSpreadStructurePlacement.class)
public class PushSpreadLimit {

    @ModifyConstant(method = "*", constant = @Constant(intValue = 4096))
    private static int pushSpreadLimit(int original) {
        return Integer.MAX_VALUE;
    }
}

