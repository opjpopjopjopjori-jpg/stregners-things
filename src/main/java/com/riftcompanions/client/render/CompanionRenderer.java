package com.riftcompanions.client.render;

import com.riftcompanions.client.model.CompanionGeoModel;
import com.riftcompanions.entity.CompanionEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** GeckoLib renderer; all actual animation selection stays in CompanionEntity. */
public final class CompanionRenderer<T extends CompanionEntity> extends GeoEntityRenderer<T> {
    public CompanionRenderer(final EntityRendererProvider.Context context) {
        super(context, new CompanionGeoModel<>());
        // The modular outer layers, packs, and silhouette pieces extend beyond the base humanoid torso.
        this.shadowRadius = 0.48F;
    }
}
