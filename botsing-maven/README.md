# Botsing Maven plugin

Botsing Maven plugin let's you run [Botsing](https://github.com/STAMP-project/botsing) as a Maven plugin.

## Install

You can install it in your Maven local repository with this command:

```
mvn clean install
```

## Simple Usage

If you have a Maven project you can run Botsing using Maven from the project folder with this command:

```
mvn eu.stamp-project:botsing-maven:botsing -Dcrash_log=ACC-474/ACC-474.log -Dtarget_frame=2
```

* crash_log is the parameter to tell Botsing where is the log file to analyze.
* target_frame is the parameter to tell Botsing how many lines of the stacktrace to replicate

To have more information on the parameters that you can use, please refer to the [Botsing project](https://github.com/STAMP-project/botsing).

## Dependencies options

Dependencies are fundamental to run Botsing so it can reproduce the stacktrace. Botsing Maven plugin has three ways to specify where to find dependencies:

1. from pom.xml
1. specifiyng a Maven artifact
1. from a folder

### Dependencies from pom.xml

Botsing Maven plugin will read the pom.xml in the current folder and find the dependencies specified in it. This will be the default behaviour if none else has been specified.

### Dependencies from artifact

Botsing Maven plugin will search in the Maven repository for this artifact and all the dependencies required.

The command will be something like: 

```
mvn eu.stamp-project:botsing-maven:botsing -Dcrash_log=ACC-474.log -Dmax_target_frame=2 -Dgroup_id=org.apache.commons -Dartifact_id=commons-collections4 -Dversion=4.0
```

### Dependencies from folder

Botsing Maven plugin will search in the specified folder and gets all the libraries inside it. The command will be something like 

```
mvn eu.stamp-project:botsing-maven:botsing -Dcrash_log=ACC-474/ACC-474.log -Dtarget_frame=2 -Dproject_cp=lib
```

## Target frame options

The target_frame lets you specify how many rows of the stacktrace Botsing has to reproduce. Botsing Maven plugin has three ways to specify it:

1. directly using target_frame (e.g. -Dtarget_frame=2)
1. specifying the maximum value (e.g. -Dmax_target_frame=2), in this case it will start from the maximum value provided and decrease it until a reproduction test have been found
1. reading it from the maximum rows of the stacktrace, no parameter for the target frame should be provided

## Help

To view a list of all the parameters that can be set use this command:

```
mvn eu.stamp-project:botsing-maven:help -Ddetail=true -Dgoal=botsing
```