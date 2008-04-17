package org.labkey.test;

import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.SeleniumException;
import junit.framework.TestCase;
import org.apache.commons.lang.time.FastDateFormat;
import static org.labkey.test.WebTestHelper.*;
import org.labkey.test.util.Crawler;
import org.labkey.test.util.PasswordUtil;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: Mark Igra
 * Date: Feb 7, 2007
 * Time: 5:31:38 PM
 */
public abstract class BaseSeleniumWebTest extends TestCase implements Cleanable, WebTest
{
    protected DefaultSeleniumWrapper selenium;
    private static final int DEFAULT_SELENIUM_PORT = 4444;
    private static final String DEFAULT_SELENIUM_SERVER = "localhost";
    private String _lastPageTitle = null;
    private URL _lastPageURL = null;
    private String _lastPageText = null;
    private Stack<String> _locationStack = new Stack<String>();
    private List<String> _createdProjects = new ArrayList<String>();
    private List<FolderIdentifier> _createdFolders = new ArrayList<FolderIdentifier>();
    protected boolean _testFailed = true;
    protected int defaultWaitForPage = 60000;
    public final static int WAIT_FOR_GWT = 5000;
    protected int longWaitForPage = defaultWaitForPage * 5;
    private boolean _fileUploadAvailable;

    private static final int MAX_SERVER_STARTUP_WAIT_SECONDS = 60;
    
    private final static String FIREFOX_BROWSER = "*firefox";
    private final static String FIREFOX_UPLOAD_BROWSER = "*chrome";
    private final static String IE_BROWSER = "*iexplore";
    //protected final static String IE_UPLOAD_BROWSER = "*iehta";

    public BaseSeleniumWebTest()
    {

    }

    public static int getSeleniumServerPort() {
        String portString = System.getProperty("selenium.server.port", "" + DEFAULT_SELENIUM_PORT);
        return Integer.parseInt(portString);
    }

    public static int getSeleniumServer() {
        String portString = System.getProperty("selenium.server", DEFAULT_SELENIUM_SERVER);
        return Integer.parseInt(portString);
    }

    public String getLabKeyRoot()
    {
        return WebTestHelper.getLabKeyRoot();
    }

    public String getContextPath()
    {
        return WebTestHelper.getContextPath();
    }

    public void setUp() throws Exception {
        selenium = new DefaultSeleniumWrapper();
        selenium.start();
        //Now inject our standard javascript functions...
        InputStream inputStream = BaseSeleniumWebTest.class.getResourceAsStream("seleniumHelpers.js");
        String script = getStreamContentsAsString(inputStream);
        System.out.println("Loading scripts from seleniumHelpers.js");
        System.out.println(selenium.getEval(script));
    }

    /**
     * Override if using file upload features in the test. Returning true will attempt to use
     * a version of the browser that allows file upload fields to be set. Defaults to false.
     * Use isFileUploadAvailable to see if request worked.
     * @return
     */
    protected boolean isFileUploadTest()
    {
        return false;
    }

    public boolean isFileUploadAvailable()
    {
        return _fileUploadAvailable;
    }

    public String getBrowser()
    {
        String browser = System.getProperty("selenium.browser", FIREFOX_BROWSER);
        String browserPath = System.getProperty("selenium.browser.path", "");
        if (browserPath.length() > 0)
            browserPath = " " + browserPath;

        if (!FIREFOX_BROWSER.equals(browser))
                fail("Due to XPATH problems on IE, only firefox is currently supported");
        
        //File upload is "experimental" in selenium, so only use it when
        //necessary
        if (isFileUploadTest())
        {
            if(FIREFOX_BROWSER.equals(browser))
            {
                browser = FIREFOX_UPLOAD_BROWSER;
                _fileUploadAvailable = true;
            }
        }

        return browser + browserPath;
    }

