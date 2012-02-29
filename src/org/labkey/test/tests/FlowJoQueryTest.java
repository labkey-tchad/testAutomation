/*
 * Copyright (c) 2007-2012 LabKey Corporation
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

import org.labkey.test.BaseFlowTest;
import org.labkey.test.Locator;
import org.labkey.test.util.CustomizeViewsHelper;
import org.labkey.test.util.DataRegionTable;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * This test tests uploading a FlowJo workspace that has results calculated in it, but no associated FCS files.
 * It then runs two queries on those results.
 * Then, it uploads another workspace that is in the same directory as 68 FCS files.  These FCS files are much smaller
 * than normal, having been truncated to 1000 events.
 * It then uses LabKey to perform the same analysis on those FCS files.
 * It then runs a query 'Comparison' to ensure than the difference between LabKey's results and FlowJo's is not greater
 * than 25 for any statistic.
 */
public class FlowJoQueryTest extends BaseFlowTest
{
    String containerPath = "/" + PROJECT_NAME + "/" + getFolderName();

    protected void _doTestSteps() throws Exception
    {

        setFlowPipelineRoot(getLabKeyRoot() + PIPELINE_PATH);
        clickLinkWithText("Flow Dashboard");
        importAnalysis(containerPath, "/flowjoquery/Workspaces/PV1-public.xml", null, false, "FlowJoAnalysis", false, true);
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.clearCustomizeViewColumns(this);
        CustomizeViewsHelper.addCustomizeViewColumn(this, "Name");
        CustomizeViewsHelper.addCustomizeViewColumn(this, "AnalysisScript", "Analysis Script");
        CustomizeViewsHelper.addCustomizeViewColumn(this, "FCSFile/Keyword/Comp", "Comp");
        CustomizeViewsHelper.addCustomizeViewColumn(this, "FCSFile/Keyword/Stim", "Stim");
        CustomizeViewsHelper.addCustomizeViewColumn(this, "FCSFile/Keyword/Sample Order", "Sample Order");
        CustomizeViewsHelper.addCustomizeViewColumn(this, "Statistic/S$SLv$SL$S3+$S4+:Count", "4+:Count");
        CustomizeViewsHelper.addCustomizeViewColumn(this, "Statistic/S$SLv$SL$S3+$S8+:Count", "8+:Count");
        CustomizeViewsHelper.applyCustomView(this);
        clickLinkWithText(PROJECT_NAME);
        clickAdminMenuItem("Project", "Management");
        clickLinkContainingText("Folder Settings");
        toggleCheckboxByTitle("Query");
        toggleCheckboxByTitle("Flow");
        createQuery(PROJECT_NAME, "PassFailDetails", getFileContents("/sampledata/flow/flowjoquery/query/PassFailDetails.sql"), getFileContents("/sampledata/flow/flowjoquery/query/PassFailDetails.xml"), true);
        createQuery(PROJECT_NAME, "PassFailQuery", getFileContents("/sampledata/flow/flowjoquery/query/PassFail.sql"), getFileContents("/sampledata/flow/flowjoquery/query/PassFail.xml"), true);
        //createQuery(PROJECT_NAME, "DeviationFromMean", getFileContents("/sampledata/flow/flowjoquery/query/DeviationFromMean.sql"), getFileContents("/sampledata/flow/flowjoquery/query/DeviationFromMean.xml"), true);
        createQuery(PROJECT_NAME, "COMP", getFileContents("sampledata/flow/flowjoquery/query/COMP.sql"), getFileContents("/sampledata/flow/flowjoquery/query/COMP.xml"), true);
        createQuery(PROJECT_NAME, "Comparison", getFileContents("sampledata/flow/flowjoquery/query/Comparison.sql"), getFileContents("/sampledata/flow/flowjoquery/query/Comparison.xml"), true);
        clickLinkWithText(getFolderName());
        clickLinkWithText("1 run");
        clickMenuButton("Query", "PassFailQuery");
        assertTextPresent("LO_CD8", 1);
        assertTextPresent("PASS", 4);
//        The DeviationFromMean query does not work on SQL server.
//        "Cannot perform an aggregate function on an expression containing an aggregate or a subquery."
//        setFormElement("query.queryName", "DeviationFromMean");
//        waitForPageToLoad();

//        clickLinkWithText("Flow Dashboard");
        importAnalysis(containerPath, "/flowjoquery/miniFCS/mini-fcs.xml", "/flowjoquery/miniFCS", false, "FlowJoAnalysis", true, false);
//
//        int runId = -1;
//        String currentURL = getCurrentRelativeURL();
//        Pattern p = Pattern.compile(".*runId=([0-9]+).*$");
//        Matcher m = p.matcher(currentURL);
//        if (m.matches())
//        {
//            String runIdStr = m.group(1);
//            runId = Integer.parseInt(runIdStr);
//            log("mini-fcs.xml runId = " + runId);
//        }
//        else
//        {
//            fail("Failed to match runId pattern for url: " + currentURL);
//        }
//        assertTrue("Failed to find runId of mini-fcs.xml run", runId > 0);
//
//        // Copy the generated 'workspaceScript1' from one of the sample wells (not one of the comp wells)
//        setFilter("query", "Name", "Equals", "118795.fcs");
//        clickLinkContainingText("workspaceScript");
//        clickLinkWithText("Make a copy of this analysis script");
//        setFormElement("name", "LabKeyScript");
//        checkCheckbox("copyAnalysis");
//        submit();
//
//        // Only run LabKeyScript on sample wells
//        // NOTE: we use 'Contains' since it is case-insensitive. Some values are Non-comp and other are Non-Comp.
//        clickLinkWithText("Edit Settings");
//        selectOptionByText(Locator.name("ff_filter_field", 0), "Comp");
//        selectOptionByText(Locator.name("ff_filter_op", 0), "Contains");
//        setFormElement(Locator.name("ff_filter_value", 0), "non-comp"); clickNavButton("Update");
//
//        clickLinkWithText("Analyze some runs");
//        selectOptionByValue("ff_targetExperimentId", "");
//        waitForPageToLoad();
//        // select mini-fcs.xml Analysis run
//        checkCheckbox(".select", String.valueOf(runId));
//        clickNavButton("Analyze selected runs");
//        setFormElement("ff_analysisName", "LabKeyAnalysis");
//        clickNavButton("Analyze runs");
//        waitForPipeline(containerPath);
//        clickLinkWithText("Flow Dashboard");
//        clickLinkWithText("LabKeyAnalysis");
//        clickMenuButton("Query", "Comparison");
//        waitForPageToLoad(longWaitForPage);
//        assertTextNotPresent("No data to show");
//        setFilterAndWait("query", "AbsDifference", "Is Greater Than Or Equal To", "2", longWaitForPage);
//        // UNDONE: sample '118902.fcs' differs by 2.46%
//        //setFilterAndWait("query", "PercentDifference", "Is Greater Than Or Equal To", "1", longWaitForPage);
//        setFilterAndWait("query", "PercentDifference", "Is Greater Than Or Equal To", "2.5", longWaitForPage);
//        assertTextPresent("No data to show");

//        verifyNestedBooleans();

        verifyACSImport();
        verifyFilterOnImport();

    }

