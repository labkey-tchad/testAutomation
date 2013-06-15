/*
 * Copyright (c) 2005-2013 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.test;

import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestFailure;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import junit.runner.BaseTestRunner;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.labkey.test.testpicker.TestHelper;
import org.labkey.test.tests.BasicTest;
import org.labkey.test.tests.DatabaseDiagnosticsTest;
import org.labkey.test.util.Crawler;
import org.labkey.test.util.DevModeOnlyTest;
import org.labkey.test.util.JUnitFooter;
import org.labkey.test.util.JUnitHeader;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.MethodPerfAspect;
import org.labkey.test.util.PostgresOnlyTest;
import org.labkey.test.util.SqlserverOnlyTest;
import org.labkey.test.util.AdvancedSqlTest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * User: brittp
 * Date: Nov 15, 2005
 * Time: 5:18:46 PM
 */
public class Runner extends TestSuite
{
    private static final int DEFAULT_MAX_TEST_FAILURES = 10;
    private static final TestSet DEFAULT_TEST_SET = TestSet.DRT;
    private static Map<Test, Long> _testStats = new LinkedHashMap<>();
    private static int _testCount;
    private static List<Class> _remainingTests;
    private static List<String> _passedTests = new ArrayList<>();
    private static List<String> _failedTests = new ArrayList<>();
    private static List<String> _erroredTests = new ArrayList<>();

    private Set<TestFailure> _failures = new HashSet<>();
    private boolean _cleanOnly;

    private Runner(boolean cleanOnly)
    {
        _cleanOnly = cleanOnly;
    }

    private void updateRemainingTests(Test test, boolean failed, boolean errored)
    {
        Class testClass = getTestClass(test);
        _remainingTests.remove(testClass);
        if (failed)
            _failedTests.add(test.toString());
        else if (errored)
            _erroredTests.add(test.toString());
        else
            _passedTests.add(test.toString());
    }

    private static void writeRemainingTests()
    {
        ArrayList<String> failedAndRemaining = new ArrayList<>();
        failedAndRemaining.addAll(_failedTests);
        failedAndRemaining.addAll(_erroredTests);
        for (Class clazz : _remainingTests)
            failedAndRemaining.add(clazz.getName());
        writeClasses(failedAndRemaining, getRemainingTestsFile());
    }

    private static void writeClasses(List<String> tests, File file)
    {
        PrintWriter pw = null;

        try
        {
            pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));

