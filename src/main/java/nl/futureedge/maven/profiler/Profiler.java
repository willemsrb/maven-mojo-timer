package nl.futureedge.maven.profiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.project.DependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.apache.maven.toolchain.building.ToolchainsBuildingRequest;
import org.apache.maven.toolchain.building.ToolchainsBuildingResult;
import org.eclipse.aether.RepositoryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Profiler.
 */
@Named
@Singleton
public final class Profiler extends AbstractEventSpy {

    private static final Logger LOGGER = LoggerFactory.getLogger(Profiler.class.getName());

    private ThreadLocal<Long> startSettingsBuilding = new ThreadLocal<>();
    private ThreadLocal<Long> startToolchainBuilding = new ThreadLocal<>();
    private ThreadLocal<Long> startDependencyResolution = new ThreadLocal<>();
    private ThreadLocal<Long> startArtifactDownloading = new ThreadLocal<>();
    private ThreadLocal<Long> startArtifactDeploying = new ThreadLocal<>();

    private ThreadLocal<Long> startMojoExecution = new ThreadLocal<>();

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
     *     <li>XX.XXX sec (for under 100 seconds)</li>
     *     <li> XX:XX min (for under 100 minutes)</li>
     *     <li>XXX:XX hrs (for the rest)</li>
     * </ul>
     * @param durationInMillis duration in milliseconds
     * @return
     */
    private String format(final long durationInMillis) {
        final long durationInSeconds = durationInMillis / 1000;
        if(durationInSeconds < 100) {
            // Under 100 seconds
            final long millis = durationInMillis % 1000;
            return String.format("%1$2d.%2$03d sec", durationInSeconds, millis);
        } else {
            final long durationInMinutes = durationInSeconds / 60;
            if(durationInMinutes < 100) {
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
    public void onEvent(final Object event) {
        if (event instanceof RepositoryEvent) {
            handleRepositoryEvent((RepositoryEvent) event);
        } else if (event instanceof ExecutionEvent) {
            handleExecutionEvent((ExecutionEvent) event);
        } else if (event instanceof SettingsBuildingRequest
                || event instanceof SettingsBuildingResult) {
            handleSettingsBuilding(event);
        } else if (event instanceof ToolchainsBuildingRequest
                || event instanceof ToolchainsBuildingResult) {
            handToolchainsBuilding(event);
        } else if (event instanceof DependencyResolutionRequest
                || event instanceof DependencyResolutionResult) {
            handleDependencyResolution(event);
        } else if (event instanceof MavenExecutionRequest
                || event instanceof MavenExecutionResult) {
            // Ignore as this is the overall build execution
        } else {
            unsupportedEvents.add(event.getClass().getName());
        }
    }

    private void handleSettingsBuilding(final Object event) {
        if (event instanceof SettingsBuildingRequest) {
            startSettingsBuilding.set(System.currentTimeMillis());
        }
        if (event instanceof SettingsBuildingResult) {
            final long duration = System.currentTimeMillis() - startSettingsBuilding.get();
            register("maven:settings-building", duration);
        }
    }

    private void handToolchainsBuilding(final Object event) {
        if (event instanceof ToolchainsBuildingRequest) {
            startToolchainBuilding.set(System.currentTimeMillis());
        }
        if (event instanceof ToolchainsBuildingResult) {
            final long duration = System.currentTimeMillis() - startToolchainBuilding.get();
            register("maven:toolchains-building", duration);
        }
    }

    private void handleDependencyResolution(final Object event) {
        if (event instanceof DependencyResolutionRequest) {
            startDependencyResolution.set(System.currentTimeMillis());
        }
        if (event instanceof DependencyResolutionResult) {
            final long duration = System.currentTimeMillis() - startDependencyResolution.get();
            register("maven:dependency-resolution", duration);
        }
    }

    private void handleRepositoryEvent(final RepositoryEvent event) {
        if (RepositoryEvent.EventType.ARTIFACT_DOWNLOADING.equals(event.getType())) {
            startArtifactDownloading.set(System.currentTimeMillis());
        }
        if (RepositoryEvent.EventType.ARTIFACT_DOWNLOADED.equals(event.getType())) {
            final long duration = System.currentTimeMillis() - startArtifactDownloading.get();
            register("maven:repository:artifact-download", duration);
        }
        if (RepositoryEvent.EventType.ARTIFACT_DEPLOYING.equals(event.getType())) {
            startArtifactDeploying.set(System.currentTimeMillis());
        }
        if (RepositoryEvent.EventType.ARTIFACT_DEPLOYED.equals(event.getType())) {
            final long duration = System.currentTimeMillis() - startArtifactDeploying.get();
            register("maven:repository:artifact-deployment", duration);
        }
    }

    private void handleExecutionEvent(final ExecutionEvent event) {
        if (ExecutionEvent.Type.MojoStarted.equals(event.getType())) {
            startMojoExecution.set(System.currentTimeMillis());
        }

        if (ExecutionEvent.Type.MojoSucceeded.equals(event.getType())
                || ExecutionEvent.Type.MojoFailed.equals(event.getType())) {
            final long duration = System.currentTimeMillis() - startMojoExecution.get();
            register(event.getMojoExecution().getGroupId()
                            + ":" + event.getMojoExecution().getArtifactId()
                            + ":" + event.getMojoExecution().getGoal()
                            + "@" + event.getMojoExecution().getExecutionId()
                    , duration);
        }
    }

    private void register(final String identifier, final long duration) {
        executions.computeIfAbsent(identifier, key -> Collections.synchronizedList(new ArrayList<>()));
        executions.get(identifier).add(duration);
    }
}
