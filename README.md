# Maven Mojo Timer [![Build Status](https://travis-ci.org/willemsrb/maven-mojo-timer.svg?branch=master)](https://travis-ci.org/willemsrb/maven-mojo-timer) [![Quality Gate](https://sonarqube.com/api/badges/gate?key=nl.future-edge:maven-mojo-timer)](https://sonarqube.com/dashboard/index?id=nl.future-edge%3Amaven-mojo-timer) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/nl.future-edge/maven-mojo-timer/badge.svg)](https://maven-badges.herokuapp.com/maven-central/nl.future-edge/maven-mojo-timer)
Profiles the execution time of a maven build

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
