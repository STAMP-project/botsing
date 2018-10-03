# Botsing

[![Build Status](https://travis-ci.org/STAMP-project/botsing.svg?branch=master)](https://travis-ci.org/STAMP-project/botsing)
[![Coverage Status](https://coveralls.io/repos/github/STAMP-project/botsing/badge.svg?branch=master)](https://coveralls.io/github/STAMP-project/botsing?branch=master)

Botsing is a Java framework for crash reproduction. It relies on [EvoSuite](http://www.evosuite.org) for code instrumentation.


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
