package com.riftcompanions.client.model;

import com.riftcompanions.RiftCompanions;
import com.riftcompanions.entity.CompanionEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Role-driven model selector; logic IDs and display textures remain separated. */
public final class CompanionGeoModel<T extends CompanionEntity> extends GeoModel<T> {
    @Override
    public ResourceLocation getModelResource(final T animatable) {
        return new ResourceLocation(RiftCompanions.MOD_ID, "geo/" + animatable.getRole().id() + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(final T animatable) {
        final String texture = com.riftcompanions.content.ContentProfileRegistry.profile(animatable.getRole()).textureFile();
        final String folder = com.riftcompanions.config.CompanionConfig.CONTENT_MODE.get() == com.riftcompanions.content.ContentMode.PUBLIC ? "public" : "personal";
        return new ResourceLocation(RiftCompanions.MOD_ID, "textures/entity/" + folder + "/" + texture);
    }

    @Override
    public ResourceLocation getAnimationResource(final T animatable) {
        return new ResourceLocation(RiftCompanions.MOD_ID, "animations/" + animatable.getRole().id() + ".animation.json");
    }
}