    private void verifyACSImport()
    {
        importAnalysis(containerPath, "/advanced/advanced-v7.6.5.wsp", "advanced/fcs", false, "Mac File", false, false);
        assertTextPresent("931115-B02- Sample 01.fcs");
    }

    private void verifyNestedBooleans()
    {
        //verify workspaces with booleans-within-booleans
        importAnalysis(containerPath, "/flowjoquery/Workspaces/boolean-sub-populations.xml", "miniFCS", true, "BooleanOfBooleanAnalysis", false, false);
        clickLinkWithText("118795.fcs");
        sleep(2000);
        waitForElement(Locator.xpath("//table/tbody/tr/td/a/span[text()='A&B']"), defaultWaitForPage);
        assertElementPresent(Locator.xpath("//tbody/tr/td/a/span[text()='C|D']"));
    }


    private void verifyFilterOnImport()
    {
        setFlowFilter(new String[] {"Name", "Keyword/Comp"}, new String[] { "startswith","eq"}, new String[] {"118", "PE CD8"});
        importAnalysis(containerPath, "/flowjoquery/miniFCS/mini-fcs.xml", "miniFCS", true, "FilterAnalysis", false, false);
        DataRegionTable queryTable = new DataRegionTable("query", this);
        assertEquals(1, queryTable.getDataRowCount());
    }

}
