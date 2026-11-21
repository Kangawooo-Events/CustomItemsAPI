package arnett.customItemsAPI.items.modules.callbacks.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)

/**
 * Calls method to make changes to ItemMeta<br>
 * MUST have ONE ItemMeta parameter.
 */
public @interface ModifyNewItemMeta {
}
