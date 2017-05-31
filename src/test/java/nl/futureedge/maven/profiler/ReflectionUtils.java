package nl.futureedge.maven.profiler;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ReflectionUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReflectionUtils.class.getName());

    private ReflectionUtils() {
        throw new IllegalStateException("Do not instantiate");
    }

    /**
     * Instantiate a class; searches for the correct constructor (does not have to be public) based on the given arguments.
     *
     * @param clazzName class name
     * @param arguments arguments
     * @param <T> class type
     * @return class instance
     * @throws ReflectiveOperationException on errors
     */
    public static <T> T instantiate(final String clazzName, final Object... arguments) throws ReflectiveOperationException {
        LOGGER.debug("Instantiate: ", clazzName);
        final Class<T> clazz = (Class<T>) Class.forName(clazzName);
        final Constructor<T> constructor = findConstructor(clazz, arguments);
        boolean wasAccessible = constructor.isAccessible();
        constructor.setAccessible(true);
        T result = constructor.newInstance(arguments);
        constructor.setAccessible(wasAccessible);
        return result;
    }

    @SuppressWarnings("unchecked")
    private static <T> Constructor<T> findConstructor(final Class<T> clazz, final Object[] arguments) throws ReflectiveOperationException {
        final List<Constructor<?>> constructors = new ArrayList<>(Arrays.asList(clazz.getDeclaredConstructors()));
        LOGGER.debug("Constructors: " + constructors.size());
        removeConstructorWithInvalidArgumentLengths(constructors, arguments.length);
        LOGGER.debug("Constructors (after length check): " + constructors.size());
        for (int i = 0; i < arguments.length; i++) {
            removeConstructorWithIncompatibleArgument(constructors, i, arguments[i]);
        }
        LOGGER.debug("Constructors (after argument check): " + constructors.size());

        if (constructors.isEmpty()) {
            throw new NoSuchMethodException("No constructor found that matches with the given arguments for class: " + clazz.getName());
        }
        if (constructors.size() > 1) {
            throw new NoSuchMethodException("More than one 1 constructor found that matches with the given arguments for class: "
                    + clazz.getName());
        }

        return (Constructor<T>) constructors.get(0);
    }

    private static void removeConstructorWithInvalidArgumentLengths(final List<Constructor<?>> constructors, final int length) {
        final Iterator<Constructor<?>> iterator = constructors.iterator();
        while (iterator.hasNext()) {
            final Constructor<?> constructor = iterator.next();
            if (constructor.getParameterTypes().length != length) {
                iterator.remove();
            }
        }
    }

    private static void removeConstructorWithIncompatibleArgument(final List<Constructor<?>> constructors, final int i, final Object argument) {
        if (argument == null) {
            return;
        }

        final Iterator<Constructor<?>> iterator = constructors.iterator();
        while (iterator.hasNext()) {
            final Constructor<?> constructor = iterator.next();
            final Class<?> argumentType = constructor.getParameterTypes()[i];

            if (!argumentType.isInstance(argument)) {
                // System.out.println("Constructor verwijderd. Argumenttype: " + argumentType.getName() +
                // ", gegeven argument: " + argument.getClass().getName());
                iterator.remove();
            }
        }
    }

}
