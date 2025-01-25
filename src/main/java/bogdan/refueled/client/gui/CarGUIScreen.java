package bogdan.refueled.client.gui;

import bogdan.refueled.RefueledMain;
import bogdan.refueled.common.accessors.ICarInvoker;
import bogdan.refueled.common.gui.CarGUI;
import bogdan.refueled.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;

import static bogdan.refueled.Utils.round;

public class CarGUIScreen extends AbstractContainerScreen<CarGUI> {
    private static final ResourceLocation CAR_GUI_TEXTURE = new ResourceLocation(RefueledMain.MODID, "textures/gui/refueled_gui.png");

    private static final int fontColor = 4210752;

    private final Inventory playerInv;
    private final Entity car;

    public CarGUIScreen(CarGUI carGUI, Inventory playerInv, Component title) {
        super(carGUI, playerInv, title);
        this.playerInv = playerInv;
        this.car = carGUI.getCar();

        imageWidth = 176;
        imageHeight = 222;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        //Titles
        guiGraphics.drawString(font, car.getDisplayName().getVisualOrderText(), 7, 61, fontColor, false);
        guiGraphics.drawString(font, playerInv.getDisplayName().getVisualOrderText(), 70, 129, fontColor, false);

        guiGraphics.drawString(font, getFuelString().getVisualOrderText(), 7, 9, fontColor, false);
        guiGraphics.drawString(font, getHealthString().getVisualOrderText(), 7, 35, fontColor, false);
        guiGraphics.drawString(font, getBatteryString().getVisualOrderText(), 95, 9, fontColor, false);
        guiGraphics.drawString(font, getTemperatureString().getVisualOrderText(), 95, 35, fontColor, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        renderBackground(guiGraphics);
        renderTooltip(guiGraphics, mouseX, mouseY);

        guiGraphics.blit(CAR_GUI_TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        drawFuel(guiGraphics, getFuelPercent());
        drawHealth(guiGraphics, getHealthPercent());
        drawBattery(guiGraphics, getBatteryPercent());
        drawTemp(guiGraphics, getTemperaturePercent());
    }

    // FUEL

    public float getFuelPercent() {
        float fuelPerc = ((float) ((ICarInvoker) car).car$getFuel() / ((float) ((ICarInvoker) car).car$getMaxFuel())) * 100F;
        return round(fuelPerc, 2);
    }

    public Component getFuelString() {
        return Component.translatable("gui.refueled_fuel", String.valueOf(getFuelPercent()));
    }

    public void drawFuel(GuiGraphics guiGraphics, float percent) {
        percent = Math.min(100F, percent);
        //72x10
        int scaled = (int) (72F * percent / 100D);
        int i = this.leftPos;
        int j = this.topPos;
        guiGraphics.blit(CAR_GUI_TEXTURE, i + 8, j + 20, 176, 0, scaled, 10);
    }

    // BATTERY

    public float getBatteryPercent() {
        return ((float) ((ICarInvoker) car).car$getBattery() / (float) ((ICarInvoker) car).car$getMaxBattery());
    }
    public Component getBatteryString() {
        return Component.translatable("gui.refueled_battery", String.valueOf((int) (getBatteryPercent() * 100f)));
    }

    public void drawBattery(GuiGraphics guiGraphics, float percent) {
        percent = Math.min(100F, percent);
        int scaled = (int) (72F * percent);
        int i = this.leftPos;
        int j = this.topPos;
        guiGraphics.blit(CAR_GUI_TEXTURE, i + 96, j + 20, 176, 20, scaled, 10);
    }

    // ENGINE TEMPERATURE

    public float getTemperatureCelsius() {
        return round(((ICarInvoker) car).car$getTemperature(), 2);
    }

    public float getTemperatureFarenheit() {
        return round((((ICarInvoker) car).car$getTemperature() * 1.8F) + 32F, 2);
    }

    public Component getTemperatureString() {
        if (ClientConfig.temperatureFahrenheit.get()) {
            return Component.translatable("gui.refueled_temperature_fahrenheit", String.valueOf(getTemperatureFarenheit()));
        } else {
            return Component.translatable("gui.refueled_temperature_celsius", String.valueOf(getTemperatureCelsius()));
        }
    }

    public float getTemperaturePercent() {
        float temp = ((ICarInvoker) car).car$getTemperature();
        if (temp > 100F) {
            temp = 100F;
        }
        if (temp < 0F) {
            temp = 0F;
        }
        return temp / 100F;
    }

    public void drawTemp(GuiGraphics guiGraphics, float percent) {
        percent = Math.min(100F, percent);
        int scaled = (int) (72F * percent);
        int i = this.leftPos;
        int j = this.topPos;
        guiGraphics.blit(CAR_GUI_TEXTURE, i + 96, j + 46, 176, 30, scaled, 10);
    }

    // CAR HEALTH

    public float getHealthPercent() {
        float health = (((ICarInvoker) car).car$getHealth() / ((ICarInvoker) car).car$getMaxHealth()) * 100f;
        health = Math.max(health, 0);
        return round(health, 2);
    }

    public Component getHealthString() {
        return Component.translatable("gui.refueled_health", String.valueOf(getHealthPercent()));
    }

    public void drawHealth(GuiGraphics guiGraphics, float percent) {
        percent = Math.min(100F, percent);
        int scaled = (int) (72F * percent / 100D);
        int i = this.leftPos;
        int j = this.topPos;
        guiGraphics.blit(CAR_GUI_TEXTURE, i + 8, j + 46, 176, 10, scaled, 10);
    }
}
