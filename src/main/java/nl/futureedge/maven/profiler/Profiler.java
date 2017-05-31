package nl.futureedge.maven.profiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.eventspy.AbstractEventSpy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Profiler.
 */
@Named
@Singleton
public final class Profiler extends AbstractEventSpy {

    private static final Logger LOGGER = LoggerFactory.getLogger(Profiler.class.getName());

    private ThreadLocal<Map<String, Long>> starts = ThreadLocal.withInitial(HashMap::new);
    private SortedMap<String, List<Long>> executions = Collections.synchronizedSortedMap(new TreeMap<>());
    private SortedSet<String> unsupportedEvents = Collections.synchronizedSortedSet(new TreeSet<>());

    @Override
    public void close() {
        if (!executions.isEmpty()) {
            LOGGER.info("Execution times:");
            final SortedMap<Long, String> displays = new TreeMap<>(Comparator.reverseOrder());
            for (final Map.Entry<String, List<Long>> execution : executions.entrySet()) {
                createDisplay(displays, execution.getKey(), execution.getValue());
            }

            for (final String display : displays.values()) {
                LOGGER.info(display);
            }
        }

        if (!unsupportedEvents.isEmpty()) {
            LOGGER.info("------------------------------------------------------------------------");
            LOGGER.info("Unsupported events encountered:");
            for (String unsupportedEvent : unsupportedEvents) {
                LOGGER.info(" - {}", unsupportedEvent);
            }
        }
        LOGGER.info("------------------------------------------------------------------------");
    }

    private void createDisplay(final SortedMap<Long, String> displays, final String key, final List<Long> values) {
        long fastest = Long.MAX_VALUE;
        long slowest = Long.MIN_VALUE;
        long total = 0;

        for (final Long value : values) {
            fastest = fastest < value ? fastest : value;
            slowest = slowest > value ? slowest : value;
            total += value;
        }

        final long average = total / values.size();
        final String display = String.format("[%1$s] executions: %3$3d, min: %4$s, max: %5$s, avg: %6$s - %2$s",
                format(total), key, values.size(), format(fastest), format(slowest), format(average));

        displays.put(total, display);
    }

    /**
     * Format a duration to a fixed length.
     * <ul>
     * <li>XX.XXX sec (for under 100 seconds)</li>
     * <li> XX:XX min (for under 100 minutes)</li>
     * <li>XXX:XX hrs (for the rest)</li>
     * </ul>
     * @param durationInMillis duration in milliseconds
     * @return formatted duration
     */
    private String format(final long durationInMillis) {
        final long durationInSeconds = durationInMillis / 1000;
        if (durationInSeconds < 100) {
            // Under 100 seconds
            final long millis = durationInMillis % 1000;
            return String.format("%1$2d.%2$03d sec", durationInSeconds, millis);
        } else {
            final long durationInMinutes = durationInSeconds / 60;
            if (durationInMinutes < 100) {
                final long seconds = durationInSeconds % 60;
                return String.format("%1$3d:%2$02d min", durationInMinutes, seconds);
            } else {
                final long durationInHours = durationInMinutes / 60;
                final long minutes = durationInMinutes % 60;
                return String.format("%1$3d:%2$02d hrs", durationInHours, minutes);
            }
        }
    }

    @Override
    public void onEvent(final Object object) {
        Event event = determineEvent(object);
        if (event == null) {
            unsupportedEvents.add(object.getClass().getName());

        } else if (event.getIdentifier() != null) {
            if (event.isStart()) {
                starts.get().put(event.getIdentifier(), System.currentTimeMillis());
            } else {
                if (starts.get().containsKey(event.getIdentifier())) {
                    final long duration = System.currentTimeMillis() - starts.get().get(event.getIdentifier());
                    executions.computeIfAbsent(event.getIdentifier(), key -> Collections.synchronizedList(new ArrayList<>()));
                    executions.get(event.getIdentifier()).add(duration);
                } else {
                    // Uncorrelated end
                    LOGGER.warn("Received end event for event type that was not started: " + event.getIdentifier());
                }
            }
        }
    }

