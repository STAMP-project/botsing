---
layout: default
---

# Contributing

Botsing is licensed under Apache-2.0. Pull request as are welcome! :tada:

## Building Botsing

Currently, Botsing using a customized version of the `evoSuite-client`. Hence, the building process contains two steps:

1. Installing the customized version of EvoSuite-client:
```Bash
mvn install:install-file -Dfile=botsing-reproduction/evosuite-client-botsing-1.0.7.jar -DgroupId=org.evosuite -DartifactId=evosuite-client-botsing -Dversion=1.0.7 -Dpackaging=jar
```

2. Build the Botsing project:
```Bash
mvn package
```

## Coding style

The coding style is described in [`checkstyle.xml`](https://github.com/STAMP-project/botsing/blob/master/checkstyle.xml). Please (successfully) run the command `mvn checkstyle:check` before submitting a pull request.

## Adding a Maven dependency

Dependencies are managed at the module level. Each module declares a list of Maven dependencies, if you want to add one, simply add it to the list (see for instance [`botsing-reproduction/pom.xml`](https://github.com/STAMP-project/botsing/blob/master/botsing-reproduction/pom.xml)). **However**, dependency version must be declared as a property in the parent [`pom.xml`](https://github.com/STAMP-project/botsing/blob/master/pom.xml) file using the following syntax:
```xml
<properties>
  ...
  <!-- Dependencies versions -->
  <!-- To ensure a proper management of dependencies, all versions have to be declared here -->
  <depdencendy-artifactId.version>1.1.1</depdencendy-artifactId.version>
  ...
</properties>
```

And referenced in the dependencies of the module using the following syntax:
```xml
<dependencies>
  <dependency>
    <groupId>com.groupId</groupId>
    <artifactId>depdencendy-artifactId</artifactId>
    <version>${depdencendy-artifactId.version}</version>
  </dependency>
</dependencies>
```
Please check in the list of properties that the dependency version is not already there before adding a new one.
