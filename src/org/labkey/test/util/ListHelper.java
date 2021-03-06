/*
 * Copyright (c) 2008-2019 LabKey Corporation
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

package org.labkey.test.util;

import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.LabKeySiteWrapper;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.domain.DomainFieldRow;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.components.html.OptionSelect;
import org.labkey.test.pages.list.EditListDefinitionPage;
import org.labkey.test.params.FieldDefinition;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WrapsDriver;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class ListHelper extends LabKeySiteWrapper
{
    public static final String IMPORT_ERROR_SIGNAL = "importFailureSignal"; // See query/import.jsp
    private WrapsDriver _wrapsDriver;

    public ListHelper(WrapsDriver wrapsDriver)
    {
        _wrapsDriver = wrapsDriver;
    }

    public ListHelper(WebDriver driver)
    {
        this(() -> driver);
    }

    @Override
    public WebDriver getWrappedDriver()
    {
        return _wrapsDriver.getWrappedDriver();
    }

    public void uploadCSVData(String listData)
    {
        clickImportData();
        setFormElement(Locator.id("tsv3"), listData);
        _extHelper.selectComboBoxItem("Format:", "Comma-separated text (csv)");
        submitImportTsv_success();
    }

    public void uploadData(String listData)
    {
        clickImportData();
        submitTsvData(listData);
    }

    public void submitTsvData(String listData)
    {
        setFormElement(Locator.id("tsv3"), listData);
        submitImportTsv_success();
    }

    public void submitImportTsv_success()
    {
        clickButton("Submit");
        waitForElement(Locator.css(".labkey-data-region"), 30000);
    }

    // null means any error
    public void submitImportTsv_error(String error)
    {
        doAndWaitForPageSignal(() -> clickButton("Submit", 0),
                IMPORT_ERROR_SIGNAL);
        if (error != null)
        {
            String errors = String.join(", ", getTexts(Locators.labkeyError.findElements(getDriver())));
            assertTrue("Didn't find expected error ['" + error + "'] in [" + errors + "]", errors.contains(error));
        }
    }

    public void submitImportTsv_errors(List<String> errors)
    {
        doAndWaitForPageSignal(() -> clickButton("Submit", 0),
                IMPORT_ERROR_SIGNAL);
        if (errors == null || errors.isEmpty())
            waitForElement(Locator.css(".labkey-error"));
        else
        {
            for (String err : errors)
            {
                waitForElement(Locator.css(".labkey-error").containing(err));
            }
        }
    }

    @LogMethod
    public void importDataFromFile(@LoggedParam File inputFile)
    {
        importDataFromFile(inputFile, BaseWebDriverTest.WAIT_FOR_PAGE * 5);
    }

    @LogMethod
    public void importDataFromFile(@LoggedParam File inputFile, int wait)
    {
        clickImportData();
        click(Locator.tagWithClass("h3", "panel-title").containing("Upload file"));
        setFormElement(Locator.name("file"), inputFile);
        clickButton("Submit", wait);
    }

    public void checkIndexFileAttachements(boolean index)
    {
        Locator indexFileAttachmentsCheckbox = Locator.checkboxByLabel("Index file attachments", false);
        if (index)
            checkCheckbox(indexFileAttachmentsCheckbox);
        else
            uncheckCheckbox(indexFileAttachmentsCheckbox);
    }

    /**
     * From the list data grid, insert a new entry into the current list
     *
     * @param data key = the the name of the field, value = the value to enter in that field
     */

    public void insertNewRow(Map<String, ?> data)
    {
        insertNewRow(data, true);
    }

    public void insertNewRow(Map<String, ?> data, boolean validateText)
    {
        DataRegionTable list = new DataRegionTable("query", getDriver());
        list.clickInsertNewRow();
        setRowData(data, validateText);
    }

    protected void setRowData(Map<String, ?> data, boolean validateText)
    {
        for (String key : data.keySet())
        {
            WebElement field = waitForElement(Locator.name("quf_" + key));
            String inputType = field.getAttribute("type");
            Object value = data.get(key);
            if (value instanceof File)
                setFormElement(field, (File) value);
            else if (value instanceof Boolean)
                setCheckbox(field, (Boolean) value);
            else if (value instanceof OptionSelect.SelectOption)
                new OptionSelect<>(field).selectOption((OptionSelect.SelectOption)value);
            else
            {
                String strVal = String.valueOf(value);
                switch (inputType)
                {
                    case "file":
                        setFormElement(field, new File(strVal));
                        break;
                    case "checkbox":
                        setCheckbox(field, strVal.toLowerCase().equals("true"));
                        break;
                    default:
                        setFormElement(field, strVal);
                }
            }
        }
        clickButton("Submit");

        if (validateText)
        {
            assertTextPresent(String.valueOf(data.values().iterator().next()));  //make sure some text from the map is present
        }
    }

    /**
     * From the list data grid, edit an existing row
     *
     * @param id the row number (1 based)
     * @deprecated use {@link DataRegionTable#updateRow(String, Map)}
     */
    @Deprecated
    public void updateRow(int id, Map<String, String> data)
    {
        updateRow(id, data, true);
    }

    /**
     *
     * @deprecated use {@link DataRegionTable#updateRow(String, Map, boolean)}
     */
    @Deprecated
    public void updateRow(int id, Map<String, String> data, boolean validateText)
    {
        DataRegionTable dr = new DataRegionTable("query", getDriver());
        clickAndWait(dr.updateLink(id - 1));
        setRowData(data, validateText);
    }

    /**
     * Starting at the grid view of a list, delete it
     */
    public void deleteList()
    {
        String url = getCurrentRelativeURL().replace("grid.view", "deleteListDefinition.view");
        beginAt(url);
        clickButton("OK");
    }

    /**
     * Starting at the grid view of a list, confirm dependencies, and delete it
     */
    public void deleteList(String confirmText)
    {
        String url = getCurrentRelativeURL().replace("grid.view", "deleteListDefinition.view");
        beginAt(url);
        assertTextPresent(confirmText);
        clickButton("OK");
    }

    @LogMethod
    public void createListFromTab(String tabName, String listName, ListColumnType listKeyType, String listKeyName, ListColumn... cols)
    {
        beginCreateListFromTab(tabName, listName);
        createListHelper(listKeyType, listKeyName, cols);
    }

    @LogMethod
    public void createList(String containerPath, @LoggedParam String listName, ListColumnType listKeyType, String listKeyName, ListColumn... cols)
    {
        beginCreateList(containerPath, listName);
        createListHelper(listKeyType, listKeyName, cols);
    }

    private void createListHelper(ListColumnType listKeyType, String listKeyName, ListColumn... cols)
    {
        EditListDefinitionPage listDefinitionPage = new EditListDefinitionPage(getDriver());
        DomainFormPanel fieldsPanel = listDefinitionPage.setKeyField(listKeyType, listKeyName);
        for (ListColumn col : cols)
        {
            fieldsPanel.addField(col);
        }
        listDefinitionPage.clickSave();
    }

    private void beginCreateListFromTab(String tabName, String listName)
    {
        clickTab(tabName.replace(" ", ""));
        beginCreateListHelper(listName);
    }

    public void clickTab(String tabname)
    {
        log("Selecting tab " + tabname);
        clickAndWait(Locator.folderTab(tabname));
    }

    // initial "create list" steps common to both manual and import from file scenarios
    public EditListDefinitionPage beginCreateList(String containerPath, String listName)
    {
        beginAt(WebTestHelper.buildURL("project", containerPath, "begin"));
        return beginCreateListHelper(listName);
    }

    private EditListDefinitionPage beginCreateListHelper(String listName)
    {
        if (!isElementPresent(Locator.linkWithText("Lists")))
        {
            PortalHelper portalHelper = new PortalHelper(getDriver());
            portalHelper.addWebPart("Lists");
        }

        waitAndClickAndWait(Locator.linkWithText("manage lists"));

        log("Add List");
        clickButton("Create New List");
        EditListDefinitionPage listDefinitionPage = new EditListDefinitionPage(getDriver());
        listDefinitionPage.setName(listName);
        return listDefinitionPage;
    }

    public void createListFromFile(String containerPath, String listName, File inputFile)
    {
        EditListDefinitionPage listEditPage = beginCreateList(containerPath, listName);
        listEditPage.getFieldsPanel()
            .setInferFieldFile(inputFile);

        // assumes we intend to key on auto-integer
        DomainFieldRow keyRow = listEditPage.getFieldsPanel().getField("Key");
        if (keyRow != null)
            keyRow.clickRemoveField(false);
        listEditPage.selectAutoIntegerKeyField();

        // assumes we intend to import from file
        listEditPage.clickSave();
    }

    /**
     * Import a list archive to a target folder
     * @param folderName target folder
     * @param inputFile Full path/filename to list archive
     */
    public void importListArchive(String folderName, String inputFile)
    {
        importListArchive(folderName, new File(inputFile));
    }

    public void importListArchive(String folderName, File inputFile)
    {
        clickFolder(folderName);
        importListArchive(inputFile);
    }

    public void importListArchive(File inputFile)
    {
        assertTrue("Unable to locate input file: " + inputFile, inputFile.exists());

        if (!isElementPresent(Locator.linkWithText("Lists")))
        {
            PortalHelper portalHelper = new PortalHelper(getDriver());
            portalHelper.addWebPart("Lists");
        }

        goToManageLists().importListArchive(inputFile);
        assertElementNotPresent(Locators.labkeyError);
    }

    public void clickImportData()
    {
        if(isElementPresent(Locator.lkButton("Import Data")))
        {
            // Probably at list-editListDefinition after creating a list
            waitAndClick(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT, Locator.lkButton("Import Data"), BaseWebDriverTest.WAIT_FOR_PAGE);
        }
        else
        {
            // Importing from list data region
            DataRegionTable list = DataRegionTable.DataRegion(getDriver()).find();
            list.clickImportBulkData();
        }
        waitForElement(Locator.id("tsv3"));
    }

    public void bulkImportData(String listData)
    {
        clickImportData();
        setFormElement(Locator.name("text"), listData);
        submitImportTsv_success();
    }

    public EditListDefinitionPage goToEditDesign(String listName)
    {
        goToList(listName);
        clickAndWait(Locator.lkButton("Design"));
        return new EditListDefinitionPage(getDriver());
    }

    public void goToList(String listName)
    {
        // if we are on the Manage List page, click the list name first
        if (isElementPresent(Locators.bodyTitle("Available Lists")))
            clickAndWait(Locator.linkWithText(listName));
    }

    /**
     * @deprecated Use {@link FieldDefinition.RangeType}
     */
    @Deprecated
    public enum RangeType
    {
        Equals("Equals"), NE("Does Not Equal"), GT("Greater than"), GTE("Greater than or Equals"), LT("Less than"), LTE("Less than or Equals");
        private final String _description;

        RangeType(String description)
        {
            _description = description;
        }

        public String toString()
        {
            return _description;
        }

        private FieldDefinition.RangeType toNew()
        {
            for (FieldDefinition.RangeType thisType : FieldDefinition.RangeType.values())
            {
                if (name().equals(thisType.name()))
                    return thisType;
            }
            throw new IllegalArgumentException("Type mismatch: " + this);
        }
    }

    /**
     * @deprecated Use {@link FieldDefinition.ColumnType}
     */
    @Deprecated
    public enum ListColumnType
    {
        MultiLine("Multi-Line Text"),
        Integer("Integer"),
        String("Text (String)"),
        Subject("Subject/Participant (String)"),
        DateAndTime("Date Time"),
        Boolean("Boolean"),
        Decimal("Decimal"),
        File("File"),
        AutoInteger("Auto-Increment Integer"),
        Flag("Flag (String)"),
        Attachment("Attachment"),
        User("User");

        private final String _description;

        ListColumnType(String description)
        {
            _description = description;
        }

        public String toString()
        {
            return _description;
        }

        public FieldDefinition.ColumnType toNew()
        {
            for (FieldDefinition.ColumnType thisType : FieldDefinition.ColumnType.values())
            {
                if (name().equals(thisType.name()))
                    return thisType;
            }
            throw new IllegalArgumentException("Type mismatch: " + this);
        }

        public static ListColumnType fromNew(FieldDefinition.ColumnType newType)
        {
            for (ListColumnType thisType : values())
            {
                if (newType.name().equals(thisType.name()))
                    return thisType;
            }
            throw new IllegalArgumentException("Type mismatch: " + newType);
        }
    }

    /**
     * @deprecated Use {@link FieldDefinition.LookupInfo}
     */
    @Deprecated
    public static class LookupInfo extends FieldDefinition.LookupInfo
    {
        public LookupInfo(@Nullable String folder, String schema, String table)
        {
            super(folder, schema, table);
        }
    }

    /**
     * @deprecated Use {@link FieldDefinition.RegExValidator}
     */
    @Deprecated
    public static class RegExValidator extends FieldDefinition.RegExValidator
    {
        public RegExValidator(String name, String description, String message, String expression)
        {
            super(name, description, message, expression);
        }
    }

    /**
     * @deprecated Use {@link FieldDefinition.RangeValidator}
     */
    @Deprecated
    public static class RangeValidator extends FieldDefinition.RangeValidator
    {
        public RangeValidator(String name, String description, String message, RangeType firstType, String firstRange)
        {
            super(name, description, message, firstType.toNew(), firstRange);
        }
    }

    /**
     * @deprecated Use {@link FieldDefinition}
     */
    @Deprecated
    public static class ListColumn extends FieldDefinition
    {
        public ListColumn(String name, String label, ListColumnType type, String description, String format, LookupInfo lookup, FieldValidator validator, String url, Integer scale)
        {
            super(name);
            setLabel(label);
            setType(type.toNew());
            setDescription(description);
            setFormat(format);
            setLookup(lookup);
            if (validator != null)
                setValidators(List.of(validator));
            setURL(url);
            setScale(scale);
        }

        public ListColumn(String name, String label, ListColumnType type, String description, String format, LookupInfo lookup, FieldValidator validator, String url)
        {
            this(name, label, type, description, format, lookup, validator, url, null);
        }

        public ListColumn(String name, String label, ListColumnType type, String description, LookupInfo lookup)
        {
            this(name, label, type, description, null, lookup, null, null);
        }

        public ListColumn(String name, String label, ListColumnType type, String description, String format)
        {
            this(name, label, type, description, format, null, null, null);
        }

        public ListColumn(String name, String label, ListColumnType type, String description)
        {
            this(name, label, type, description, null, null, null, null);
        }

        public ListColumn(String name, String label, ListColumnType type, String description, FieldValidator validator)
        {
            this(name, label, type, description, null, null, validator, null);
        }

        public ListColumn(String name, String label, ListColumnType type)
        {
            this(name, label, type, null, null, null, null, null);
        }

        public ListColumn(String name, ListColumnType type)
        {
            this(name, null, type);
        }
    }
}
