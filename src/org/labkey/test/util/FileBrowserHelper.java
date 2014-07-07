/*
 * Copyright (c) 2013-2014 LabKey Corporation
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

import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.labkey.test.BaseWebDriverTest.WAIT_FOR_EXT_MASK_TO_DISSAPEAR;
import static org.labkey.test.BaseWebDriverTest.WAIT_FOR_JAVASCRIPT;
import static org.labkey.test.BaseWebDriverTest.WAIT_FOR_PAGE;

public class FileBrowserHelper
{
    BaseWebDriverTest _test;
    public FileBrowserHelper(BaseWebDriverTest test)
    {
        _test = test;
    }

    @LogMethod(quiet = true)
    public void expandFileBrowserRootNode()
    {
        expandFileBrowserTree();
        _test.waitAndClick(Locator.css("div.treenav-panel div.x4-panel-body tr[data-recordindex = '0']"));
        _test.waitForElement(Locator.css("div.treenav-panel div.x4-panel-body tr.x4-grid-row-selected[data-recordindex = '0']"), WAIT_FOR_JAVASCRIPT);
    }

    public void expandFileBrowserTree()
    {
        Locator collapsedTreePanel = Locator.css("div.treenav-panel.x4-collapsed");
        if (_test.isElementPresent(collapsedTreePanel))
        {
            _test.click(Locator.tagWithAttribute("a", "data-qtip", "Show or hide the folder tree"));
            _test.waitForElementToDisappear(collapsedTreePanel);
        }
    }

    @LogMethod(quiet = true)
    public void selectFileBrowserItem(@LoggedParam String path)
    {
        boolean startAtRoot = false;

        String[] parts = {};
        StringBuilder nodeId = new StringBuilder();
        if (path.startsWith("/"))
        {
            startAtRoot = true;
            path = path.substring(1);
        }
        if (!path.equals(""))
        {
            parts = path.split("/");
            nodeId.append('/');
        }
        startAtRoot = startAtRoot || parts.length > 1;

        waitForFileGridReady();

        if (startAtRoot)
        {
            openFolderTree();
            expandFileBrowserRootNode();
        }

        for (int i = 0; i < parts.length; i++)
        {
            waitForFileGridReady();

            nodeId.append(parts[i]);

            nodeId.append('/');

            if (i == parts.length - 1 && !path.endsWith("/")) // Trailing '/' indicates directory
            {
                // select last item: click on tree node name
                clickFileBrowserFileCheckbox(parts[i]);
            }
            else
            {
                WebElement gridRow = Locators.gridRow().waitForElement(_test.getDriver(), WAIT_FOR_JAVASCRIPT);
                Locator.XPathLocator folderTreeNode = Locator.tag("tr").withPredicate("starts-with(@id, 'treeview')").attributeEndsWith("data-recordid", nodeId.toString());

                _test.waitForElement(folderTreeNode);
                _test.waitForElementToDisappear(Locator.xpath("//tbody[starts-with(@id, 'treeview')]/tr[not(starts-with(@id, 'treeview'))]")); // temoporary row exists during expansion animation
                // select/expand tree node
                _test.scrollIntoView(folderTreeNode);
                _test.clickAt(folderTreeNode, 1, 1, 0);
                _test.waitForElement(folderTreeNode.withClass("x4-grid-row-selected"));
                _test._ext4Helper.waitForMaskToDisappear();
                _test.shortWait().until(ExpectedConditions.stalenessOf(gridRow));
            }
        }
    }

    public void clickFileBrowserFileCheckbox(@LoggedParam String fileName)
    {
        waitForFileGridReady();
        scrollToGridRow(fileName);
        Locator rowSelected = Locators.gridRowCheckbox(fileName, true);
        Boolean wasChecked = _test.isElementPresent(rowSelected);

        _test.click(Locators.gridRowCheckbox(fileName, wasChecked));

        if (wasChecked)
            _test.waitForElementToDisappear(rowSelected);
        else
            _test.waitForElement(rowSelected);
    }

    public void clickFileBrowserFileCheckboxWithPartialText(String partialFileName)
    {
        String firstMatch = _test.getText(Locator.tagWithAttribute("td", "role", "gridcell").append(Locator.tag("span").containing(partialFileName)));
        clickFileBrowserFileCheckbox(firstMatch);
    }

    //In case desired element is not present due to infinite scrolling
    private void scrollToGridRow(String nodeIdEndsWith)
    {
        Locator lastFileGridItem = Locators.gridRow().withPredicate("last()");
        Locator targetFile = Locators.gridRowWithNodeId(nodeIdEndsWith);

        _test.waitForElement(lastFileGridItem);

        String previousLastItemText = null;
        String currentLastItemText = lastFileGridItem.findElement(_test.getDriver()).getAttribute("data-recordid");
        while (!_test.isElementPresent(targetFile) && !currentLastItemText.equals(previousLastItemText))
        {
            _test.scrollIntoView(lastFileGridItem);
            previousLastItemText = currentLastItemText;
            currentLastItemText = lastFileGridItem.findElement(_test.getDriver()).getAttribute("data-recordid");
        }
    }

    public void selectFileBrowserRoot()
    {
        selectFileBrowserItem("/");
    }

    public void renameFile(String currentName, String newName)
    {
        selectFileBrowserItem(currentName);
        clickFileBrowserButton(BrowserAction.RENAME);
        _test.waitForElement(Ext4Helper.Locators.window("Rename"));
        _test.setFormElement(Locator.name("renameText-inputEl"), newName);
        _test.clickButton("Rename", WAIT_FOR_EXT_MASK_TO_DISSAPEAR);
        _test.waitForElement(Locator.css(".labkey-filecontent-grid div.x4-grid-cell-inner").withText(newName));
    }

    public void moveFile(String fileName, String destinationPath)
    {
        selectFileBrowserItem(fileName);
        clickFileBrowserButton(BrowserAction.MOVE);
        _test.waitForElement(Ext4Helper.Locators.window("Choose Destination"));
        //NOTE:  this doesn't yet support nested folders
        Locator folder = Locator.xpath("//div[contains(@class, 'x4-window')]//div/span[contains(@class, 'x4-tree-node-text') and text() = '" + destinationPath + "']");
        _test.waitForElement(folder);
        _test.shortWait().until(LabKeyExpectedConditions.animationIsDone(folder));
        _test.sleep(500);
        _test.click(folder);
        _test.clickButton("Move", WAIT_FOR_EXT_MASK_TO_DISSAPEAR);

        _test.waitForElementToDisappear(Locator.css(".labkey-filecontent-grid div.x4-grid-cell-inner").withText(fileName));
        selectFileBrowserItem(destinationPath + "/" + fileName);
        _test.waitForElement(Locator.css(".labkey-filecontent-grid div.x4-grid-cell-inner").withText(fileName));
    }

    public void deleteFile(String fileName)
    {
        selectFileBrowserItem(fileName);
        clickFileBrowserButton(BrowserAction.DELETE);
        _test.waitForElement(Ext4Helper.Locators.window("Delete Files"));
        _test.clickButton("Yes", WAIT_FOR_EXT_MASK_TO_DISSAPEAR);
        _test.waitForElementToDisappear(Locator.css(".labkey-filecontent-grid div.x4-grid-cell-inner").withText(fileName));
    }

    public void createFolder(String folderName)
    {
        clickFileBrowserButton(BrowserAction.NEW_FOLDER);
        _test.setFormElement(Locator.name("folderName"), folderName);
        _test.clickButton("Submit", WAIT_FOR_EXT_MASK_TO_DISSAPEAR);
        _test.waitForElement(Locator.css(".labkey-filecontent-grid div.x4-grid-cell-inner").withText(folderName));
    }

    public void addToolbarButton(String buttonId)
    {
        String checkboxXpath = "//*[contains(@class, 'x4-grid-checkcolumn')]";
        String checkboxSelectedXpath = "/td[1]//*[contains(@class, 'x4-grid-checkcolumn-checked')]";
        Locator toolbarShownLocator = Locator.xpath("//tr[@data-recordid='" + buttonId + "']").append(checkboxSelectedXpath);

        _test.assertElementNotPresent(toolbarShownLocator);
        _test.click(Locator.xpath("//tr[@data-recordid='" + buttonId + "']").append(checkboxXpath));
    }

    public void removeToolbarButton(String buttonId)
    {
        String checkboxXpath = "//*[contains(@class, 'x4-grid-checkcolumn')]";
        String checkboxSelectedXpath = "/td[1]//*[contains(@class, 'x4-grid-checkcolumn-checked')]";
        Locator toolbarShownLocator = Locator.xpath("//tr[@data-recordid='" + buttonId + "']").append(checkboxSelectedXpath);

        _test.assertElementPresent(toolbarShownLocator);
        _test.click(Locator.xpath("//tr[@data-recordid='" + buttonId + "']").append(checkboxXpath));
    }

    public void goToConfigureButtonsTab()
    {
        if (!_test.isElementPresent(Ext4Helper.Locators.window("Manage File Browser Configuration")))
            goToAdminMenu();

        _test._ext4Helper.clickExt4Tab("Toolbar and Grid Settings");
        _test.waitForText("Configure Toolbar Options");
    }

    public void goToAdminMenu()
    {
        clickFileBrowserButton(BrowserAction.ADMIN);
        _test.waitForElement(Ext4Helper.Locators.window("Manage File Browser Configuration"));
    }

    public void selectImportDataAction(@LoggedParam String actionName)
    {
        waitForFileGridReady();
        waitForImportDataEnabled();
        clickFileBrowserButton(BrowserAction.IMPORT_DATA);
        _test.waitForElement(Ext4Helper.Locators.window("Import Data"));
        Locator.XPathLocator actionRadioButton = Locator.xpath("//input[@type='button' and not(@disabled)]/../label[contains(text(), " + Locator.xq(actionName) + ")]");
        if (!_test.isElementPresent(actionRadioButton))
        { // Retry if action isn't present
            _test._ext4Helper.clickWindowButton("Import Data", "Cancel", 0, 0);
            _test._ext4Helper.waitForMaskToDisappear();
            clickFileBrowserButton(BrowserAction.IMPORT_DATA);
            _test.waitForElement(Ext4Helper.Locators.window("Import Data"));
        }
        _test.click(actionRadioButton);
        _test.clickAndWait(Ext4Helper.Locators.ext4Button("Import"));
    }

    public void uploadFile(File file)
    {
        uploadFile(file, null, null, false);
    }

    @LogMethod
    public void uploadFile(@LoggedParam final File file, @Nullable String description, @Nullable List<FileBrowserExtendedProperty> fileProperties, boolean replace)
    {
        _test.waitFor(new BaseWebDriverTest.Checker()
        {
            public boolean check()
            {
                return _test.getFormElement(Locator.xpath("//label[text() = 'Choose a File:']/../..//input[contains(@class, 'x4-form-field')]")).equals("");
            }
        }, "Upload field did not clear after upload.", WAIT_FOR_JAVASCRIPT);

        _test.setFormElement(Locator.css(".single-upload-panel input:last-of-type[type=file]"), file);
        _test.waitFor(new BaseWebDriverTest.Checker()
        {
            public boolean check()
            {
                return _test.getFormElement(Locator.xpath("//label[text() = 'Choose a File:']/../..//input[contains(@class, 'x4-form-field')]")).contains(file.getName());
            }
        }, "Upload field did not clear after upload.", WAIT_FOR_JAVASCRIPT);

        if (description != null)
            _test.setFormElement(Locator.name("description"), description);

        _test.clickButton("Upload", WAIT_FOR_EXT_MASK_TO_DISSAPEAR);

        if (replace)
        {
            _test.waitForElement(Ext4Helper.ext4Window("File Conflict:"));
            _test.assertTextPresent("Would you like to replace it?");
            _test.clickButton("Yes", 0);
        }

        if (fileProperties != null && fileProperties.size() > 0)
        {
            _test.waitForElement(Ext4Helper.ext4Window("Extended File Properties"));
            _test.waitForText("File (1 of ");
            for (FileBrowserExtendedProperty prop : fileProperties)
            {
                if (prop.isCombobox())
                    _test._ext4Helper.selectComboBoxItem(prop.getName(), Ext4Helper.TextMatchTechnique.CONTAINS, prop.getValue());
                else
                    _test.setFormElement(Locator.name(prop.getName()), prop.getValue());
            }
            waitForImportDataEnabled();
            _test.clickButton("Save", 0);
            _test._ext4Helper.waitForMaskToDisappear();

            for (FileBrowserExtendedProperty prop : fileProperties)
                _test.waitForElement(Locator.css(".labkey-filecontent-grid div.x4-grid-cell-inner").withText(prop.getValue()));
        }

        _test.waitForElement(Locator.css(".labkey-filecontent-grid div.x4-grid-cell-inner").withText(file.getName()));
        if (description != null)
            _test.waitForElement(Locator.css(".labkey-filecontent-grid div.x4-grid-cell-inner").withText(description));

        // verify that the description field is empty
        _test.assertFormElementEquals(Locator.name("description"), "");
    }

    public void importFile(String filePath, String importAction)
    {
        selectFileBrowserItem(filePath);
        selectImportDataAction(importAction);
    }

    @LogMethod (quiet = true)
    public void clickFileBrowserButton(@LoggedParam BrowserAction action)
    {
        waitForFileGridReady();
        WebElement button = action.findButton(_test);
        if (button.isDisplayed())
            _test.clickAndWait(action.findButton(_test), action._triggersPageLoad ? WAIT_FOR_PAGE : 0);
        else
            clickFileBrowserButtonOverflow(action);
    }

    private void clickFileBrowserButtonOverflow(BrowserAction action)
    {
        Locator overflowMenuButton = Locator.css("div.fbrowser > div > a.x4-box-menu-after");
        Locator menuItem = Locator.css("a.x4-menu-item-link").withText(action._buttonText);

        _test.click(overflowMenuButton);
        _test.waitAndClick(WAIT_FOR_JAVASCRIPT, menuItem, action._triggersPageLoad ? WAIT_FOR_PAGE : 0);
    }

    public List<WebElement> findBrowserButtons()
    {
        return Locator.css(".fbrowser > .x4-toolbar a.x4-btn[data-qtip]").findElements(_test.getDriver());
    }

    public List<BrowserAction> getAvailableBrowserActions()
    {
        List<BrowserAction> actions = new ArrayList<>();
        List<WebElement> buttons = findBrowserButtons();

        for (WebElement button : buttons)
        {
            String cssClassString = button.getAttribute("class");
            String[] cssClasses = cssClassString.split("[ ]+");
            BrowserAction action = null;
            for (int i = 0; action == null && i < cssClasses.length; i++)
            {
                action = BrowserAction.getActionFromButtonClass(cssClasses[i]);
            }
            if (action == null)
                throw new IllegalStateException("No button found for unrecognize action: " + button.getAttribute("data-qtip"));
            actions.add(action);
        }

        return actions;
    }

    public static enum BrowserAction
    {
        FOLDER_TREE("FolderTree", "Toggle Folder Tree", "folderTreeToggle"),
        UP("Up", "Parent Folder", "parentFolder"),
        RELOAD("Reload", "Refresh", "refresh"),
        NEW_FOLDER("FolderNew", "Create Folder", "createDirectory"),
        DOWNLOAD("Download", "Download", "download"),
        DELETE("Delete", "Delete", "deletePath"),
        RENAME("Rename", "Rename", "renamePath"),
        MOVE("Move", "Move", "movePath"),
        EDIT_PROPERTIES("EditFileProps", "Edit Properties", "editFileProps"),
        UPLOAD("Upload", "Upload Files", "upload"),
        IMPORT_DATA("DBCommit", "Import Data", "importData"),
        EMAIL_SETTINGS("EmailSettings", "Email Preferences", "emailPreferences"),
        AUDIT_HISTORY("AuditLog", "Audit History", "auditLog", true),
        ADMIN("Configure", "Admin", "customize");

        private String _iconName;
        private String _buttonText;
        private String _extId; // from Browser.js
        private boolean _triggersPageLoad;

        private BrowserAction(String iconName, String buttonText, String extId, boolean triggersPageLoad)
        {
            _iconName = iconName;
            _buttonText = buttonText;
            _extId = extId;
            _triggersPageLoad = triggersPageLoad;
        }

        private BrowserAction(String iconName, String buttonText, String extId)
        {
            this(iconName, buttonText, extId, false);
        }

        private static BrowserAction getActionFromButtonClass(String cssClass)
        {
            for (BrowserAction a : BrowserAction.values())
            {
                if (a.buttonCls().equals(cssClass))
                    return a;
            }
            return null;
        }

        public String toString()
        {
            return _buttonText;
        }

        public WebElement findButton(final BaseWebDriverTest test)
        {
            return Locator.css("." + buttonCls()).findElement(test.getDriver());
//            List<WebElement> possibleButtons = getButtonIconLocator().findElements(test.getDriver());
//            if (possibleButtons.size() > 0)
//                return possibleButtons.get(0);
//
//            possibleButtons = getButtonTextLocator().findElements(test.getDriver());
//            if (possibleButtons.size() > 0)
//                return possibleButtons.get(0);
//
//            throw new ElementNotFoundException("File browser button", "text", _buttonText);
        }

        private String buttonCls()
        {
            return _extId + "Btn";
        }

        public Locator button()
        {
            return Locator.tagWithClass("a", buttonCls());
        }

        public Locator getButtonIconLocator()
        {
            return Locator.css(".icon" + _iconName);
        }

        public Locator getButtonTextLocator()
        {
            return button().containing(_buttonText);
        }
    }

    public void waitForFileGridReady()
    {
        _test.waitForElement(Locator.xpath("//div[contains(@class, 'labkey-file-grid-initialized')]"), 6 * WAIT_FOR_JAVASCRIPT);
    }

    public void waitForImportDataEnabled()
    {
        _test.waitForElement(Locator.xpath("//div[contains(@class, 'labkey-import-enabled')]"), 6 * WAIT_FOR_JAVASCRIPT);
    }

    public void openFolderTree()
    {
        Locator collapsedTreePanel = Locator.css("#treeNav-body.x4-collapsed");
        if (_test.isElementPresent(collapsedTreePanel))
        {
            WebElement rootNode = Locator.css("#treeNav-body tr[data-recordindex = '0']").findElement(_test.getDriver());
            clickFileBrowserButton(BrowserAction.FOLDER_TREE);
            _test.waitForElementToDisappear(collapsedTreePanel);
            _test.shortWait().until(ExpectedConditions.stalenessOf(rootNode));
        }
    }

    public static abstract class Locators
    {
        public static Locator.XPathLocator gridRowCheckbox(String fileName, boolean checkForSelected)
        {
            return Locator.xpath("//tr[contains(@class, '" + (checkForSelected ? "x4-grid-row-selected" : "x4-grid-row") + "') and ./td//span[text()='" + fileName + "']]//div[@class='x4-grid-row-checker']");
        }

        public static Locator.XPathLocator gridRow()
        {
            return Locator.tag("tr")
                    .withClass("x4-grid-data-row")
                    .withPredicate("starts-with(@id, 'gridview')");
        }

        public static Locator.XPathLocator gridRow(String fileName)
        {
            return gridRowWithNodeId("/" + fileName);
        }

        public static Locator.XPathLocator gridRowWithNodeId(String nodeIdEndsWith)
        {
            return gridRow().attributeEndsWith("data-recordid", nodeIdEndsWith);
        }

        public static Locator.XPathLocator treeRow(String nodeIdEndsWith)
        {
            return Locator.tag("tr")
                    .withClass("x4-grid-data-row")
                    .withPredicate("starts-with(@id, 'treeview')")
                    .attributeEndsWith("data-recordid", nodeIdEndsWith);
        }
    }
}
