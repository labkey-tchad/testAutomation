/*
 * Copyright (c) 2013-2017 LabKey Corporation
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
package org.labkey.test.tests;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PermissionsHelper;
import org.openqa.selenium.WebElement;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@Category({DailyA.class})
public class AdminConsoleTest extends BaseWebDriverTest
{
    private final boolean IS_BOOTSTRAP_LAYOUT_WHITELISTED = setIsBootstrapWhitelisted(true);
    protected static final String APP_ADMIN_USER = "app_admin_test_user@adminconsole.test";
    protected static final String APP_ADMIN_USER_PASS = PasswordUtil.getPassword();

    public String getProjectName()
    {
        return null;
    }

    @Test
    public void testRibbonBar()
    {
        goToAdminConsole().clickSiteSettings();
        waitForElement(Locator.name("showRibbonMessage"));
        Locator.name("ribbonMessageHtml").findElement(getDriver()).clear();

        WebElement checkbox = Locator.checkboxByName("showRibbonMessage").findElement(getDriver());

        //only select if not already checked
        if (!("true".equals(checkbox.getAttribute("checked"))))
            click(Locator.checkboxByName("showRibbonMessage"));

        clickButton("Save");

        waitForElement(Locator.xpath("//div[contains(text(), 'Cannot enable the ribbon message without providing a message to show')]"));

        String linkText = "and also click this...";
        String html = "READ ME!!!  <a href='<%=contextPath%>/project/home/begin.view'>" + linkText + "</a>";

        //only check if not already checked
        checkbox = Locator.checkboxByName("showRibbonMessage").findElement(getDriver());
        if (!("true".equals(checkbox.getAttribute("checked"))))
            click(Locator.checkboxByName("showRibbonMessage"));

        setFormElement(Locator.name("ribbonMessageHtml"), html);
        clickButton("Save");

        Locator ribbon = Locator.tagWithClass("div", "alert alert-warning").containing("READ ME!!!");
        waitForElement(ribbon);

        Locator ribbonLink = Locator.tagWithClass("div", "default-template-alert").append(Locator.linkContainingText("and also click this..."));
        assertElementPresent(ribbonLink);
        String href = ribbonLink.findElement(getDriver()).getAttribute("href");
        String expected = WebTestHelper.getBaseURL() + "/project/home/begin.view";
        assertEquals("Incorrect URL", expected, href);

        goToHome();
        impersonateRole("Reader");
        assertElementPresent(ribbon);
        assertElementPresent(ribbonLink);
        stopImpersonating();

        goToAdminConsole().clickSiteSettings();
        waitForElement(Locator.name("showRibbonMessage"));
        click(Locator.checkboxByName("showRibbonMessage"));
        clickButton("Save");
        assertElementNotPresent(ribbon);
        assertElementNotPresent(ribbonLink);
    }

    @Test
    public void testAppAdminRole()
    {
       // log out as siteAdmin, log in as appAdmin
        signOut();
        signIn(APP_ADMIN_USER, APP_ADMIN_USER_PASS);
        clickButton("Submit");

        // verify that all of the following links are visible to AppAdmin:
        goToAdminConsole().goToAdminConsoleLinksSection();
        List<String> actualLinkTexts = new ArrayList<>();
        List<String> expectedLinkTexts = Arrays.asList("change user properties",
                "folder types",
                "look and feel settings",
                "missing value indicators",
                "profiler",
                "project display order",
                "short urls",
                "audit log",
                "etl- all job histories",
                "etl- run site scope etls",
                "full-text search",
                "ms1",
                "pipeline",
                "site-wide terms of use",
                "actions",
                "caches",
                "credits",
                "data sources",
                "dump heap",
                "environment variables",
                "memory usage",
                "queries",
                "reset site errors",
                "running threads",
                "site validation",
                "system properties",
                "test email configuration",
                "view all site errors",
                "view all site errors since reset",
                "view primary site log file");
        for(String linkText: expectedLinkTexts)
        {
            WebElement elem = Locator.linkWithText(linkText).findElementOrNull(getDriver());
            if (elem != null)
                actualLinkTexts.add(linkText);
        }
        assertEquals(expectedLinkTexts, actualLinkTexts);

        // confirm that NONE of the following are visible to AppAdmin:
        List<String> notShownLinks = Arrays.asList("files","flow cytometry","experimental features","mascot server",
                "ldap sync admin","notification service","ms2", "check database", "loggers", "sql scripts");
        for (String linkText: notShownLinks)
        {
            assertElementNotPresent(Locator.linkWithText(linkText));
        }

        //analytics settings
        URL url = getURL(); // will capture with redirect url
        goToAdminConsole().clickAnalyticsSettings();
        assertElementNotPresent(Locator.button("submit"));
        clickButton("done");
        assertTrue("expect to return to admin console" , url.toString().startsWith(getURL().toString()));
        url = getURL(); // will capture without redirect url

        //authentication
        goToAdminConsole().clickAuthentication();
        assertElementNotPresent("expect 'enable' links to be disabled for appAdmin", Locator.linkWithText("enable"));
        assertElementNotPresent("expect 'configure' links to be disabled for appAdmin", Locator.linkWithText("configure"));
        clickButton("Done");
        assertTrue("expect to return to admin console" , url.toString().startsWith(getURL().toString()));

        //email customization
        goToAdminConsole().clickEmailCustomization();
        clickButton("Cancel");
        assertTrue("expect to return to admin console" , url.toString().startsWith(getURL().toString()));

        //site settings
        goToAdminConsole().clickSiteSettings();
        clickButton("Done");
        assertTrue("expect to return to admin console" , url.toString().startsWith(getURL().toString()));

        //system maintenance
        goToAdminConsole().clickSystemMaintenance();
        clickButton("Done");
        assertTrue("expect to return to admin console" , url.toString().startsWith(getURL().toString()));

        // views and scripting
        goToAdminConsole().clickViewsAndScripting();
        assertNull(Locator.buttonContainingText("Edit").findElementOrNull(getDriver()));
        clickButton("Done");
        assertTrue("expect to return to admin console" , url.toString().startsWith(getURL().toString()));
    }

    public List<String> getAssociatedModules()
    {
        return Arrays.asList("admin");
    }

    @Override
    public void checkQueries()
    {

    }

    @Override
    public void checkViews()
    {

    }

    @BeforeClass
    public static void doSetup() throws Exception
    {
          AdminConsoleTest initTest = (AdminConsoleTest)getCurrentTest();
          initTest.createTestUser();
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _userHelper.deleteUsers(false, APP_ADMIN_USER);
    }

    private void createTestUser()
    {
        _userHelper.createUser(APP_ADMIN_USER, true, false);
        setInitialPassword(APP_ADMIN_USER, APP_ADMIN_USER_PASS);

        ApiPermissionsHelper apiPermissionsHelper = new ApiPermissionsHelper(this);
        apiPermissionsHelper.addMemberToRole(APP_ADMIN_USER, "Application Admin", PermissionsHelper.MemberType.user, "/");
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
