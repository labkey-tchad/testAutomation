/*
 * Copyright (c) 2007-2014 LabKey Corporation
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

package org.labkey.test.ms2;

import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.ListHelper;

import static org.junit.Assert.*;

public abstract class AbstractMS2SearchEngineTest extends MS2TestBase
{
    protected static final String TEST_ASSAY_NAME = "AutomatedTestAssay";
    private static final String ANNOTATION_RUN_NAME = "Automated Test Annotation Run";

    abstract protected void doCleanup(boolean afterTest) throws TestTimeoutException;

    abstract protected void setupEngine();

    abstract protected void basicChecks();

    public void basicMS2Check()
    {
        createProjectAndFolder();

        log("Start analysis running.");
        clickAndWait(Locator.linkWithText("MS2 Dashboard"));
        clickButton("Process and Import Data");
        _fileBrowserHelper.importFile("bov_sample/CAexample_mini.mzXML", "Create New Mass Spec Metadata Assay Design");

        log("Create a new MS2 sample prep assay definition.");
        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.xpath("//input[@id='AssayDesignerName']"), TEST_ASSAY_NAME);

        _listHelper.addField("Run Fields", 0, "IntegerField", "IntegerField", ListHelper.ListColumnType.Integer);
        _listHelper.addField("Run Fields", 1, "TextField", "TextField", ListHelper.ListColumnType.String);
        _listHelper.addField("Run Fields", 2, "BooleanField", "BooleanField", ListHelper.ListColumnType.Boolean);

        sleep(1000);
        clickButton("Save", 0);
        waitForText("Save successful.", 20000);

        clickAndWait(Locator.linkWithText("MS2 Dashboard"));
        clickButton("Process and Import Data");
        _fileBrowserHelper.importFile("bov_sample/CAexample_mini.mzXML", "Use " + TEST_ASSAY_NAME);

        log("Describe MS2 run.");
        clickButton("Next");
        setFormElement("name", ANNOTATION_RUN_NAME);
        setFormElement("integerField", "10");
        setFormElement("textField", "Text value");
        click(Locator.checkboxByName("booleanField"));

        int seconds = 0;
        while (!isTextPresent("<None>") && seconds < 20)
        {
            seconds++;
            sleep(1000);
        }
        selectOptionByText(Locator.id("sampleSetListBox0"), "<None>");
        setFormElement(Locator.id("sampleTextBox0"), "verify:001");

        clickButton("Save and Finish");

        log("Return to search page");
        clickAndWait(Locator.linkWithText("MS2 Dashboard"));

        assertElementPresent(Locator.linkWithText(ANNOTATION_RUN_NAME));

        clickButton("Process and Import Data");
        _fileBrowserHelper.selectFileBrowserItem("bov_sample/");
        setupEngine();

        waitForElement(Locator.xpath("//select[@name='sequenceDB']/option[.='" + DATABASE + "']" ), WAIT_FOR_JAVASCRIPT);
        assertTextPresent("Minimum PeptideProphet prob", "Minimum ProteinProphet prob", "Quantitation engine");

        searchMS2LibraCheck();

        log("Set analysis parameters.");
        setFormElement("protocolName", "test2");
        setFormElement("protocolDescription", "This is a test protocol for Verify.");
        selectOptionByText(Locator.name("sequenceDB"), DATABASE);
        setFormElement("configureXml", "");
        waitAndClick(Locator.xpath("//a[@class='labkey-button']/span[text() = 'OK']"));
        setFormElement("configureXml", INPUT_XML);
        assertTextPresent("Quantitation mass tolerance", "Quantitation residue mass label");
        setFormElement("minPeptideProphetProb", "0");
        clickButton("Search");
        // Search is submitted as AJAX, and upon success the browser is redirected to a new page. Wait for it to load
        waitForElement(Locator.linkWithText("Data Pipeline"), WAIT_FOR_JAVASCRIPT);
        sleep(5000); // without this sleep, some machines try to redirect back to the begin.view page after the Data Pipeline link is clicked
        log("View the analysis log.");
        clickAndWait(Locator.linkWithText("Data Pipeline"));

        waitForPipelineJobsToComplete(1, SAMPLE_BASE_NAME + " (test2)", false);

        clickAndWait(Locator.xpath("//a[contains(text(), '" + SAMPLE_BASE_NAME + " (test2)')]/../../td/a"));

        log("View log file.");

        pushLocation();
        clickAndWait(Locator.linkWithText(LOG_BASE_NAME + ".log"));

        log("Verify log.");
        assertTextPresent("search");
        popLocation();

        if(isQuickTest)
            return;

        log("Analyze again.");
        clickAndWait(Locator.linkWithText("MS2 Dashboard"));
        clickButton("Process and Import Data");
        _fileBrowserHelper.selectFileBrowserItem("bov_sample/");

        setupEngine();

        log("Make sure new protocol is listed.");
        waitForElement(Locator.xpath("//select[@name='protocol']/option[.='test2']"), WAIT_FOR_JAVASCRIPT);
        assertEquals("test2", getSelectedOptionText(Locator.name("protocol")));

        if (!isElementPresent(Locator.linkWithText("running")) && isElementPresent(Locator.linkWithText("completed")))
            assertTextPresent("running");

        log("Verify no work for protocol.");
        assertNavButtonNotPresent("Search");

        log("View full status.");
        clickFolder(FOLDER_NAME);

        assertElementPresent(Locator.linkContainingText(SAMPLE_BASE_NAME + " (test2)"));

        clickAndWait(Locator.tagWithAttribute("a", "title", "Experiment run graph"));

        log("Verify msPicture");
        assertElementPresent(Locator.imageMapLinkByTitle("graphmap", ANNOTATION_RUN_NAME));
        pushLocation();
        clickAndWait(Locator.imageMapLinkByTitle("graphmap", "Data: " + SAMPLE_BASE_NAME + ".mzXML.image..itms.png (Run Output)"));
        assertElementPresent(Locator.linkWithText("msPicture"), 2);
        beginAt(getAttribute(Locator.xpath("//img[contains(@src, 'showFile.view')]"), "src"));
        // Firefox sets the title of the page when we view an image separately from an HTML page, so use that to verify
        // that we got something that matches what we expect. IE doesn't do this, so assume that we're good if we don't
        // get a 404, error message, etc
        if(getBrowserType() == BrowserType.FIREFOX)
            assertTitleContains("showFile.view (PNG Image");
        popLocation();

        log("Verify experiment view");
        clickAndWait(Locator.imageMapLinkByTitle("graphmap", "bov_sample/" + SAMPLE_BASE_NAME + " (test2)"));

        log("Verify experiment run view.");
        clickAndWait(Locator.imageMapLinkByTitle("graphmap", "Data: CAexample_mini.mzXML"));
        assertTextPresent("bov_sample/" + SAMPLE_BASE_NAME);
        assertTextPresent("Data File CAexample_mini.mzXML");
        assertTextPresent("AutomatedTestAssay");

        clickAndWait(Locator.linkWithText(ANNOTATION_RUN_NAME));
        clickAndWait(Locator.imageMapLinkByTitle("graphmap", "Material: verify:001"));

        assertTextPresent("verify:001");
        assertTextPresent("Not a member of a sample set");

        clickAndWait(Locator.linkWithText("MS2 Dashboard"));
        clickAndWait(Locator.linkWithImage(getContextPath() + "/MS2/images/runIcon.gif"));

        // Make sure we're not using a custom default view for the current user
        selectOptionByText(Locator.name("viewParams"), "<Standard View>");
        clickButton("Go");
        selectOptionByText(Locator.name("grouping"), "Peptides (Legacy)");
        clickAndWait(Locator.id("viewTypeSubmitButton"));

        log("Test adding columns");
        clickButton("Pick Peptide Columns");
        clickButton("Pick", 0);
        clickButton("Pick Columns");
        assertTextPresent("Run Description");
        assertTextPresent("Next AA");

        basicChecks();
    }

    protected  void searchMS2LibraCheck()
    {
        selectOptionByText(Locator.xpath("//tr[td/table/tbody/tr/td/div[contains(text(),'Quantitation engine')]]/td/select"),"Libra");
        assertTextPresent("Libra config name", "Libra normalization channel");
        setFormElement(Locator.xpath("//tr[td/table/tbody/tr/td/div[text()='Libra config name']]/td/input"), "foo");
        fireEvent(Locator.xpath("//tr[td/table/tbody/tr/td/div[text()='Libra config name']]/td/input"), SeleniumEvent.change);
        String text = getFormElement(Locator.name("configureXml"));
        assertTrue(text.contains("foo"));
    }
}
