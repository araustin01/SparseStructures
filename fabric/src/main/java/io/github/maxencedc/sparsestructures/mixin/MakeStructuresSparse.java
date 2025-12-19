package io.github.maxencedc.sparsestructures.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.Decoder;
import io.github.maxencedc.sparsestructures.IdBasedSalt;
import io.github.maxencedc.sparsestructures.SparseStructuresCommon;
import io.github.maxencedc.sparsestructures.StructureSetsSet;
import net.minecraft.registry.MutableRegistry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryLoader;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.entry.RegistryEntryInfo;
import net.minecraft.resource.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(RegistryLoader.class)
public class MakeStructuresSparse {

    @Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Decoder;parse(Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;"), method = "loadElementFromResource", locals = LocalCapture.CAPTURE_FAILHARD)
    private static <E> void loadElementFromResource(MutableRegistry<E> registry, Decoder<E> codec, RegistryOps<JsonElement> ops, RegistryKey<E> resourceKey, Resource resource, RegistryEntryInfo registrationInfo, CallbackInfo ci, @Local JsonElement jsonElement) {
        String string = registry.getKey().getValue().getPath();
        if (!string.equals("worldgen/structure_set")) return;

        JsonObject jsonObject = jsonElement.getAsJsonObject();
        JsonObject placement = jsonObject.getAsJsonObject("placement");
        if (placement.get("type").getAsString().equals("minecraft:concentric_rings")) return;

        StructureSetsSet.addStructureSet(resourceKey.getValue().toString());

        double factor = SparseStructuresCommon.config.getSpreadFactor(resourceKey, jsonObject);

        if (factor == 0) {
            placement.addProperty("frequency", 0.0);
            return;
        }

        int spacing;
        int separation;

        spacing = (placement.get("spacing") == null) ? 1 : (int)(placement.get("spacing").getAsDouble() * factor);
        separation = (placement.get("separation") == null) ? 1 : (int)(placement.get("separation").getAsDouble() * factor);
        if (separation >= spacing) {
            spacing = Math.max(1, spacing);
            separation = spacing - 1;
        }

        placement.addProperty("spacing", spacing);
        placement.addProperty("separation", separation);

        if (SparseStructuresCommon.config.idBasedSalt()) {
            int salt = IdBasedSalt.getSalt(resourceKey.getValue().toString());
            placement.addProperty("salt", salt);
        }
    }
}