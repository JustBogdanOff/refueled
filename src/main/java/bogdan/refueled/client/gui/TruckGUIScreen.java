package bogdan.refueled.client.gui;

import bogdan.refueled.RefueledMain;
import bogdan.refueled.common.gui.TruckGUI;
import com.dragn0007.dragnvehicles.vehicle.truck.Truck;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class TruckGUIScreen extends AbstractContainerScreen<TruckGUI> {
    public TruckGUIScreen(TruckGUI truckGUI, Inventory playerInv, Component title) {
        super(truckGUI, playerInv, title);
        this.playerInv = playerInv;
        this.truck = truckGUI.truck;
        this.maxSteps = (truck.inventory.getContainerSize() / 8) - (truck.inventory.getContainerSize() % 8 == 0 ? 6 : 5);

        imageWidth = 176;
        imageHeight = 222;

        drawSlots();
    }

    private final Inventory playerInv;
    private final Truck truck;

    private static final int fontColor = 4210752;
    public static final ResourceLocation TRUCK_GUI_TEXTURE = new ResourceLocation(RefueledMain.MODID, "textures/gui/truck_gui.png");

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderBackground(guiGraphics);

        guiGraphics.blit(TRUCK_GUI_TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        for(int i = 0; i < renderedCount; i++){
            guiGraphics.blit(TRUCK_GUI_TEXTURE, leftPos + 7 + (i % 8) * 18, topPos + 17 + (i / 8) * 18, 186, 0, 18, 18);
        }
    }


    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.blit(TRUCK_GUI_TEXTURE, 155, 18 + currentOffset, 176, maxSteps > 0 ? ((focused || dragging) ? 17 : 0) : 34, 10, 17);

        guiGraphics.drawString(font, truck.getDisplayName().getVisualOrderText(), 7, 7, fontColor, false);
        guiGraphics.drawString(font, playerInv.getDisplayName().getVisualOrderText(), 7, 129, fontColor, false);
        renderTooltip(guiGraphics, mouseX - leftPos, mouseY - topPos);
    }

    private boolean focused, dragging;
    // height of 6 slots, minus the padding of 2 pixels total and scroll button height
    private final int maxOffset = 89, maxSteps;
    private int currentOffset = 0, lastOffset = 0, renderedCount = 0;

    private void drawSlots(){
        renderedCount = 0;
        var currentRow = (currentOffset * maxSteps / maxOffset) * 8;
        for(int i = 0; i < truck.inventory.getContainerSize(); i++) {
            if (currentRow <= i && i < Math.min(truck.inventory.getContainerSize(), currentRow + (6 * 8))) {
                var slot = new Slot(truck.inventory, i, 8 + (i % 8) * 18, 18 + ((i - currentRow) / 8) * 18);
                slot.index = i;
                getMenu().slots.set(i, slot);
                renderedCount++;
            } else getMenu().slots.set(i, new Slot(truck.inventory, i, 152, 18) {
                @Override
                public boolean isHighlightable() {
                    return false;
                }

                @Override
                public boolean mayPlace(@NotNull ItemStack pStack) {
                    return false;
                }

                @Override
                public boolean mayPickup(@NotNull Player pPlayer) {
                    return false;
                }

                @Override
                public boolean isActive() {
                    return false;
                }
            });
        }
    }

    @Override
    protected void containerTick() {
        if(maxSteps > 0 && currentOffset != lastOffset){
            drawSlots();
            lastOffset = currentOffset;
        }
        super.containerTick();
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        if(super.mouseScrolled(pMouseX, pMouseY, pDelta))
            return true;
        else if(maxSteps > 0 && (leftPos + 7 <= pMouseX && pMouseX <= leftPos + 166 && topPos + 17 <= pMouseY && pMouseY <= topPos + 17 + 6 * 18)){
            var nextStep = (currentOffset * maxSteps / maxOffset) - Mth.floor(pDelta);
            currentOffset = Mth.clamp(nextStep * maxOffset / maxSteps, 0, maxOffset);
            return true;
        }
        else return false;
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if(maxSteps > 0 && (leftPos + 155 <= pMouseX && pMouseX <= leftPos + 165 && topPos + 18 + currentOffset <= pMouseY && pMouseY <= topPos + 35 + currentOffset)){
            focused = !focused;
            return true;
        }

        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        if(maxSteps > 0) {
            if (!dragging && (leftPos + 155 <= pMouseX && pMouseX <= leftPos + 165 && topPos + 18 + currentOffset <= pMouseY && pMouseY <= topPos + 35 + currentOffset))
                dragging = true;

            if (dragging) {
                currentOffset = Mth.clamp(currentOffset + Mth.floor(pDragY), 0, maxOffset);
                return true;
            }
        }

        return super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);
    }


    @Override
    public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
        if(maxSteps > 0 && dragging)
            dragging = false;
        return super.mouseReleased(pMouseX, pMouseY, pButton);
    }
}
