package bogdan.refueled.client.gui;

import bogdan.refueled.RefueledMain;
import bogdan.refueled.common.accessors.IVehicleAccess;
import bogdan.refueled.common.gui.CarGUI;
import bogdan.refueled.config.ClientConfig;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static bogdan.refueled.Utils.round;

public class CarGUIScreen extends AbstractContainerScreen<CarGUI> {
    private static final ResourceLocation CAR_GUI_TEXTURE = new ResourceLocation(RefueledMain.MODID, "textures/gui/vehicle_gui.png");

    private static final int fontColor = 4210752;

    private final Inventory playerInv;
    private final Entity car;

    public CarGUIScreen(CarGUI carGUI, Inventory playerInv, Component title) {
        super(carGUI, playerInv, title);
        this.playerInv = playerInv;
        this.car = carGUI.car;

        imageWidth = 176;
        imageHeight = 218;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, car.getDisplayName().getVisualOrderText(), 7, 55, fontColor, false);
        guiGraphics.drawString(font, playerInv.getDisplayName().getVisualOrderText(), 7, 123, fontColor, false);
        renderTooltip(guiGraphics, mouseX - leftPos, mouseY - topPos);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        renderBackground(guiGraphics);
        guiGraphics.blit(CAR_GUI_TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    private Label fuelLabel, healthLabel, batteryLabel, tempLabel;

    @Override
    protected void init(){
        super.init();

        fuelLabel = addRenderableWidget(new Label(Label.FUEL, font, leftPos, topPos, fontColor));
        healthLabel = addRenderableWidget(new Label(Label.HEALTH, font, leftPos, topPos, fontColor));
        batteryLabel = addRenderableWidget(new Label(Label.BATTERY, font, leftPos, topPos, fontColor));
        tempLabel = addRenderableWidget(new Label(Label.TEMPERATURE, font, leftPos, topPos, fontColor));

        addRenderableWidget(new Button(leftPos + 127, topPos + 7, 19, 19, false));
        addRenderableWidget(new Button(leftPos + 150, topPos + 7, 19, 19, true));
    }

    @Override
    protected void containerTick() {
        if(ClientConfig.pinnedType.get() != 0 && (!fuelLabel.isHovered() && !healthLabel.isHovered() && !batteryLabel.isHovered() && !tempLabel.isHovered())){
            ticksSinceAnyHover = Math.min(3, ticksSinceAnyHover + 1);
        }
        else ticksSinceAnyHover = 0;

        super.containerTick();
    }

    private int ticksSinceAnyHover = 0;

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if(ClientConfig.pinnedType.get() != 0 && !(fuelLabel.isHovered() || healthLabel.isHovered() || batteryLabel.isHovered() || tempLabel.isHovered())){
            var type = ClientConfig.pinnedType.get() - 1;
            var texOffset = Mth.floor((40d * fuelLabel.getPercent(type) * 0.01 * ticksSinceAnyHover) / 3);

            guiGraphics.blit(CAR_GUI_TEXTURE, leftPos + 8, topPos + 48 - texOffset, 176 + 11 * type, 40 - texOffset, 11, texOffset);
        }
    }

    private static class Button extends AbstractButton{
        public Button(int pX, int pY, int pWidth, int pHeight, boolean displayTemp) {
            super(pX, pY, pWidth, pHeight, displayTemp ? Component.literal(ClientConfig.temperatureFahrenheit.get() ? "°F" : "°C") : Component.literal(ClientConfig.displayInUnits.get() ? "/" : "%"));
            this.displayTemp = displayTemp;
            this.isDefault = displayTemp ? ClientConfig.temperatureFahrenheit.get() : ClientConfig.displayInUnits.get();
        }
        private final boolean displayTemp;
        private boolean isDefault;

        @Override
        public void onPress() {
            if(isDefault) {
                if(displayTemp){
                    setMessage(Component.literal("°C"));
                    ClientConfig.temperatureFahrenheit.set(false);
                }
                else{
                    setMessage(Component.literal("%"));
                    ClientConfig.displayInUnits.set(false);
                }
                isDefault = false;
            }
            else{
                if(displayTemp){
                    setMessage(Component.literal("°F"));
                    ClientConfig.temperatureFahrenheit.set(true);
                }
                else{
                    setMessage(Component.literal("/"));
                    ClientConfig.displayInUnits.set(true);
                }

                isDefault = true;
            }
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            this.setFocused(false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            narrationElementOutput.add(NarratedElementType.TITLE, this.getMessage());
        }
    }

    private class Label extends AbstractWidget{
        public Label(int type, Font font, int pX, int pY, int fontColor) {
            super(pX + 25, pY + 8 + 11 * type, font.width(new Component[][]{getFuelString(), getHealthString(), getBatteryString(), getTemperatureString()}[type][isOtherwise(type)]), font.lineHeight, Component.empty());
            this.type = type;
            this.font = font;
            this.fontColor = fontColor;
            this.oX = pX;
            this.oY = pY;
            this.string = new Component[][]{getFuelString(), getHealthString(), getBatteryString(), getTemperatureString()}[type];
        }

