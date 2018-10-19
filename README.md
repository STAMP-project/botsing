# Botsing

[![Build Status](https://travis-ci.org/STAMP-project/botsing.svg?branch=master)](https://travis-ci.org/STAMP-project/botsing)
[![Coverage Status](https://coveralls.io/repos/github/STAMP-project/botsing/badge.svg?branch=master)](https://coveralls.io/github/STAMP-project/botsing?branch=master)

Botsing is a Java framework for crash reproduction. It relies on [EvoSuite](http://www.evosuite.org) for code instrumentation.


## Usage

### Command line interface

The latest version of Botsing command line (botsgin-reproduction-X-X-X.jar) is available at [https://github.com/STAMP-project/botsing/releases](https://github.com/STAMP-project/botsing/releases). 

Botsing has three mandatory parameters:
 - `-crash_log` the file with the stack trace. The stack trace should be clean (no error message) and cannot contain any nested exceptions.
 - `-target_frame` the target frame to reproduce. This number should be between 1 and the number of frames in the stack trace.
 - `-projectCP` the classpath of the project and all its dependencies. The classpath can be a folder containing all the  `.jar` files required to run the software under test.
 
By default, Botsing uses the following parameter values:
 - `-Dsearch_budget=1800`, a time budget of 30 min. This value can be modified by specifying an additional parameter in format `-Dsearch_budget=60` (here, for 60 seconds). 
 - `-Dpopulation=100`, a default population with 100 individuals. This value may be modified using `-Dpopulation=10` (here, for 10 individuals).
 - `-Dtest_dir=crash-reproduction-tests`, the output directory where the tests will be created (if any test is generated). This value may be modified using `-Dtest_dir=newoutputdir`.

To check the list of options, use:

```sh
$ java -jar botsing-reproduction.jar -help
usage: java -jar botsing-reproduction.jar -crash_log stacktrace.log -target_frame 2
            -projectCP dep1.jar;dep2.jar  )
 -crash_log <arg>      File with the stack trace
 -D <property=value>   use value for given property
 -help                 Prints this help message.
 -projectCP <arg>      classpath of the project under test and all its
                       dependencies
 -target_frame <arg>   Level of the target frame
```

#### Example

```
java -jar botsing-reproduction.jar -crash_log LANG-1b.log -target_frame 2 -projectCP ~/bin
```


## Contributing

Botsing is licensed under Apache-2.0, pull request as are welcome.

### Coding style

The coding style is described in [`checkstyle.xml`](checkstyle.xml). Please (successfully) run the command `mvn checkstyle:check` before submitting a pull request.


### Adding a dependency

Dependencies are managed at the module level. Each module declares a list of Maven dependencies, if you want to add one, simply add it to the list (see for instance [`botsing-reproduction/pom.xml`](botsing-reproduction/pom.xml)). **However**, dependency version must be declared as a property in the parent [`pom.xml`](pom.xml) file using the following syntax:
```
<properties>
  ...
  <!-- Dependencies versions -->
  <!-- To ensure a proper management of dependencies, all versions have to be declared here -->
  <depdencendy-artifactId.version>1.1.1</depdencendy-artifactId.version>
  ...
</properties>
```

And referenced in the dependencies of the module using the following syntax:
```
<dependencies>
  <dependency>
    <groupId>com.groupId</groupId>
    <artifactId>depdencendy-artifactId</artifactId>
    <version>${depdencendy-artifactId.version}</version>
  </dependency>
</dependencies>
```
Please check in the list of properties that the dependency version is not already there before adding a new one.

## Funding

Botsing is partially funded by research project STAMP (European Commission - H2020)
![STAMP - European Commission - H2020](https://github.com/STAMP-project/docs-forum/blob/master/docs/images/logo_readme_md.png)
