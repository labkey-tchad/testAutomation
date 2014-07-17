/*
 * Copyright (c) 2012-2014 LabKey Corporation
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

import org.junit.Assert;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.LuminexAll;
import org.labkey.test.util.LogMethod;

import java.io.File;
import java.util.Calendar;

@Category({DailyA.class, LuminexAll.class, Assays.class})
public class LuminexAsyncImportTest extends LuminexTest
{
    @Override
    protected void ensureConfigured()
    {
        setUseXarImport(true);
        super.ensureConfigured();
    }

    protected void runUITests()
    {
        click(Locator.name("backgroundUpload"));
        addTransformScript(new File(TestFileUtils.getLabKeyRoot(), getAssociatedModuleDirectory() + RTRANSFORM_SCRIPT_FILE_LABKEY), 0);
        addTransformScript(new File(TestFileUtils.getLabKeyRoot(), getAssociatedModuleDirectory() + RTRANSFORM_SCRIPT_FILE_LAB), 1);
        saveAssay();

        importFirstRun();
        importSecondRun(1, Calendar.getInstance(), TEST_ASSAY_LUM_FILE5);
        reimportFirstRun(0, Calendar.getInstance(), TEST_ASSAY_LUM_FILE5);
        importBackgroundFailure();
        importBackgroundWarning();
    }

    private void importFirstRun() {
        // IMPORT 1ST RUN
        // First file to be import which will subsequently be re-imported
        importRunForTestLuminexConfig(TEST_ASSAY_LUM_FILE5, Calendar.getInstance(), 0);
        assertTextPresent(TEST_ASSAY_LUM + " Upload Jobs");
        waitForPipelineJobsToFinish(2);
        assertElementNotPresent(Locator.linkWithText("ERROR"));
        clickAndWait(Locator.linkWithText("COMPLETE", 0));
        assertLuminexLogInfoPresent();
        assertElementNotPresent(Locator.linkWithText("ERROR")); //Issue 14082
        assertTextPresent("Starting assay upload", "Finished assay upload");
        clickButton("Data"); // data button links to the run results
        assertTextPresent(TEST_ASSAY_LUM + " Results");
    }

    private void importBackgroundFailure()
    {
        uploadPositivityFile("No Fold Change", TEST_ASSAY_LUM_FILE11, "1", "", true, false);
        assertTextPresent(TEST_ASSAY_LUM + " Upload Jobs");
        waitForPipelineJobsToFinish(5);
        clickAndWait(Locator.linkWithText("ERROR"));
        assertTextPresent("Error: No value provided for 'Positivity Fold Change'.", 3);
        checkExpectedErrors(2);
    }

    private void importBackgroundWarning()
    {
        uploadPositivityFile("No Baseline Data", TEST_ASSAY_LUM_FILE12, "1", "3", true, false);
        assertTextPresent(TEST_ASSAY_LUM + " Upload Jobs");
        waitForPipelineJobsToFinish(6);
        setFilter("StatusFiles", "Description", "Equals", "No Baseline Data");
        if (isElementPresent(Locator.linkWithText("ERROR")))
        {
            clickAndWait(Locator.linkWithText("ERROR"));
            Assert.fail();
        }
        clickAndWait(Locator.linkWithText("COMPLETE", 0));
        assertTextPresent("Warning: No baseline visit data found", 6);
    }

    private void importSecondRun(int index, Calendar testDate, File file) {
        // add a second run with different run values
        int i = index;
        goToTestAssayHome();
        clickButton("Import Data");
        setFormElement(Locator.name("network"), "NEWNET" + (i + 1));
        clickButton("Next");
        testDate.add(Calendar.DATE, 1);
        importLuminexRunPageTwo("Guide Set plate " + (i+1), "new"+isotype, "new"+conjugate, "", "", "NewNote" + (i+1),
                "new Experimental", "NewTECH" + (i+1), df.format(testDate.getTime()), file, i, true);
        uncheckCheckbox(Locator.name("_titrationRole_standard_Standard1"));
        checkCheckbox(Locator.name("_titrationRole_qccontrol_Standard1"));
        clickButton("Save and Finish");
        waitForPipelineJobsToFinish(3);
        assertElementNotPresent(Locator.linkWithText("ERROR"));
    }

    private void reimportFirstRun(int index, Calendar testDate, File file)
    {
        // test Luminex re-run import, check for identical values
        int i = index;
        goToTestAssayHome();
        checkDataRegionCheckbox("Runs", 1);
        clickButton("Re-import run");
        // verify that all old values from the first imported run are present
        assert(getFormElement(Locator.name("network")).equals("NETWORK1"));
        clickButton("Next");
        testDate.add(Calendar.DATE, 1);
        reimportLuminexRunPageTwo("Guide Set plate " + (i+1), isotype, conjugate, "", "", "Notebook" + (i+1),
                "Experimental", "TECH" + (i+1), df.format(testDate.getTime()), file, i);
        uncheckCheckbox(Locator.name("_titrationRole_standard_Standard1"));
        checkCheckbox(Locator.name("_titrationRole_qccontrol_Standard1"));
        clickButton("Save and Finish");
        waitForPipelineJobsToFinish(4);
        assertElementNotPresent(Locator.linkWithText("ERROR"));
    }

    private void reimportLuminexRunPageTwo(String name, String isotype, String conjugate, String stndCurveFitInput,
                                             String unkCurveFitInput, String notebookNo, String assayType, String expPerformer,
                                             String testDate, File file, int i)
    {
        // verify that all old values from the first imported run are present
        assert(getFormElement(Locator.name("name")).equals(name));
        setFormElement(Locator.name("name"), name);
        assert(getFormElement(Locator.name("isotype")).equals(isotype));
        setFormElement(Locator.name("isotype"), isotype);
        assert(getFormElement(Locator.name("conjugate")).equals(conjugate));
        setFormElement(Locator.name("conjugate"), conjugate);
        assert(getFormElement(Locator.name("stndCurveFitInput")).equals(stndCurveFitInput));
        setFormElement(Locator.name("stndCurveFitInput"), stndCurveFitInput);
        assert(getFormElement(Locator.name("unkCurveFitInput")).equals(unkCurveFitInput));
        setFormElement(Locator.name("unkCurveFitInput"), unkCurveFitInput);
        uncheckCheckbox(Locator.name("curveFitLogTransform"));
        assert(getFormElement(Locator.name("notebookNo")).equals(notebookNo));
        setFormElement(Locator.name("notebookNo"), notebookNo);
        assert(getFormElement(Locator.name("assayType")).equals(assayType));
        setFormElement(Locator.name("assayType"), assayType);
        assert(getFormElement(Locator.name("expPerformer")).equals(expPerformer));
        setFormElement(Locator.name("expPerformer"), expPerformer);
        assert(getFormElement(Locator.name("testDate")).equals(testDate));
        setFormElement(Locator.name("testDate"), testDate);
        click(Locator.xpath("//a[contains(@class, 'labkey-file-add-icon-enabled')]"));
        setFormElement(Locator.name("__primaryFile__"), file);
        waitForText("A file with name '" + file.getName() + "' already exists");
        clickButton("Next", 60000);
    }

    private void assertLuminexLogInfoPresent()
    {
        waitForText("Finished assay upload");

        //Check for Analyte Properties
        assertTextPresentInThisOrder("----- Start Analyte Properties -----", "----- Stop Analyte Properties -----");
        assertTextPresent("Properties for GS Analyte (2)", "Properties for GS Analyte (1)");
        assertTextPresent("*LotNumber:", "*NegativeControl:", "*PositivityThreshold:");

        //Check for Well Role Properties
        assertTextPresentInThisOrder("----- Start Well Role Properties -----", "----- Stop Well Role Properties -----");
        assertTextPresent("Standard", "QC Control", "Unknown");

        //Check for Run Properties
        assertTextPresentInThisOrder("----- Start Run Properties -----", "----- End Run Properties -----");
        assertTextPresent("Uploaded Files", "Assay ID", "Isotype", "Conjugate", "Test Date", "Replaces Previous File", "Date file was modified",
                "Specimen Type", "Additive", "Derivative", "Subtract Negative Bead", "Calc of Standards", "Calc of Unknown",
                "Curve Fit Log", "Notebook Number", "Assay Type", "Experiment Performer", "Calculate Positivity",
                "Baseline Visit", "Positivity Fold Change");

        //Check for Batch Properties
        assertTextPresentInThisOrder("----- Start Batch Properties -----", "----- End Batch Properties -----");
        assertTextPresent("Participant Visit", "Target Study", "Species", "Lab ID", "Analysis", "Network", "Transform Script Version", "Ruminex Version");
    }

    @LogMethod
    private void importRunForTestLuminexConfig(File file, Calendar testDate, int i)
    {
        goToTestAssayHome();
        clickButton("Import Data");
        setFormElement(Locator.name("network"), "NETWORK" + (i + 1));
        clickButton("Next");

        testDate.add(Calendar.DATE, 1);
        importLuminexRunPageTwo("Guide Set plate " + (i+1), isotype, conjugate, "", "", "Notebook" + (i+1),
                "Experimental", "TECH" + (i+1), df.format(testDate.getTime()), file, i);
        uncheckCheckbox(Locator.name("_titrationRole_standard_Standard1"));
        checkCheckbox(Locator.name("_titrationRole_qccontrol_Standard1"));
        clickButton("Save and Finish");
    }
}
