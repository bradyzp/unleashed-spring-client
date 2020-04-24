package net.jastrab.unleashedspringclient.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ReflectUtils {

    /**
     * Update beans of entity1 with any non-null values declared in entity2
     *
     * @param entity1  the entity to be updated with non-null values from entity2
     * @param entity2  the entity with values to be used for updating
     * @param excluded array of property names which should be excluded from updates
     * @param <T>      a java bean with getters/setters
     * @return reference to entity1
     */
    public static <T> T updateEntity(T entity1, T entity2, String... excluded) {

        List<String> excludedGetters = Arrays.stream(excluded)
                .map(String::toLowerCase)
                .map(value -> "get" + value)
                .collect(Collectors.toList());

        Arrays.stream(entity1.getClass().getDeclaredMethods())
                .filter(method -> method.getName().startsWith("get"))
                .filter(method -> !excludedGetters.contains(method.getName().toLowerCase()))
                .forEach(getter -> safeInvoke(getter, entity2).map(value -> {
                    try {
                        final Method setter = entity1.getClass()
                                .getDeclaredMethod(getter.getName().replaceFirst("get", "set"), getter.getReturnType());
                        return safeInvoke(setter, entity1, value);
                    } catch (NoSuchMethodException ignored) {
                        return Optional.empty();
                    }
                }));

        return entity1;
    }

    private static Optional<Object> safeInvoke(Method method, Object target, Object... args) {
        try {
            return Optional.ofNullable(method.invoke(target, args));
        } catch (IllegalAccessException | InvocationTargetException ignored) {
            return Optional.empty();
        }
    }
}
