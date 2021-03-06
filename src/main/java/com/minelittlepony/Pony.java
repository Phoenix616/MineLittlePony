package com.minelittlepony;

import com.minelittlepony.model.PMAPI;
import com.minelittlepony.model.PlayerModel;
import com.minelittlepony.util.PonyFields;
import com.voxelmodpack.hdskins.DynamicTextureImage;
import com.voxelmodpack.hdskins.ThreadDownloadImageETag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class Pony {

    private static PonyConfig config = MineLittlePony.getConfig();

    private static int ponyCount = 0;
    private final int ponyId = ponyCount++;

    public ResourceLocation textureResourceLocation;
    public PonyData metadata = new PonyData();

    private int skinCheckCount;
    private boolean skinChecked;

    public Pony(@Nonnull AbstractClientPlayer player) {
        this.textureResourceLocation = player.getLocationSkin();
        MineLittlePony.logger.debug("+ Initialising new pony #%d for player %s (%s) with resource location %s.", this.ponyId,
                player.getName(), player.getUniqueID(), this.textureResourceLocation);
        this.checkSkin(this.textureResourceLocation);
    }

    public Pony(@Nonnull ResourceLocation aTextureResourceLocation) {
        this.textureResourceLocation = aTextureResourceLocation;
        MineLittlePony.logger.debug("+ Initialising new pony #%d with resource location %s.", this.ponyId, this.textureResourceLocation);
        this.checkSkin(this.textureResourceLocation);
    }

    public void invalidateSkinCheck() {
        this.skinChecked = false;
        metadata = new PonyData();
    }

    public void checkSkin() {
        if (!this.skinChecked) {
            this.checkSkin(this.textureResourceLocation);
        }
    }

    public void checkSkin(ResourceLocation textureResourceLocation) {
        BufferedImage skinImage = this.getBufferedImage(textureResourceLocation);
        if (skinImage != null) {
            this.checkSkin(skinImage);
        }
    }

    public BufferedImage getBufferedImage(@Nonnull ResourceLocation textureResourceLocation) {
        BufferedImage skinImage = null;
        try {
            IResource skin = Minecraft.getMinecraft().getResourceManager().getResource(textureResourceLocation);
            skinImage = TextureUtil.readBufferedImage(skin.getInputStream());
            MineLittlePony.logger.debug("Obtained skin from resource location %s", textureResourceLocation);
            // this.checkSkin(skinImage);
        } catch (IOException e) {

            try {
                ITextureObject e2 = Minecraft.getMinecraft().getTextureManager().getTexture(textureResourceLocation);
                if (e2 instanceof ThreadDownloadImageData) {

                    skinImage = PonyFields.downloadedImage.get((ThreadDownloadImageData) e2);
                    if (skinImage != null) {
                        MineLittlePony.logger.debug("Successfully reflected downloadedImage from texture object", e);
                        // this.checkSkin(skinImage);
                    }
                } else if (e2 instanceof ThreadDownloadImageETag) {
                    skinImage = ((ThreadDownloadImageETag) e2).getBufferedImage();
                } else if (e2 instanceof DynamicTextureImage) {
                    skinImage = ((DynamicTextureImage) e2).getImage();
                }
            } catch (Exception ignored) {

            }
        }

        return skinImage;
    }

    public void checkSkin(BufferedImage bufferedimage) {
        MineLittlePony.logger.debug("\tStart skin check #%d for pony #%d with image %s.", ++this.skinCheckCount, this.ponyId);
        metadata = PonyData.parse(bufferedimage);
        this.skinChecked = true;
    }

    public boolean isPegasusFlying(EntityPlayer player) {
        //noinspection SimplifiableIfStatement
        if (this.metadata.getRace() == null || !this.metadata.getRace().hasWings()) {
            return false;
        }
        return player.capabilities.isFlying || !(player.onGround || player.isRiding() || player.isOnLadder() || player.isInWater() || player.isElytraFlying());
    }

    public PlayerModel getModel(boolean ignorePony, boolean smallArms) {
        boolean is_a_pony = false;
        switch (ignorePony ? PonyLevel.BOTH : config.getPonyLevel()) {
            case HUMANS:
                is_a_pony = false;
                break;
            case BOTH:
                is_a_pony = metadata.getRace() != null;
                break;
            case PONIES:
                is_a_pony = true;
        }

        PlayerModel model;
        if (is_a_pony) {
            model = smallArms ? PMAPI.ponySmall : PMAPI.pony;
        } else {
            model = smallArms ? PMAPI.humanSmall : PMAPI.human;
        }
        return model;
    }

    public ResourceLocation getTextureResourceLocation() {
        return this.textureResourceLocation;
    }

}
