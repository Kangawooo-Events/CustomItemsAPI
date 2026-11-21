package arnett.customItemsAPI.items.data;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

public record ItemDataEntry<P, C>(
        NamespacedKey namespacedKey,
        PersistentDataType<P, C> dataType,
        C defaultValue
) {

}
