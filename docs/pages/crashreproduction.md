---
layout: default
---

# Reproducing a stack trace with Botsing

The latest version of Botsing command line (botsing-reproduction-X-X-X.jar) is available at [https://github.com/STAMP-project/botsing/releases](https://github.com/STAMP-project/botsing/releases).

Botsing has three mandatory parameters:
 - `-crash_log` the file with the stack trace. The stack trace should be clean (no error message) and cannot contain any nested exceptions.
 - `-target_frame` the target frame to reproduce. This number should be between 1 and the number of frames in the stack trace.
 - `-project_cp` the classpath of the project and all its dependencies. The classpath can be a folder containing all the  `.jar` files required to run the software under test.

By default, Botsing uses the following parameter values:
 - `-Dsearch_budget=1800`, a time budget of 30 min. This value can be modified by specifying an additional parameter in format `-Dsearch_budget=60` (here, for 60 seconds).
 - `-Dpopulation=100`, a default population with 100 individuals. This value may be modified using `-Dpopulation=10` (here, for 10 individuals).
 - `-Dtest_dir=crash-reproduction-tests`, the output directory where the tests will be created (if any test is generated). This value may be modified using `-Dtest_dir=newoutputdir`.

To check the list of options, use:

```Bash
$ java -jar botsing-reproduction.jar -help
usage: java -jar botsing-reproduction.jar -crash_log stacktrace.log -target_frame 2

            -project_cp dep1.jar;dep2.jar  )
 -crash_log <arg>      File with the stack trace
 -D <property=value>   use value for given property
 -help                 Prints this help message.
 -project_cp <arg>      classpath of the project under test and all its
                       dependencies
 -target_frame <arg>   Level of the target frame
```

## Example

```Bash
java -jar botsing-reproduction.jar -crash_log LANG-1b.log -target_frame 2 -project_cp ~/bin
```

## Maven plugin

See the [documentation for the Maven plugin](https://github.com/STAMP-project/botsing/tree/master/botsing-maven) for more information.
