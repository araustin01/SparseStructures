package io.github.maxencedc.sparsestructures.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Decoder;
import io.github.maxencedc.sparsestructures.Constants;
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
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RegistryLoader.class)
public class MakeStructuresSparse {

    private static final ThreadLocal<MutableRegistry<?>> currentRegistry = new ThreadLocal<>();
    private static final ThreadLocal<RegistryKey<?>> currentResourceKey = new ThreadLocal<>();

    @Inject(method = "*", at = @At("HEAD"))
    private static <E> void sparsestructures$captureContext(
        MutableRegistry<E> registry,
        Decoder<E> codec,
        RegistryOps<JsonElement> ops,
        RegistryKey<E> resourceKey,
        Resource resource,
        RegistryEntryInfo registrationInfo,
        CallbackInfo ci
    ) {
        currentRegistry.set(registry);
        currentResourceKey.set(resourceKey);
    }

    @Inject(method = "*", at = @At("RETURN"))
    private static <E> void sparsestructures$clearContext(
        MutableRegistry<E> registry,
        Decoder<E> codec,
        RegistryOps<JsonElement> ops,
        RegistryKey<E> resourceKey,
        Resource resource,
        RegistryEntryInfo registrationInfo,
        CallbackInfo ci
    ) {
        currentRegistry.remove();
        currentResourceKey.remove();
    }

    @ModifyArg(
        method = "*",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/serialization/Decoder;parse(Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;"
        ),
        index = 1
    )
    private static Object sparsestructures$modifyStructureSetJson(Object original) {
        if (!(original instanceof JsonElement)) {
            return original;
        }

        JsonElement jsonElement = (JsonElement) original;

        try {
            MutableRegistry<?> registry = currentRegistry.get();
            RegistryKey<?> resourceKey = currentResourceKey.get();

            // Safety check - we need both registry and resource key
            if (registry == null || resourceKey == null) {
                return jsonElement;
            }

            // Check if we're loading structure sets
            String registryPath = registry.getKey().getValue().getPath();
            if (!registryPath.equals("worldgen/structure_set")) {
                return jsonElement;
            }

            if (!jsonElement.isJsonObject()) {
                return jsonElement;
            }

            JsonObject jsonObject = jsonElement.getAsJsonObject();

            // Check if this has a placement object
            if (!jsonObject.has("placement")) {
                return jsonElement;
            }

            JsonObject placement = jsonObject.getAsJsonObject("placement");

            // Skip concentric rings (strongholds, etc.)
            if (placement.has("type") &&
                placement.get("type").getAsString().equals("minecraft:concentric_rings")) {
                return jsonElement;
            }

            // Track this structure set
            StructureSetsSet.addStructureSet(resourceKey.getValue().toString());

            // Get the spread factor for this structure set
            double factor = SparseStructuresCommon.config.getSpreadFactor(resourceKey, jsonObject);

            if (factor == 0) {
                placement.addProperty("frequency", 0.0);
                return jsonElement;
            }

            if (factor != 1.0) {
                int spacing = placement.has("spacing") ?
                    (int)(placement.get("spacing").getAsDouble() * factor) : 1;
                int separation = placement.has("separation") ?
                    (int)(placement.get("separation").getAsDouble() * factor) : 1;

                if (separation >= spacing) {
                    spacing = Math.max(1, spacing);
                    separation = spacing - 1;
                }

                placement.addProperty("spacing", spacing);
                placement.addProperty("separation", separation);
            }

            // Apply ID-based salt if configured
            if (SparseStructuresCommon.config.idBasedSalt()) {
                int salt = IdBasedSalt.getSalt(resourceKey.getValue().toString());
                placement.addProperty("salt", salt);
            }

        } catch (Exception e) {
            Constants.LOG.error("Failed to modify structure set JSON", e);
        }

        return jsonElement;
    }
}

