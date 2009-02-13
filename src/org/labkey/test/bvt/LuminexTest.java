/*
 * Copyright (c) 2007-2009 LabKey Corporation
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

package org.labkey.test.bvt;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.ListHelper;

import java.io.File;

/**
 * User: jeckels
 * Date: Nov 20, 2007
 */
public class LuminexTest extends AbstractAssayTest
{
    private final static String TEST_ASSAY_PRJ_LUMINEX = "Luminex Test";            //project for luminex test

    protected static final String TEST_ASSAY_LUM = "TestAssayLuminex";
    protected static final String TEST_ASSAY_LUM_DESC = "Description for Luminex assay";

    protected static final String TEST_ASSAY_LUM_ANALYTE_PROP_NAME = "testAssayAnalyteProp";
    protected static final int TEST_ASSAY_LUM_ANALYTE_PROP_ADD = 5;
    protected static final String[] TEST_ASSAY_LUM_ANALYTE_PROP_TYPES = { "Text (String)", "Boolean", "Number (Double)", "Integer", "DateTime" };
    protected static final String TEST_ASSAY_LUM_SET_PROP_SPECIES = "testSpecies1";
    protected static final String TEST_ASSAY_LUM_RUN_NAME = "testRunName1";
    protected static final String TEST_ASSAY_LUM_SET_PROP_SPECIES2 = "testSpecies2";
    protected static final String TEST_ASSAY_LUM_RUN_NAME2 = "testRunName2";
    protected static final String TEST_ASSAY_LUM_RUN_NAME3 = "WithIndices.xls";
    protected final String TEST_ASSAY_LUM_FILE1 = getLabKeyRoot() + "/sampledata/Luminex/10JAN07_plate_1.xls";
    protected final String TEST_ASSAY_LUM_FILE2 = getLabKeyRoot() + "/sampledata/Luminex/pnLINCO20070302A.xls";
    protected final String TEST_ASSAY_LUM_FILE3 = getLabKeyRoot() + "/sampledata/Luminex/WithIndices.xls";
    protected final String TEST_ASSAY_LUM_ANALYTE_PROP = "testAnalyteProp";
    private static final String THAW_LIST_NAME = "LuminexThawList";
    private static final String TEST_ASSAY_LUM_RUN_NAME4 = "testRunName4";

    public String getAssociatedModuleDirectory()
    {
        return "luminex";
    }

    /**
     * Performs Luminex designer/upload/publish.
     */
    protected void doTestSteps()
    {
        log("Starting Assay BVT Test");
        //revert to the admin user
        revertToAdmin();

        log("Testing Luminex Assay Designer");
        //create a new test project
        createProject(TEST_ASSAY_PRJ_LUMINEX);

        //setup a pipeline for it
        setupPipeline(TEST_ASSAY_PRJ_LUMINEX);

        //create a study within this project to which we will publish
        clickLinkWithText(TEST_ASSAY_PRJ_LUMINEX);
        addWebPart("Study Overview");
        clickNavButton("Create Study");
        clickNavButton("Create Study");
        clickLinkWithText(TEST_ASSAY_PRJ_LUMINEX);

        //add the Assay List web part so we can create a new luminex assay
        addWebPart("Assay List");

        //create a new luminex assay
        clickLinkWithText("Manage Assays");
        clickNavButton("New Assay Design");
        selectOptionByText("providerName", "Luminex");
        clickNavButton("Next");

        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_GWT);

        log("Setting up Luminex assay");
        selenium.type("//input[@id='AssayDesignerName']", TEST_ASSAY_LUM);
        selenium.type("//textarea[@id='AssayDesignerDescription']", TEST_ASSAY_LUM_DESC);

        sleep(1000);
        clickNavButton("Save", 0);
        waitForText("Save successful.", 20000);

        ListHelper.ListColumn participantCol = new ListHelper.ListColumn("ParticipantID", "ParticipantID", ListHelper.ListColumnType.String, "Participant ID");
        ListHelper.ListColumn visitCol = new ListHelper.ListColumn("VisitID", "VisitID", ListHelper.ListColumnType.Double, "Visit id");
        ListHelper.createList(this, TEST_ASSAY_PRJ_LUMINEX, THAW_LIST_NAME, ListHelper.ListColumnType.String, "Index", participantCol, visitCol);
        ListHelper.uploadData(this, TEST_ASSAY_PRJ_LUMINEX, THAW_LIST_NAME, "Index\tParticipantID\tVisitID\n" +
                "1\tListParticipant1\t1001.1\n" +
                "2\tListParticipant2\t1001.2\n" +
                "3\tListParticipant3\t1001.3\n" +
                "4\tListParticipant4\t1001.4");
        clickLinkWithText(TEST_ASSAY_PRJ_LUMINEX);

