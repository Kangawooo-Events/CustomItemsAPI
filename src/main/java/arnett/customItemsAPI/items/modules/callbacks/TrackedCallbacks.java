package arnett.customItemsAPI.items.modules.callbacks;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

public class TrackedCallbacks {

    static Set<Class<? extends Annotation>> trackedCallbacks;

    /**
     * Adds an annotation class to one which is tracked in item library classes, must be added before registration to
     * matter
     */
    public static void trackCallbackType(Class<? extends Annotation> callbackType)
    {
        trackedCallbacks.add(callbackType);
    }

    /**
     * checks if an annotation class is one which is tracked in item library classes
     */
    public static boolean isTracked(Class<? extends Annotation> callbackType)
    {
        return trackedCallbacks.contains(callbackType);
    }

    //not implemented yet. meant for ItemLibrary maybe
    //region Callbacks

    /*=================================================================================================
                        -  Callbacks  -
    =================================================================================================*/

    HashMap<Class<? extends Annotation>, HashMap<Method, Annotation>> callbacks = new HashMap<>();

    /**
     * Tracks all callbacks for this item library
     */
    public void loadCallbacks()
    {
        //go through each method to store callbacks (so it doesn't have to search at runtime)
        for(Method method : getClass().getMethods())
        {

            //get annotations of override methods filtered by there types
            HashSet<Annotation> annotations = new HashSet<>();
            MethodUtils.getOverrideHierarchy(method, ClassUtils.Interfaces.INCLUDE).forEach(higherarchyMethod -> {
                annotations.addAll(Arrays.asList(higherarchyMethod.getAnnotations()));
            });

            for (Annotation annotation : annotations)
            {
                if(TrackedCallbacks.isTracked(annotation.annotationType()))
                    callbacks.computeIfAbsent(annotation.annotationType(),
                            ann -> new HashMap<>()).put(method, annotation);
            }
        }
    }

    /**
     * Invokes callbacks of type
     * @param type type of callbacks to invoke
     * @param foreachResult What to do with the result
     * @param params What parameters to pass
     */
    public final void invokeCallbacks(Class<? extends Annotation> type, BiConsumer<Object, Annotation> foreachResult, Object... params)
    {
        callbacks.getOrDefault(type, new HashMap<>(0)).forEach((method, annotation) -> {
            try {
                if(foreachResult != null)
                    foreachResult.accept(method.invoke(this, params), annotation);
                else
                    method.invoke(this, params);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        });
    }

    //region Overloads

    /*=================================================================================================
                        -  Overloads  -
    =================================================================================================*/

    /**
     * Invokes callbacks of type
     * @param type type of callbacks to invoke
     */
    public final void invokeCallbacks(Class<? extends Annotation> type) {
        invokeCallbacks(type, null);
    }


    //endregion

    //endregion
}
