package arnett.customItemsAPI.CustomItems;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public abstract class CustomItemData {

    protected JavaPlugin plugin;

    public abstract NamespacedKey getIdentifier();

    public static NamespacedKey customItemTag = new NamespacedKey("customitems", "customitem");

    public abstract NamespacedKey getItemModelKey();

    public List<Listener> getListeners()
    {
        return List.of();
    }

    public List<String> giveCommandArguments()
    {
        return List.of();
    }

    public abstract Material getBaseMaterial();

    public abstract String getName();

    public ItemStack getItem() {
        //create item stack of config set material
        ItemStack safe = ItemStack.of(getBaseMaterial());

        //change the item model
        ItemMeta meta = safe.getItemMeta();

        meta.setItemModel(getItemModelKey());

        meta.getPersistentDataContainer().set(getIdentifier(), PersistentDataType.BOOLEAN, true);

        //tag it as a custom item with a namespace which can be easily accessed later to get the specific item type
        meta.getPersistentDataContainer().set(customItemTag, PersistentDataType.STRING, getIdentifier().toString());

        safe.setItemMeta(meta);

        return safe;
    }

    public LiteralArgumentBuilder<CommandSourceStack> getGiveCommand()
    {
        return Commands.literal(getName()).executes(context -> {

            //is this being sent by a player (since we need to have a player to give the item to)
            if (context.getSource().getSender() instanceof Player player)
            {

                player.give(getItem());

                //successful execution
                return Command.SINGLE_SUCCESS;
            }
            else
            {
                //throw the error to player
                throw new SimpleCommandExceptionType(MessageComponentSerializer.message().serialize(
                        Component.text("Must be sent by a player")
                )).create();

            }
        });
    }

    public List<Recipe> getRecipes() {
        return List.of();
    }

    public List<NamespacedKey> getRecipeKeys(){
        return List.of();
    }

    public void setPlugin(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isItem(ItemStack stack)
    {
        //start with general material check
        if(!stack.getType().equals(getBaseMaterial()))
            return false;

        return stack.getPersistentDataContainer().has(getIdentifier());
    }
}
