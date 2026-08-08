package com.riftcompanions.animation;

/** Texture-compatible enhanced animations using 512x512 RGBA islands:
 *  - eye_tile / accent_tile / thread_tile for face expressions
 *  - hair_tile for secondary hair motion
 *  - jacket / shirt / field / cloth / weather for clothing
 *  - pants / leather / boot / metal for lower body */
public final class TextureCompatibleAnimation {
    private TextureCompatibleAnimation() {}

    public static String eyeExpressionAnimation(final String roleId, final String expression) {
        return "animation." + roleId + ".eye_" + expression;
    }

    public static String hairMotionAnimation(final String roleId, final boolean intense) {
        return "animation." + roleId + ".hair_" + (intense ? "intense" : "gentle");
    }

    public static String clothingLayerAnimation(final String roleId, final String layer, final String state) {
        return "animation." + roleId + "." + layer + "_" + state;
    }

    public static String fullTextureCompatibleAnimation(final String roleId, final String emotionalState, final String actionType) {
        return "animation.story.full." + roleId + "." + emotionalState + "." + actionType;
    }
}
