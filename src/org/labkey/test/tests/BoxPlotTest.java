/*
 * Copyright (c) 2013-2015 LabKey Corporation
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

import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.Charting;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.Reports;
import org.labkey.test.components.ChartLayoutDialog;
import org.labkey.test.components.ChartTypeDialog;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;
import org.openqa.selenium.WebElement;

import java.util.List;

@Category({DailyA.class, Reports.class, Charting.class})
public class BoxPlotTest extends GenericChartsTest
{
    @LogMethod
    protected void testPlots()
    {
        doManageViewsBoxPlotTest();
        doDataRegionBoxPlotTest();
        doQuickChartBoxPlotTest();
   }

    private static final String BOX_PLOT_MV_1 = "RCF-1\n0.0\n5.0\n10.0\n15.0\n20.0\n25.0\n4c.Induration1stmeasure";
    private static final String BOX_PLOT_MV_2 = "Mice A\nMice B\nMice C\nNot in Mouse Group: Cat Mice Let\n37.0\n40.0\nTestTitle\nTestXAxis\nTestYAxis";
    private static final String BOX_PLOT_NAME_MV = "ManageViewsBoxPlot";
    private static final String BOX_PLOT_DESC_MV = "This box plot was created through the manage views UI";
    @LogMethod
    private void doManageViewsBoxPlotTest()
    {
        ChartTypeDialog chartTypeDialog;
        ChartLayoutDialog chartLayoutDialog;

        clickProject(getProjectName());
        clickFolder(getFolderName());
        goToManageViews();
        clickAddChart("Box Plot");

        //TODO: weird timing with these combo boxes.
        _ext4Helper.selectComboBoxItem("Query", "RCF-1 (RCF-1: Reactogenicity-Day 2)");

        // Todo: put better wait here
        sleep(5000);
        _ext4Helper.clickWindowButton("Select Query", "Ok", 0, 0);
        waitForText(WAIT_FOR_JAVASCRIPT, "4c.Induration 1st measure");

        chartTypeDialog = new ChartTypeDialog(this);
        chartTypeDialog.waitForDialog();

        chartTypeDialog.setYAxis("4c.Induration 1st measure");
        chartTypeDialog.clickApply();

        //Verify box plot
        assertSVG(BOX_PLOT_MV_1);

        log("Set Plot Title");
        clickButton("Chart Layout", 0);
        chartLayoutDialog = new ChartLayoutDialog(this);
        chartLayoutDialog.waitForDialog();
        chartLayoutDialog.setPlotTitle(CHART_TITLE);
        chartLayoutDialog.clickYAxisTab();
        chartLayoutDialog.setScaleType(ChartLayoutDialog.ScaleType.Log);
        chartLayoutDialog.setYAxisLabel("TestYAxis");
        chartLayoutDialog.clickApply();

        clickButton("Chart Type", 0);
        chartTypeDialog = new ChartTypeDialog(this);
        chartTypeDialog.setYAxis("2.Body temperature");
        chartTypeDialog.clickApply();

        log("Set X Axis");
        clickButton("Chart Type", 0);
        chartTypeDialog = new ChartTypeDialog(this);
        chartTypeDialog.setXAxis("Mouse Group: " + MOUSE_GROUP_CATEGORY);
        chartTypeDialog.clickApply();

        clickButton("Chart Layout", 0);
        chartLayoutDialog = new ChartLayoutDialog(this);
        chartLayoutDialog.clickXAxisTab();
        chartLayoutDialog.setXAxisLabel("TestXAxis");
        chartLayoutDialog.clickApply();

        assertSVG(BOX_PLOT_MV_2);

        clickButton("Save", 0);
        _extHelper.waitForExtDialog("Save");
        //Verify name requirement
        _ext4Helper.clickWindowButton("Save", "Save", 0, 0);
        _extHelper.waitForExtDialog("Error");
        _ext4Helper.clickWindowButton("Error", "OK", 0, 0);
        _extHelper.waitForExtDialogToDisappear("Error");

        //Test cancel button
        setFormElement(Locator.name("reportName"), "TestReportName");
        setFormElement(Locator.name("reportDescription"), "TestReportDescription");
        clickDialogButtonAndWaitForMaskToDisappear("Save", "Cancel");
        assertTextNotPresent("TestReportName");

        savePlot(BOX_PLOT_NAME_MV, BOX_PLOT_DESC_MV);
    }

    private static final String BOX_PLOT_DR_1 = "RCH-1\n36.6\n36.7\n36.8\n36.9\n37.0\n37.1\n37.2\nRCH-1:Reactogenicity-Day1\n2.Bodytemperature";
    private static final String BOX_PLOT_DR_2 = "RCH-1\n36.5\n37.0\n37.5\n38.0\n38.5\n39.0\n39.5\n40.0\nRCH-1:Reactogenicity-Day1\n2.Bodytemperature";
    private static final String BOX_PLOT_NAME_DR = "DataRegionBoxPlot";
    private static final String BOX_PLOT_DESC_DR = "This box plot was created through a data region's 'Views' menu";
    /// Test Box Plot created from a filtered data region.
    @LogMethod
    private void doDataRegionBoxPlotTest()
    {
        ChartTypeDialog chartTypeDialog;
        ChartLayoutDialog chartLayoutDialog;

        clickProject(getProjectName());
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText("RCH-1: Reactogenicity-Day 1"));
        DataRegionTable datasetTable = new DataRegionTable("Dataset", this);
        datasetTable.setFilter("RCHtempc", "Is Less Than", "39");
        datasetTable.clickHeaderButton("Charts", "Create Box Plot");

        chartTypeDialog = new ChartTypeDialog(this);
        chartTypeDialog.waitForDialog();
        chartTypeDialog.setYAxis("2.Body temperature");
        chartTypeDialog.clickApply();

        //Verify box plot
        assertSVG(BOX_PLOT_DR_1);

        //Change filter and check box plot again
        clickButton("View Data", 0);
        datasetTable = new DataRegionTable("Dataset-chartdata", this);
        datasetTable.clearFilter("RCHtempc", 0);
        waitForText("40.0");
        clickButton("View Chart", 0);
        assertSVG(BOX_PLOT_DR_2);

        //Enable point click function for this box plot
        clickButton("Chart Layout", 0);
        chartLayoutDialog = new ChartLayoutDialog(this);
        chartLayoutDialog.waitForDialog();
        chartLayoutDialog.clickDeveloperTab();
        chartLayoutDialog.clickDeveloperEnable();
        chartLayoutDialog.clickApply();
        Locator svgPathLoc = Locator.css("svg a path");

        // We need to specifically click the last element because those are the outliers.
        List<WebElement> paths = svgPathLoc.findElements(getDriver());
        fireEvent(paths.get(paths.size()-1), SeleniumEvent.click);
        _extHelper.waitForExtDialog("Data Point Information");
        assertTextPresentInThisOrder("MouseId/Cohort: Group 2", "RCHtempc:");
        clickButton("OK", 0);

        savePlot(BOX_PLOT_NAME_DR, BOX_PLOT_DESC_DR);
    }

    private static final String BOX_PLOT_QC = "Group 1\nGroup 2\n0.0\n20000000.0\n40000000.0\n60000000.0\n80000000.0\n100000000.0\n120000000.0\nTypes\nStudy:Cohort\nDouble";
    private static final String BOX_PLOT_NAME_QC = "QuickChartBoxPlot";
    private static final String BOX_PLOT_DESC_QC = "This box plot was created through the 'Quick Chart' column header menu option";
    @LogMethod
    private void doQuickChartBoxPlotTest()
    {
        clickProject(getProjectName());
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText("Types"));

        DataRegionTable datasetTable = new DataRegionTable("Dataset", this);
        datasetTable.createQuickChart("dbl");

        //Verify box plot
        assertSVG(BOX_PLOT_QC);

        savePlot(BOX_PLOT_NAME_QC, BOX_PLOT_DESC_QC);
    }

}