    private Event determineEvent(final Object event) {
        final Set<String> classesAndInterfaces = determineClassesAndInterfaces(event.getClass());

        final Event result;
        if (classesAndInterfaces.contains("org.eclipse.aether.RepositoryEvent")) {
            result = determineRepositoryEvent(event);
        } else if (classesAndInterfaces.contains("org.apache.maven.execution.ExecutionEvent")) {
            result = determineExectionEvent(event);
        } else if (classesAndInterfaces.contains("org.apache.maven.settings.building.SettingsBuildingRequest")) {
            result = new Event("maven:settings-building", true);
        } else if (classesAndInterfaces.contains("org.apache.maven.settings.building.SettingsBuildingResult")) {
            result = new Event("maven:settings-building", false);
        } else if (classesAndInterfaces.contains("org.apache.maven.toolchain.building.ToolchainsBuildingRequest")) {
            result = new Event("maven:toolchains-building", true);
        } else if (classesAndInterfaces.contains("org.apache.maven.toolchain.building.ToolchainsBuildingResult")) {
            result = new Event("maven:toolchains-building", false);
        } else if (classesAndInterfaces.contains("org.apache.maven.project.DependencyResolutionRequest")) {
            result = new Event("maven:dependency-resolution", true);
        } else if (classesAndInterfaces.contains("org.apache.maven.project.DependencyResolutionResult")) {
            result = new Event("maven:dependency-resolution", false);
        } else if (classesAndInterfaces.contains("org.apache.maven.execution.MavenExecutionRequest")) {
            result = new Event(null, true);
        } else if (classesAndInterfaces.contains("org.apache.maven.execution.MavenExecutionResult")) {
            result = new Event(null, false);
        } else {
            result = null;
        }
        return result;
    }

    private Set<String> determineClassesAndInterfaces(final Class<?> clazz) {
        final Set<String> result = new HashSet<>();
        Class<?> theClass = clazz;
        while (theClass != null) {
            for (final Class<?> theInterface : theClass.getInterfaces()) {
                result.add(theInterface.getName());
            }
            result.add(theClass.getName());
            theClass = theClass.getSuperclass();
        }
        return result;
    }


    private Event determineRepositoryEvent(final Object event) {
        try {
            final Class<?> repositoryClass = Class.forName("org.eclipse.aether.RepositoryEvent");
            final String eventType = repositoryClass.getMethod("getType").invoke(event).toString();

            final Event result;
            switch (eventType) {
                case "ARTIFACT_DOWNLOADING":
                    result = new Event("maven:repository:artifact-download", true);
                    break;
                case "ARTIFACT_DOWNLOADED":
                    result = new Event("maven:repository:artifact-download", false);
                    break;
                case "ARTIFACT_DEPLOYING":
                    result = new Event("maven:repository:artifact-deployment", true);
                    break;
                case "ARTIFACT_DEPLOYED":
                    result = new Event("maven:repository:artifact-deployment", false);
                    break;
                default:
                    result = new Event(null, true);
            }
            return result;
        } catch (ReflectiveOperationException e) {
            // Could not determine repository event type
            LOGGER.warn("Could not determine repository event type", e);
            return null;
        }
    }

    private Event determineExectionEvent(Object event) {
        try {
            final Class<?> executionEventInterface = Class.forName("org.apache.maven.execution.ExecutionEvent");
            final String eventType = executionEventInterface.getMethod("getType").invoke(event).toString();

            final Event result;
            switch (eventType) {
                case "MojoStarted":
                    result = new Event(getExecutionEventIdentifier(executionEventInterface, event), true);
                    break;
                case "MojoSucceeded":
                case "MojoFailed":
                    result = new Event(getExecutionEventIdentifier(executionEventInterface, event), false);
                    break;
                default:
                    result = new Event(null, true);
            }
            return result;
        } catch (ReflectiveOperationException e) {
            // Could not determine repository event type
            LOGGER.warn("Could not determine execution event type", e);
            return null;
        }
    }

    private String getExecutionEventIdentifier(Class<?> executionEventInterface, Object event) throws ReflectiveOperationException {
        Object mojoExecution = executionEventInterface.getMethod("getMojoExecution").invoke(event);
        Class<?> mojoExecutionClass = Class.forName("org.apache.maven.plugin.MojoExecution");
        String groupId = (String) mojoExecutionClass.getMethod("getGroupId").invoke(mojoExecution);
        String artifactId = (String) mojoExecutionClass.getMethod("getArtifactId").invoke(mojoExecution);
        String goal = (String) mojoExecutionClass.getMethod("getGoal").invoke(mojoExecution);
        String executionId = (String) mojoExecutionClass.getMethod("getExecutionId").invoke(mojoExecution);
        return groupId + ":" + artifactId + ":" + goal + "@" + executionId;
    }

    private static final class Event {
        private final String identifier;
        private final boolean start;

        Event(final String identifier, final boolean start) {
            this.identifier = identifier;
            this.start = start;
        }

        String getIdentifier() {
            return identifier;
        }

        boolean isStart() {
            return start;
        }
    }
}
