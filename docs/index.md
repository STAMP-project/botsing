---
layout: default
---

# What is Botsing?

Botsing is a Java framework for search-based crash reproduction. It implements and extends the EvoCrash approach and relies on [EvoSuite](http://www.evosuite.org) for Java code instrumentation during test generation and execution.

# How does it work?

The goal of search-based crash reproduction is to produce a JUnit test able to reproduce a given *crash stack trace* (or stack trace for short) produced whenever a crash happens in a Java application. For instance, the following stack trace indicates that an exception has been thrown in method `FastDateParser.toArray` at line `413` and propagated through the method calls stack:
```
java.lang.ArrayIndexOutOfBoundsException:
	at org.apache.commons.lang3.time.FastDateParser.toArray(FastDateParser.java:413)
	at org.apache.commons.lang3.time.FastDateParser.getDisplayNames(FastDateParser.java:381)
	at org.apache.commons.lang3.time.FastDateParser$TextStrategy.addRegex(FastDateParser.java:664)
````
Each line starting with the `at` keyword is called a *frame* and numbered from top to bottom. Each frame indicates where the exception is propagated in a particular class. The first frame (method `FastDateParser.toArray` in our example) is the location where the exception is originally thrown.

If we try to reproduce this stack trace with Botsing, it may generate the following test case:
```Java
@Test(timeout = 4000)
public void test0()  throws Throwable  {
    Locale locale0 = FastDateParser.JAPANESE_IMPERIAL;
    TimeZone timeZone0 = TimeZone.getTimeZone("wFwY^-\"x`;{.Dk[P");
    FastDateParser fastDateParser0 = null;
    try {
      fastDateParser0 = new FastDateParser("GJ=", timeZone0, locale0);
      fail("Expecting exception: ArrayIndexOutOfBoundsException");
    } catch(ArrayIndexOutOfBoundsException e) {
       verifyException("org.apache.commons.lang3.time.FastDateParser", e);
    }
}
````
This test case can then be executed in a debugger to understand the propagation of the exception trough the different methods in the stack trace. For instance, by adding breakpoints before and after each line indicated in the stack trace.

To generate test cases able to reproduce a given stack trace, Botsing relies on a [search-based algorithm](https://en.wikipedia.org/wiki/Search-based_software_engineering). In particular, Botsing uses a *guided genetic algorithm*, defined in the EvoCrash approach. More specifically, for a given stack trace and a given frame in that stack trace, for instance, frame 3 in the example above, Botsing will:

  1. generate a set of (random) unit tests (called **population**) for class `FastDateParser` and

  2. gradually refine those test using *guided operators* that will modify the tests. At each iteration of the algorithm, the different tests are evaluated using a *fitness function* assessing how close each test is to reproduce the crash stack trace.

Eventually, Botsing produced a test able to reproduce the crash or exhausts the allocated **search time budget**.

# Botsing Demo

<iframe width="560" height="315" src="https://www.youtube.com/embed/k6XaQjHqe48" frameborder="0" allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
