package nl.futureedge.maven.profiler;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.DependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.apache.maven.toolchain.building.ToolchainsBuildingRequest;
import org.apache.maven.toolchain.building.ToolchainsBuildingResult;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositorySystemSession;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class ProfilerTest {

    private Profiler subject = new Profiler();

    @Test
    public void testEvents() throws ReflectiveOperationException {
        subject.onEvent(Mockito.mock(SettingsBuildingRequest.class));
        subject.onEvent(Mockito.mock(SettingsBuildingResult.class));
        subject.onEvent(Mockito.mock(ToolchainsBuildingRequest.class));
        subject.onEvent(Mockito.mock(ToolchainsBuildingResult.class));
        subject.onEvent(Mockito.mock(DependencyResolutionRequest.class));
        subject.onEvent(Mockito.mock(DependencyResolutionResult.class));
        subject.onEvent(Mockito.mock(MavenExecutionRequest.class));
        subject.onEvent(Mockito.mock(MavenExecutionResult.class));

        subject.onEvent(repositoryEvent(RepositoryEvent.EventType.ARTIFACT_DOWNLOADING));
        subject.onEvent(repositoryEvent(RepositoryEvent.EventType.ARTIFACT_DOWNLOADED));
        subject.onEvent(repositoryEvent(RepositoryEvent.EventType.ARTIFACT_DEPLOYING));
        subject.onEvent(repositoryEvent(RepositoryEvent.EventType.ARTIFACT_DEPLOYED));
        subject.onEvent(repositoryEvent(RepositoryEvent.EventType.ARTIFACT_RESOLVING));
        subject.onEvent(repositoryEvent(RepositoryEvent.EventType.ARTIFACT_RESOLVED));

        subject.onEvent(executionEvent(ExecutionEvent.Type.MojoStarted, "group", "artifact", "goal1", "execution"));
        subject.onEvent(executionEvent(ExecutionEvent.Type.MojoSucceeded, "group", "artifact", "goal1", "execution"));
        subject.onEvent(executionEvent(ExecutionEvent.Type.MojoStarted, "group", "artifact", "goal1", "execution"));
        subject.onEvent(executionEvent(ExecutionEvent.Type.MojoFailed, "group", "artifact", "goal1", "execution"));
        subject.onEvent(executionEvent(ExecutionEvent.Type.MojoStarted, "group", "artifact", "goal2", "execution"));
        subject.onEvent(executionEvent(ExecutionEvent.Type.MojoSucceeded, "group", "artifact", "goal2", "execution"));

        subject.onEvent("unknownEventType");

        // Check collected data
        Field executionsField = Profiler.class.getDeclaredField("executions");
        executionsField.setAccessible(true);
        SortedMap<String, List<Long>> executions = (SortedMap<String, List<Long>>) executionsField.get(subject);
        System.out.println("Executions: " + executions);
        Assert.assertEquals(7, executions.size());
        Assert.assertEquals(1, executions.get("maven:settings-building").size());
        Assert.assertEquals(1, executions.get("maven:toolchains-building").size());
        Assert.assertEquals(1, executions.get("maven:dependency-resolution").size());
        Assert.assertEquals(1, executions.get("maven:repository:artifact-download").size());
        Assert.assertEquals(1, executions.get("maven:repository:artifact-deployment").size());
        List<Long> goal1 = executions.get("group:artifact:goal1@execution");
        Assert.assertEquals(2, goal1.size());
        Assert.assertEquals(1, executions.get("group:artifact:goal2@execution").size());

        Field unsupportedEventsField = Profiler.class.getDeclaredField("unsupportedEvents");
        unsupportedEventsField.setAccessible(true);
        SortedSet<String> unsupportedEvents = (SortedSet<String>) unsupportedEventsField.get(subject);
        System.out.println("Unsupported events: " + unsupportedEvents);
        Assert.assertEquals(1, unsupportedEvents.size());
        Assert.assertTrue(unsupportedEvents.contains("java.lang.String"));

        executions.clear();
        executions.put("group:artifact:goal1@execution", goal1);
        goal1.clear();

        // Display a minutes (3:46 min + 345 ms)
        goal1.add(226345L);

        // Display as seconds (6.123 sec)
        goal1.add(6123L);

        // Display as hours (2:23 hrs + 15 sec + 677 ms)
        goal1.add(8595677L);

        Logger logger = (Logger) LogManager.getLogger(Profiler.class.getName());
        ListAppender listAppender = new ListAppender("ProfilerTest");
        listAppender.start();
        try {
            logger.addAppender(listAppender);
            subject.close();

        } finally {
            logger.removeAppender(listAppender);
            listAppender.stop();
        }

        List<String> messages = new ArrayList<>();
        for (LogEvent logEvent : listAppender.getEvents()) {
            messages.add(logEvent.getMessage().getFormattedMessage());
        }

        List<String> expected = new ArrayList<>();
        expected.add("Execution times:");
        expected.add("[  2:27 hrs] executions:   3, min:  6.123 sec, max:   2:23 hrs, avg:  49:02 min - group:artifact:goal1@execution");
        expected.add("------------------------------------------------------------------------");
        expected.add("Unsupported events encountered:");
        expected.add(" - java.lang.String");
        expected.add("------------------------------------------------------------------------");

        Assert.assertEquals(expected, messages);
    }

    private RepositoryEvent repositoryEvent(RepositoryEvent.EventType type) {
        RepositoryEvent.Builder builder = new RepositoryEvent.Builder(Mockito.mock(RepositorySystemSession.class), type);
        return builder.build();
    }


    private Object executionEvent(ExecutionEvent.Type type, String groupId, String artifactId, String goal, String executionId) {
        Plugin plugin = new Plugin();
        plugin.setGroupId(groupId);
        plugin.setArtifactId(artifactId);

        MojoExecution mojoExecution = new MojoExecution(plugin, goal, executionId);

        ExecutionEvent event = Mockito.mock(ExecutionEvent.class);
        Mockito.when(event.getType()).thenReturn(type);
        Mockito.when(event.getMojoExecution()).thenReturn(mojoExecution);

        return event;
    }
}
