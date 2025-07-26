package bogdan.refueled.client;

import bogdan.refueled.common.blocks.blockentities.GasStationBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

@SuppressWarnings("NullableProblems")
public class GasStationBlockEntityRenderer implements BlockEntityRenderer<GasStationBlockEntity> {

    private final Minecraft mc;
    protected BlockEntityRendererProvider.Context renderer;

    public GasStationBlockEntityRenderer(BlockEntityRendererProvider.Context renderer) {
        this.renderer = renderer;
        mc = Minecraft.getInstance();
    }

    @Override
    public void render(GasStationBlockEntity te, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!te.hasLevel()) {
            return;
        }

        Component fluid = te.getFluidText(),
                amount = te.getAmountText();

        poseStack.pushPose();
        poseStack.translate(0.5D, 1D, 0.5D);
        poseStack.mulPose(Axis.XP.rotationDegrees(180F));

        Direction dir = te.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);

        poseStack.mulPose(Axis.YP.rotationDegrees(dir.toYRot()));

        Font font = renderer.getFont();

        int textWidth = Math.max(font.width(fluid), font.width(amount));
        float textScale = 0.36f / textWidth;
        textScale = Math.min(textScale, 0.01f);

        float posX = -(textScale * textWidth) / 2F;

        poseStack.translate(posX, -0.86, -0.318);

        poseStack.scale(textScale, textScale, textScale);

        font.drawInBatch(fluid, 0F, 0F, 0xFFFFFF, false, poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, packedLight);
        font.drawInBatch(amount, 0F, font.lineHeight, 0xFFFFFF, false, poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.mulPose(Axis.YP.rotationDegrees(180));
        poseStack.translate(-0.54 / textScale, 0, -0.636 / textScale);
        font.drawInBatch(fluid, 0F, 0F, 0xFFFFFF, false, poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, packedLight);
        font.drawInBatch(amount, 0F, font.lineHeight, 0xFFFFFF, false, poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();

        if (mc.getEntityRenderDispatcher().shouldRenderHitBoxes() && !Minecraft.getInstance().showOnlyReducedInfo()) {
            poseStack.pushPose();
            renderBoundingBox(te, partialTicks, poseStack, buffer, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    public void renderBoundingBox(GasStationBlockEntity te, float partialTicks, PoseStack matrixStack, MultiBufferSource buffer, int combinedLightIn, int combinedOverlayIn) {
        AABB axisalignedbb = te.fuelingBox.move(-te.getBlockPos().getX(), -te.getBlockPos().getY(), -te.getBlockPos().getZ());
        LevelRenderer.renderLineBox(matrixStack, buffer.getBuffer(RenderType.lines()), axisalignedbb, 1, 1, 1, 1);
    }
}