        public static final int FUEL = 0, HEALTH = 1, BATTERY = 2, TEMPERATURE = 3;

        private static int isOtherwise(int type){
            return (type == TEMPERATURE && ClientConfig.temperatureFahrenheit.get()) || (type != TEMPERATURE && ClientConfig.displayInUnits.get()) ? 0 : 1;
        }

        // FUEL: 25, 8
        // HEALTH: 25, 19
        // BATTERY: 25, 30
        // TEMP: 25, 41
        public final int type;
        private final int fontColor, oX, oY;
        private final Font font;
        private final Component[] string;

        private float getMax(int type){
            return new float[]{((IVehicleAccess) car).refuel$getMaxFuel(), ((IVehicleAccess) car).refuel$getMaxHealth(), ((IVehicleAccess) car).refuel$getMaxBattery(), 100f}[type];
        }

        private float getNumber(int type){
            return new float[]{getFuel(), getHealth(), getBattery(), getTemperature()}[type];
        }

        public float getPercent(int type){
            if(type == TEMPERATURE) return Mth.clamp(((((IVehicleAccess) car).refuel$getTemperature() + 50) / 150) * 100, 0, 100);
            return Mth.clamp((getNumber(type) / getMax(type)) * 100, 0, 100);
        }

        private int lastTick = 0, lastHoverTick = 0;
        private boolean lastHovered = false;

        @Override
        protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            final var unit = getNumber(type);
            final var percent = getPercent(type);

            var offset = Mth.floor(40d * percent * 0.01);
            if(isHovered) {
                var texOffset = Mth.floor(Mth.lerp(partialTick, 40d * percent * 0.01 * 0.25 * (lastHovered ? Math.min(4, lastTick - lastHoverTick + 1) : 0), 40d * percent * 0.01 * 0.25 * (lastHovered ? Math.min(4, car.tickCount - lastHoverTick + 1) : 0)));
                guiGraphics.blit(CAR_GUI_TEXTURE, oX + 8, oY + 48 - texOffset, 176 + 11 * type, 40 - texOffset, 11, texOffset);
            }
            else if(car.tickCount != lastTick) lastHoverTick = car.tickCount;
            lastTick = car.tickCount;
            lastHovered = isHovered;

            String number = String.valueOf(round(percent, 2));
            if(type == TEMPERATURE) number = String.valueOf(getTemperature());
            else if(ClientConfig.displayInUnits.get()) number = unit == Math.floor(unit) ? String.valueOf(Mth.floor(unit)) : String.valueOf(round(unit,2));

            var color = fontColor;
            if(isHovered) try {
                var RGBA = NativeImage.read(Minecraft.getInstance().getResourceManager().open(CAR_GUI_TEXTURE)).getPixelRGBA(176 + 11 * type, 40 - offset);
                    color = ((((((RGBA >> 24 & 255) << 8) + (RGBA & 255)) << 8) + (RGBA >> 8 & 255)) << 8) + (RGBA >> 16 & 255);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            guiGraphics.drawString(font, (ClientConfig.pinnedType.get() == type + 1 ? String.valueOf(ChatFormatting.PREFIX_CODE) + ChatFormatting.UNDERLINE.getChar() : "") + number, getX(), getY(), color, false);
            guiGraphics.drawString(font, string[isOtherwise(type)], getX() + font.width(number), getY(), fontColor, false);
            setWidth(font.width(number));
        }

        @Override
        protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {}

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if(this.clicked(mouseX, mouseY)){
                if(ClientConfig.pinnedType.get() != type + 1) ClientConfig.pinnedType.set(type + 1);
                else ClientConfig.pinnedType.set(0);
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }



    public Component[] getFuelString() {
        return new Component[]{
                Component.translatable("gui.refueled.fuel.unit", ((IVehicleAccess) car).refuel$getMaxFuel()),
                Component.translatable("gui.refueled.fuel")
        };
    }

    public Component[] getHealthString() {
        var max = ((IVehicleAccess) car).refuel$getMaxHealth();
        return new Component[]{
                Component.translatable("gui.refueled.health.unit", (max == Math.floor(max) ? String.valueOf(Mth.floor(max)) : max)),
                Component.translatable("gui.refueled.health")
        };
    }

    public Component[] getBatteryString(){
        return new Component[]{
                Component.translatable("gui.refueled.battery.unit", ((IVehicleAccess) car).refuel$getMaxBattery()),
                Component.translatable("gui.refueled.battery")
        };
    }

    public Component[] getTemperatureString() {
        return new Component[]{
                Component.literal("°F"),
                Component.literal("°C")
        };
    }

    public int getFuel(){
        return ((IVehicleAccess) car).refuel$getFuel();
    }

    public float getHealth(){
        return round(((IVehicleAccess) car).refuel$getHealth(), 2);
    }

    public int getBattery(){
        return ((IVehicleAccess) car).refuel$getBattery();
    }

    public float getTemperature() {
        if(ClientConfig.temperatureFahrenheit.get()) return round((((IVehicleAccess) car).refuel$getTemperature() * 1.8F) + 32F, 2);

        return round(((IVehicleAccess) car).refuel$getTemperature(), 2);
    }
}
