---
layout: default
---

# Preprocessing a stack trace with Botsing

The latest version of Botsing preprocessing command line tool (botsing-preprocessing-X-X-X.jar) is available at [https://github.com/STAMP-project/botsing/releases](https://github.com/STAMP-project/botsing/releases).

Botsing preprocessing has these mandatory parameters (key/value):
 - `-i` represents the input file path (`crash_log`) with the stack trace to clean. For example `-i=path-name-of-crash-log`
 - `-o` represents the output file path (`output_log`) cleaned of the error message and/or nested exceptions. For example `-o=path-name-of-output-log`

The actions to perform (clean) in the input log file are:
 - `-f` to flatten the stack trace. This action needs to use `-p` parameter (key/value) to set the package has frames pointing to the software under test (as **regexp**). For example `-p=my.package.*`.
 - `-e` to remove the error message. For example `-e`.

## Example

To clean the nested stack trace:

```Bash
java -jar botsing-preprocessing.jar -i=crash_log.txt -o=output_log.log -f -p=com.example.*
```

or to remove the error message in the log file

```Bash
java -jar botsing-preprocessing.jar -e -i=crash_log.txt -o==output_log.log
```

Note that you can use also both actions (`-f` and `-e`).

# Maven plugin

See the [documentation for the Maven plugin](https://github.com/STAMP-project/botsing/tree/master/botsing-maven) for more information.
