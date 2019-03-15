# Botsing

[![Build Status](https://travis-ci.org/STAMP-project/botsing.svg?branch=master)](https://travis-ci.org/STAMP-project/botsing)
[![Coverage Status](https://coveralls.io/repos/github/STAMP-project/botsing/badge.svg?branch=master)](https://coveralls.io/github/STAMP-project/botsing?branch=master)
[![Maven Central](https://img.shields.io/maven-central/v/eu.stamp-project/botsing-reproduction.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22eu.stamp-project%22%20AND%20a:%22botsing-reproduction%22)

Botsing is a Java framework for crash reproduction. It relies on [EvoSuite](http://www.evosuite.org) for code instrumentation.

## Usage

### From Maven

See the [documentation for the Maven Plugin](https://github.com/STAMP-project/botsing/tree/master/botsing-maven).

### Command line interface


#### botsing reproduction
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

```sh
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

#### Example

```
java -jar botsing-reproduction.jar -crash_log LANG-1b.log -target_frame 2 -project_cp ~/bin
```


## Contributing

Botsing is licensed under Apache-2.0, pull request as are welcome.

### Coding style

The coding style is described in [`checkstyle.xml`](checkstyle.xml). Please (successfully) run the command `mvn checkstyle:check` before submitting a pull request.

### Building Botsing

Currently, Botsing using a customized version of the EvoSuite-client. Hence, the building process contains two steps:
1- Installing the customized version of EvoSuite-client:
```
mvn install:install-file -Dfile=botsing-reproduction/evosuite-client-botsing-1.0.7.jar -DgroupId=org.evosuite -DartifactId=evosuite-client-botsing -Dversion=1.0.7 -Dpackaging=jar
```
2- Build the Botsing project:

```
mvn package
```

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

#### botsing preprocessing

The latest version of Botsing command line (botsing-preprocessing-X-X-X.jar) is available at [https://github.com/STAMP-project/botsing/releases](https://github.com/STAMP-project/botsing/releases). 

Botsing preprocessing has these mandatory parameters:
 - `-i` represents the input file path (`crash_log`) with the stack trace to clean. For example `-i=path-name-of-crash-log`
 - `-o` represents the output file path (`output_log`) cleaned of the error message and/or nested exceptions. For example `-o=path-name-of-output-log` 
 
These parameters define actions to perform (clean) in the stack trace: 
 - `-f` to flatten the stack trace.
 - `-e` to remove the error message.
 
 - `-p` to set the regexp in the stack trace use case. For example `-p=my.package.*` 

#### Example

To clean the nested stack trace

```
java -jar botsing-preprocessing.jar -i=crash_log.txt -o=output_log.log -f -p=com.example.*
```

or to remove the error message

```
java -jar botsing-preprocessing.jar -e -i=crash_log.txt -o==output_log.log 
```

Note that you can use also both actions (`-f` and `-e`).

## Background

Botsing (Dutch for 'crash') is a complete re-implementation of the crash replication tool [EvoCrash](http://www.evocrash.org) ([github](https://github.com/STAMP-project/EvoCrash)).
Whereas EvoCrash was a full clone of EvoSuite (making it hard to update EvoCrash as EvoSuite evolves), Botsing relies on EvoSuite as a (maven) dependency only. Furthermore, it comes with an extensive test suite, making it easier to extend. The license adopted is Apache, in order to facilitate adoption in industry and academia.

The underlying evolutionary algorithm and fitness function are described in:

* Mozhan Soltani, Annibale Panichella, and Arie van Deursen. Search-Based Crash Reproduction and Its Impact on Debugging. _IEEE Transactions on Software Engineering_, 2018. ([DOI](http://dx.doi.org/10.1109/TSE.2018.2877664), [preprint](https://pure.tudelft.nl/portal/en/publications/searchbased-crash-reproduction-and-its-impact-on-debugging(1281ce36-7afc-43d9-ad83-b69c60fbd49a).html))

* Mozhan Soltani, Pouria Derakhshanfar, Annibale Panichella, Xavier Devroey, Andy Zaidman, and Arie van Deursen. Single-objective versus Multi-Objectivized Optimization for Evolutionary Crash Reproduction. In Colanzi and McMinn, editors, _Search-Based Software Engineering - 10th International Symposium, SSBSE 2018 - Proceedings_. Lecture Notes in Computer Science, Springer. 2018. p. 325-340. ([DOI](http://dx.doi.org/10.1007/978-3-319-99241-9_18), [preprint](https://pure.tudelft.nl/portal/en/publications/singleobjective-versus-multiobjectivized-optimization-for-evolutionary-crash-reproduction(ccece8a1-79cd-4303-adca-34a920bf7d14).html)).


## Funding

Botsing is partially funded by research project STAMP (European Commission - H2020) ICT-16-10 No.731529.

![STAMP - European Commission - H2020](docs/logo_readme_md.png)
