package arnett.customItemsAPI.items.modules.callbacks;

import arnett.customItemsAPI.items.ItemLibrary;
import arnett.customItemsAPI.items.modules.callbacks.annotations.ItemEvent;
import com.google.common.collect.HashBasedTable;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.bukkit.event.Event;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiConsumer;

public class ItemCallbackManager {

    static HashBasedTable<Class<? extends ItemLibrary>, Class<? extends Event>, LinkedHashMap<Method, ItemEvent>>
            eventCallbacks = HashBasedTable.create();
    static HashBasedTable<Class<?>, Class<? extends Annotation>, HashMap<Method, Annotation>> trackedCallbacks = HashBasedTable.create();
    static HashSet<Class<? extends Annotation>> trackedAnnotationTypes = new HashSet<>();


    //region Tracking

    /*=================================================================================================
                        -  Tracking  -
    =================================================================================================*/

    /**
     * Registers an annotation class as one that is tracked for callback checks (MUST BE ADDED BEFORE ITEM IS REGISTERED)
     */
    public static void trackAnnotation(Class<? extends Annotation> type)
    {
        trackedAnnotationTypes.add(type);
    }

    //endregion


    //region Callbacks

    /*=================================================================================================
                        -  Callbacks  -
    =================================================================================================*/

    public static void loadTackedCallbacks(Class<?> clazz)
    {
        //go through each method to store callbacks (so it doesn't have to search at runtime)
        for(Method method : clazz.getMethods())
        {
            //get annotations of override methods filtered by there types
            HashSet<Annotation> annotations = new HashSet<>();
            MethodUtils.getOverrideHierarchy(method, ClassUtils.Interfaces.INCLUDE).forEach(higherarchyMethod -> {
                annotations.addAll(Arrays.asList(higherarchyMethod.getAnnotations()));
            });

            for (Annotation annotation : annotations) {
                if(trackedAnnotationTypes.contains(annotation.annotationType()))
                {
                    trackedCallbacks.row(clazz).computeIfAbsent(
                            annotation.annotationType(),
                            (a) -> new HashMap<>()
                    ).put(method, annotation);
                }
            }
        }
    }
    /**
     * Invokes callbacks of type
     * @param object object to invoke callbacks on
     * @param type type of callbacks to invoke
     * @param foreachResult What to do with the result
     * @param params What parameters to pass
     */
    public static void invokeCallbacks(Object object, Class<? extends Annotation> type, BiConsumer<Object, Annotation> foreachResult, Object... params)
    {
        var callbacks = trackedCallbacks.row(object.getClass()).getOrDefault(type, new HashMap<>(0));
        System.out.println("Invoking Callback " + object.getClass().getSimpleName() + ":" + type.getTypeName() + " - " + callbacks.size());

        callbacks.forEach((method, annotation) -> {
            try {
                if(foreachResult != null)
                    foreachResult.accept(method.invoke(object, params), annotation);
                else
                    method.invoke(object, params);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        });
    }


    /**
     * Tracks all callbacks for this item library
     */
    static public void loadEventCallbacks(Class<? extends ItemLibrary> library)
    {
        //go through each method to store callbacks (so it doesn't have to search at runtime)
        for(Method method : library.getMethods())
        {
            //get annotations of override methods filtered by there types
            HashSet<Annotation> annotations = new HashSet<>();
            MethodUtils.getOverrideHierarchy(method, ClassUtils.Interfaces.INCLUDE).forEach(higherarchyMethod -> {
                annotations.addAll(Arrays.asList(higherarchyMethod.getAnnotations()));
            });

            //sort annotations by priority
            var sortedAnnotations = annotations.stream().filter(
                    annotation -> annotation instanceof ItemEvent
            ).map(ItemEvent.class::cast).sorted(Comparator.comparing(ItemEvent::priority)).toList();

            for (ItemEvent annotation : sortedAnnotations)
            {
                var first = method.getParameterTypes()[0];
                if (Event.class.isAssignableFrom(first))
                {
                    //add it to the event callbacks by its first parameter type which should be an event extension
                    eventCallbacks.row(library).computeIfAbsent((Class<? extends Event>) first,
                            ann -> new LinkedHashMap<>()).put(method, annotation);
                    System.out.println("REGISTERING CALLBACK " + first.getSimpleName() + ":" + annotation.value());
                    break;
                }
                else
                {
                    System.out.println("NOT EVENT " + method.getName());
                }
            }
        }
    }


    /**
     * Invokes callbacks of type
     * @param library library of callbacks to invoke
     * @param params other parameters if required
     */
    public static void invokeEventCallbacks(ItemLibrary library, Event event, String altTag, Object... params)
    {

        var callbacks = eventCallbacks.row(library.getClass()).getOrDefault(event.getClass(), new LinkedHashMap<>(0));
        System.out.println("Invoking Callback " + library.name() + " " + event.getEventName() + ":" + altTag + " - " + callbacks.size());

        callbacks.forEach((method, annotation) -> {

            if(!annotation.value().equals(altTag))
                return;

            try {
                if(params.length == 0)
                {
                    method.invoke(library, event);
                }
                else
                {
                    var combinedParams = new Object[params.length + 1];
                    combinedParams[0] = event;
                    for (int i = 0; i < params.length; i++) {
                        combinedParams[i + 1] = params[i];
                    }
                    method.invoke(library, combinedParams);
                }
            } catch (Exception e) {
                System.err.println("Unable to invoke " + library.name() + "'s " + event.getEventName() + ":" + altTag + " function.");
                System.err.println("Params: ");
                for (int i = 0; i < params.length && i < method.getParameterTypes().length - 1; i++) {
                    System.err.println("sent " + params[i].getClass().getSimpleName());
                    System.err.println("in place of " + method.getParameterTypes()[i + 1].getSimpleName());
                }
                if(params.length + 1 != method.getParameterTypes().length)
                {
                    System.err.println("Mismatch in size, sent: " + params.length + ", needed: " + method.getParameterTypes().length);
                }
                e.printStackTrace();
            }
        });
    }


    //region Overloads

    /*=================================================================================================
                        -  Overloads  -
    =================================================================================================*/

    /**
     * Invokes callbacks of type
     * @param object object to invoke callbacks on
     * @param type type of callbacks to invoke
     */
    public static void invokeCallbacks(Object object, Class<? extends Annotation> type) {
        invokeCallbacks(object, type, null);
    }

    /**
     * Invokes callbacks of type
     * @param library library of callbacks to invoke
     */
    public static void invokeEventCallbacks(ItemLibrary library, Event event)
    {
        invokeEventCallbacks(library, event, "");
    }

    //endregion

    //endregion
}
