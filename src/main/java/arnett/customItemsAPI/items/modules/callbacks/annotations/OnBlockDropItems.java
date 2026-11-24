package arnett.customItemsAPI.items.modules.callbacks.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)

/**
 * Called when this placement is broken, takes one BLOCK parameter for the block broken and returns a List<ItemStack>
 *     containing each extra item to be dropped
 */
public @interface OnBlockDropItems {
}
