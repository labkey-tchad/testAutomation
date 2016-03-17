/*
 * Copyright (c) 2015-2016 LabKey Corporation
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

import org.apache.http.HttpStatus;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyB;
import org.labkey.test.pages.AssayDesignerPage;
import org.labkey.test.util.DataRegionExportHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExcelHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@Category(DailyB.class)
public class InlineImagesAssayTest extends BaseWebDriverTest
{
    protected DataRegionTable dataRegion;

    protected final static File XLS_FILE = TestFileUtils.getSampleData("InlineImages/foo.xls");
    protected final static File PNG01_FILE =  TestFileUtils.getSampleData("InlineImages/crest.png");
    protected final static File LRG_PNG_FILE = TestFileUtils.getSampleData("InlineImages/screenshot.png");

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("assay");
    }

    @Override
    protected String getProjectName()
    {
        return "InlineImagesAssayTestProject";
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    public void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @BeforeClass
    public static void initTest()
    {
        InlineImagesAssayTest init = (InlineImagesAssayTest)getCurrentTest();
        init.doInit();
    }

    private void doInit()
    {
        _containerHelper.createProject(getProjectName(), "Assay");
    }

    @Test
    public final void testAssayInlineImages() throws Exception
    {

        String assayName = "InlineImageTest";
        String runName = "inlineImageRun01";
        String importData = 
                "Specimen ID\tParticipant ID\tVisit ID\tDate\tData File Field\n" +
                "100\t1A2B\t3\t\t" + LRG_PNG_FILE.getName() + "\n" +
                "101\t2A2B\t3\n" +
                "102\t3A2B\t3";

        DataRegionTable list;
        DataRegionExportHelper exportHelper;
        File exportedFile;
        Workbook workbook;
        Sheet sheet;
        List<String> exportedColumn;

        log("Create an Assay.");

        AssayDesignerPage assayDesigner = _assayHelper.createAssayAndEdit("General", assayName);

        log("Mark the assay as editable.");
        assayDesigner.setEditableRuns(true);
        assayDesigner.setEditableResults(true);

        log("Create a 'File' column for the assay batch.");
        assayDesigner.addBatchField("BatchFileField", "Batch File Field", "File");

        log("Create a 'File' column for the assay run.");
        assayDesigner.addRunField("RunFileField", "Run File Field", "File");

        log("Create a 'File' column for the assay data.");
        assayDesigner.addDataField("DataFileField", "Data File Field", "File");

        log("Save the changes.");
        assayDesigner.save();
        assayDesigner.saveAndClose();
        sleep(1000);

        log("Populate the assay with data.");
        clickAndWait(Locator.linkWithText(assayName));
        clickButton("Import Data");
        setFormElement(Locator.name("batchFileField"), XLS_FILE);
        clickButton("Next");
        setFormElement(Locator.name("name"), runName);
        setFormElement(Locator.name("TextAreaDataCollector.textArea"), importData);

        clickButton("Save and Finish");

        log("Verify link to attached file and icon is present as expected.");
        assertElementPresent("Did not find link to file " + XLS_FILE.getName() + " in grid.", Locator.xpath("//a[contains(text(), '" + XLS_FILE.getName() + "')]"), 1);
        assertElementPresent("Did not find expected file icon in grid.", Locator.xpath("//a[contains(text(), 'foo.xls')]//img[contains(@src, 'xls.gif')]"), 1);

        log("Set the 'File' column on the runs.");

        click(Locator.linkWithText("view runs"));
        click(Locator.linkWithText("edit"));

        setFormElement(Locator.name("quf_RunFileField"), PNG01_FILE);
        clickButton("Submit");
        waitForElement(Locator.linkWithText("edit")); // Wait to make sure the grid has been renedered.

        log("Verify inline image is present as expected.");
        assertElementPresent("Did not find expected inline image for " + PNG01_FILE.getName() + " in grid.", Locator.xpath("//img[contains(@title, '" + PNG01_FILE.getName() + "')]"), 1);

        log("Hover over the thumbnail and make sure the pop-up is as expected.");
        mouseOver(Locator.xpath("//img[contains(@title, '" + PNG01_FILE.getName() + "')]"));
        shortWait().until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#helpDiv")));
        String src = Locator.xpath("//div[@id='helpDiv']//img[contains(@src, 'downloadFileLink')]").findElement(getDriver()).getAttribute("src");
        assertEquals("Bad response from run field pop-up", HttpStatus.SC_OK, WebTestHelper.getHttpGetResponse(src));

        // Not going to try and download the file as part of the automaiton, although that could be added if wanted int he future.

        log("View the results grid.");
        click(Locator.linkWithText("view results"));

        log("Verify that the correct number of file fields are populated as expected.");
        assertElementPresent("Did not find the expected number of links for the file " + XLS_FILE.getName(), Locator.xpath("//a[contains(text(), '" + XLS_FILE.getName() + "')]"), 3);
        assertElementPresent("Did not find the expected number of icons for images for " + PNG01_FILE.getName() + " from the runs.", Locator.xpath("//img[contains(@title, '" + PNG01_FILE.getName() + "')]"), 3);

        log("Verify that the reference to the png that was in the data used for populating is listed as unavailable.");
        assertTextPresent(LRG_PNG_FILE.getName() + " (unavailable)");

        log("Add the image to one of the result's 'File' column.");

        List<WebElement> editLinks = Locator.linkWithText("edit").findElements(getDriver());
        editLinks.get(2).click();

        setFormElement(Locator.name("quf_DataFileField"), LRG_PNG_FILE);
        clickButton("Submit");
        waitForElement(Locator.linkWithText("edit")); // Wait to make sure the grid has been renedered.

        log("Validate that two links to this image file are now present.");
        assertElementPresent("Did not find the expected number of icons for images for " + LRG_PNG_FILE.getName() + " from the runs.", Locator.xpath("//img[contains(@title, '" + LRG_PNG_FILE.getName() + "')]"), 2);

        log("Export the grid to excel.");
        list = new DataRegionTable("Data", this);
        exportHelper = new DataRegionExportHelper(list);
        exportedFile = exportHelper.exportExcel(DataRegionExportHelper.ExcelFileType.XLS);

        workbook = ExcelHelper.create(exportedFile);
        sheet = workbook.getSheetAt(0);

        assertEquals("Wrong number of rows exported to " + exportedFile.getName(), 3, sheet.getLastRowNum());

        log("Validate that the value for the file columns is as expected.");
        exportedColumn = ExcelHelper.getColumnData(sheet, 4);
        assertEquals("Values in 'File' column not exported as expected [" + exportedFile.getName() + "]",
                Arrays.asList("Data File Field", LRG_PNG_FILE.getName(), "", LRG_PNG_FILE.getName()),
                exportedColumn);

        exportedColumn = ExcelHelper.getColumnData(sheet, 5);
        assertEquals("Values in 'File' column not exported as expected [" + exportedFile.getName() + "]",
                Arrays.asList("Run File Field", "assaydata" + File.separator + PNG01_FILE.getName(), "assaydata" + File.separator + PNG01_FILE.getName(), "assaydata" + File.separator + PNG01_FILE.getName()),
                exportedColumn);

        exportedColumn = ExcelHelper.getColumnData(sheet, 7);
        assertEquals("Values in 'File' column not exported as expected [" + exportedFile.getName() + "]",
                Arrays.asList("Batch File Field", "assaydata" + File.separator + XLS_FILE.getName(), "assaydata" + File.separator + XLS_FILE.getName(), "assaydata" + File.separator + XLS_FILE.getName()),
                exportedColumn);

        log("Remove the 'File' column from the batch and see that things still work.");

        assayDesigner = _assayHelper.clickEditAssayDesign();
        assayDesigner.removeBatchField("BatchFileField");
        assayDesigner.saveAndClose();
        waitForElement(Locator.linkWithText("view results")); // Make sure page has loaded completely.
        click(Locator.linkWithText("view results"));

        log("Verify that the file fields from the batch are no longer present.");
        assertElementPresent("Found a link to file " + XLS_FILE.getName() + " in grid, it should not be there.", Locator.xpath("//a[contains(text(), '" + XLS_FILE.getName() + "')]"), 0);
        assertElementPresent("Found a file icon in grid, it should not be there.", Locator.xpath("//a[contains(text(), 'foo.xls')]//img[contains(@src, 'xls.gif')]"), 0);

        log("Verify that the other 'File' fields are not affected.");
        assertElementPresent("Did not find the expected number of icons for images for " + PNG01_FILE.getName() + " from the runs.", Locator.xpath("//img[contains(@title, '" + PNG01_FILE.getName() + "')]"), 3);
        assertElementPresent("Did not find the expected number of icons for images for " + LRG_PNG_FILE.getName() + " from the runs.", Locator.xpath("//img[contains(@title, '" + LRG_PNG_FILE.getName() + "')]"), 2);


        log("Export the grid to excel again and make sure that everything is as expected.");
        list = new DataRegionTable("Data", this);
        exportHelper = new DataRegionExportHelper(list);
        exportedFile = exportHelper.exportExcel(DataRegionExportHelper.ExcelFileType.XLS);

        workbook = ExcelHelper.create(exportedFile);
        sheet = workbook.getSheetAt(0);

        assertEquals("Wrong number of rows exported to " + exportedFile.getName(), 3, sheet.getLastRowNum());

        log("Validate that the value for the file columns is as expected.");        exportedColumn = ExcelHelper.getColumnData(sheet, 4);
        assertEquals("Value of 'File' column not exported as expected [" + exportedFile.getName() + "]",
                Arrays.asList("Data File Field", LRG_PNG_FILE.getName(), "", LRG_PNG_FILE.getName()),
                exportedColumn);

        exportedColumn = ExcelHelper.getColumnData(sheet, 5);
        assertEquals("Value of 'File' column not exported as expected [" + exportedFile.getName() + "]",
                Arrays.asList("Run File Field", "assaydata" + File.separator + PNG01_FILE.getName(), "assaydata" + File.separator + PNG01_FILE.getName(), "assaydata" + File.separator + PNG01_FILE.getName()),
                exportedColumn);

        exportedColumn = ExcelHelper.getColumnData(sheet, 7);
        assertEquals("Value of removed 'File' column not exported as expected [" + exportedFile.getName() + "]",
                Arrays.asList("", "", "", ""), exportedColumn);

        List<String> exportedHeaders = ExcelHelper.getRowData(sheet, 0);
        assertFalse("Value of removed 'File' column not exported as expected [" + exportedFile.getName() + "]",
                exportedHeaders.contains("Batch File Field"));
    }
}
