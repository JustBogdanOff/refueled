package bogdan.refueled.server;

import bogdan.refueled.RefueledMain;
import bogdan.refueled.common.accessors.IVehicleAccess;
import bogdan.refueled.config.ServerConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static bogdan.refueled.Utils.isCar;

public class RefueledCommands {
    public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("refueled").requires(commandSource -> commandSource.hasPermission(2));

        command.then(
                Commands.literal("vehicle").then(
                        Commands.argument("entity", EntityArgument.entity()).then(
                                Commands.literal("set").then(
                                        Commands.literal("fuel").then(
                                                Commands.argument("type", FuelTypeArgument.id()).then(
                                                        Commands.argument("amount", IntegerArgumentType.integer(0)).executes(
                                                                commandContext -> {
                                                                    Entity vehicle = EntityArgument.getEntity(commandContext, "entity");
                                                                    if (isCar(vehicle)){
                                                                        int amount = commandContext.getArgument("amount", Integer.class);
                                                                        ResourceLocation fluid = commandContext.getArgument("type", ResourceLocation.class);
                                                                        ((IVehicleAccess) vehicle).refuel$setFuelType(fluid.toString());
                                                                        ((IVehicleAccess) vehicle).refuel$setFuel(amount);

                                                                        try {
                                                                            commandContext.getSource().sendSuccess(() -> Component.translatable("message.command.set.fuel.success", vehicle.getDisplayName().getString(), ForgeRegistries.FLUIDS.getValue(fluid).getFluidType().getDescription(), amount), false);
                                                                        } catch (NullPointerException e){
                                                                            RefueledMain.LOGGER.debug("Fluid forge registry is null when trying to use the set fuel command on entity {} ({})", vehicle.getDisplayName(), vehicle.getStringUUID());
                                                                        }
                                                                        return 1;
                                                                    }
                                                                    else
                                                                        commandContext.getSource().sendFailure(Component.translatable("message.command.fail.entity"));

                                                                    return 0;
                                                                }
                                                        )
                                                )
                                        )
                                ).then(
                                        Commands.literal("health").then(
                                                Commands.argument("amount", DoubleArgumentType.doubleArg(0)).executes(
                                                        commandContext -> {
                                                            Entity vehicle = EntityArgument.getEntity(commandContext, "vehicle");
                                                            if (isCar(vehicle)){
                                                                float health = commandContext.getArgument("amount", Double.class).floatValue();
                                                                ((IVehicleAccess) vehicle).refuel$setHealth(health);

                                                                commandContext.getSource().sendSuccess(() -> Component.translatable("message.command.set.health.success", vehicle.getDisplayName().getString(), health), false);
                                                                return 1;
                                                            }
                                                            else
                                                                commandContext.getSource().sendFailure(Component.translatable("message.command.fail.entity"));

                                                            return 0;
                                                })
                                        )
                                ).then(
                                        Commands.literal("energy").then(
                                                Commands.argument("amount", IntegerArgumentType.integer(0)).executes(
                                                        commandContext -> {
                                                            Entity vehicle = EntityArgument.getEntity(commandContext, "vehicle");
                                                            if (isCar(vehicle)){
                                                                int energy = commandContext.getArgument("amount", Integer.class);
                                                                ((IVehicleAccess) vehicle).refuel$setBattery(energy);

                                                                commandContext.getSource().sendSuccess(() -> Component.translatable("message.command.set.energy.success", vehicle.getDisplayName().getString(), energy), false);
                                                                return 1;
                                                            }
                                                            else
                                                                commandContext.getSource().sendFailure(Component.translatable("message.command.fail.entity"));

                                                            return 0;
                                                        }
                                                )
                                        )
                                ).then(
                                        Commands.literal("temperature").then(
                                                Commands.argument("amount", DoubleArgumentType.doubleArg()).executes(
                                                        commandContext -> {
                                                            Entity vehicle = EntityArgument.getEntity(commandContext, "vehicle");
                                                            if (isCar(vehicle)){
                                                                float heat = commandContext.getArgument("amount", Double.class).floatValue();
                                                                ((IVehicleAccess) vehicle).refuel$setTemperature(heat);

                                                                if(heat <= -273.15)
                                                                    commandContext.getSource().sendSuccess(() -> Component.translatable("message.command.set.heat.success.abszero", vehicle.getDisplayName().getString()), false);
                                                                else
                                                                    commandContext.getSource().sendSuccess(() -> Component.translatable("message.command.set.heat.success", vehicle.getDisplayName().getString(), heat), false);
                                                                return 1;
                                                            }
                                                            else
                                                                commandContext.getSource().sendFailure(Component.translatable("message.command.fail.entity"));

                                                            return 0;
                                                        }
                                                )
                                        )
                                )
                ))
        ).then(
                Commands.literal("serverconfig").then(
                        Commands.literal("add").then(
                                Commands.argument("type", BlockStateArgument.block(context)).then(
                                        Commands.argument("efficiency", DoubleArgumentType.doubleArg()).executes(
                                                commandContext -> {
                                                    var input = commandContext.getArgument("type", BlockInput.class);
                                                    ResourceLocation type = ForgeRegistries.FLUIDS.getKey(input.getState().getFluidState().getType());
                                                    if(type != null){
                                                        List<List<String>> newList = new ArrayList<>(ServerConfig.fuelEff.get());
                                                        Double efficiency = commandContext.getArgument("efficiency", Double.class);
                                                        for(int i = 0; i < newList.size(); i++){
                                                            var keyValue = newList.get(i);
                                                            if(Objects.equals(keyValue.get(0), type.toString())){
                                                                if(efficiency == Double.parseDouble(keyValue.get(1))){
                                                                    commandContext.getSource().sendFailure(Component.translatable("message.command.fail.config.exists", ForgeRegistries.FLUIDS.getValue(type).getFluidType().getDescription(), String.valueOf(efficiency)));
                                                                    return 0;
                                                                }
                                                                else if(efficiency > 0){
                                                                    newList.set(i, List.of(type.toString(), String.valueOf(efficiency)));
                                                                    ServerConfig.fuelEff.set(newList);
                                                                    ServerConfig.fuelEff.save();
                                                                    commandContext.getSource().sendSuccess(() -> Component.translatable("message.command.success.config", ForgeRegistries.FLUIDS.getValue(type).getFluidType().getDescription(), efficiency), false);
                                                                    return 1;
                                                                }
                                                                else {
                                                                    commandContext.getSource().sendFailure(Component.translatable("message.command.fail.config.efficiency", efficiency));
                                                                    return 0;
                                                                }
                                                            }
                                                        }


                                                        if(efficiency > 0){
                                                            newList.add(List.of(type.toString(), String.valueOf(efficiency)));
                                                            ServerConfig.fuelEff.set(newList);
                                                            ServerConfig.fuelEff.save();
                                                            commandContext.getSource().sendSuccess(() -> Component.translatable("message.command.success.config", ForgeRegistries.FLUIDS.getValue(type).getFluidType().getDescription(), efficiency), false);
                                                            return 1;
                                                        }
                                                        else commandContext.getSource().sendFailure(Component.translatable("message.command.fail.config.efficiency", efficiency));
                                                    }
                                                    else commandContext.getSource().sendFailure(Component.translatable("message.command.fail.config.location", input.getState().getBlock().getName()));

                                                    return 0;
                                                }
                                        )
                                )
                        )
                )
        );

        dispatcher.register(command);
    }

    @SubscribeEvent
    public void registerCommand(RegisterCommandsEvent event){
        this.register(event.getDispatcher(), event.getBuildContext());
    }
}