        clickLinkWithText("Assay List");
        clickLinkWithText(TEST_ASSAY_LUM);

        if(isFileUploadAvailable())
        {
            log("Uploading Luminex Runs");
            clickNavButton("Import Data");
            setFormElement("species", TEST_ASSAY_LUM_SET_PROP_SPECIES);
            clickNavButton("Next");
            setFormElement("name", TEST_ASSAY_LUM_RUN_NAME);
            File file1 = new File(TEST_ASSAY_LUM_FILE1);
            setFormElement("uploadedFile", file1);
            clickNavButton("Next", 60000);
            clickNavButton("Save and Import Another Run");
            clickLinkWithText(TEST_ASSAY_LUM);

            clickNavButton("Import Data");
            assertEquals(TEST_ASSAY_LUM_SET_PROP_SPECIES, selenium.getValue("species"));
            setFormElement("species", TEST_ASSAY_LUM_SET_PROP_SPECIES2);
            clickNavButton("Next");
            setFormElement("name", TEST_ASSAY_LUM_RUN_NAME2);
            setFormElement("uploadedFile", new File(TEST_ASSAY_LUM_FILE2));
            clickNavButton("Next", 60000);
            selenium.type("//input[@type='text' and contains(@name, '_analyte_')][1]", "StandardName1b");
            selenium.type("//input[@type='text' and contains(@name, '_analyte_')][1]/../../../tr[4]//input[@type='text']", "StandardName2");
            selenium.type("//input[@type='text' and contains(@name, '_analyte_')][1]/../../../tr[5]//input[@type='text']", "StandardName4");
            selenium.click("//input[contains(@name,'unitsOfConcentrationCheckBox')]");
            selenium.type("//input[@type='text' and contains(@name, 'unitsOfConcentration')]", "10 g/ml");
            clickNavButton("Save and Finish");

            // Upload another run using a thaw list pasted in as a TSV
            clickNavButton("Import Data");
            assertEquals(TEST_ASSAY_LUM_SET_PROP_SPECIES2, selenium.getValue("species"));
            setFormElement("participantVisitResolver", "Lookup");
            setFormElement("ThawListType", "Text");
            setFormElement("ThawListTextArea", "Index\tSpecimenID\tParticipantID\tVisitID\n" +
                    "1\tSpecimenID1\tParticipantID1\t1.1\n" +
                    "2\tSpecimenID2\tParticipantID2\t1.2\n" +
                    "3\tSpecimenID3\tParticipantID3\t1.3\n" +
                    "4\tSpecimenID4\tParticipantID4\t1.4");
            clickNavButton("Next");
            setFormElement("uploadedFile", new File(TEST_ASSAY_LUM_FILE3));
            clickNavButton("Next", 60000);
            assertEquals("StandardName1b", selenium.getValue("//input[@type='text' and contains(@name, '_analyte_')][1]"));
            assertEquals("StandardName4", selenium.getValue("//input[@type='text' and contains(@name, '_analyte_')][1]/../../../tr[4]//input[@type='text'][1]"));
            assertEquals("10 g/ml", selenium.getValue("//input[@type='text' and contains(@name, 'unitsOfConcentration')]"));
            assertEquals("10 g/ml", selenium.getValue("//input[@type='text' and contains(@name, '_analyte_')][1]/../../../tr[4]//input[@type='text' and contains(@name, 'unitsOfConcentration')]"));
            clickNavButton("Save and Finish");

            // Upload another run using a thaw list that pointed at the list we uploaded earlier
            clickNavButton("Import Data");
            assertEquals(TEST_ASSAY_LUM_SET_PROP_SPECIES2, selenium.getValue("species"));
            assertEquals("off", selenium.getValue("//input[@name='participantVisitResolver' and @value='SampleInfo']"));
            assertEquals("on", selenium.getValue("//input[@name='participantVisitResolver' and @value='Lookup']"));
            assertEquals("on", selenium.getValue("//input[@name='ThawListType' and @value='Text']"));
            assertEquals("off", selenium.getValue("//input[@name='ThawListType' and @value='List']"));
            setFormElement("ThawListType", "List");
            waitForElement(Locator.id("button_Choose list..."), BaseSeleniumWebTest.WAIT_FOR_GWT);
            clickNavButton("Choose list...", 0);
            setFormElement("schema", "lists");
            setFormElement("table", THAW_LIST_NAME);
            clickNavButton("Close", 0);
            clickNavButton("Next");
            setFormElement("name", TEST_ASSAY_LUM_RUN_NAME4);
            setFormElement("uploadedFile", new File(TEST_ASSAY_LUM_FILE3));
            clickNavButton("Next", 60000);
            assertEquals("StandardName1b", selenium.getValue("//input[@type='text' and contains(@name, '_analyte_')][1]"));
            assertEquals("StandardName4", selenium.getValue("//input[@type='text' and contains(@name, '_analyte_')][1]/../../../tr[4]//input[@type='text'][1]"));
            assertEquals("10 g/ml", selenium.getValue("//input[@type='text' and contains(@name, 'unitsOfConcentration')]"));
            assertEquals("10 g/ml", selenium.getValue("//input[@type='text' and contains(@name, '_analyte_')][1]/../../../tr[4]//input[@type='text' and contains(@name, 'unitsOfConcentration')]"));
            clickNavButton("Save and Finish");

            log("Check that upload worked");
            clickLinkWithText(TEST_ASSAY_LUM_RUN_NAME);
            assertTextPresent("Hu IL-1b (32)");

            clickLinkWithText(TEST_ASSAY_LUM + " Runs");
            clickLinkWithText(TEST_ASSAY_LUM_RUN_NAME3);
            assertTextPresent("IL-1b (1)");
            assertTextPresent("ParticipantID1");
            assertTextPresent("ParticipantID2");
            assertTextPresent("ParticipantID3");
            setFilter(TEST_ASSAY_LUM + " Data", "ParticipantID", "Equals", "ParticipantID1");
            assertTextPresent("1.1");
            setFilter(TEST_ASSAY_LUM + " Data", "ParticipantID", "Equals", "ParticipantID2");
            assertTextPresent("1.2");

            clickLinkWithText(TEST_ASSAY_LUM + " Runs");
            clickLinkWithText(TEST_ASSAY_LUM_RUN_NAME4);
            assertTextPresent("IL-1b (1)");
            assertTextPresent("ListParticipant1");
            assertTextPresent("ListParticipant2");
            assertTextPresent("ListParticipant3");
            assertTextPresent("ListParticipant4");
            setFilter(TEST_ASSAY_LUM + " Data", "ParticipantID", "Equals", "ListParticipant1");
            assertTextPresent("1001.1");
            setFilter(TEST_ASSAY_LUM + " Data", "ParticipantID", "Equals", "ListParticipant2");
            assertTextPresent("1001.2");

            clickLinkWithText(TEST_ASSAY_LUM + " Runs");
            clickLinkWithText(TEST_ASSAY_LUM_RUN_NAME2);
            assertTextPresent("IL-1b (1)");
            assertTextPresent("9011-04");

            setFilter(TEST_ASSAY_LUM + " Data", "FI", "Equals", "20");
            selenium.click(".toggle");
            clickNavButton("Copy Selected to Study");
            selectOptionByText("targetStudy", "/" + TEST_ASSAY_PRJ_LUMINEX + " (" + TEST_ASSAY_PRJ_LUMINEX + " Study)");
            clickNavButton("Next");
            setFormElement("participantId", "ParticipantID");
            setFormElement("visitId", "100.1");
            clickNavButton("Copy to Study");

            log("Verify that the data was published");
            assertTextPresent("ParticipantID");
            assertTextPresent("100.1");
            assertTextPresent(TEST_ASSAY_LUM_RUN_NAME2);
            assertTextPresent("LX10005314302");
        }
    } //doTestSteps()


    /**
     * Cleanup entry point.
     */
    protected void doCleanup()
    {
        revertToAdmin();
        try
        {
            deleteProject(TEST_ASSAY_PRJ_LUMINEX);
            deleteFile(getTestTempDir());
        }
        catch(Throwable T) {/* ignore */}
    } //doCleanup()

    protected boolean isFileUploadTest()
    {
        return true;
    }
}