            for (String test : tests)
                pw.println(test);
        }
        catch(IOException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            if (null != pw)
                pw.close();
        }
   }

    private static List<Class> readClasses(File file)
    {
        List<Class> testClasses = new ArrayList<>();

        if (file.exists())
        {
            BufferedReader reader = null;
            String line = null;

            try
            {
                reader = new BufferedReader(new FileReader(file));

                while ((line = reader.readLine()) != null)
                    if (null != StringUtils.trimToNull(line))
                    {
                        if (line.contains("$")) // Prevent exception for Junit tests.
                            testClasses.add(Class.forName(line.substring(0, line.indexOf('$'))));
                        else
                            testClasses.add(Class.forName(line));
                    }
            }
            catch (IOException e)
            {
                System.out.println("Error reading " + file.getAbsolutePath());
            }
            catch (ClassNotFoundException e)
            {
                System.out.println("Can't find class '" + line + "'");
            }
            finally
            {
                try
                {
                    if (null != reader)
                        reader.close();
                }
                catch(IOException e)
                {
                    //
                }
            }
        }

        return testClasses;
    }

    private static Class[] readClasses(File recentlyFailedTestsFile, List<Class> tests)
    {
        List<Class> recentlyFailedTests = readClasses(recentlyFailedTestsFile);
        ArrayList<Class> filteredRecentlyFailedTests = new ArrayList<>();

        for (Class item: recentlyFailedTests)
        {
            if (tests.contains(item))
            {
                filteredRecentlyFailedTests.add(item);
            }
        }
        
        return filteredRecentlyFailedTests.toArray(new Class[filteredRecentlyFailedTests.size()]);
    }

    private static File getRemainingTestsFile()
    {
        String labkeyRoot = WebTestHelper.getLabKeyRoot();
        return new File(labkeyRoot, "server/test/remainingTests.txt");
    }

    public static String getProgress()
    {
        int completed = _testCount -_remainingTests.size() + 1;
        return " (" + completed + " of " + _testCount + ")";
    }

    public static boolean isFinalTest()
    {
        return 0 == (_remainingTests.size() - 1);
    }

    private static void saveTestDuration(Test currentWebTest, long durationMs)
    {
        _testStats.put(currentWebTest, durationMs);
    }

    public void runTest(Test test, TestResult testResult)
    {
        long startTimeMs = System.currentTimeMillis();
        if (_cleanOnly)
        {
            try
            {
                if (test instanceof TestSuite)
                {
                    Enumeration<Test> en = ((TestSuite) test).tests();
                    while (en.hasMoreElements())
                    {
                        Test singleTest = en.nextElement();
                        if (singleTest instanceof Cleanable)
                            ((Cleanable) singleTest).cleanup();
                    }
                }
                else if (test instanceof Cleanable)
                    ((Cleanable) test).cleanup();
                else if (test instanceof JUnit4TestAdapter)
                {
                    JUnit4TestAdapter adapter = (JUnit4TestAdapter)test;
                    if (Cleanable.class.isAssignableFrom(adapter.getTestClass()))
                    {
                        Cleanable cleanable = (Cleanable)adapter.getTestClass().newInstance();
                        cleanable.cleanup();
                    }
                }
            }
            catch (Throwable t)
            {
                System.out.println("WARNING: failure cleaning test: " + t.getMessage());
                System.out.println("Failures may be expected if the test was already cleaned.");
                t.printStackTrace(System.out);
                // fall through
            }
        }
        else
        {
            boolean failed = false;
            boolean errored = false;
            
            int _maxTestFailures;
            _maxTestFailures = Integer.getInteger("maxTestFailures", DEFAULT_MAX_TEST_FAILURES); // 0 is unlimited

            if (_failedTests.size() + _erroredTests.size() < _maxTestFailures || _maxTestFailures <= 0)
            {
                int failCount = testResult.failureCount();
                int errorCount = testResult.errorCount();
                super.runTest(test, testResult);
                failed = testResult.failureCount() > failCount;
                errored = testResult.errorCount() > errorCount;
            }
            else
            {
                testResult.addError(test, new Throwable(test.toString() + " not run: reached " + _maxTestFailures + " failures."));
                errored = true;
            }

            if (failed)
                dumpFailures(testResult.failures());
            if (errored)
                dumpFailures(testResult.errors());
            updateRemainingTests(test, failed, errored);
            writeRemainingTests();
        }

        long testTimeMs = System.currentTimeMillis() - startTimeMs;
        saveTestDuration(test, testTimeMs);
        MethodPerfAspect.savePerfStats(test);

        if (_remainingTests.isEmpty())
        {
            writeTimeReport();
            if (_failedTests.isEmpty() && _erroredTests.isEmpty())
            {
                getRemainingTestsFile().delete();
            }
        }

        // Pause between tests for long test suites.  Will hopefully increase stability.
        int ms = Integer.getInteger("pauseBetweenTests.ms", 0);
        if(ms > 0)
        {
            try
            {
                Thread.sleep(ms);
            }
            catch (InterruptedException e)
            {
                /* ignore */
            }
        }
    }

    private void dumpFailures(Enumeration<TestFailure> failures)
    {
        while (failures.hasMoreElements())
        {
            TestFailure failure = failures.nextElement();
            if (!_failures.contains(failure))
            {
                _failures.add(failure);
                System.out.println();
                System.out.println(failure.failedTest());
                System.out.println(BaseTestRunner.getFilteredTrace(failure.trace()));
            }
        }
    }


    // Set up only the requested tests
    private static List<Class> getTestClasses(TestSet testSet, List<String> testNames)
    {
        Map<String, Class> nameMap = new HashMap<>();
        for (Class testClass : testSet.getTestList())
        {
            String simpleName = testClass.getSimpleName().toLowerCase();
            nameMap.put(simpleName, testClass);
            if (simpleName.endsWith("test"))
            {
                simpleName = simpleName.substring(0, simpleName.length() - 4);
                nameMap.put(simpleName, testClass);
            }
            // Allow "lists", "samplesets", etc.
            if (!simpleName.endsWith("s"))
            {
                nameMap.put(simpleName + "s", testClass);
            }
        }

        List<Class> testClasses = new ArrayList<>(testNames.size());

        for (String testName : testNames)
        {
            Class testClass = nameMap.get(testName.toLowerCase());
            if (testClass == null)
            {
                System.err.println("Couldn't find test '" + testName + "' in suite '" + testSet.name() + "'.  Valid tests are:");

                List<String> sortedTests = testSet.getTestNames();
                Collections.sort(sortedTests);

                for (String c : sortedTests)
                    System.err.println("    " + c);
                System.exit(1);
            }
            testClasses.add(testClass);
        }

        return testClasses;
    }


    private static TestSuite getSuite(List<Class> testClasses, boolean cleanOnly) throws Exception
    {
        // Remove duplicate tests (e.g., don't run "basic" test twice if bvt & drt are selected via ant test) but keep the order
        Set<Class> testClassesCopy = new LinkedHashSet<>(testClasses);
        TestSuite suite = new Runner(cleanOnly);

        addTests(suite, testClassesCopy);

        return suite;
    }

    private static void addTests(TestSuite suite, Set<Class> testClasses) throws Exception
    {
        for (Class testClass : testClasses)
        {
            Test test = null;
            boolean illegalTest = false;
            Boolean isServerSideTest = false;
            try
            {
                Method suiteMethod = testClass.getMethod("suite");
                test = (TestSuite)suiteMethod.invoke(null);
            }
            catch (NoSuchMethodException e)
            {
                // ok
            }
            /* TODO: reinstate once upgradeHelper is removed from JUnitTest
            // For now, fail suite if JUnit test fails to get its test list.
            catch (InvocationTargetException e)
            {
                test = new ErrorTest(testClass.getName(), e.getCause());
            }
            catch (IllegalAccessException e)
            {
                test = new ErrorTest(testClass.getName(), e.getCause());
            }
            */

            if (test == null)
            {
                Class interfaces[] = testClass.getInterfaces();
                String databaseType = System.getProperty("databaseType");
                String databaseVersion = System.getProperty("databaseVersion");
                for (Class i : interfaces)
                {
                    if (i.getName().equals(PostgresOnlyTest.class.getCanonicalName()))
                    {
                        if(databaseType != null && !("postgres".equals(databaseType) || "pg".equals(databaseType)))
                        {
                            illegalTest = true;
                            System.out.println("** Skipping " + testClass.getSimpleName() + " test for unsupported database: " + databaseType + " " + databaseVersion);
                        }
                        break;
                    }
                    if (i.getName().equals(SqlserverOnlyTest.class.getCanonicalName()))
                    {
                        if(databaseType != null && !("sqlserver".equals(databaseType) || "mssql".equals(databaseType)))
                        {
                            illegalTest = true;
                            System.out.println("** Skipping " + testClass.getSimpleName() + " test for unsupported database: " + databaseType + " " + databaseVersion);
                        }
                        break;
                    }
                    if (i.getName().equals(DevModeOnlyTest.class.getCanonicalName()))
                    {
                        if(!"true".equals(System.getProperty("devMode")))
                        {
                            illegalTest = true;
                            System.out.println("** Skipping " + testClass.getSimpleName() + ": server must be in dev mode");
                        }
                        break;
                    }
                    if (i.getName().equals(AdvancedSqlTest.class.getCanonicalName()))
                    {
                        if(databaseType != null && "2005".equals(databaseVersion))
                        {
                            illegalTest = true;
                            System.out.println("** Skipping " + testClass.getSimpleName() + " test for unsupported database: " + databaseType + " " + databaseVersion);
                        }
                        break;
                    }
                }
                test = new JUnit4TestAdapter(testClass);
            }
            else isServerSideTest = true;

            if (isServerSideTest && !"DRT".equals(System.getProperty("suite")) && !"CONTINUE".equals(System.getProperty("suite")))
            {
                // Clear errors and enable dumbster before JUnitTest runs.
                suite.addTest(new JUnit4TestAdapter(JUnitHeader.class));
            }
            if(!illegalTest)
                suite.addTest(test);

            if (isServerSideTest && !"DRT".equals(System.getProperty("suite")) && !"CONTINUE".equals(System.getProperty("suite")))
            {
                // Check for leaks and errors after JUnitTest runs
                suite.addTest(new JUnit4TestAdapter(JUnitFooter.class));
            }
        }
    }

    // for error reporting
    private static class ErrorTest extends TestCase
    {
        Throwable t;

        ErrorTest(String name, Throwable t)
        {
            super(name);
            this.t = t;
        }

        @Override
        public String toString()
        {
            return getName();
        }

        @Override
        public void run(TestResult testResult)
        {
            testResult.startTest(this);
            testResult.addError(this, t);
            testResult.endTest(this);
        }
    }


    // A simple MultiMap
    private static class TestMap extends HashMap<String, Collection<Class>>
    {
        public Collection<Class> put(String key, Class clazz)
        {
            Collection<Class> collection = get(key);

            if (null == collection)
            {
                collection = new ArrayList<>();
                put(key, collection);
            }

            collection.add(clazz);
            return collection;
        }
    }


    private static void writeTimeReport()
    {
        int width = 60;
        long total = 0;
        System.out.println("======================= Time Report ========================");

        int totalCrawlTime = 0;
        int totalUniquePages = 0;
        int totalUniqueActions = 0;
        boolean crawl = false;

        for (Map.Entry<Test, Long> entry : _testStats.entrySet())
        {
            total += entry.getValue();
        }
        for (Map.Entry<Test, Long> entry : _testStats.entrySet())
        {
            String testName = entry.getKey().toString();
            long duration = entry.getValue();

            long percent = Math.round(100.0 * (duration / (double) total));
            String percentStr = (percent < 10 ? " " : "") + percent + "%";
            String durationAndPercent =
                (_passedTests.contains(testName) ? "passed" :
                    (_failedTests.contains(testName) ? "FAILED" :
                        (_erroredTests.contains(testName) ? "ERROR" : "not run"))) +
                " - " +
                formatDuration(duration) + " " + percentStr;
            testName = testName.substring(testName.lastIndexOf('.') + 1);

            System.out.println(getFixedWidthString(testName, durationAndPercent, width));

            HashMap<LogMethod.MethodType, Long> perfStats = MethodPerfAspect.getPerfStats(testName);
            Iterator it = perfStats.entrySet().iterator();
            while (it.hasNext())
            {
                Map.Entry<LogMethod.MethodType, Long> stats = (Map.Entry)it.next();
                System.out.println("\t" + stats.getKey() + "\t" + formatDuration(stats.getValue()));
            }

            if (Crawler.getCrawlStats().containsKey(testName))
            {
                crawl = true;
                Crawler.CrawlStats crawlStats = Crawler.getCrawlStats().get(testName);
                System.out.println(getFixedWidthString("Crawler Statistics: ", "", width));

                String[] statTitles = {"MaxDepth", "CrawlTime", "NewPages", "NewActions"};
                int crawlTestLengthSeconds = ((crawlStats.getCrawlTestLength() / 1000) % 60);
                String[] stats = { (Integer.toString(crawlStats.getMaxDepth())),
                        ((crawlStats.getCrawlTestLength() / 60000) + ":" + (crawlTestLengthSeconds >= 10 ? crawlTestLengthSeconds : ("0" + crawlTestLengthSeconds))),
                        Integer.toString(crawlStats.getNewPages()),
                        Integer.toString((crawlStats.getUniqueActions() - totalUniqueActions)) };

                int columnWidth = 10;
                System.out.println(getRowString(statTitles, columnWidth));
                System.out.println(getRowString(stats, columnWidth));

                totalCrawlTime += crawlStats.getCrawlTestLength();
                totalUniquePages += crawlStats.getNewPages();
                totalUniqueActions = crawlStats.getUniqueActions();
            }
        }
        if (crawl)
        {
            System.out.println(getFixedWidthString("Total Crawler Statistics: ", "", width));

            String[] statTitles = {"TotCrawlTime", "TotPages", "TotActions"};
            int totalCrawlTimeSeconds = ((totalCrawlTime / 1000) % 60);
            String[] stats = { ((totalCrawlTime / 60000) + ":" + (totalCrawlTimeSeconds >= 10 ? totalCrawlTimeSeconds : ("0" + totalCrawlTimeSeconds))),
                    Integer.toString(totalUniquePages),
                    Integer.toString(totalUniqueActions) };
            int columnWidth = 13;
            System.out.println(getRowString(statTitles, columnWidth));
            System.out.println(getRowString(stats, columnWidth));
        }
        System.out.println("------------------------------------------------------------");
        System.out.println(getFixedWidthString("Total duration:", formatDuration(total), width) + "\n");
        System.out.println("Completed " + FastDateFormat.getInstance("yyyy-MM-dd HH:mm").format(new Date()));
    }

    private static String formatDuration(long ms)
    {
        long min = ms / DateUtils.MILLIS_PER_MINUTE;
        ms = ms % DateUtils.MILLIS_PER_MINUTE;
        long sec = ms / DateUtils.MILLIS_PER_SECOND;
        return min + ":" + (sec < 10 ? "0" : "") + sec;
    }

    private static String getRowString(String[] list, int columnWidth)
    {
        String rowString = "";
        for (String item : list)
        {
            if (item.length() > columnWidth)
                item = item.substring(0, columnWidth);
            while (item.length() < columnWidth)
                item = item.concat(" ");
            rowString = rowString.concat(item);
        }
        return rowString;
    }

    private static String getFixedWidthString(String prefix, String suffix, int length)
    {
        int contentLength = prefix.length() + suffix.length();
        int padding = Math.max(0, length - contentLength);
        StringBuilder result = new StringBuilder();
        result.append(prefix);
        for (int i = 0; i < padding; i++)
            result.append(" ");
        result.append(suffix);
        return result.toString();
    }

    public static List<Class> getAllTests()
    {
        List<Class> tests = new ArrayList<>();
        for (TestSet testSet : TestSet.values())
        {
            for (Class testClass : testSet.getTestList())
            {
                if (!tests.contains(testClass))
                    tests.add(testClass);
            }
        }
        return tests;
    }

    protected static TestSet getTestSet()
    {
        String suiteName = System.getProperty("suite");
        if (suiteName != null)
        {
            try
            {
                return TestSet.valueOf(suiteName);
            }
            catch (Exception e)
            {
                System.out.println("Couldn't find suite '" + suiteName + "'.  Valid suites are:");
                for (TestSet s : TestSet.values())
                    System.out.println("   " + s.name());
            }
        }
        return DEFAULT_TEST_SET;
    }

    protected static List<String> getTestNames()
    {
        String testNames = System.getProperty("test");
        List<String> tests = new ArrayList<>();
        if (testNames != null && testNames.length() > 0)
        {
            String[] testNameArray = testNames.split(",");
            tests.addAll(Arrays.asList(testNameArray));
        }
        return tests;
    }

    protected static File getDumpDir()
    {
        File dumpDir = null;
        String outputDir = System.getProperty("failure.output.dir");
        if (outputDir != null)
            dumpDir = new File(outputDir);
        if (dumpDir == null || !dumpDir.exists())
            dumpDir = new File(System.getProperty("java.io.tmpdir"));
        if (!dumpDir.exists())
        {
            throw new RuntimeException("Couldn't determine directory for placement of output files. " +
                    "Tried system properties failure.output.dir and java.io.tmpdir");
        }
        return dumpDir;
    }

    /** Entry point for Ant JUnit runner. */
    public static TestSuite suite() throws Throwable
    {
        try
        {
            TestSet set = getTestSet();
            List<String> testNames = getTestNames();

            if (TestSet.TEST == set && testNames.isEmpty())
            {
                TestHelper.ResultPair pair = TestHelper.run();
                if (pair != null)
                {
                    set = pair.set;
                    testNames = pair.testNames;
                }
            }

            return suite(testNames, set);
        }
        catch (InvocationTargetException e)
        {
            System.err.print(BaseTestRunner.getFilteredTrace(e.getTargetException()));
            throw e.getTargetException();
        }
        catch (Exception e)
        {
            System.err.print(BaseTestRunner.getFilteredTrace(e));
            throw e;
        }
    }

    public static TestSuite suite(List<String> testNames, TestSet set) throws Exception
    {
        boolean skipLeakCheck = "false".equals(System.getProperty("memCheck"));
        boolean disableAssertions = "true".equals(System.getProperty("disableAssertions"));
        boolean cleanOnly = "true".equals(System.getProperty("cleanOnly"));
        boolean skipClean = "false".equals(System.getProperty("clean"));
        boolean shuffleTests = "true".equals(System.getProperty("shuffleTests"));
        boolean testRecentlyFailed = "true".equals(System.getProperty("testRecentlyFailed"));
        boolean testNewAndModified = "true".equals(System.getProperty("testNewAndModified"));
        String recentlyFailedTestsFile = System.getProperty("teamcity.tests.recentlyFailedTests.file");
        String changedFilesFile = System.getProperty("teamcity.build.changedFiles.file");

        if (cleanOnly && skipClean)
        {
            throw new RuntimeException("Invalid parameters: cannot specify both 'cleanOnly=true' and 'clean=false'.");
        }

        if (!skipLeakCheck && disableAssertions)
        {
            throw new RuntimeException("Invalid parameters: 'memCheck = true' and 'disableAssertions = true'.  Unable to do leak check with assertions disabled.");
        }

        if (TestSet.CONTINUE == set)
        {
            set.setTests(readClasses(getRemainingTestsFile()));
            if (shuffleTests)
            {
                set.randomizeTests();
            }
        }
        else if (TestSet.TEST == set && !testNames.isEmpty())
        {
            set.setTests(getAllTests());
        }
        else if (testNames.isEmpty())
        {
            if (shuffleTests)
            {
                set.randomizeTests();
            }
            if (testNewAndModified)
            {
                frontLoadTestsOfModifiedModules(set, changedFilesFile);
            }
            if (testRecentlyFailed && 0<recentlyFailedTestsFile.length())
            {
                //put previously failed tests at the front of the test queue (determined by TeamCity).
                Class[] recentlyFailedTests = readClasses(new File(recentlyFailedTestsFile), set.getTestList());
                for (Class test: recentlyFailedTests)
                {
                    set.prioritizeTest(test, 0);
                }
            }
        }

        set.prioritizeTest(BasicTest.class, 0); // Always start with BasicTest (if present)

        set.prioritizeTest(DatabaseDiagnosticsTest.class, set.getTestList().size() - 1); // Always end with DatabaseDiagnosticsTest (if present)

        List<Class> testClasses = testNames.isEmpty() ? set.getTestList() : getTestClasses(set, testNames);

        TestSuite suite = getSuite(testClasses, cleanOnly);

        if (suite.testCount() == 0)
        {
            System.out.println("No tests to run.");
        }
        else
        {
            _remainingTests = new ArrayList<>(suite.testCount());

            System.out.println("Running the following tests:");
            for (Enumeration<Test> e = suite.tests(); e.hasMoreElements(); )
            {
                Test test = e.nextElement();
                Class testClass = getTestClass(test);
                _remainingTests.add(testClass);
                System.out.println("  " + testClass.getSimpleName());
            }
            _testCount = _remainingTests.size();
            writeRemainingTests();
        }

        return suite;
    }

    private static void frontLoadTestsOfModifiedModules(TestSet set, String changedFilesFile)
    {
        List<String> moduleDirs = getModifiedModuleDirectories(changedFilesFile);

        // If changedFilesFile exists where TeamCity indicates then order the tests starting from most recently modified
        if (null != moduleDirs)
        {
            TestMap tm = new TestMap(); // Stores Tests, keyed by associated module directory.

            // Record the associated module directories for all selected tests.
            for (Class testClass : set.getTestList())
            {
                if (!WebTest.class.isAssignableFrom(testClass))
                    continue;
                try
                {
                    Constructor<WebTest> c = testClass.getConstructor();
                    WebTest test = null;
                    String directory = null;
                    try
                    {
                        test = c.newInstance();
                        directory = test.getAssociatedModuleDirectory();
                    }
                    finally
                    {
                        if (test != null)
                            ((BaseSeleniumWebTest)test).tearDown(); // Kill firefox window created by test
                    }

                    if (directory == null || directory.length() == 0)
                        continue;

                    File testDir = new File(WebTestHelper.getLabKeyRoot(), directory);

                    if (!testDir.exists())
                    {
                        throw new RuntimeException("Module directory \"" + directory + "\" specified in " + testClass + " does not exist!");
                    }

                    tm.put(directory, testClass);
                }
                catch(Exception e)
                {
                    System.out.println("Error: " + e);
                }
            }

            // Reorder tests by associated modules' modification date.
            int movedTests = 0;
            for (String moduleDir : moduleDirs)
            {
                Collection<Class> associatedTests = tm.get(moduleDir);

                if (null != associatedTests)
                {
                    for (Class test : associatedTests)
                    {
                        // Bubble up associated Test, if present.
                        if (set.prioritizeTest(test, movedTests))
                        {
                            movedTests++;
                        }
                    }
                }
            }
        }
    }

    // Return a list of modified module directories, ordered starting with most recently modified.
    private static List<String> getModifiedModuleDirectories(String changedFilesFile)
    {
        String labkeyRoot = WebTestHelper.getLabKeyRoot();
        File changedFiles = new File(changedFilesFile);
        Map<String, Long> moduleDirs = new HashMap<>(10);
        String modulePrefix = "server/modules/";

        if (changedFiles.exists())
        {
            BufferedReader reader = null;
            try
            {
                reader = new BufferedReader(new FileReader(changedFiles));
                String line;

                while ((line = reader.readLine()) != null)
                {
                    if (line.length() > 0 && line.charAt(0) != '?' && line.charAt(0) != '-' && line.charAt(0) != ':')
                    {
                        String path = line.substring(0, line.indexOf(':') - 1);

                        // If path starts with "server/modules/" then find the end index of module name.  If path doesn't
                        //  start with module prefix or the next separator is missing, set index to -1
                        int i = (path.startsWith(modulePrefix) ? path.indexOf("/", modulePrefix.length()) : -1);

                        // Anything outside "server/modules/" is labeled "none"
                        String moduleDir = (-1 == i ? "none" : path.substring(modulePrefix.length(), i));

                        // Note: We don't have a modification date for deleted or renamed files.  They end up with lastModified == 0 and are treated as the oldest modifications.
                        long lastModified = new File(labkeyRoot, path).lastModified();
                        Long mostRecent = moduleDirs.get(moduleDir);

                        if (null == mostRecent || lastModified > mostRecent)
                            moduleDirs.put(moduleDir, lastModified);
                    }
                }

                // Now sort modules by most recent file modification date
                Map<Long, String> orderedModuleDirs = new TreeMap<>();

                for (String moduleDir : moduleDirs.keySet())
                    orderedModuleDirs.put(-moduleDirs.get(moduleDir), moduleDir);  // Start with most recent change

                return new ArrayList<>(orderedModuleDirs.values());
            }
            catch(IOException e)
            {
                System.err.print(e.getMessage());
            }
            finally
            {
                if (reader != null) { try { reader.close(); } catch (IOException e) {} }
            }
        }

        return Collections.emptyList();
    }

    private static Class getTestClass(Test test)
    {
        if (test instanceof JUnit4TestAdapter)
            return ((JUnit4TestAdapter) test).getTestClass();
        else
            return test.getClass();
    }
}
