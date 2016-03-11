/*
 * Copyright (c) 2010-2016 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.DeleteRowsCommand;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.SaveRowsCommand;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.remoteapi.query.UpdateRowsCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Data;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PortalHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test trigger script matrix, expands on ScriptValidationTest which covers custom schemas (Vehicles)
 */
@Category({Data.class})
public class TriggerScriptTest extends BaseWebDriverTest
{
    //List constants
    private static final String TRIGGER_MODULE = "triggerTestModule";
    private static final String SIMPLE_MODULE = "simpletest";
    private static final String LIST_NAME = "Employees";
    private static final String LIST_SCHEMA = "lists";
    private static final String AFTER_INSERT_ERROR = "This is the After Insert Error";
    private static final String AFTER_UPDATE_ERROR = "This is the After Update Error";
    private static final String BEFORE_UPDATE_COMPANY = "Before Update changed me";
    private static final String BEFORE_DELETE_ERROR = "This is the Before Delete Error";
    private static final String AFTER_DELETE_ERROR = "This is the After Delete Error";

    //Dataset constants
    private static final String STUDY_SCHEMA = "study";
    private static final String DATASET_NAME = "Demographics";
    private static final String INDIVIDUAL_TEST = "Individual Test ";
    private static final String API_TEST = "API Test ";
    private static final String ARCHIVE_TEST = "Archive Test ";
    private static final String IMPORT_TEST = "Import Test ";

    private static final String DATA_CLASSES_SCHEMA = "exp.data";
    private static final String DATA_CLASSES_NAME = "DataClassTest";

    protected final PortalHelper _portalHelper = new PortalHelper(this);

    @Override
    public List<String> getAssociatedModules()
    {
        List<String> modules = new ArrayList<>();
        modules.add("Query");
        modules.add(STUDY_SCHEMA);
        modules.add(SIMPLE_MODULE);
        modules.add(TRIGGER_MODULE);

        return modules;
    }
    @Nullable

    @Override
    protected String getProjectName()
    {
        return "Test Trigger Script Project";
    }

    protected String getFolderName()
    {
        return "My Study";
    }

    private String getStudyFolder()
    {
        return getProjectName() + "/" + getFolderName();
    }

    @Override public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    public static class EmployeeRecord
    {
        public String name, ssn, company;
        public Long key;

        public EmployeeRecord(String name, String ssn, String company)
        {
            this(name, ssn, company, null);
        }

        public EmployeeRecord(String name, String ssn, String company, Long key)
        {
            this.name = name;
            this.ssn = ssn;
            this.company = company;
            this.key = key;
        }

        public Map<String, Object> toMap()
        {
            return Maps.<String, Object>of("Name", name, "SSN", ssn, "Company", company, "Key", key);
        }

        public Map<String, String> toStringMap()
        {
            return Maps.of("name", name, "ssn", ssn, "company", company);
        }

        public String toDelimitedString(String delimiter)
        {
            return name + delimiter + ssn + delimiter + company + "\n";
        }

        public static EmployeeRecord fromMap(Map<String, Object> map)
        {
            EmployeeRecord newbie = new EmployeeRecord((String)map.get("Name"), (String)map.get("ssn"), (String)map.get("Company"));
            if(map.containsKey("Key"))
                newbie.key = (Long)map.get("Key");

            return newbie;
        }

        public static String getTsvHeaders()
        {
            return "Name\tSSN\tCompany\n";
        }

    }

    /**
     * Delegate interface to move test to the appropriate Data UI
     */
    private interface GoToDataUI
    {
        void goToDataUIPage();
    }

    @BeforeClass
    public static void projectSetup() throws Exception
    {
        TriggerScriptTest init = (TriggerScriptTest) getCurrentTest();
        init.doSetup();
    }

    protected void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.enableModule(getProjectName(), "Query");
        _containerHelper.enableModule(getProjectName(), SIMPLE_MODULE);
        _containerHelper.enableModule(getProjectName(), TRIGGER_MODULE);

