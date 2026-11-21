package arnett.customItemsAPI.items.data.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)

/**
 * Makes a method as adding a data entry (or multiple) to the automatically handled placement data.
 * MUST return a ItemDataEnrty object and have ZERO parameters.
 */
public @interface ProvidePlacementDataEntry {
}
