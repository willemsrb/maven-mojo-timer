# Maven Mojo Timer [![Build Status](https://travis-ci.org/willemsrb/maven-mojo-timer.svg?branch=master)](https://travis-ci.org/willemsrb/maven-mojo-timer) [![Quality Gate](https://sonarqube.com/api/badges/gate?key=nl.future-edge:maven-mojo-timer)](https://sonarqube.com/dashboard/index?id=nl.future-edge%3Amaven-mojo-timer) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/nl.future-edge/maven-mojo-timer/badge.svg)](https://maven-badges.herokuapp.com/maven-central/nl.future-edge/maven-mojo-timer)
Profiles the time spent in various parts of a maven build.

## Usage
### Command line
Add `-Dmaven.ext.class.path=maven-mojo-timer.jar` to your `mvn` command.

### Extension
Add a `.mvn/extension.xml` file to your project containing
```
<?xml version="1.0" encoding="UTF-8"?>
<extensions>
  <extension>
    <groupId>nl.future-edge</groupId>
    <artifactId>maven-mojo-timer</artifactId>
    <version>1.0</version>
  </extension>
</extensions>
```

## Report
After the build the extension reports the cumulative time spent by various parts of the build:

| name                                                    | description              |
|---------------------------------------------------------|----------------------- --|
| maven:settings-building                                 | Build setup (settings)   |
| maven:toolchains-building                               | Build setup (toolchains) |
| maven:dependency-resolution                             | Dependency resolution    |
| maven:repository:artifact-download                      | Downloading artifacts    |
| maven:repository:artifact-deployment                    | Deploying artifacts      |
| &lt;groupId>:&lt;artifactId>:&lt;goal>:&lt;executionId> | Mojo executions          |

For example:

> [INFO] ------------------------------------------------------------------------
> [INFO] BUILD SUCCESS
> [INFO] ------------------------------------------------------------------------
> [INFO] Total time: 6.819 s
> [INFO] Finished at: 2017-05-29T13:39:20+02:00
> [INFO] Final Memory: 31M/632M
> [INFO] ------------------------------------------------------------------------
> [INFO] Execution times:
> [INFO] [ 2.145 sec] executions:   1, min:  2.145 sec, max:  2.145 sec, avg:  2.145 sec - org.apache.maven.plugins:maven-surefire-plugin:test@default-test
> [INFO] [ 1.561 sec] executions:   1, min:  1.561 sec, max:  1.561 sec, avg:  1.561 sec - org.apache.maven.plugins:maven-javadoc-plugin:jar@attach-javadocs
> [INFO] [ 0.620 sec] executions:   1, min:  0.620 sec, max:  0.620 sec, avg:  0.620 sec - org.apache.maven.plugins:maven-compiler-plugin:compile@default-compile
> [INFO] [ 0.242 sec] executions:   1, min:  0.242 sec, max:  0.242 sec, avg:  0.242 sec - org.apache.maven.plugins:maven-resources-plugin:resources@default-resources
> [INFO] [ 0.225 sec] executions:   1, min:  0.225 sec, max:  0.225 sec, avg:  0.225 sec - maven:dependency-resolution
> [INFO] [ 0.196 sec] executions:   1, min:  0.196 sec, max:  0.196 sec, avg:  0.196 sec - org.apache.maven.plugins:maven-source-plugin:jar-no-fork@attach-sources
> [INFO] [ 0.192 sec] executions:   1, min:  0.192 sec, max:  0.192 sec, avg:  0.192 sec - org.apache.maven.plugins:maven-failsafe-plugin:verify@integration-test
> [INFO] [ 0.139 sec] executions:   1, min:  0.139 sec, max:  0.139 sec, avg:  0.139 sec - org.apache.maven.plugins:maven-failsafe-plugin:integration-test@integration-test
> [INFO] [ 0.077 sec] executions:   1, min:  0.077 sec, max:  0.077 sec, avg:  0.077 sec - org.apache.maven.plugins:maven-install-plugin:install@default-install
> [INFO] [ 0.043 sec] executions:   1, min:  0.043 sec, max:  0.043 sec, avg:  0.043 sec - org.apache.maven.plugins:maven-gpg-plugin:sign@sign-artifacts
> [INFO] [ 0.016 sec] executions:   1, min:  0.016 sec, max:  0.016 sec, avg:  0.016 sec - maven:settings-building
> [INFO] [ 0.011 sec] executions:   1, min:  0.011 sec, max:  0.011 sec, avg:  0.011 sec - org.apache.maven.plugins:maven-resources-plugin:testResources@default-testResources
> [INFO] [ 0.010 sec] executions:   1, min:  0.010 sec, max:  0.010 sec, avg:  0.010 sec - org.apache.maven.plugins:maven-compiler-plugin:testCompile@default-testCompile
> [INFO] ------------------------------------------------------------------------