package arnett.customItemsAPI.items.modules.callbacks.annotations;


import arnett.customItemsAPI.items.modules.callbacks.ItemEventPriority;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Callback indicator used for ItemLibrary
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ItemEvent {

    /**
     * @return Tag for identification used in case you want to have an alternative or more specific invocation
     */
    String value() default "";


    ItemEventPriority priority() default ItemEventPriority.NORMAL;

}
