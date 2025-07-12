package bogdan.refueled.server;

import bogdan.refueled.common.accessors.IVehicleAccess;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import static bogdan.refueled.Utils.isCar;

public class RefueledCommands {
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("refueled").requires(commandSource -> commandSource.hasPermission(2));

        command.then(
                Commands.argument("vehicle", EntityArgument.entity()).then(
                        Commands.literal("set").then(
                                Commands.literal("fuel").then(
                                        Commands.argument("type", FuelTypeArgument.id()).then(
                                                Commands.argument("amount", IntegerArgumentType.integer(0)).executes(
                                                        commandContext -> {
                                                            Entity vehicle = EntityArgument.getEntity(commandContext, "vehicle");
                                                            if (isCar(vehicle)){
                                                                int amount = commandContext.getArgument("amount", Integer.class);
                                                                ((IVehicleAccess) vehicle).refuel$setFuel(amount);
                                                                ResourceLocation fluid = commandContext.getArgument("type", ResourceLocation.class);
                                                                ((IVehicleAccess) vehicle).refuel$setFuelType(fluid.toString());

                                                                commandContext.getSource().sendSuccess(() -> Component.translatable("message.command.set.fuel.success", vehicle.getDisplayName().getString(), ForgeRegistries.FLUIDS.getValue(fluid).getFluidType().getDescription(), amount), false);
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
                )
        );

        dispatcher.register(command);
    }

    @SubscribeEvent
    public void registerCommand(RegisterCommandsEvent event){
        this.register(event.getDispatcher());
    }
}
