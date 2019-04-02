# Botsing Maven plugin

Botsing Maven plugin let's you run [Botsing](https://github.com/STAMP-project/botsing) inside your Maven project as a Maven plugin.

## Install

You can install it in your Maven local repository following this steps:

1. From the project folder compile the project running `mvn compile`

2. Install it in your Maven local repository running `mvn install`

## Usage

If you have a Maven project you can run Botsing using Maven from the project folder with this command:

```
mvn eu.stamp-project:botsing-maven:botsing -Dcrash_log=ACC-474/ACC-474.log -Dtarget_frame=2
```

* crash_log is the parameter to tell Botsing where is the log file to analyze.
* target_frame is the parameter to tell Botsing how many lines of the log to replicate

To have more information on the parameters that you can use, please refer to the [Botsing project](https://github.com/STAMP-project/botsing).