package bogdan.refueled.client.gui;

import bogdan.refueled.RefueledMain;
import bogdan.refueled.common.blocks.blockentities.GasStationBlockEntity;
import bogdan.refueled.common.gui.GasStationGUI;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class GasStationGUIScreen extends AbstractContainerScreen<GasStationGUI> {
    public GasStationGUIScreen(GasStationGUI gasStationGUI, Inventory playerInv, Component title) {
        super(gasStationGUI, playerInv, title);
        this.playerInv = playerInv;
        this.gasStation = gasStationGUI.gasStation;

        imageWidth = 176;
        imageHeight = 222;
    }

    private final Inventory playerInv;
    private final GasStationBlockEntity gasStation;
    private int clicked = 0;

    private static final int fontColor = 4210752;
    public static final ResourceLocation GAS_STATION_TEXTURE = new ResourceLocation(RefueledMain.MODID, "textures/gui/gas_station_gui.png");

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderBackground(guiGraphics);

        guiGraphics.blit(GAS_STATION_TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        mouseX -= leftPos;
        mouseY -= topPos;
        if(mouseX >= 115 && mouseX < 133 && mouseY >= 21 && mouseY < 39)
            guiGraphics.blit(GAS_STATION_TEXTURE, leftPos + 115, topPos + 21, 176 + clicked, 18, 18, 18);
        else
            guiGraphics.blit(GAS_STATION_TEXTURE, leftPos + 115, topPos + 21, 176 + clicked, 0, 18, 18);
    }


    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, gasStation.getFluidText(), 7, 10 + font.lineHeight, fontColor, false);
        guiGraphics.drawString(font, gasStation.getAmountText(), 7, 10 + font.lineHeight * 2, fontColor, false);

         menu.foundEntities.forEach(id -> {
            Entity selected = playerInv.player.level().getEntity(id);
            var index = menu.foundEntities.indexOf(id);
            var color = 139;
            if(selected == gasStation.selected) {
                guiGraphics.fill(RenderType.guiOverlay(),7, 37 + font.lineHeight * index + 2 * index, 10 + font.width(selected.getDisplayName().getString()), 39 + font.lineHeight * (index + 1) + 2 * index, 0, FastColor.ARGB32.color(255, 255, 255, 255));
                color = 85;
            }
            else if(isOverEntityList(mouseX, mouseY, index))
                guiGraphics.fill(RenderType.guiOverlay(),7, 37 + font.lineHeight * index + 2 * index, 10 + font.width(selected.getDisplayName()), 39 + font.lineHeight * (index + 1) + 2 * index, 0, FastColor.ARGB32.color(255, 255, 255, 255));

            guiGraphics.fill(RenderType.guiOverlay(), 8, 38 + font.lineHeight * index + 4 * index, 9 + font.width(selected.getDisplayName()), 38 + font.lineHeight * (index + 1) + 2 * index, 10, FastColor.ARGB32.color(255, color, color, color));
            guiGraphics.drawString(font,  selected.getDisplayName(), 9, 39 + font.lineHeight * index, 0xffffff, false);
         });


        guiGraphics.drawString(font, gasStation.getBlockState().getBlock().getName().getVisualOrderText(), 7, 7, fontColor, false);
        guiGraphics.drawString(font, playerInv.getDisplayName().getVisualOrderText(), 7, 83, fontColor, false);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 333);
        guiGraphics.blit(GAS_STATION_TEXTURE, 151, 39, 0, 176, 36, 18, 18, 256, 256);
        guiGraphics.pose().popPose();

        renderTooltip(guiGraphics, mouseX - leftPos, mouseY - topPos);
    }

    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (pMouseX - leftPos >= 115.0 && pMouseX - leftPos < 133.0 && pMouseY - topPos >= 21.0 && pMouseY - topPos < 39.0){
            clicked = 18;
            released = false;
            if(menu.clickMenuButton(playerInv.player, 0)) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                return true;
            }
        }
        for(var id : menu.foundEntities){
            var index = menu.foundEntities.indexOf(id);
            if(isOverEntityList(pMouseX - leftPos, pMouseY - topPos, index) && menu.clickMenuButton(playerInv.player, index + 1)) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, index + 1);
                return true;
            }
        }

        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        if(released && !gasStation.fueling)
            clicked = 0;
    }

    private boolean released = false;

    @Override
    public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
        released = true;
        return super.mouseReleased(pMouseX, pMouseY, pButton);
    }

    private boolean isOverEntityList(double mX, double mY, int index){
        return mX >= 7
                && mX < 10 + font.width(playerInv.player.level().getEntity(menu.foundEntities.get(index)).getDisplayName().getString())
                && mY >= 37 + font.lineHeight * index + 2 * index
                && mY < 39 + font.lineHeight * (index + 1) + 2 * index;
    }
}
