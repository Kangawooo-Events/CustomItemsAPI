package arnett.customItemsAPI.items.modules.callbacks.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)

/**
 * Called when this item is placed when the interaction is craeted.
 * Must have 3 parameters, an Interaction, ItemStack, and Player for the placed interaction, placed item and placer
 * respectively.
 */
public @interface OnInteractionPlaced {
}
