# Botsing Maven plugin

Botsing Maven plugin let's you run [Botsing](https://github.com/STAMP-project/botsing) inside your Maven project as a Maven plugin.

## Install

You can install it in your Maven local repository following this steps:

1. Clone the git repository: `git clone https://github.com/luandrea/botsing.git && cd botsing`

2. From the project folder compile the project running `mvn compile`

3. Install it in your Maven local repository running `mvn install`

## Usage

If you have a Maven project you can run Botsing using Maven from the project folder like in this example: 

```
mvn eu.stamp-project:botsing-maven:botsing -Dlog_file=ACC-474/ACC-474.log -Dtarget_frame_level=2
```

To have more information on the parameters that you can use, please refer to the [Botsing project](https://github.com/STAMP-project/botsing).