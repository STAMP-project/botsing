# Botsing Demo

## Download Botsing
Download the latest version of `botsing-reproduction.jar` from [here](https://github.com/STAMP-project/botsing/releases).

## Prepare data for running Botsing
For using Botsing, you need to provide two data: 
 - **The compiled version of the software under test (SUT):** a folder containing all the  `.jar` files required to run the software under test. As an example, you can see `application/LANG-9b`.
 - **Crash Log:** the file with the stack trace. The stack trace should be clean (no error message) and cannot contain any nested exceptions. As an example, you can see `crashes/LANG-9b.log`.
 
 ## Run Botsing
 Run botsing with the following command:

```
java -jar <Path_to_botsing-reproduction.jar> -project_cp <Path_to_Directory_with_SUT_jars> -crash_log <Path_to_crash_log> -target_frame <Level_of_target_frame> -Dsearch_budget=<time_in_second> -D<property=value>  
```

`<Path_to_botsing-reproduction.jar>` Path to the latest version of the botsing-reproduction jar file.

`<Path_to_Directory_with_SUT_jars>` Path to the compiled version of the software under test.

`<Path_to_crash_log>` Path to a file File with the stack trace.

`<Level_of_target_frame>` This argument indicates that the user wants to replicate the first `n` lines of the stack trace.

`-Dsearch_budget=<time_in_second>` Maximum search duration in seconds.

`-D<property=value>` Since Botsing uses EvoSuite during the search process, the user can set the evosuite properties through Botsing. For more information about the EvoSuite options click [here](https://github.com/EvoSuite/evosuite/blob/master/client/src/main/java/org/evosuite/Properties.java).



As an example, you can run the following sample command in this demo:
 
 
```
java -jar botsing-reproduction.jar -project_cp application/LANG-9b -crash_log crashes/LANG-9b.log -target_frame 3 -Dsearch_budget=120
```

After running this command, Botsing strives to generate a replicator test, which throws a `java.lang.ArrayIndexOutOfBoundsException` (as is indicated in `crashes/LANG-9b.log`) including first 3 frames of this stack trace, for 2 minutes.

Botsing stops searching either when it reaches the replicator test or when its search budget is finished.

## Using the results of Botsing execution

If Botsing achieves to the replicator test, it will save it in the output directory (`CrashReproduction-tests` by default).
If we run this test on the software under test, it throws the same stack trace as the given one.

For instance, one of the result of the sample command, which we mentioned in the previous section, is the following test (The generated test is different everytime):
```java
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
```
if we remove the try/catch from this test and run it, it will throw the following exception:

```
java.lang.ArrayIndexOutOfBoundsException:
	at org.apache.commons.lang3.time.FastDateParser.toArray(FastDateParser.java:413)
	at org.apache.commons.lang3.time.FastDateParser.getDisplayNames(FastDateParser.java:381)
	at org.apache.commons.lang3.time.FastDateParser$TextStrategy.addRegex(FastDateParser.java:664)
```

If we want to try replicating more frames of the `crashes/LANG-9b.log`, we should re-run the command by a higher value for the target_frame.