        //create List
        ListHelper.ListColumn[] columns = new ListHelper.ListColumn[] {
                new ListHelper.ListColumn("name", "Name", ListHelper.ListColumnType.String, ""),
                new ListHelper.ListColumn("ssn","SSN", ListHelper.ListColumnType.String,""),
                new ListHelper.ListColumn("company","Company",ListHelper.ListColumnType.String,"")

        };

        _listHelper.createList(getProjectName(), LIST_NAME, ListHelper.ListColumnType.AutoInteger, "Key", columns );
        //Setup study and dataset
        _containerHelper.createSubfolder(getProjectName(), getProjectName(), getFolderName(), "Study", null, true);

        log("Create list in subfolder to prevent query validation failure");
        _listHelper.createList(getProjectName(), "People",
                ListHelper.ListColumnType.AutoInteger, "Key",
                new ListHelper.ListColumn("Name", "Name", ListHelper.ListColumnType.String, "Name"),
                new ListHelper.ListColumn("Age", "Age", ListHelper.ListColumnType.Integer, "Age"),
                new ListHelper.ListColumn("Crazy", "Crazy", ListHelper.ListColumnType.Boolean, "Crazy?"));

        importFolderFromZip(TestFileUtils.getSampleData("studies/LabkeyDemoStudy.zip"));

        //Setup Data Class
        goToProjectHome();

