package org.labkey.test.tests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.labkey.test.BaseWebDriverTest;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PortalHelper;
import static org.junit.Assert.*;

/**
 * User: RyanS
 * Date: 11/19/13
 */

@Category(DailyA.class)
public class CustomizeEmailTemplateTest extends SpecimenBaseTest
{
    private static final String _projectName = "EmailTemplateProject";
    private final PortalHelper _portalHelper = new PortalHelper(this);
    private static final String _assayPlan = "assay plan";
    private static final String _shipping = "123 main street";
    private static final String _comments = "this is my comment";
    private static final String _delim = "::";
    private static final String _emailBody1 = "<Div id=\"body\">" +
            "action==^action^"  + _delim + "\n" +
            "attachments==^attachments^" + _delim + "\n" +
            "comments==^comments^" + _delim + "\n" +
            "contextPath==^contextPath^" + _delim + "\n" +
            "currentDateTime==^currentDateTime^" + _delim + "\n" +
            "destinationLocation==^destinationLocation^" + _delim + "\n" +
            "folderName==^folderName^" + _delim + "\n" +
            "folderPath==^folderPath^" + _delim + "\n" +
            "folderURL==^folderURL^" + _delim + "\n" +
            "homePageURL==^homePageURL^" + _delim + "\n" +
            "modifiedBy==^modifiedBy^" + _delim + "\n" +
            "organizationName==^organizationName^" + _delim + "\n" +
            "simpleStatus==^simpleStatus^" + _delim + "\n" +
            "siteShortName==^siteShortName^" + _delim + "\n" +
            "specimenList==^specimenList^" + _delim + "\n" +
            "specimenRequestNumber==^specimenRequestNumber^" + _delim + "\n" +
            "status==^status^" + _delim + "\n" +
            "studyName==^studyName^" + _delim + "\n" +
            "subjectSuffix==^subjectSuffix^" + _delim + "\n" +
            "supportLink==^supportLink^" + _delim + "\n" +
            "systemDescription==^systemDescription^" + _delim + "\n" +
            "systemEmail==^systemEmail^" +
            "<Div>";

    @Nullable
    @Override
    protected String getProjectName()
    {
        return _projectName;
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    @LogMethod(category = LogMethod.MethodType.SETUP)
    protected void doCreateSteps()
    {
        enableEmailRecorder();
        initializeFolder();

        clickButton("Create Study");
        setFormElement(Locator.name("label"), getStudyLabel());
        click(Locator.radioButtonByNameAndValue("simpleRepository", "false"));
        clickButton("Create Study");

        setPipelineRoot(getPipelinePath());
        startSpecimenImport(1);
        waitForSpecimenImport();
        setupRequestStatuses();
        setupActorsAndGroups();
        setupRequestForm();
        setupActorNotification();
        setCustomNotificationTemplate();
        createSpecimenRequest();
    }

    private void setCustomNotificationTemplate()
    {
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Notifications"));
        checkCheckbox(Locator.checkboxById("newRequestNotifyCheckbox"));
        waitForElement(Locator.xpath("//textarea[@id='newRequestNotify']"));
        setFormElement(Locator.id("newRequestNotify"), "ryans@labkey.com");
        clickButton("Save");
        clickAndWait(Locator.linkWithText("Manage Notifications"));
        clickAndWait(Locator.linkWithText("Edit Email Template"));
        selectOptionByText(Locator.name("templateClass"), "Specimen request notification");
        setFormElement(Locator.id("emailSubject"), _projectName);
        setFormElement(Locator.id("emailMessage"), _emailBody1);
        clickButton("Save");
    }

    private void createSpecimenRequest()
    {
        clickTab("Specimen Data");
        click(Locator.linkWithText("By Individual Vial"));
        click(Locator.xpath("//img[@src=\"/labkey/_images/cart.png\"]/../../../../..//td[@class=\"labkey-selectors\"]/input"));
        clickMenuButton("Request Options", "Create New Request");
        selectOptionByText(Locator.name("destinationLocation"), DESTINATION_SITE);
        setFormElement(Locator.id("input0"), _assayPlan);
        setFormElement(Locator.id("input2"), _comments);
        setFormElement(Locator.id("input1"), _shipping);
        setFormElement(Locator.id("input3"), "sample last one input");
        clickButton("Create and View Details");
        clickButton("Submit Request", 0);
        assertAlert("Once a request is submitted, its specimen list may no longer be modified.  Continue?");
    }

    @Override
    protected void doVerifySteps() throws Exception
    {
        goToModule("Dumbster");
        waitForElement(Locator.linkContainingText(_projectName));
        click(Locator.linkContainingText(_projectName));
        String emailBody= getTextFromElement(Locator.xpath("//Div[@id='body']"));
        String[] bodyContents = emailBody.split(_delim);
        Map<String, String> emailNVPs = new HashMap<>();
        for (String line : bodyContents)
        {
            String[] nvp = line.split("==");
            if(nvp.length==2)
            {
                emailNVPs.put(nvp[0].trim(), nvp[1]);
            }
            if(nvp.length==1)
            {
                emailNVPs.put(nvp[0].trim(), "");
            }
        }
        Assert.assertEquals("New Request", emailNVPs.get("status"));
        Assert.assertEquals("New Request Created", emailNVPs.get("action"));
        Assert.assertEquals("/labkey", emailNVPs.get("contextPath"));
        Assert.assertEquals("My Study", emailNVPs.get("folderName"));
        Assert.assertEquals("/EmailTemplateProject/My Study", emailNVPs.get("folderPath"));
        Assert.assertEquals("http://localhost:8080/labkey", emailNVPs.get("homePageURL"));
    }


    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/study";
    }

}
