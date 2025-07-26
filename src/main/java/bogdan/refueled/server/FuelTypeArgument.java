package bogdan.refueled.server;

import bogdan.refueled.RefueledMain;
import bogdan.refueled.config.ServerConfig;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class FuelTypeArgument implements ArgumentType<ResourceLocation> {
    private static final Collection<String> EXAMPLES = Arrays.asList("minecraft:lava", "create:honey", "minecraft:empty");

    @Override
    public ResourceLocation parse(StringReader reader) throws CommandSyntaxException {
        return ResourceLocation.read(reader);
    }

    public static FuelTypeArgument id(){
        return new FuelTypeArgument();
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        S s = context.getSource();

        if (s instanceof SharedSuggestionProvider) {
            Collection<String> collection = new ArrayList<>(List.of());
            ServerConfig.fuelEff.get().forEach(list -> collection.add(list.get(0)));
            try {
                //noinspection DataFlowIssue
                collection.add(ForgeRegistries.FLUIDS.getKey(Fluids.EMPTY).toString());
            } catch (NullPointerException e) {
                RefueledMain.LOGGER.debug("Null fluids forge registry when suggesting fuels for a command");
            }
            return SharedSuggestionProvider.suggest(collection, builder);
        }

        return Suggestions.empty();
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