        _portalHelper.addWebPart("Datasets");
        _portalHelper.addWebPart("Data Classes");

    }

    @Before
    public void goToProjectStart()
    {
        clickProject(getProjectName());
    }

    @Test
    public void testListIndividualTriggers() throws Exception
    {
        EmployeeRecord caughtAfter = new EmployeeRecord("Emp 1", "1112223333", "Test"),
                changedBefore = new EmployeeRecord("Emp 2", "2223334444", "Some Other");

        //Insert row into List
        insertSingleRowViaUI(caughtAfter);
        String testName = INDIVIDUAL_TEST;
        String step = "AfterInsert";

        //Check AfterInsert event
        log("** " + testName + step + " Event");
        assertTextPresent(AFTER_INSERT_ERROR, 1);
        clickButton("Cancel");

        //Check BeforeInsert event
        step = "BeforeInsert";
        log("** " + testName + step + " Event");
        insertSingleRowViaUI(changedBefore);
        assertElementNotPresent("Transaction was committed after error", Locator.tagWithText("td", "Emp 1"));
        assertElementPresent(Locator.tagWithText("td","Inserting Single"));

        //Check BeforeDelete Event
        step = "BeforeDelete";
        log("** " + testName + step + " Event");
        deleteSingleRowViaUI("Company", "Inserting Single", "query");
        assertTextPresent(BEFORE_DELETE_ERROR);
        clickButton("Back");

        //Check AfterUpdate Event
        step = "AfterUpdate";
        log("** " + testName + step + " Event");
        click(Locator.linkWithText("Edit"));
        clickButton("Submit");
        assertTextPresent(AFTER_UPDATE_ERROR);
        clickButton("Cancel");

        //Check BeforeUpdate Event
        step = "BeforeUpdate";
        log("** " + testName + step + " Event");
        changedBefore.name = "Emp 3";
        _listHelper.updateRow(1, changedBefore.toStringMap());
        assertTextPresent(BEFORE_UPDATE_COMPANY);

        //Check AfterDelete Event
        step = "AfterDelete";
        log("** " + testName + step + " Event");
        deleteSingleRowViaUI("Company", BEFORE_UPDATE_COMPANY, "query");
        assertTextPresent(AFTER_DELETE_ERROR);
        clickButton("Back");
        //Verify validation error prevented delete
        assertElementPresent(Locator.tagWithText("td", "Emp 3"));
        cleanUpListRows();
    }

    @Test
    public void testListImportTriggers() throws Exception
    {
        goToManagedList(LIST_NAME);
        _listHelper.clickImportData();

        EmployeeRecord caughtAfter = new EmployeeRecord("Emp 1", "1112223333", "Test"),
                changedBefore = new EmployeeRecord("Emp 5", "2223334444", "Some Other");

        String testName = IMPORT_TEST;
        String step = "AfterInsert";

        //Check AfterInsert event
        log("** " + testName + step + " Event");
        String tsvData = EmployeeRecord.getTsvHeaders();
        String delimiter = "\t";
        tsvData += caughtAfter.toDelimitedString(delimiter);
        tsvData += changedBefore.toDelimitedString(delimiter);

        setFormElement(Locator.id("tsv3"), tsvData);
        _listHelper.submitImportTsv_error(AFTER_INSERT_ERROR);

        //Check BeforeInsert event
        step = "BeforeInsert";
        log("** " + testName + step + " Event");
        Locator.id("tsv3").findElement(getDriver()).clear();
        tsvData = EmployeeRecord.getTsvHeaders();
        tsvData += changedBefore.toDelimitedString(delimiter);
        setFormElement(Locator.id("tsv3"), tsvData);
        _listHelper.submitImportTsv_success();

        assertElementPresent(Locator.tagWithText("td","Importing TSV"));
        cleanUpListRows();
    }

    @Test
    public void testListAPITriggers() throws Exception
    {
        String ssn1 = "111111112";
        String ssn2 = "222211111";

        EmployeeRecord row1 = new EmployeeRecord("Emp 1", ssn1, "LK"),
                       row2 = new EmployeeRecord("Emp 6", ssn2, "KL");

        Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());

        String testName = API_TEST;
        String step = "AfterInsert";
        log("** " + testName + step + " Event");

        //Check After Insert Event
        InsertRowsCommand insCmd = new InsertRowsCommand(LIST_SCHEMA, LIST_NAME);
        SaveRowsResponse resp;

        insCmd.addRow(row1.toMap()); //can add multiple rows to insert many at once
        insCmd.addRow(row2.toMap());
        assertAPIErrorMessage(insCmd, AFTER_INSERT_ERROR, cn);

        //Check Before Insert Event
        step = "BeforeInsert";
        log("** " + testName + step + " Event");
        insCmd = new InsertRowsCommand(LIST_SCHEMA, LIST_NAME);
        EmployeeRecord row3 = new EmployeeRecord("Emp 7","123","DeleteMe");

        insCmd.addRow(row2.toMap());
        insCmd.addRow(row3.toMap());
        resp = insCmd.execute(cn, getProjectName());
        row2 = EmployeeRecord.fromMap(resp.getRows().get(0));
        Assert.assertEquals("API BeforeInsert", row2.company);

        row3 = EmployeeRecord.fromMap(resp.getRows().get(1));

        //Check After Update Event
        step = "AfterUpdate";
        log("** " + testName + step + " Event");
        UpdateRowsCommand updCmd = new UpdateRowsCommand(LIST_SCHEMA, LIST_NAME);
        row2.ssn = ssn1;
        updCmd.addRow(row2.toMap());
        updCmd.addRow(row3.toMap());
        assertAPIErrorMessage(updCmd, AFTER_UPDATE_ERROR, cn);

        //Check Before Update Event
        step = "BeforeUpdate";
        log("** " + testName + step + " Event");
        updCmd = new UpdateRowsCommand(LIST_SCHEMA,LIST_NAME);
        row2.name = "Emp 8";
        updCmd.addRow(row2.toMap());
        updCmd.addRow(row3.toMap());
        resp = updCmd.execute(cn, getProjectName());
        EmployeeRecord updateCo = EmployeeRecord.fromMap(resp.getRows().get(0));
        Assert.assertEquals(BEFORE_UPDATE_COMPANY, updateCo.company);
        //Check update persisted
        Assert.assertEquals(ssn1, updateCo.ssn);

        //Check After Delete Event
        step = "AfterDelete";
        log("** " + testName + step + " Event");
        DeleteRowsCommand delCmd = new DeleteRowsCommand(LIST_SCHEMA, LIST_NAME);
        delCmd.addRow(row2.toMap());
        assertAPIErrorMessage(delCmd, AFTER_DELETE_ERROR, cn);

        //Check Before Delete Event
        step = "BeforeDelete";
        log("** " + testName + step + " Event");
        delCmd = new DeleteRowsCommand(LIST_SCHEMA, LIST_NAME);
        delCmd.addRow(row3.toMap());
        assertAPIErrorMessage(delCmd, BEFORE_DELETE_ERROR, cn);

        cleanUpListRows();
    }

    /********************************
     * Dataset Trigger Script Tests
     ********************************/

    @Test
    public void testDatasetIndividualTriggers() throws Exception
    {
        GoToDataUI goToDataset = new GoToDataUI()
        {
            @Override
            public void goToDataUIPage()
            {
                goToDataset(DATASET_NAME);
            }
        };

        doIndividualTriggerTest("Dataset", goToDataset, "ParticipantId", true );

        //For some reason these only get logged for datasets...
        checkExpectedErrors(4);
    }

    @Ignore("Issue 25741: JS triggers for bulk import data (CSV or Excel) of datasets don't fire\n")
    @Test
    //TODO: enable this test when Issue 25741 is resolved
    public void testDatasetImportTriggers() throws Exception
    {
        String flagField = "Comments"; //Field to watch in trigger script
        String testName = IMPORT_TEST;

        String delimiter = "\t";


        Map<String,String> caughtAfter = new HashMap();
        String badParticipant = "101";
        Date date1 = new Date();
        caughtAfter.put("ParticipantId", badParticipant);
        caughtAfter.put("date", date1.toString());
        caughtAfter.put("Gender", "f");
        caughtAfter.put(flagField, "AfterInsert");

        Map<String,String> changedBefore = new HashMap();
        String key2 = "102";
        Date date2 = new Date();
        changedBefore.put("ParticipantId", key2);
        changedBefore.put("date", date2.toString());
        changedBefore.put("Gender", "f");
        changedBefore.put(flagField, testName);

        //Check AfterInsert event
        String step = "AfterInsert";
        log("** " + testName + step + " Event");
        String importHeaders = joinMapKeys(caughtAfter, delimiter);
        String row1 = joinMapValues(caughtAfter, delimiter);
        String row2 = joinMapValues(changedBefore, delimiter);
        String tsvData = importHeaders + "\n";
        tsvData += row1 + "\n";
        tsvData += row2 + "\n";

        goToDataset(DATASET_NAME);
        clickButton("Import Data");
        setFormElement(Locator.id("tsv3"), tsvData);

    }

    @Test
    public void testDatasetAPITriggers() throws Exception
    {
        doAPITriggerTest(STUDY_SCHEMA,DATASET_NAME,"ParticipantId", true);
    }

    /********************************
     * Data Class Trigger Script Tests
     ********************************/

    @Test
    public void testDataClassIndividualTriggers() throws Exception
    {
        //Generate delegate to move to dataset UI
        GoToDataUI goToDataset = new GoToDataUI()
        {
            @Override
            public void goToDataUIPage()
            {
                goToDataClass(DATA_CLASSES_NAME);
            }
        };

        setupDataClass();
        doIndividualTriggerTest("query", goToDataset, "Name", false );
        deleteDataClass();
    }


    @Test
    public void testDataClassAPITriggers() throws Exception
    {
        setupDataClass();
        doAPITriggerTest(DATA_CLASSES_SCHEMA, DATA_CLASSES_NAME, "Name", false);
        deleteDataClass();
    }

    /**
     * Run an api test against a schema and query based on preset trigger script
     * @param schemaName
     * @param queryName
     * @param keyColumnName Name of key column
     * @param requiresDate param to add a date column to inserted items
     * @throws Exception
     */
    private void doAPITriggerTest(String schemaName, String queryName, String keyColumnName, boolean requiresDate) throws Exception
    {
        Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
        String flagField = "Comments"; //Field to watch in trigger script
        String updateField = "Country"; //field that is set by trigger scripts and Updates

        Map<String,Object> row1 = new HashMap();
        String text1 = "123";
        row1.put(keyColumnName, text1);
        if(requiresDate)
            row1.put("Date", new Date());

        row1.put(flagField, "AfterInsert");

        Map<String,Object> row2 = new HashMap();
        String text2 = "321";
        row2.put(keyColumnName, text2);
        if(requiresDate)
            row2.put("Date", new Date());

        String testName = API_TEST;
        String step = "AfterInsert";
        log("** " + testName + step + " Event");

        //Check After Insert Event
        InsertRowsCommand insCmd = new InsertRowsCommand(schemaName, queryName);

        insCmd.addRow(row1); //can add multiple rows to insert many at once
        insCmd.addRow(row2);
        assertAPIErrorMessage(insCmd, AFTER_INSERT_ERROR, cn);

        //Check Before Insert Event
        step = "BeforeInsert";
        log("** " + testName + step + " Event");
        insCmd = new InsertRowsCommand(schemaName, queryName);

        Map<String,Object> row3 = new HashMap();
        row3.put(keyColumnName, "213");
        if(requiresDate)
            row3.put("Date", new Date());

        row2.put(flagField, testName);

        insCmd.addRow(row2);
        insCmd.addRow(row3);
        SaveRowsResponse resp = insCmd.execute(cn, getProjectName());
        row2 = resp.getRows().get(0);
        Assert.assertEquals("API BeforeInsert", row2.get("Country"));

        row3 = resp.getRows().get(1);

        //Check After Update Event
        step = "AfterUpdate";
        log("** " + testName + step + " Event");
        UpdateRowsCommand updCmd = new UpdateRowsCommand(schemaName, queryName);
        row2.put(flagField, "AfterUpdate");
        updCmd.addRow(row2);
        updCmd.addRow(row3);
        assertAPIErrorMessage(updCmd, AFTER_UPDATE_ERROR, cn);

        //Check Before Update Event
        step = "BeforeUpdate";
        log("** " + testName + step + " Event");
        updCmd = new UpdateRowsCommand(schemaName,queryName);
        row2.put(flagField, "BeforeUpdate");
        row2.put(updateField, "Labkey");
        row3.put(flagField,"BeforeDelete");  //For later.
        updCmd.addRow(row2);
        updCmd.addRow(row3);
        resp = updCmd.execute(cn, getProjectName());
        Map updateCo = resp.getRows().get(0);
        Assert.assertEquals(BEFORE_UPDATE_COMPANY, updateCo.get(updateField));
        //Check update persisted
        Assert.assertEquals("BeforeUpdate", updateCo.get(flagField));

        //Check After Delete Event
        step = "After Delete";
        log("** " + testName + step + " Event");
        DeleteRowsCommand delCmd = new DeleteRowsCommand(schemaName, queryName);
        delCmd.addRow(row2);
        assertAPIErrorMessage(delCmd, AFTER_DELETE_ERROR, cn);

        //Check Before Delete Event
        step = "BeforeDelete";
        log("** " + testName + step + " Event");
        delCmd = new DeleteRowsCommand(schemaName, queryName);
        delCmd.addRow(row3);
        assertAPIErrorMessage(delCmd, BEFORE_DELETE_ERROR, cn);


    }

    /**
     * Execute a set of tests against a datatype and preset trigger script
     * @param schemaName
     * @param goToData
     * @param keyColumnName
     * @param requiresDate
     * @throws Exception
     */
    private void doIndividualTriggerTest(String schemaName, GoToDataUI goToData, String keyColumnName, boolean requiresDate) throws Exception
    {
        String flagField = "Comments"; //Field to watch in trigger script
        String updateField = "Country"; //Field updated by trigger script
        String testName = INDIVIDUAL_TEST;

        Map<String,String> caughtAfter = new HashMap();
        String badParticipant = "101345";
        Date date1 = new Date();
        caughtAfter.put(keyColumnName, badParticipant);
        if(requiresDate)
            caughtAfter.put("date", date1.toString());
        caughtAfter.put(flagField, "AfterInsert");

        Map<String,String> changedBefore = new HashMap();
        String key2 = "103506";
        Date date2 = new Date();
        changedBefore.put(keyColumnName, key2);
        if(requiresDate)
            changedBefore.put("date", date2.toString());
        changedBefore.put(flagField, testName);

        //Check AfterInsert event
        String step = "AfterInsert";
        log("** " + testName + step + " Event");
        //  Insert row into List
        goToData.goToDataUIPage();
        insertSingleRowViaUI(caughtAfter);
        assertTextPresent(AFTER_INSERT_ERROR, 1);
        clickButton("Cancel");

        //Check BeforeInsert event
        step = "BeforeInsert";
        log("** " + testName + step + " Event");
        goToData.goToDataUIPage();
        insertSingleRowViaUI(changedBefore);
        assertElementNotPresent("Transaction was committed after error", Locator.tagWithText("td", badParticipant));
        assertElementPresent(Locator.tagWithText("td","Inserting Single"));

        //Check BeforeDelete Event
        step = "BeforeDelete";
        log("** " + testName + step + " Event");

        //Check previous step prepared row for delete
        assertElementPresent(Locator.tagWithText("td", "BeforeDelete"));
        deleteSingleRowViaUI(flagField, step, schemaName);
        assertTextPresent(BEFORE_DELETE_ERROR);
        clickButton("Back");
        //Verify validation error prevented delete
        assertElementPresent(Locator.tagWithText("td", "BeforeDelete"));

        //Check AfterUpdate Event
        step = "AfterUpdate";
        log("** " + testName + step + " Event");
        changedBefore.put(flagField, step);
        updateDataSetRow(1,schemaName, changedBefore);
        assertTextPresent(AFTER_UPDATE_ERROR);
        clickButton("Cancel");

        //Check BeforeUpdate Event
        step = "BeforeUpdate";
        log("** " + testName + step + " Event");
        changedBefore.put(flagField, step);
        updateDataSetRow(1,schemaName, changedBefore);
        assertTextPresent(BEFORE_UPDATE_COMPANY);
        assertTextPresent("BeforeUpdate");  //Check change was retained

        //Check AfterDelete Event
        step = "AfterDelete";
        log("** " + testName + step + " Event");
        deleteSingleRowViaUI(updateField, BEFORE_UPDATE_COMPANY, schemaName);
        assertTextPresent(AFTER_DELETE_ERROR);
        clickButton("Back");
        //Verify validation error prevented delete
        assertElementPresent(Locator.tagWithText("td", "BeforeUpdate"));
    }

    /**
     * Verify error message recieved from api call matches the expected error
     * @param cmd command to run
     * @param expected error message to check
     * @param cn connecion object to run against
     * @throws IOException
     */
    private void assertAPIErrorMessage(SaveRowsCommand cmd, String expected, Connection cn) throws IOException
    {
        try
        {
            SaveRowsResponse resp = cmd.execute(cn, getProjectName());
            Assert.fail("No error triggered. Expected: " + expected);
        }
        catch (CommandException e)
        {
            Assert.assertTrue("Trigger script error message was wrong", e.getMessage().contains(expected));
        }
    }

    /**
     * Insert a single record into list
     * @param record
     */
    private void insertSingleRowViaUI(EmployeeRecord record)
    {
        goToManagedList(LIST_NAME);
        _listHelper.insertNewRow(record.toStringMap());
    }

    /**
     * insert single record into dataset
     * @param record
     */
    private void insertSingleRowViaUI(Map<String,String> record)
    {
        clickButton("Insert New");
        record.entrySet().forEach((entry) -> setFormElement( Locator.xpath("//*[@name='quf_"+ entry.getKey() + "']"), entry.getValue()));
        clickButton("Submit");
    }

    /**
     * delete single record via the table UI
     * @param columnName Column to look at
     * @param columnValue value to look for
     * @param tableName DataRegionTable name
     */
    private void deleteSingleRowViaUI(String columnName, String columnValue, String tableName)
    {
        DataRegionTable drt = new DataRegionTable(tableName, this);
        int rowId = drt.getRow(columnName, columnValue);
        drt.checkCheckbox(rowId);
        clickButton("Delete", 0);
        acceptAlert();
    }

    /**
     * Edit a dataset fields base on map
     * @param id dataset entry to edit
     * @param data Field/value map
     */
    public void updateDataSetRow(int id, String tableName, Map<String, String> data)
    {
        DataRegionTable dr = new DataRegionTable(tableName, this);
        this.clickAndWait(dr.updateLink(id - 1));
        data.entrySet().forEach((entry) -> setFormElement( Locator.xpath("//*[@name='quf_"+ entry.getKey() + "']"), entry.getValue()));
        clickButton("Submit");
    }

    /**
     * Delete list rows added by test
     */
    private void cleanUpListRows()
    {
        goToManagedList(LIST_NAME);
        clickButton("Delete All Rows", 0);
        waitForElement(Locator.xpath("//*[text()='Confirm Deletion']"));
        clickButton("Yes", 0);
        waitForText("Success");
        clickButton("OK");
    }

    /**
     * Navigate to a particular test
     * @param listName
     */
    private void goToManagedList(String listName)
    {
        goToManageLists();
        clickAndWait(Locator.linkWithText(listName));
    }

    /**
     * Navigate to particular Dataset
     * @param datasetName
     */
    private void goToDataset(String datasetName)
    {
        goToProjectHome();
        clickAndWait(Locator.linkWithText(datasetName));
    }

    /**
     * Navigate to particular Dataset
     * @param dataClassName
     */
    private void goToDataClass(String dataClassName)
    {
        goToProjectHome();
        clickAndWait(Locator.linkWithText("Data Classes"));
        clickAndWait(Locator.linkWithText(dataClassName));
    }

    /**
     * Generate delimited string of keys from a map
     * @param data
     * @param delimiter
     * @return
     */
    private String joinMapValues(Map<String,String> data, String delimiter )
    {
        StringBuilder sb = new StringBuilder();
        data.values().forEach(val -> sb.append(val).append(delimiter));
        return sb.toString().trim();
    }

    /**
     * Generate delimited string of keys from a map
     * @param data
     * @param delimiter
     * @return
     */
    private String joinMapKeys(Map<String,String> data, String delimiter )
    {
        StringBuilder sb = new StringBuilder();
        data.keySet().forEach(val -> sb.append(val).append(delimiter));
        return sb.toString().trim();
    }

    /**
     * Setup the data class
     */
    private void setupDataClass()
    {
        //Setup Data Class
        goToProjectHome();
        clickAndWait(Locator.linkWithText("Data Classes"));
        clickButton("Insert New");
        setFormElement(Locator.name("name"), DATA_CLASSES_NAME);
        clickButton("Create");
        clickButton("Add Field", 0);
        clickButton("Add Field", 0);
        setFormElement(Locator.input("ff_name0"), "Comments");
        setFormElement(Locator.input("ff_name1"), "Country");

        setFormElement(Locator.input("ff_label0"), "Comments");
        setFormElement(Locator.input("ff_label1"), "Country");
        clickButton("Save");
    }

    /**
     * Clean up the Data Class
     */
    private void deleteDataClass()
    {
        goToProjectHome();
        DataRegionTable drt = new DataRegionTable("qwp3", this);
        int rowId = drt.getRow("Name", DATA_CLASSES_NAME);
        drt.checkCheckbox(rowId);
        clickButton("Delete");
        clickButton("Confirm Delete");
    }
}