    static String getStreamContentsAsString(InputStream is) throws IOException
    {
        StringBuffer contents = new StringBuffer();
        BufferedReader input = null;

        try
        {
            input = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = input.readLine()) != null)
            {
                contents.append(line);
                contents.append("\n");
            }
        }
        finally
        {
            try
            {
                if (input != null) input.close();
            }
            catch (IOException e)
            {
            }
        }
        return contents.toString();
    }

    public void copyFile(File original, File copy)
    {
        InputStream fis = null;
        OutputStream fos = null;
        try
        {
            copy.getParentFile().mkdirs();
            fis = new BufferedInputStream(new FileInputStream(original));
            fos = new BufferedOutputStream(new FileOutputStream(copy));
            int read;
            byte[] buffer = new byte[1024];
            while ((read = fis.read(buffer, 0, buffer.length)) > 0)
                fos.write(buffer, 0, read);
        }
        catch (IOException e)
        {
            fail(e.getMessage());
        }
        finally
        {
            if (fis != null) try { fis.close(); } catch (IOException e) {}
            if (fos != null) try { fos.close(); } catch (IOException e) {}
        }
    }



    public void tearDown() throws Exception {
        boolean skipTearDown = _testFailed && System.getProperty("close.on.fail", "true").equalsIgnoreCase("false");
        if (!skipTearDown)
            selenium.stop();
    }

    public void log(String str)
    {
        System.out.println(str);
    }

    private static final Pattern LABKEY_ERROR_TITLE_PATTERN = Pattern.compile("\\d\\d\\d\\D.*Error.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOMCAT_ERROR_PATTERN = Pattern.compile("HTTP Status\\s*(\\d\\d\\d)\\D");
    public int getResponseCode()
    {
        //We can't seem to get response codes via javascript, so we rely on default titles for error pages
        String title = selenium.getTitle();
        if (!title.toLowerCase().contains("error"))
            return 200;

        Matcher m = LABKEY_ERROR_TITLE_PATTERN.matcher(title);
        if (m.matches())
            return Integer.parseInt(title.substring(0, 3));

        //Now check the Tomcat page. This is going to be unreliable over time
        m = TOMCAT_ERROR_PATTERN.matcher(getResponseText());
        if (m.find())
            return Integer.parseInt(m.group(1));

        return 200;
    }

    public String getResponseText()
    {
        return selenium.getHtmlSource();
    }

    public URL getURL()
    {
        try
        {
            return new URL(selenium.getLocation());
        }
        catch (MalformedURLException x)
        {
            throw new RuntimeException("Bad location from selenium tester: " + selenium.getLocation(), x);
        }
    }

    public String[] getLinkAddresses()
    {
        String js = "getLinkAddresses();";
        String linkStr = selenium.getEval(js);
        String[] linkArray = linkStr.split("\\\\n");
        ArrayList<String> links = new ArrayList<String>(linkArray.length);
        for (String link : linkArray)
            if (link != null && link.trim().length() > 0 && !link.startsWith("#"))
                links.add(link);

        return links.toArray(new String[links.size()]);
    }


    public List<String> getCreatedProjects()
    {
        return _createdProjects;
    }

    public String getCurrentRelativeURL()
    {

        URL url = getURL();
        String urlString = selenium.getLocation();
        if ("80".equals(WebTestHelper.getWebPort()) && url.getAuthority().endsWith(":-1"))
        {
            int portIdx = urlString.indexOf(":-1");
            urlString = urlString.substring(0, portIdx) + urlString.substring(portIdx + (":-1".length()));
        }

        String baseURL = WebTestHelper.getBaseURL();
        assertTrue("Expected URL to begin with " + baseURL + ", but found " + urlString, urlString.indexOf(baseURL) == 0);
        return urlString.substring(baseURL.length());
    }

    public void pushLocation()
    {
        _locationStack.push(getCurrentRelativeURL());
    }

    public void popLocation()
    {
        String location = _locationStack.pop();
        assertNotNull("Cannot pop without a push.", location);
        beginAt(location);
    }

    public void refresh()
    {
        refresh(defaultWaitForPage);
    }

    public void refresh(int millis)
    {
        selenium.refresh();
        waitForPageToLoad(millis);
    }


    public void sleep(long ms)
    {
        try
        {
            Thread.sleep(ms);
        }
        catch (InterruptedException e)
        {
        }
    }
    
    public void signIn()
    {
        try
        {
            PasswordUtil.ensureCredentials();
        }
        catch (IOException e)
        {
            fail("Unable to ensure credentials: " + e.getMessage());
        }
        waitForStartup();
        log("Signing in");
        //
        beginAt("/login/logout.view");
        checkForUpgrade();
        beginAt("/login/login.view");
        assertTitleEquals("Sign In");
        assertFormPresent("login");
        setText("email", PasswordUtil.getUsername());
        setText("password", PasswordUtil.getPassword());
        submit("SUBMIT");

        if (isTextPresent("Type in your email address and password"))
            fail("Could not log in with the saved credentials.  Please verify that the test user exists on this installation or reset the credentials using 'ant setPassword'");
        assertTextPresent("Sign out");
        assertTextPresent("My Account");

        ensureAdminMode();
    }

    public void ensureAdminMode()
    {
        //Now switch to admin mode if available
        if (isLinkPresentWithText("Show Admin"))
        {
            clickLinkWithText("Show Admin");
        }
    }
    
    private void waitForStartup()
    {
        boolean hitFirstPage = false;
        log("Verifying that server has started...");
        long ms = System.currentTimeMillis();
        while (!hitFirstPage && ((System.currentTimeMillis() - ms)/1000) < MAX_SERVER_STARTUP_WAIT_SECONDS)
        {
            try
            {
                beginAt("/login/logout.view");

                if (getResponseCode() != 404)
                {
                    hitFirstPage = true;
                }
                else
                {
                    long elapsedMs = System.currentTimeMillis() - ms;
                    log("Server is not ready.  Waiting " + (MAX_SERVER_STARTUP_WAIT_SECONDS -
                            (elapsedMs / 1000)) + " more seconds...");
                }
            }
            catch (SeleniumException e)
            {
                // ignore timeouts that occur during startup; a poorly timed request
                // as the webapp is loading may hang forever, causing a timeout.
                log("Ignoring selenium exception: " + e.getMessage());
            }
            finally
            {
                if (!hitFirstPage)
                {
                    sleep(1000);
                }
            }

        }
        if (!hitFirstPage)
        {
            fail("Webapp failed to start up after " + MAX_SERVER_STARTUP_WAIT_SECONDS + " seconds.");
        }
        log("Server is running.");
    }

    private void checkForUpgrade()
    {
        // check to see if we're the first user:
        if (isTextPresent("You are the first user"))
        {
            assertTitleEquals("Register First User");
            log("Need to bootstrap");
            log("Trying to register some bad email addresses");
            pushLocation();
            setFormElement("email", "bogus@bogus@bogus");
            submit("register");
            assertTextPresent("The string 'bogus@bogus@bogus' is not a valid email address. Please enter an email address in this form: user@domain.tld");
            setFormElement("email", "");
            submit("register");
            assertTextPresent("The string '' is not a valid email address. Please enter an email address in this form: user@domain.tld");

            log("Registering with the test email address");
            setText("email", PasswordUtil.getUsername());
            submit("register");

            log("Attempting to register another initial user");
            popLocation();
            assertTextPresent("Initial user has already been created.");

            selenium.goBack();
            waitForPageToLoad();
            assertTextPresent("Type in a new password twice");
            assertTitleEquals("Choose a Password");

            log("Testing bad passwords");
            submit("set");
            assertTextPresent("Enter a valid password. Passwords must be six characters or more and can't match your email address.");

            setFormElement("password", "short");
            setFormElement("password2", "short");
            submit("set");
            assertTextPresent("Enter a valid password. Passwords must be six characters or more and can't match your email address.");

            setFormElement("password", "short");
            setFormElement("password2", "short");
            submit("set");
            assertTextPresent("Enter a valid password. Passwords must be six characters or more and can't match your email address.");

            setFormElement("password", PasswordUtil.getUsername());
            setFormElement("password2", PasswordUtil.getUsername());
            submit("set");
            assertTextPresent("Enter a valid password. Passwords must be six characters or more and can't match your email address.");

            setFormElement("password", "LongEnough");
            setFormElement("password2", "ButDontMatch");
            submit("set");
            assertTextPresent("Your password entries didn't match.");

            log("Set the test password");
            setText("password", PasswordUtil.getPassword());
            setText("password2", PasswordUtil.getPassword());
            submit("set");
        }

        if (isTitleEqual("Sign In"))
        {
            // if the logout page takes us to the sign-in page, then we may have a schema update to do:
            assertFormPresent("login");
            setText("email", PasswordUtil.getUsername());
            setText("password", PasswordUtil.getPassword());
            submit("SUBMIT");
            if (isNavButtonPresent("Express Install"))
                clickNavButton("Express Install");
            if (isNavButtonPresent("Express Upgrade"))
                clickNavButton("Express Upgrade");
            int waitMs = 10 * 60 * 1000; // we'll wait at most ten minutes
            while (waitMs > 0 && (!(isNavButtonPresent("Next") || isLinkPresentWithText("Home"))))
            {
                try
                {
                    // Pound the server aggressively with requests for the home page to test synchronization
                    // in the sql script runner.
                    for (int i = 0; i < 5; i++)
                    {
                        beginAt("/project/home/begin.view");
                        Thread.sleep(200);
                        waitMs -= 200;
                    }
                    Thread.sleep(2000);
                    waitMs -= 2000;
                    if (isTextPresent("error occurred"))
                        fail("A script failure occurred.");
                }
                catch (InterruptedException e)
                {
                    log(e.getMessage());
                }
                catch (SeleniumException e)
                {
                    log("Ignoring Selenium Error");
                    log(e.getMessage());
                }
            }

            if (waitMs <= 0)
                fail("Script runner took more than 10 minutes to complete.");

            if (isNavButtonPresent("Next"))
            {
                clickNavButton("Next");
                // Save the default site config properties
                clickNavButton("Save");
            }
            else
            {
                clickLinkWithText("Home");
            }
        }
    }


    public void populateLastPageInfo()
    {
        _lastPageTitle = getLastPageTitle();
        _lastPageURL = getLastPageURL();
        _lastPageText = getLastPageText();
    }

    public String getLastPageTitle()
    {
        if (_lastPageTitle == null)
        {
            if (null != selenium.getTitle())
                return selenium.getTitle();
            else
                return "[no title: content type is not html]";
        }
        return _lastPageTitle;
    }

    public String getLastPageText()
    {
        return _lastPageText != null ? _lastPageText : selenium.getHtmlSource();
    }

    public boolean isPageEmpty()
    {
        //IE and Firefox have different notions of empty.
        //IE returns html for all pages even empty text...
        String text = selenium.getHtmlSource();
        if (null == text || text.trim().length() == 0)
            return true;

        text = selenium.getText("//body");
        return null == text || text.trim().length() == 0;
    }

    public URL getLastPageURL()
    {
        try
        {
            return _lastPageURL != null ? _lastPageURL : new URL(selenium.getLocation());
        }
        catch (MalformedURLException x)
        {
            return null;
        }
    }

    public void resetErrors()
    {
        beginAt("/admin/resetErrorMark.view");
    }

    public void testSteps() throws Exception
    {
        try
        {
            Runner.setCurrentWebTest(this);
            log("\n\n=============== Starting " + getClass().getSimpleName() + Runner.getProgress() + " =================");
            signIn();
            if (getTargetServer().equals(DEFAULT_TARGET_SERVER))
                resetErrors();

            try
            {
                log("Pre-cleaning " + getClass().getSimpleName());
                doCleanup();
            }
            catch (Throwable t)
            {
                t.printStackTrace();
                // fall through
            }

            checkLeaksAndErrors();

            doTestSteps();

            checkLeaksAndErrors();

            if (enableLinkCheck())
            {
                Crawler crawler = new Crawler(this);
                crawler.crawlAllLinks();
            }

            checkLeaksAndErrors();
            _testFailed = false;

            try
            {
                if (!skipCleanup())
                    doCleanup();
                else
                    log("Skipping test cleanup as requested.");
            }
            catch (Throwable t)
            {
                log("WARNING: an exception occurred while cleaning up: " + t.getMessage());
                // fall through
            }

            checkLeaksAndErrors();

            try
            {
                signOut();
            }
            catch (Throwable t)
            {
                // fall through
            }
        }
        finally
        {
            try
            {
                populateLastPageInfo();
            }
            catch (Throwable t)
            {
                System.out.println("Unable to determine information about the last page: server not started or -Dlabkey.port incorrect?");
            }
            log("=============== Completed " + getClass().getSimpleName() + Runner.getProgress() + " =================");
        }
    }

    protected abstract void doTestSteps() throws Exception;

    protected abstract void doCleanup() throws Exception;

    public void cleanup() throws Exception
    {
        log("========= Cleaning up " + getClass().getSimpleName() + " =========");
        // explicitly go back to the site, just in case we're on a 404 or crash page:
        beginAt("");
        signIn();
        doCleanup();

        beginAt("");

        // The following checks verify that the test deleted all projects and folders that it created.
        for (FolderIdentifier folder : _createdFolders)
            assertLinkNotPresentWithText(folder.getFolderName());

        for (String projectName : _createdProjects)
            assertLinkNotPresentWithText(projectName);

        log("========= " + getClass().getSimpleName() + " cleanup complete =========");
    }

    private boolean skipCleanup()
    {
        return "false".equals(System.getProperty("clean"));
    }

    public boolean enableLinkCheck()
    {
        return "true".equals(System.getProperty("linkCheck"));
    }

    public boolean skipLeakCheck()
    {
        return "false".equals(System.getProperty("memCheck"));
    }

    public void checkLeaksAndErrors()
    {
        if (getTargetServer().equals(DEFAULT_TARGET_SERVER))
        {
            checkErrors();
            checkLeaks();
        }
    }

    public void checkLeaks()
    {
        if (skipLeakCheck())
            return;
        
        log("Starting memory leak check...");
        int leakCount = MAX_LEAK_LIMIT + 1;
        for (int attempt = 0; attempt < GC_ATTEMPT_LIMIT && leakCount > MAX_LEAK_LIMIT; attempt++)
        {
            if (attempt > 0)
            {
                log("Found " + leakCount + " in-use objects; rerunning GC.  ("
                        + (GC_ATTEMPT_LIMIT - attempt) + " attempt(s) remaining.)");
            }
            beginAt("/admin/memTracker.view?gc=true&clearCaches=true");
            if (!isTextPresent("In-Use Objects"))
                fail("Asserts must be enabled to track memory leaks; please add -ea to your server VM params and restart.");
            leakCount = getImageWithAltTextCount("expand/collapse");
        }

        if (leakCount > MAX_LEAK_LIMIT)
            fail(leakCount + " in-use objects exceeds allowed limit of " + MAX_LEAK_LIMIT + ".");
        else
            log("Found " + leakCount + " in-use objects.  This is within the expected number of " + MAX_LEAK_LIMIT + ".");
    }


    public void checkErrors()
    {
        // Need to remember our location or the next test could start with a blank page
        pushLocation();
        beginAt("/admin/showErrorsSinceMark.view");

        assertTrue("There were errors during the test run", isPageEmpty());
        log("No new errors found.");
        popLocation();
    }


    public File dumpHtml(File dir)
    {
        FileWriter writer = null;
        File file;
        try
        {
            FastDateFormat dateFormat = FastDateFormat.getInstance("yyyyMMddHHmm");
            file = new File(dir, dateFormat.format(new Date()) + getClass().getSimpleName() + ".html");
            writer = new FileWriter(file);
            writer.write(getLastPageText());
            return file;
        }
        catch (IOException e)
        {
            return null;
        }
        finally
        {
            if (writer != null)
                try
                {
                    writer.close();
                }
                catch (IOException e)
                {
                }
        }
    }


    public void beginAt(String relativeURL)
    {
        if (relativeURL.indexOf(getContextPath() + "/") == 0)
            relativeURL = relativeURL.substring(getContextPath().length() + 1);
        if (relativeURL.length() == 0)
            log("Navigating to root");
        else
        {
            log("Navigating to " + relativeURL);
            if (relativeURL.charAt(0) != '/')
            {
                relativeURL = "/" + relativeURL;
            }
        }
        selenium.open(getBaseURL() + relativeURL);
    }

    public void assertAlert(String msg)
    {
        assertEquals(msg, selenium.getAlert());
    }

    public void dismissAlerts()
    {
        boolean present = false;
        while (selenium.isAlertPresent())
            log("Found unexpected alert: " + selenium.getAlert());
    }

    public void logJavascriptAlerts()
    {
        while (selenium.isAlertPresent())
        {
            log("JavaScript Alert Ignored: " + selenium.getAlert());
        }
    }

    public void createProject(String projectName)
    {
        createProject(projectName, null);
    }

    public void createProject(String projectName, String folderType)
    {
        ensureAdminMode();
        log("Creating project with name " + projectName);
        if (isLinkPresentWithText(projectName))
            fail("Cannot create project; A link with text " + projectName + " already exists.  " +
                    "This project may already exist, or its name appears elsewhere in the UI.");
        clickLinkWithText("Create Project");
        setText("name", projectName);
        if (null != folderType)
            checkCheckbox("folderType", folderType, true);
        else
            checkCheckbox("folderType", "None", true);
        submit();
        _createdProjects.add(projectName);
    }

    public void createPermissionsGroup(String groupName)
    {
        log("Creating permissions group " + groupName);
        assertTextPresent("Permissions for /");
        setFormElement("name", groupName);
        submit(Locator.formWithAction("newGroup.post"));
    }

    

    public void createSubfolder(String project, String child, String[] tabsToAdd)
    {
        // create a child of the top-level project folder:
        createSubfolder(project, project, child, "None", tabsToAdd);
    }

    public void createSubfolder(String project, String parent, String child, String folderType, String[] tabsToAdd)
    {
        createSubfolder(project, parent, child, folderType, tabsToAdd, false);
    }

    public void createSubfolder(String project, String parent, String child, String folderType, String[] tabsToAdd, boolean inheritPermissions)
    {
        if (isLinkPresentWithText(child))
            fail("Cannot create folder; A link with text " + child + " already exists.  " +
                    "This folder may already exist, or the name appears elsewhere in the UI.");
        assertLinkNotPresentWithText(child);
        log("Creating subfolder " + child + " under project " + parent);
        clickLinkWithText(project);
        clickLinkWithText("Manage Folders");
        // click last index, since this text appears in the nav tree
        clickLinkWithText(parent, countLinksWithText(parent) - 1);
        clickNavButton("Create Subfolder");
        setText("name", child);
        checkCheckbox("folderType", folderType, true);
        submit();

        _createdFolders.add(new FolderIdentifier(project, child));
        if (!"None".equals(folderType))
        {
            if (inheritPermissions)
            {
                checkCheckbox("inheritPermissions");
                clickNavButton("Update");
            }
            clickNavButton("Done"); //Leave permissions where they are
            if (null == tabsToAdd || tabsToAdd.length == 0)
                return;

            clickLinkWithText("Customize Folder");
        }
        // verify that we're on the customize tabs page, then submit:
        assertTextPresent("Customize folder /" + project);

        if (tabsToAdd != null)
        {
            for (String tabname : tabsToAdd)
                toggleCheckboxByTitle(tabname, false);
        }

        submit();
        if ("None".equals(folderType))
        {
            if (inheritPermissions)
            {
                checkCheckbox("inheritPermissions");
                clickNavButton("Update");
            }
            clickNavButton("Done"); //Permissions
        }


        if (tabsToAdd != null)
        {
            for (String tabname : tabsToAdd)
                assertTabPresent(tabname);
        }

        // verify that there's a link to our new folder:
        assertLinkPresentWithText(child);
    }

    public void deleteFile(File parent)
    {
        if (parent.exists())
        {
            File[] children = parent.listFiles();
            if (children != null)
            {
                for (File child : children)
                {
                    deleteFile(child);
                }
            }
            if (!parent.delete())
            {
                log("Could not delete file " + parent);
                fail("Could not delete file " + parent);
            }
        }
    }

    public void deleteFolder(String project, String folderName)
    {
        log("Deleting folder " + folderName + " under project " + project);
        clickLinkWithText(project);
        ensureAdminMode();
        clickLinkWithText("Manage Folders");
        // click index 1, since this text appears in the nav tree as well as the folder management tree:
        clickLinkWithText(folderName, 1);
        clickNavButton("Delete");
        // confirm delete:
        clickNavButton("Delete");
        // verify that we're not on an error page with a check for a project link:
        assertLinkPresentWithText(project);
        assertLinkNotPresentWithText(folderName);
    }

    public void deleteProject(String project)
    {
        log("Deleting project " + project);
        clickLinkWithText(project);
        ensureAdminMode();
        //Delete even if terms of use is required
        if (isElementPresent(Locator.name("approvedTermsOfUse")))
        {
            clickCheckbox("approvedTermsOfUse", false);
            clickNavButton("Agree");
        }

        clickLinkWithText("Manage Folders");
        clickNavButton("Delete");
        // confirm delete:
        clickNavButton("Delete");
        // verify that we're not on an error page with a check for a project link:
        assertLinkNotPresentWithText(project);
    }

    public void addWebPart(String webPartName)
    {
        Locator.XPathLocator selects = Locator.xpath("//form[@action='addWebPart.view']//select[@name='name']");

        for (int i = 0; i <= 1; i++)
        {
            Locator loc = selects.index(i);
            String[] labels = selenium.getSelectOptions(loc.toString());
            for (String label : labels)
            {
                if (label.equals(webPartName))
                {
                    selenium.select(loc.toString(), webPartName);
                    submit(Locator.xpath("//form[@action='addWebPart.view']").index(i));
                    return;
                }
            }
        }

        throw new RuntimeException("Could not find webpart with name: " + webPartName);
    }

    
    public boolean isTitleEqual(String match)
    {
        return match.equals(selenium.getTitle());
    }

    public void assertTitleEquals(String match)
    {
        assertEquals("Wrong page title", match, selenium.getTitle());
    }

    public boolean isFormPresent(String form)
    {
        boolean present = isElementPresent(Locator.tagWithName("form", form));
        if (!present)
            present = isElementPresent(Locator.tagWithId("form", form));

        return present;
    }

    public void assertFormPresent(String form)
    {
        assertTrue("Form '" + form + "' was not present", isFormPresent(form));
    }

    public boolean isTextPresent(String text)
    {
        //Need to unencode here? Selenium turns &nbsp; into space???
        text = text.replace("&nbsp;", " ");
        return selenium.isTextPresent(text);
    }

    public String getText(Locator elementLocator)
    {
        return selenium.getText(elementLocator.toString());
    }

    public void assertTextPresent(String text)
    {
        text = text.replace("&nbsp;", " ");
        assertTrue("Text '" + text + "' was not present", isTextPresent(text));
    }

    public void assertTextPresent(String text, int amount)
    {
        assertTextPresent(text, amount, false);
    }

    public void assertTextPresent(String text, int amount, boolean browserDependent)
    {
        text = text.replace("&nbsp;", " ");
        String source = selenium.getHtmlSource();
        int current_index = 0;
        int count = 0;

        while ((current_index = source.indexOf(text, current_index + 1)) != -1)
            count++;

        if (browserDependent)
        {
            if (count == 0)
                log("Your browser is probably out of date");
            else
                assertTrue("Text '" + text + "' was not present " + amount + " times.  It was present " + count + " times", count == amount);
        }
        else
            assertTrue("Text '" + text + "' was not present " + amount + " times.  It was present " + count + " times", count == amount);
    }

    public void assertTextNotPresent(String text)
    {
        text = text.replace("&nbsp;", " ");
        assertFalse("Text '" + text + "' was present", isTextPresent(text));
    }

    public String getTextInTable(String dataRegion, int row, int column)
    {
        return selenium.getText("//table[@id='"+dataRegion+"']/tbody/tr["+row+"]/td["+column+"]");
    }

    public void assertTextAtPlaceInTable(String textToCheck, String dataRegion, int row, int column)
    {
       assertTrue(textToCheck+" is not at that place in the table", textToCheck.compareTo(getTextInTable(dataRegion, row, column))==0);
    }

    /**
     * Searches only the displayed text in the body of the page, not the HTML source.
     */
    public boolean isTextBefore(String text1, String text2)
    {
        String source = selenium.getBodyText();
        return (source.indexOf(text1) < source.indexOf(text2));
    }

    public void assertTextBefore(String text1, String text2)
    {
        assertTrue("'" + text1 + "' is not before '" + text2 + "'", isTextBefore(text1, text2));
    }

    public void waitForPageToLoad(int millis)
    {
        selenium.waitForPageToLoad(Integer.toString(millis));
    }

    public void waitForPageToLoad()
    {
        waitForPageToLoad(defaultWaitForPage);
    }

    public void waitFor(Checker checker, String failMessage, int wait)
    {
        int time = 0;
        while (!checker.check() && time < wait)
        {
            sleep(100);
            time += 100;
        }
        if (!checker.check())
            fail(failMessage);
    }

    protected File getTestTempDir()
    {
        File buildDir = new File(getLabKeyRoot(), "build");
        return new File(buildDir, "testTemp");
    }

    public interface Checker
    {
        public boolean check();
    }

    public void waitForElement(final Locator locator, int wait)
    {
        String failMessage = "Element with locator " + locator + " did not appear";
        waitFor(new Checker()
        {
            public boolean check()
            {
                return isElementPresent(locator);
            }
        }, failMessage, wait);
    }

    public void waitForText(final String text, int wait)
    {
        String failMessage = text + " did not appear";
        waitFor(new Checker()
        {
            public boolean check()
            {
                return isTextPresent(text);
            }
        }, failMessage, wait);
    }

    public void waitForImageWithSrc(final String src, int wait)
    {
        waitForImageWithSrc(src, wait, false);
    }

    public void waitForImageWithSrc(final String src, int wait, final boolean substringMatch)
    {
        String failMessage = "Image with src " + src + " did not appear";
        waitFor(new Checker()
        {
            public boolean check()
            {
                return isImagePresentWithSrc(src, substringMatch);
            }
        }, failMessage, wait);
    }

    public void submit()
    {
        submit(Locator.dom("document.forms[0]"));
    }

    public void submit(Locator formLocator)
    {
        selenium.submit(formLocator.toString());
        waitForPageToLoad();
    }
    
    public void submit(String buttonName)
    {
        Locator l = findButton(buttonName);
        selenium.click(l.toString());
        waitForPageToLoad();
    }

    //TODO: Make this really return a button, not just any input
    public Locator findButton(String name)
    {
        Locator l = Locator.tagWithName("input", name);
        if (isElementPresent(l))
            return l;

        return null;
    }

    public boolean isElementPresent(Locator loc)
    {
        return selenium.isElementPresent(loc.toString());
    }

    public void assertElementPresent(Locator loc)
    {
        assertTrue("Element '" + loc + "' is not present", isElementPresent(loc));   
    }

    public void assertFormElementEquals(String elementName, String value)
    {
        assertFormElementEquals(new Locator(elementName), value);
    }

    public void assertFormElementEquals(Locator loc, String value)
    {
        assertElementPresent(loc);
        assertEquals("Form element '" + loc + "' was not equal to '" + value + "'", selenium.getValue(loc.toString()), value);
    }

    public void assertFormElementNotEquals(Locator loc, String value)
    {
        assertElementPresent(loc);
        assertNotSame("Form element '" + loc + "' was equal to '" + value + "'", selenium.getValue(loc.toString()), value);
    }


    public boolean isFormElementPresent(String elementName)
    {
        return isElementPresent(Locator.dom("document.forms[0]['" + elementName + "']"));
    }

    public void assertFormElementPresent(String elementName)
    {
        assertTrue("Form element '" + elementName + "' was not present", isFormElementPresent(elementName));
    }


    public void assertOptionEquals(String selectName, String value)
    {
        assertOptionEquals(new Locator(selectName), value);
    }

    public void assertOptionEquals(Locator loc, String value)
    {
        assertElementPresent(loc);
        assertEquals("Option '" + loc + "' was not equal '" + value + "'", selenium.getSelectedLabel(loc.toString()), value);
    }

    public String getSelectedOptionText(Locator loc)
    {
        return selenium.getSelectedLabel(loc.toString());
    }

    public String getSelectedOptionText(String selectName)
    {
        return getSelectedOptionText(new Locator(selectName));
    }

    public void assertElementNotPresent(Locator loc)
    {
        assertFalse("Element was present in page: " + loc, isElementPresent(loc));
    }

    public boolean isLinkPresent(String linkId)
    {
        return isElementPresent(Locator.tagWithId("a", linkId));
    }

    public void assertLinkPresent(String linkId)
    {
        assertTrue("Link with id '" + linkId + "' was not present", isLinkPresent(linkId));
    }

    public void assertLinkNotPresent(String linkId)
    {
        assertFalse("Link with id '" + linkId + "' was present", isLinkPresent(linkId));
    }

    public boolean isLinkPresentWithText(String text)
    {
        log("Checking for link with exact text '" + text + "'");
        return isElementPresent(Locator.linkWithText(text));
    }

    public boolean isLinkPresentWithText(String text, int index)
    {
        return countLinksWithText(text) > index;
    }

    public boolean isLinkPresentContainingText(String text)
    {
        log("Checking for link containing text '" + text + "'");
        return isElementPresent(Locator.linkContainingText(text));
    }

    public void assertLinkPresentContainingText(String text)
    {
        assertTrue("Could not find link containing text '" + text + "'", isLinkPresentContainingText(text));
    }
    
    public void assertLinkPresentWithText(String text)
    {
        assertTrue("Could not find link with text '" + text + "'", isLinkPresentWithText(text));
    }

    public void assertLinkNotPresentWithText(String text)
    {
        assertFalse("Found a link with text '" + text + "'", isLinkPresentWithText(text));
    }

    public boolean isLinkPresentWithTitle(String title)
    {
        log("Checking for link with exact title '" + title + "'");
        return isElementPresent(Locator.linkWithTitle(title));
    }

    public void assertLinkPresentWithTitle(String title)
    {
        assertTrue("Could not find link with title '" + title + "'", isLinkPresentWithTitle(title));
    }

    public void assertLinkNotPresentWithTitle(String title)
    {
        assertFalse("Found a link with title '" + title + "'", isLinkPresentWithTitle(title));
    }

    public void clickLinkWithText(String text)
    {
        assertLinkPresentWithText(text);
        clickLinkWithText(text, true);
    }

    public void clickLinkWithText(String text, int index)
    {
        Locator l = Locator.linkWithText(text, index);
        assertElementPresent(l);
        clickAndWait(l, defaultWaitForPage);
    }
    
    public void clickLinkWithText(String text, boolean wait)
    {
        log("Clicking link with text '" + text + "'");
        Locator l = Locator.linkWithText(text);
        assertElementPresent(l);
        if (wait)
            clickAndWait(l, defaultWaitForPage);
        else
            clickAndWait(l, 0);
    }

    public void clickLinkContainingText(String text)
    {
        log("Clicking link containing text: " + text);
        Locator l  = Locator.linkContainingText(text);
        clickAndWait(l, defaultWaitForPage);
    }

    public int countLinksWithText(String text)
    {
        //TODO: Figure out how to count with a locator. For now still need to escape javascript string...
        String js = "countLinksWithText('" + text + "');";
        String count = selenium.getEval(js);
        return Integer.parseInt(count);
    }

    public void assertLinkPresentWithTextCount(String text, int count)
    {
        assertEquals("Link with text '" + text + "' was not present " + count + " times", countLinksWithText(text), count);
    }

    public boolean isLinkPresentWithImage(String imageName)
    {
        return isElementPresent(Locator.linkWithImage(imageName));
    }

    public boolean isButtonPresentWithImage(String imageName)
    {
        return isElementPresent(Locator.buttonWithImgSrc(imageName));
    }

    public void assertLinkPresentWithImage(String imageName)
    {
        assertTrue("Link with image '" + imageName + "' was not present", isLinkPresentWithImage(imageName));
    }

    public void assertLinkNotPresentWithImage(String imageName)
    {
        assertFalse("Link with image '" + imageName + "' was present", isLinkPresentWithImage(imageName));
    }

    public void clickLinkWithImage(String image)
    {
        clickLinkWithImage(image, defaultWaitForPage);
    }

    public void clickLinkWithImage(String image, int millis)
    {
        log("Clicking link with image: " + image);
        clickAndWait(Locator.linkWithImage(image), millis);
    }

    public void clickLinkWithImageByIndex(String image, int index)
    {
        log("Clicking link with image: " + image);
        clickAndWait(Locator.linkWithImage(image, index), defaultWaitForPage);
    }

    public void click(Locator l)
    {
        selenium.click(l.toString());
    }

    public void clickAndWait(Locator l)
    {
        clickAndWait(l, defaultWaitForPage);
    }

    public void clickAndWait(Locator l, int millis)
    {
        assertElementPresent(l);
        click(l);
        if (millis > 0)
            waitForPageToLoad(millis);
    }

    public void clickLink(String linkId)
    {
        clickLink(Locator.id(linkId));
    }

    public void clickLink(Locator l)
    {
        clickAndWait(l, defaultWaitForPage);
    }

    public void clickTab(String tabname)
    {
        log("Selecting tab " + tabname);
        assertLinkPresent(getTabLinkId(tabname));
        clickLink(getTabLinkId(tabname));
    }

    public void clickButtonWithImgSrc(String src)
    {
        clickButtonWithImgSrc(src, defaultWaitForPage);
    }

    public void clickButtonWithImgSrc(String src, int millis)
    {
        log("Clicking button with image src " + src);
        Locator l = Locator.buttonWithImgSrc(src);
        assertElementPresent(l);
        clickAndWait(l, millis);
    }

    public void clickButtonWithImgSrcByIndex(String src, int index)
    {
        log("Clicking button with image src " + src);
        Locator l = Locator.buttonWithImgSrc(src, index);
        assertElementPresent(l);
        clickAndWait(l, defaultWaitForPage);
    }

    public void clickImageWithAltText(String altText)
    {
        log("Clicking first image with alt text " + altText );
        Locator l = Locator.tagWithAttribute("img", "alt", altText);
        boolean present = isElementPresent(l);
        if (!present)
            fail("Unable to find image with altText " + altText);
        clickAndWait(l, defaultWaitForPage);
    }

    public int getImageWithAltTextCount(String altText)
    {
        String js = "function countImagesWithAlt(txt) {var doc=selenium.browserbot.getCurrentWindow().document; var count = 0; for (var i = 0; i < doc.images.length; i++) {if (doc.images[i].alt == txt) count++;} return count}; ";
        js = js + "countImagesWithAlt('" + altText + "');";
        String count = selenium.getEval(js);
        return Integer.parseInt(count);
    }

    public boolean isImagePresentWithSrc(String src)
    {
        return isImagePresentWithSrc(src, false);
    }

    public boolean isImagePresentWithSrc(String src, boolean substringMatch)
    {
        return isElementPresent(Locator.imageWithSrc(src, substringMatch));
    }

    public void assertImagePresentWithSrc(String src)
    {
        assertTrue(isImagePresentWithSrc(src));
    }

    public void assertImagePresentWithSrc(String src, boolean substringMatch)
    {
        assertTrue(isImagePresentWithSrc(src, substringMatch));
    }


    public String getTableCellText(String tableName, int row, int column)
    {
        return selenium.getTable(tableName + "." + row + "." + column);
    }

    public boolean isTableCellEqual(String tableName, int row, int column, String value)
    {
        return value.equals(getTableCellText(tableName, row, column));
    }

    public void assertTableCellTextEquals(String tableName, int row, int column, String value)
    {
        assertTrue(isTableCellEqual(tableName, row, column, value));
    }

    public void assertTableRowsEqual(String tableName, int startRow, String[][] cellValues)
    {
        for (int row = 0; row < cellValues.length; row++)
            for (int col = 0; col < cellValues[row].length; col++)
                assertTableCellTextEquals(tableName, row + startRow, col, cellValues[row][col]);
    }

    public void clickImageMapLinkByTitle(String imageMapName, String areaTitle)
    {
        clickAndWait(Locator.imageMapLinkByTitle(imageMapName, areaTitle), defaultWaitForPage);
    }

    public boolean isImageMapAreaPresent(String imageMapName, String areaTitle)
    {
        System.out.println("Checking for image map area " + imageMapName + ":" + areaTitle);
        return isElementPresent(Locator.imageMapLinkByTitle(imageMapName, areaTitle));
    }

    public void assertImageMapAreaPresent(String imageMapName, String areaTitle)
    {
        assertTrue("Image map '" + imageMapName + "' did not have an area title of '" + areaTitle + "'", isImageMapAreaPresent(imageMapName, areaTitle));
    }

    public void assertTabPresent(String tabText)
    {
        assertLinkPresent(getTabLinkId(tabText));
    }

    public void assertTabNotPresent(String tabText)
    {
        assertLinkNotPresent(getTabLinkId(tabText));
    }

    public void clickNavButton(String buttonText)
    {
        clickNavButton(buttonText, defaultWaitForPage);
    }

    public void clickNavButton(String buttonText, int waitMillis)
    {
        String imgName = buildNavButtonImagePath(buttonText);
        if (isLinkPresentWithImage(imgName))
            clickLinkWithImage(imgName, waitMillis);
        else
            clickButtonWithImgSrc(imgName, waitMillis);
    }

    public void clickNavButtonByIndex(String buttonText, int index)
    {
        String imgName = buildNavButtonImagePath(buttonText);
        if (isLinkPresentWithImage(imgName))
            clickLinkWithImageByIndex(imgName, index);
        else
            clickButtonWithImgSrcByIndex(imgName, index);
    }


    public String goToNavButton(String buttonText, String controller, String folderPath)
    {
        // Returns address of NavButton
        String imgName = buildNavButtonImagePath(buttonText);
        Locator navButton = Locator.linkWithImage(imgName);
        Locator navButtonLink = Locator.raw(navButton.toString().concat("/.."));
        String localAddress = getAttribute(navButtonLink, "href");
        return (getContextPath() + "/" + controller + folderPath + "/" + localAddress);
    }

    public void clickImgButtonNoNav(String buttonText)
    {
        clickNavButton(buttonText, 0);
    }


    public void clickNavButton(String buttonText, String style)
    {
        String imgName = buildNavButtonImagePath(buttonText, style);
        if (isLinkPresentWithImage(imgName))
            clickLinkWithImage(imgName);
        else
            clickButtonWithImgSrc(imgName);
    }

    public void setText(String elementName, String text)
    {
        if (elementName.toLowerCase().indexOf("password") >= 0)
            log("Setting text of " + elementName + " to ******");
        else
            log("Setting text of " + elementName + " to " + text);
        
        selenium.typeSilent(elementName, text);
    }

    public void setFormElement(String elementName, String text)
    {
        selenium.type(elementName, text);
    }

    public void setFormElement(String elementName, File file)
    {
        assertTrue("Test must be declared as file upload by overriding isFileUploadTest().", isFileUploadAvailable());
        selenium.type(elementName, file.getAbsolutePath());
    }

    public void setFormElement(Locator element, String text)
    {
        setFormElement(element.toString(), text);
    }

    public void setFormElement(String formElementName, String[] values)
    {
        for (int i = 0; i < values.length; i++)
            setFormElement(Locator.dom("document.forms[0][\"" + formElementName + "\"][" + i + "]"), values[i]);
    }

    public void setFilter(String regionName, String columnName, String filterType)
    {
        log("Setting filter in " + regionName + " for " + columnName+" to " + filterType.toLowerCase());
        click(Locator.id(regionName + ":" + columnName + ":filter"));
        selenium.select("compare_1", "label=" + filterType);
        clickNavButton("OK");
    }

    public void setFilter(String regionName, String columnName, String filterType, String filter)
    {
        log("Setting filter in " + regionName + " for " + columnName + " to " + filterType.toLowerCase() + " " + filter);
        click(Locator.id(regionName + ":" + columnName + ":filter"));
        selenium.select("compare_1", "label=" + filterType);
        setFormElement("value_1", filter);
        clickNavButton("OK");
    }

    public void setFilterAndWait(String regionName, String columnName, String filterType, String filter, int milliSeconds)
    {
        log("Setting filter in " + regionName + " for " + columnName + " to " + filterType.toLowerCase() + " " + filter);
        click(Locator.id(regionName + ":" + columnName + ":filter"));
        selenium.select("compare_1", "label=" + filterType);
        setFormElement("value_1", filter);
        clickNavButton("OK", milliSeconds);
    }

    public void setFilter(String regionName, String columnName, String filter1Type, String filter1, String filter2Type, String filter2)
    {
        log("Setting filter in " + regionName + " for " + columnName+" to " + filter1Type.toLowerCase() + " " + filter1 + " and " + filter2Type.toLowerCase() + " " + filter2);
        click(Locator.id(regionName + ":" + columnName + ":filter"));
        selenium.select("compare_1", "label=" + filter1Type);
        setFormElement("value_1", filter1);
        selenium.select("compare_2", "label=" + filter2Type);
        setFormElement("value_2", filter2);
        clickNavButton("OK");
    }

    public void clearFilter(String regionName, String columnName)
    {
        log("Clearing filter in " + regionName + " for " + columnName);
        click(Locator.id(regionName + ":" + columnName + ":filter"));
        clickNavButton("Clear Filter");
    }

    public void clearAllFilters(String regionName, String columnName)
    {
        log("Clearing filter in " + regionName + " for " + columnName);
        click(Locator.id(regionName + ":" + columnName + ":filter"));
        clickNavButton("Clear All Filters");
    }

    final static int MAX_TEXT_LENGTH = 2000;

    public void setLongTextField(String elementName, String text)
    {
        setFormElement(elementName, "");
        int offset = 0;
        text = text.replace("'", "\\'").replace("\r\n", "\\n").replace("\n", "\\n");
        String line = null;
        while (offset < text.length())
        {
            String postString = text.substring(offset, Math.min(offset + MAX_TEXT_LENGTH, text.length()));
            if (postString.length() > 1 && postString.charAt(postString.length() - 1) == '\\' && postString.charAt(postString.length() - 2) != '\\')
                postString = postString.substring(0, postString.length() -1);

            String evalString = "appendToFormField('" + elementName + "', '" + postString + "')";
            selenium.getEval(evalString);
            offset += postString.length();
        }
    }

    public void setLongTextField(Locator loc, String text)
    {
            setLongTextField(loc.toString(), text);
    }    


    public boolean isNavButtonPresent(String buttonText)
    {
        String imgName = buildNavButtonImagePath(buttonText);
        return isLinkPresentWithImage(imgName) || isButtonPresentWithImage(imgName);
    }

    public boolean isMenuButtonPresent(String buttonText)
    {
        String imgName = buildNavButtonImagePath(buttonText, "shadedMenu");
        return isLinkPresentWithImage(imgName) || isButtonPresentWithImage(imgName);
    }

    public void assertNavButtonPresent(String buttonText)
    {
        assertTrue("Nav button '" + buttonText + "' was not present", isNavButtonPresent(buttonText));
    }

    public void assertNavButtonNotPresent(String buttonText)
    {
        assertFalse("Nav button '" + buttonText + "' was present", isNavButtonPresent(buttonText));
    }

    public void assertMenuButtonPresent(String buttonText)
    {
        assertTrue("Nav button '" + buttonText + "' was not present", isMenuButtonPresent(buttonText));
    }

    public void assertMenuButtonNotPresent(String buttonText)
    {
        assertFalse("Menu button '" + buttonText + "' was present", isMenuButtonPresent(buttonText));
    }

    public void dataRegionPageFirst(String dataRegionName)
    {
        log("Clicking page first on data region '" + dataRegionName + "'");
        clickDataRegionPageLink(dataRegionName, "First Page");
    }

    public void dataRegionPageLast(String dataRegionName)
    {
        log("Clicking page last on data region '" + dataRegionName + "'");
        clickDataRegionPageLink(dataRegionName, "Last Page");
    }

    public void dataRegionPageNext(String dataRegionName)
    {
        log("Clicking page next on data region '" + dataRegionName + "'");
        clickDataRegionPageLink(dataRegionName, "Next Page");
    }

    public void dataRegionPagePrev(String dataRegionName)
    {
        log("Clicking page previous on data region '" + dataRegionName + "'");
        clickDataRegionPageLink(dataRegionName, "Previous Page");
    }

    private void clickDataRegionPageLink(String dataRegionName, String title)
    {
        clickAndWait(Locator.xpath("//table[@id='dataregion_header_" + dataRegionName + "']//a[@title='" + title + "']"));
    }

    /** Sets selection state for rows of the data region on the current page. */
    public void checkAllOnPage(String dataRegionName)
    {
        checkCheckbox(Locator.raw("document.forms['" + dataRegionName + "'].elements['.toggle']"));
    }

    /** Clears selection state for rows of the data region on the current page. */
    public void uncheckAllOnPage(String dataRegionName)
    {
        Locator toggle = Locator.raw("document.forms['" + dataRegionName + "'].elements['.toggle']");
        checkCheckbox(toggle);
        uncheckCheckbox(toggle);
    }

    /** Sets selection state for single rows of the data region. */
    public void checkDataRegionCheckbox(String dataRegionName, String value)
    {
        checkCheckbox(Locator.xpath("//form[@id='" + dataRegionName + "']//input[@name='.select' and @value='" + value + "']"));
    }

    /** Sets selection state for single rows of the data region. */
    public void checkDataRegionCheckbox(String dataRegionName, int index)
    {
        checkCheckbox(Locator.raw("document.forms['" + dataRegionName + "'].elements['.select'][" + index + "]"));
    }

    public void toggleCheckboxByTitle(String title, boolean radio)
    {
        log("Clicking checkbox with title " + title);
        Locator l = Locator.checkboxByTitle(title, radio);
        click(l);
    }

    public void clickCheckbox(String name, boolean radio)
    {
        Locator l = Locator.checkboxByName(name, radio);
        click(l);
    }

    public void checkCheckbox(String name, String value, boolean radio)
    {
        checkCheckbox(Locator.checkboxByNameAndValue(name, value, radio));
    }


    public void checkCheckbox(String name)
    {
        checkCheckbox(Locator.name(name));
    }
    
    public void checkCheckbox(Locator checkBoxLocator)
    {
        log("Checking checkbox " + checkBoxLocator);
       //NOTE: We don't use selenium.check() because it doesn't fire click events.
        if (!isChecked(checkBoxLocator))
            click(checkBoxLocator);
        logJavascriptAlerts();
    }

    public void checkCheckbox(String name, int index, boolean radio)
    {
        checkCheckbox(Locator.checkboxByName(name, radio).index(index));
    }

    public void uncheckCheckbox(String name)
    {
        uncheckCheckbox(Locator.name(name));
    }

    public void uncheckCheckbox(Locator checkBoxLocator)
    {
        log("Unchecking checkbox " + checkBoxLocator);
        //NOTE: We don't use selenium.uncheck() because it doesn't fire click events.
        if (isChecked(checkBoxLocator))
            click(checkBoxLocator);
        logJavascriptAlerts();
    }

    public boolean isChecked(Locator checkBoxLocator)
    {
        return selenium.isChecked(checkBoxLocator.toString());
    }

    public void selectOptionByValue(String selectName, String value)
    {
        selenium.select(selectName, "value=" + value);
    }

    public void selectOptionByValue(Locator loc, String value)
    {
        selectOptionByValue(loc.toString(), value);
    }

    public void selectOptionByText(String selectName, String text)
    {
        selenium.select(selectName, text);
    }

    public void addCustomizeViewOption(String tab, String column_name)
    {
        addCustomizeViewOption(tab, column_name, column_name);
    }

    public void addCustomizeViewOption(String tab, String column_id, String column_name)
    {
        // column_id refers to the form of the name used after "column_" and is necessary to specify if it is different
        // than the column_name that appears to the user

        selenium.click(tab + ".tab");
        selenium.click("column_" + column_id);
        clickNavButton("Add >>", 0);
        int millis = 0;
        while(!selenium.isElementPresent("//div[@id='" + tab + ".list.div']//td[text()='" + column_name + "']") && millis < defaultWaitForPage)
        {
            log("If this message is appearing multiple times, you probably need to specify the column_id");
            millis = millis + 100;
            sleep(100);
        }
        if (millis >= defaultWaitForPage)
            fail("Did not recognize addition of " + column_name);

    }

    public void addCustomizeViewColumn(String column_name)
    {
        addCustomizeViewColumn(column_name, column_name);
    }

    public void addCustomizeViewColumn(String column_id, String column_name)
    {
        // column_id refers to the form of the name used after "column_" and is necessary to specify if it is different
        // than the column_name that appears to the user

        log("Adding " + column_name + " column");
        addCustomizeViewOption("columns", column_id, column_name);
    }

    public void addCustomizeViewFilter(String column_name, String filter_type)
    {
        addCustomizeViewFilter(column_name, column_name, filter_type, "");
    }

    public void addCustomizeViewFilter(String column_name, String filter_type, String filter)
    {
        addCustomizeViewFilter(column_name, column_name, filter_type, filter);
    }

    public void addCustomizeViewFilter(String column_id, String column_name, String filter_type, String filter)
    {
        // column_id refers to the form of the name used after "column_" and is necessary to specify if it is different
        // than the column_name that appears to the user

        if (filter.compareTo("") == 0)
            log("Adding " + column_name + " filter of " + filter_type);
        else
            log("Adding " + column_name + " filter of " + filter_type + " " + filter);

        if (selenium.isElementPresent("//div[@id='filter.list.div']//td[text()='" + column_name + "'][1]"))
            log("This test method does not support adding multiple filters of the same type");

        addCustomizeViewOption("filter", column_id, column_name);
        selenium.click("//div[@id='filter.list.div']//td[text()='" + column_name + "']/../td[2]/select");
        selenium.select("//div[@id='filter.list.div']//td[text()='" + column_name + "']/../td[2]/select", filter_type);

        if (filter.compareTo("") != 0)
        {
            selenium.type("//div[@id='filter.list.div']//td[text()='" + column_name + "']/../td[3]/input", filter);
            selenium.fireEvent("//div[@id='filter.list.div']//td[text()='" + column_name + "']/../td[3]/input", "blur");
        }
    }

    public void addCustomizeViewSort(String column_name, String order)
    {
        addCustomizeViewSort(column_name, column_name, order);
    }

    public void addCustomizeViewSort(String column_id, String column_name, String order)
    {
        // column_id refers to the form of the name used after "column_" and is necessary to specify if it is different
        // than the column_name that appears to the user

        log("Adding " + column_name + " sort");
        addCustomizeViewOption("sort", column_id, column_name);
        selenium.click("//div[@id='sort.list.div']//td[text()='" + column_name + "']/../td[2]/select");
        selenium.select("//div[@id='sort.list.div']//td[text()='" + column_name + "']/../td[2]/select", "label=" + order);
    }

    public void removeCustomizeViewOption(String tab, String column_name)
    {
        selenium.click(tab + ".tab");
        selenium.click("//div[@id='" + tab + ".list.div']//td[text()='" + column_name + "']");
        selenium.click("//img[@alt='Delete']");
    }

    public void removeCustomizeViewColumn(String column_name)
    {
        log("Removing " + column_name + " column");
        removeCustomizeViewOption("columns", column_name);
    }

    public void removeCustomizeViewFilter(String column_name)
    {
        log("Removing " + column_name + " filter");
        selenium.click("filter.tab");

        selenium.click("//div[@id='filter.list.div']//td[text()='" + column_name + "']");
        selenium.click("//img[@alt='Delete']");
    }

    public void removeCustomizeViewFilter(int filter_place)
    {
        log("Removing filter at position " + filter_place);
        selenium.click("filter.tab");
        selenium.click("//div[@id='filter.list.div']/table/tbody/tr[" + (filter_place * 2) + "]/td[1]");
        selenium.click("//img[@alt='Delete']");
    }

    public void removeCustomizeViewSort(String column_name)
    {
        log("Removing " + column_name + " sort");
        removeCustomizeViewOption("sort", column_name);
    }

    public void clearCustomizeViewFilters()
    {
        selenium.click("filter.tab");
        while (selenium.isElementPresent("//div[@id='filter.list.div']/table/tbody/tr[2]/td[1]"))
            selenium.click("//img[@alt='Delete']");
    }

    public void clearCustomizeViewSorts()
    {
        selenium.click("sort.tab");
        while (selenium.isElementPresent("//div[@id='sort.list.div']/table/tbody/tr[1]/td[1]"))
            selenium.click("//img[@alt='Delete']");
    }

    public void clearCustomizeViewColumns()
    {
        selenium.click("columns.tab");
        while (selenium.isElementPresent("//div[@id='columns.list.div']/table/tbody/tr[1]/td[1]"))
            selenium.click("//img[@alt='Delete']");
    }

    public void moveCustomizeViewColumn(String column_name, boolean moveUp)
    {
        moveCustomizeViewOption("columns", column_name, moveUp);
    }

    public void moveCustomizeViewFilter(String column_name, boolean moveUp)
    {
        moveCustomizeViewOption("filter", column_name, moveUp);
    }

    public void moveCustomizeViewSort(String column_name, boolean moveUp)
    {
        moveCustomizeViewOption("sort", column_name, moveUp);
    }

    public void moveCustomizeViewOption(String tab, String column_name, boolean moveUp)
    {
        selenium.click(tab + ".tab");
        selenium.click("//div[@id='" + tab + ".list.div']//td[text()='" + column_name + "']");
        selenium.click("//td[@id='" + tab + ".controls']//img[@alt='Move " + (moveUp ? "Up" : "Down") + "']");
    }

    public void addUrlParameter(String parameter)
    {
        if (!getCurrentRelativeURL().contains(parameter))
            if (getCurrentRelativeURL().contains("?"))
                beginAt(getCurrentRelativeURL().concat("&" + parameter));
            else
                beginAt(getCurrentRelativeURL().concat("?" + parameter));
    }

    public void assertPermissionSetting(String groupName, String permissionSetting)
    {
        log("Checking permission setting for group " + groupName + " equals " + permissionSetting);
        assertEquals("Permission for '" + groupName + "' was not '" + permissionSetting + "'", selenium.getSelectedLabel(Locator.permissionSelect(groupName).toString()), permissionSetting);
    }

    public void setPermissions(String groupName, String permissionString)
    {
        log("Setting permissions for group " + groupName + " to " + permissionString);
        //setWorkingForm("updatePermissions");
        selenium.select(Locator.permissionSelect(groupName).toString(), permissionString);
        clickNavButton("Update");
        assertPermissionSetting(groupName, permissionString);
    }

    public void impersonate(String fakeUser)
    {
        clickLinkWithText("Admin Console");
        setFormElement("email", fakeUser);
        clickNavButton("Impersonate");
    }

    public void createUser(String userName, String cloneUserName)
    {
        ensureAdminMode();
        clickLinkWithText("Site Users");
        clickNavButton("Add Users");

        setFormElement("newUsers", userName );
        uncheckCheckbox("sendMail");
        if (cloneUserName != null)
        {
            checkCheckbox("cloneUserCheck");
            setFormElement("cloneUser", cloneUserName);
        }
        clickNavButton("Add Users");
    }

    public void deleteUser(String userEmail)
    {
        ensureAdminMode();
        clickLinkWithText("Site Users");
        String userXPath = "//table[@id=\"dataregion_Users\"]//td[text()=\"" + userEmail + "\"]";
        if (isElementPresent(new Locator(userXPath)))
        {
            checkCheckbox(new Locator(userXPath + "/../td[1]/input"));
            clickNavButton("Delete");
        }
    }

    public void goToPipelineItem(String item)
    {
        int time = 0;
        while (getText(Locator.raw("//td[contains(text(),'" + item + "')]/../td[2]/a")).compareTo("WAITING") == 0
                && time < defaultWaitForPage)
        {
            sleep(100);
            time += 100;
            refresh();
        }
        clickAndWait(Locator.raw("//td[contains(text(),'" + item + "')]/../td[2]/a"));
        waitForElement(Locator.linkWithImage("Data.button"), 5000);
        clickLinkWithImage("Data.button");
    }

    public List<Locator> findAllMatches(Locator.XPathLocator loc)
    {
        List<Locator> locators = new ArrayList<Locator>();
        for (int i = 0; ; i++)
        {
            if (isElementPresent(loc.index(i)))
                locators.add(loc.index(i));
            else
                return locators;
        }
    }


    public String getFileContents(String rootRelativePath)
    {
        if (rootRelativePath.charAt(0) != '/')
            rootRelativePath = "/" + rootRelativePath;
        File file = new File(getLabKeyRoot() + rootRelativePath);
        return getFileContents(file);
    }

    public String getFileContents(File file)
    {
        FileInputStream fis = null;
        BufferedReader reader = null;
        try
        {
            fis = new FileInputStream(file);
            reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder content = new StringBuilder();
            int read;
            char[] buffer = new char[1024];
            while ((read = reader.read(buffer, 0, buffer.length)) > 0)
                content.append(buffer, 0, read);
            return content.toString();
        }
        catch (IOException e)
        {
            fail(e.getMessage());
            return null;
        }
        finally
        {
            if (reader != null) try { reader.close(); } catch (IOException e) {}
            if (fis != null) try { fis.close(); } catch (IOException e) {}
        }
    }

    public void signOut()
    {
        log("Signing out");
        beginAt("/login/logout.view");
        assertLinkPresentWithText("Sign in");
    }

    /*
     * This assumes that you have added the "search" webpart to your project
     */
    public void searchFor(String projectName, String searchFor, int expectedResults, String titleName)
    {
        log("Searching Project : " + projectName + " for \"" + searchFor + "\".  Expecting to find : " + expectedResults + " results");
        clickLinkWithText(projectName);
        assertElementPresent(Locator.name("search"));
        setFormElement("search", searchFor);
        clickNavButton("Search");

        if(expectedResults == 0)
        {
            assertTextPresent("found no result");
        }
        else
        {
            assertTextPresent("found " + expectedResults + " result");
        }

        log("found \"" + expectedResults + "\" result of " + searchFor);
        if (titleName != null)
        {
            clickLinkWithText(titleName);
            assertTextPresent(searchFor);
        }
    }

    public void searchFor(String projectName, String searchFor, int expectedResults)
    {
        searchFor(projectName, searchFor, expectedResults, null);
    }


    public String getAttribute(Locator locator, String attributeName)
    {
        return selenium.getAttribute(locator.toString() + "@" + attributeName);
    }

    public int getDefaultWaitForPage()
    {
        return defaultWaitForPage;
    }

    public void setDefaultWaitForPage(int defaultWaitForPage)
    {
        this.defaultWaitForPage = defaultWaitForPage;
    }

    public class DefaultSeleniumWrapper extends DefaultSelenium
    {
        DefaultSeleniumWrapper()
        {
            super("localhost", getSeleniumServerPort(), getBrowser(), WebTestHelper.getBaseURL());
        }

        private void log(String s)
        {
            BaseSeleniumWebTest.this.log("selenium - " + s);
        }

        @Override
        public void click(String locator)
        {
            log("Clicking on element: " + locator);
            super.click(locator);
        }

        @Override
        public void doubleClick(String locator)
        {
            log("Double clicking on element: " + locator);
            super.doubleClick(locator);
        }

        @Override
        public void clickAt(String locator, String coordString)
        {
            log("Clicking on element " + locator + " at location " + coordString);
            super.clickAt(locator, coordString);
        }

        @Override
        public void doubleClickAt(String locator, String coordString)
        {
            log("Double clicking on element " + locator + " at location " + coordString);
            super.doubleClickAt(locator, coordString);
        }

        @Override
        public void fireEvent(String locator, String eventName)
        {
            log("Firing event " + eventName + " on element: " + locator);
            super.fireEvent(locator, eventName);
        }

        @Override
        public void keyPress(String locator, String keySequence)
        {
            log("Pressing key sequence " + keySequence + " on element: " + locator);
            super.keyPress(locator, keySequence);
        }

        @Override
        public void keyDown(String locator, String keySequence)
        {
            log("Sending key down " + keySequence + " on element " + locator);
            super.keyDown(locator, keySequence);
        }

        @Override
        public void keyUp(String locator, String keySequence)
        {
            log("Sending key up " + keySequence + " on element " + locator);
            super.keyUp(locator, keySequence);
        }

        @Override
        public void mouseOver(String locator)
        {
            log("MouseOver: " + locator);
            super.mouseOver(locator);
        }

        @Override
        public void mouseOut(String locator)
        {
            log("MouseOut: " + locator);
            super.mouseOut(locator);
        }

        @Override
        public void mouseDown(String locator)
        {
            log("MouseDown: " + locator);
            super.mouseDown(locator);
        }

        @Override
        public void mouseDownAt(String locator, String coordString)
        {
            log("MouseDownAt " + coordString + " for element "+ locator);
            super.mouseDownAt(locator, coordString);
        }

        @Override
        public void mouseUp(String locator)
        {
            log("MouseUp: " + locator);
            super.mouseUp(locator);
        }

        @Override
        public void mouseUpAt(String locator, String coordString)
        {
            log("MouseUpAt " + coordString + " for element "+ locator);
            super.mouseUpAt(locator, coordString);
        }

        @Override
        public void mouseMove(String locator)
        {
            log("MouseMove: "+ locator);
            super.mouseMove(locator);
        }

        @Override
        public void mouseMoveAt(String locator, String coordString)
        {
            log("MouseMoveAt " + coordString + " for element "+ locator);
            super.mouseMoveAt(locator, coordString);
        }

        public void typeSilent(String locator, String value)
        {
            super.type(locator, value);
        }
        
        @Override
        public void type(String locator, String value)
        {
            log("Set value of element " + locator + " to "+ value);
            super.type(locator, value);
        }

        @Override
        public void check(String locator)
        {
            log("Check: " + locator);
            super.check(locator);
        }

        @Override
        public void uncheck(String locator)
        {
            log("Uncheck: " + locator);
            super.uncheck(locator);
        }

        @Override
        public void select(String selectLocator, String optionLocator)
        {
            log("Select " + optionLocator + " from element " + selectLocator);
            super.select(selectLocator, optionLocator);
        }

        @Override
        public void addSelection(String locator, String optionLocator)
        {
            log("Add Selection " + optionLocator + " from element " + locator);
            super.addSelection(locator, optionLocator);
        }

        @Override
        public void removeSelection(String locator, String optionLocator)
        {
            log("Remove Selection " + optionLocator + " from element " + locator);
            super.removeSelection(locator, optionLocator);
        }

        @Override
        public void submit(String formLocator)
        {
            log("Submit form " + formLocator);
            super.submit(formLocator);
        }

        @Override
        public void open(String url)
        {
            setTimeout("" + BaseSeleniumWebTest.this.defaultWaitForPage);
            super.open(url);
        }

        @Override
        public void openWindow(String url, String windowID)
        {
            log("Open window " + windowID + " for url " + url);
            super.openWindow(url, windowID);
        }

        @Override
        public void selectWindow(String windowID)
        {
            log("Select window " + windowID);
            super.selectWindow(windowID);
        }

        @Override
        public void selectFrame(String locator)
        {
            log("Select frame " + locator);
            super.selectFrame(locator);
        }

        @Override
        public void waitForPopUp(String windowID, String timeout)
        {
            log("Waiting " + timeout + " ms for pop up " + windowID);
            super.waitForPopUp(windowID, timeout);
        }

        @Override
        public void goBack()
        {
            log("Go back");
            super.goBack();
        }

        @Override
        public void refresh()
        {
            log("Refresh ");
            super.refresh();
        }

        @Override
        public String getConfirmation()
        {
            return super.getConfirmation();
        }

        @Override
        public String getValue(String locator)
        {
            return super.getValue(locator);
        }

        @Override
        public void dragdrop(String locator, String movementsString)
        {
            log("dragdrop element " + locator + " movements: " + movementsString);
            super.dragdrop(locator, movementsString);
        }

        @Override
        public void dragAndDrop(String locator, String movementsString)
        {
            log("dragAndDrop element " + locator + " movements: " + movementsString);
            super.dragAndDrop(locator, movementsString);
        }

        @Override
        public void dragAndDropToObject(String locatorOfObjectToBeDragged, String locatorOfDragDestinationObject)
        {
            log("dragAndDrop element " + locatorOfObjectToBeDragged + " to element " + locatorOfDragDestinationObject);
            super.dragAndDropToObject(locatorOfObjectToBeDragged, locatorOfDragDestinationObject);
        }

        @Override
        public void windowFocus(String windowName)
        {
            log("windowFocus: " + windowName);
            super.windowFocus(windowName);
        }

        @Override
        public void windowMaximize(String windowName)
        {
            log("windowMaximize: " + windowName);
            super.windowMaximize(windowName);
        }

        @Override
        public void setCursorPosition(String locator, String position)
        {
            log("Set cursor position for " + locator + " to " + position);
            super.setCursorPosition(locator, position);
        }
    }
}
