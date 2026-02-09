package arnett.customItemsAPI.Commands;

import arnett.cattamands.CommandArgument;
import arnett.cattamands.LeafCommand;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;

import java.util.List;
import java.util.Map;

public class GiveCommand extends LeafCommand {

    @Override
    public int execute(CommandContext<CommandSourceStack> commandContext) {
        return 0;
    }

    @Override
    public List<CommandArgument> getArguments() {
        return List.of();
    }

    @Override
    public String getName() {
        return "give";
    }

    @Override
    public String getPermission() {
        return "op";
    }

    @Override
    public String getDescription() {
        return "Gives the item";
    }
}
