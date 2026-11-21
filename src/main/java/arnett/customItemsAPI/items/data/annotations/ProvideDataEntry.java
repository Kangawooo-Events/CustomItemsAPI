package arnett.customItemsAPI.items.data.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)

/**
 * Makes a method as adding a data entry to the automatically handled item data, data which is stored on
 * the item and transferred to and from the custom placement when placed and broken<br>
 * MUST return a ItemDataEnrty object and have ZERO parameters.
 */
public @interface ProvideDataEntry {
}
