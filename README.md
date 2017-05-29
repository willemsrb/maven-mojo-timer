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
    <version>1.0.0</version>
  </extension>
</extensions>
```

## Report
After the build the extension reports the cumulative time spent by various parts of the build:

name                                                    | description
--------------------------------------------------------|-------------------------
maven:settings-building                                 | Build setup (settings)
maven:toolchains-building                               | Build setup (toolchains)
maven:dependency-resolution                             | Dependency resolution
maven:repository:artifact-download                      | Downloading artifacts
maven:repository:artifact-deployment                    | Deploying artifacts
&lt;groupId>:&lt;artifactId>:&lt;goal>:&lt;executionId> | Mojo executions

For example:
```
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 50.663 s (Wall Clock)
[INFO] Finished at: 2017-05-29T13:52:57+02:00
[INFO] Final Memory: 52M/919M
[INFO] ------------------------------------------------------------------------
[INFO] [29.573 sec] executions:  15, min:  0.776 sec, max:  7.070 sec, avg:  1.971 sec - org.apache.maven.plugins:maven-compiler-plugin:compile@default-compile
[INFO] [19.709 sec] executions:  15, min:  0.001 sec, max:  2.953 sec, avg:  1.313 sec - org.apache.maven.plugins:maven-compiler-plugin:testCompile@default-testCompile
[INFO] [ 7.721 sec] executions:   1, min:  7.721 sec, max:  7.721 sec, avg:  7.721 sec - org.apache.maven.plugins:maven-surefire-plugin:test@generate-dbunit-resources
[INFO] [ 4.382 sec] executions:  21, min:  0.074 sec, max:  0.397 sec, avg:  0.208 sec - maven:dependency-resolution
[INFO] [ 1.852 sec] executions:   1, min:  1.852 sec, max:  1.852 sec, avg:  1.852 sec - org.apache.maven.plugins:maven-plugin-plugin:descriptor@default-descriptor
[INFO] [ 1.582 sec] executions:  40, min:  0.001 sec, max:  0.270 sec, avg:  0.039 sec - org.apache.maven.plugins:maven-clean-plugin:clean@default-clean
[INFO] [ 1.505 sec] executions:  40, min:  0.012 sec, max:  0.288 sec, avg:  0.037 sec - org.apache.maven.plugins:maven-install-plugin:install@default-install
[INFO] [ 0.955 sec] executions:   1, min:  0.955 sec, max:  0.955 sec, avg:  0.955 sec - org.apache.maven.plugins:maven-plugin-plugin:helpmojo@help-descriptor
[INFO] [ 0.846 sec] executions:  14, min:  0.012 sec, max:  0.141 sec, avg:  0.060 sec - org.apache.maven.plugins:maven-jar-plugin:jar@default-jar
[INFO] [ 0.807 sec] executions:  21, min:  0.001 sec, max:  0.384 sec, avg:  0.038 sec - org.apache.maven.plugins:maven-jar-plugin:test-jar@jar-test-jar
[INFO] [ 0.742 sec] executions:   1, min:  0.742 sec, max:  0.742 sec, avg:  0.742 sec - org.apache.maven.plugins:maven-war-plugin:war@default-war
[INFO] [ 0.436 sec] executions:  21, min:  0.002 sec, max:  0.292 sec, avg:  0.020 sec - org.apache.maven.plugins:maven-failsafe-plugin:integration-test@integration-test
[INFO] [ 0.334 sec] executions:  15, min:  0.001 sec, max:  0.153 sec, avg:  0.022 sec - org.apache.maven.plugins:maven-resources-plugin:resources@default-resources
[INFO] [ 0.280 sec] executions:  15, min:  0.002 sec, max:  0.131 sec, avg:  0.018 sec - org.apache.maven.plugins:maven-surefire-plugin:test@default-test
[INFO] [ 0.149 sec] executions:   1, min:  0.149 sec, max:  0.149 sec, avg:  0.149 sec - org.codehaus.mojo:build-helper-maven-plugin:add-source@add-enums
[INFO] [ 0.130 sec] executions:  15, min:  0.002 sec, max:  0.026 sec, avg:  0.008 sec - org.apache.maven.plugins:maven-resources-plugin:testResources@default-testResources
[INFO] [ 0.063 sec] executions:  21, min:  0.001 sec, max:  0.013 sec, avg:  0.003 sec - org.apache.maven.plugins:maven-failsafe-plugin:verify@integration-test
[INFO] [ 0.021 sec] executions:   1, min:  0.021 sec, max:  0.021 sec, avg:  0.021 sec - org.apache.maven.plugins:maven-resources-plugin:copy-resources@copy-dbunit-resources
[INFO] [ 0.004 sec] executions:   1, min:  0.004 sec, max:  0.004 sec, avg:  0.004 sec - org.apache.maven.plugins:maven-plugin-plugin:addPluginArtifactMetadata@default-addPluginArtifactMetadata
[INFO] [ 0.002 sec] executions:   1, min:  0.002 sec, max:  0.002 sec, avg:  0.002 sec - org.codehaus.mojo:build-helper-maven-plugin:add-resource@add-resource
```