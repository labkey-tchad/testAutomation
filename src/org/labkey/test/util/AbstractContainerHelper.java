/*
 * Copyright (c) 2012-2015 LabKey Corporation
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
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public abstract class AbstractContainerHelper extends AbstractHelper
{
    private static List<String> _createdProjects = new ArrayList<>();
    private Set<WebTestHelper.FolderIdentifier> _createdFolders = new HashSet<>();

    public AbstractContainerHelper(BaseWebDriverTest test)
    {
        super(test);
    }

    public List<String> getCreatedProjects()
    {
        return _createdProjects;
    }

    public void clearCreatedProjects()
    {
        _createdProjects.clear();
    }

    public Set<WebTestHelper.FolderIdentifier> getCreatedFolders()
    {
        return _createdFolders;
    }

    /** @param folderType the name of the type of container to create.
     * May be null, in which case you get the server's default folder type */
    @LogMethod(quiet = true)
    public final void createProject(@LoggedParam String projectName, @Nullable String folderType)
    {
        doCreateProject(projectName, folderType);
        _createdProjects.add(projectName);
    }

    public void createSubfolder(String parentPath, String folderName)
    {
        createSubfolder(parentPath, folderName, "None");
    }

    public abstract void createSubfolder(String parentPath, String folderName, String folderType);
    protected abstract void doCreateProject(String projectName, String folderType);
    protected abstract void doCreateFolder(String projectName, String folderType, String path);

    // Projects might be created by other means
    public void addCreatedProject(String projectName)
    {
        _createdProjects.add(projectName);
    }

    public final void deleteProject(String projectName) throws TestTimeoutException
    {
        deleteProject(projectName, true);
    }

    public void deleteProject(String project, boolean failIfFail) throws TestTimeoutException
    {
        deleteProject(project, failIfFail, 120000);
    }

    @LogMethod
    public final void deleteProject(@LoggedParam String projectName, boolean failIfNotFound, int wait) throws TestTimeoutException
    {
        doDeleteProject(projectName, failIfNotFound, wait);
        _createdProjects.remove(projectName);
    }

    public abstract void doDeleteProject(String projectName, boolean failIfNotFound, int wait) throws TestTimeoutException;

    @LogMethod(quiet = true)
    public void setFolderType(@LoggedParam String folderType)
    {
        _test.goToFolderManagement();
        _test.clickAndWait(Locator.linkWithText("Folder Type"));
        _test.click(Locator.radioButtonByNameAndValue("folderType", folderType));
        _test.clickButton("Update Folder");
    }

    public void enableModule(String projectName, String moduleName)
    {
        _test.ensureAdminMode();
        _test.clickProject(projectName);
        enableModule(moduleName);
    }

    public void enableModule(String moduleName)
    {
        enableModules(Collections.singletonList(moduleName));
    }

    public void enableModules(List<String> moduleNames)
    {
        enableModules(moduleNames, false);
    }

    // NOTE: consider using this pathway all the time (e.g. drop the the checkFirst flag).
    public void enableModules(List<String> moduleNames, boolean checkFirst)
    {
        _test.goToFolderManagement();
        _test.clickAndWait(Locator.linkWithText("Folder Type"));
        for (String moduleName : moduleNames)
        {
            Locator loc = Locator.checkboxByTitle(moduleName);
            if(checkFirst)
                assertTrue(moduleName + " module was not detected. Check that the module is installed and you are on the supported backend.", _test.isElementPresent(loc));
            _test.checkCheckbox(loc);
        }
        _test.clickButton("Update Folder");
    }

    public void disableModules(String... moduleNames)
    {
        _test.goToFolderManagement();
        _test.clickAndWait(Locator.linkWithText("Folder Type"));
        for (String moduleName : moduleNames)
        {
            _test.uncheckCheckbox(Locator.checkboxByTitle(moduleName));
        }
        _test.clickButton("Update Folder");
    }

    public void createSubFolderFromTemplate(String project, String child, String template, @Nullable String[] objectsToSkip)
    {
        createSubfolder(project, project, child, "Create From Template Folder", template, objectsToSkip, null, false);
    }

    public void createSubfolder(String project, String child, String[] tabsToAdd)
    {
        // create a child of the top-level project folder:
        createSubfolder(project, project, child, "None", tabsToAdd);
    }

    public void createSubfolder(String project, String parent, String child, String folderType, @Nullable String[] tabsToAdd)
    {
        createSubfolder(project, parent, child, folderType, tabsToAdd, false);
    }

    public void createSubfolder(String project, String parent, String child, @Nullable String folderType, @Nullable String[] tabsToAdd, boolean inheritPermissions)
    {
        createSubfolder(project, parent, child, folderType, null, tabsToAdd, inheritPermissions);
    }

    public void createSubfolder(String project, String parent, String child, String folderType, @Nullable String templateFolder, String[] tabsToAdd, boolean inheritPermissions)
    {
        createSubfolder(project, parent, child, folderType, templateFolder, null, tabsToAdd, inheritPermissions);
    }

    /**
     * @param project project in which to create new folder
     * @param parent immediate parent of the new folder (project, if it's a top level subfolder)
     * @param child name of folder to create
     * @param folderType type of folder (null for custom)
     * @param templateFolder if folderType = "create from Template Folder", this is the template folder used.  Otherwise, ignored
     * @param tabsToAdd module tabs to add iff foldertype=null,  or the copy related checkboxes iff foldertype=create from template
     * @param inheritPermissions should folder inherit permissions from parent?
     */
    @LogMethod
    public void createSubfolder(@LoggedParam String project, String parent, @LoggedParam String child, @Nullable String folderType, String templateFolder, @Nullable String[] templatePartsToUncheck, @Nullable String[] tabsToAdd, boolean inheritPermissions)
    {
        startCreateFolder(project, parent, child);
        if (null != folderType && !folderType.equals("None"))
        {
            _test.click(Locator.xpath("//td[./label[text()='" + folderType + "']]/input[@type='button' and contains(@class, 'radio')]"));
            if(folderType.equals("Create From Template Folder"))
            {
                _test.log("create from template");
                _test.click(Locator.xpath("//td[./label[text()='" + folderType + "']]/input[@type='button' and contains(@class, 'radio')]"));
                _test._ext4Helper.waitForMaskToDisappear();
                _test._ext4Helper.selectComboBoxItem(Locator.xpath("//div").withClass("labkey-wizard-header").withText("Choose Template Folder:").append("/following-sibling::table[contains(@id, 'combobox')]"), templateFolder);
                _test._ext4Helper.checkCheckbox("Include Subfolders");
                if (templatePartsToUncheck != null)
                {
                    for(String part : templatePartsToUncheck)
                    {
                        _test.click(Locator.xpath("//td[label[text()='" + part + "']]/input"));
                    }
                }
            }
        }
        else {
            _test.click(Locator.xpath("//td[./label[text()='Custom']]/input[@type='button' and contains(@class, 'radio')]"));


            if (tabsToAdd != null)
            {
                for (String tabname : tabsToAdd)
                    _test.waitAndClick(Locator.xpath("//td[./label[text()='" + tabname + "']]/input[@type='button' and contains(@class, 'checkbox')]"));
            }
        }

        _test.clickButton("Next", _test.defaultWaitForPage);
        _createdFolders.add(new WebTestHelper.FolderIdentifier(project, child));

        //second page of the wizard
        _test.waitForElement(Locator.css(".labkey-nav-page-header").withText("Users / Permissions"));
        if (!inheritPermissions)
        {
            _test.waitAndClick(Locator.xpath("//td[./label[text()='My User Only']]/input"));
        }

        _test.clickButton("Finish", _test.defaultWaitForPage);
        _test.waitForElement(Locator.id("folderBar").withText(project));

        //unless we need addtional tabs, we end here.
        if (null == tabsToAdd || tabsToAdd.length == 0)
            return;


        if (null != folderType && !folderType.equals("None")) // Added in the wizard for custom folders
        {
            _test.goToFolderManagement();
            _test.clickAndWait(Locator.linkWithText("Folder Type"));

            for (String tabname : tabsToAdd)
                _test.checkCheckbox(Locator.checkboxByTitle(tabname));

            _test.submit();
            if ("None".equals(folderType))
            {
                for (String tabname : tabsToAdd)
                    _test.assertElementPresent(Locator.folderTab(tabname));
            }

            // verify that there's a link to our new folder:
            _test.assertElementPresent(Locator.linkWithText(child));
        }
    }

    private  void startCreateFolder(String project, String parent, String child)
    {
        _test.clickProject(project);
        if (!parent.equals(project))
        {
            _test.clickFolder(parent);
        }
        _test.hoverFolderBar();
        if (_test.isElementPresent(Locator.id("folderBar_menu").append(Locator.linkWithText(child))))
            throw new IllegalArgumentException("Folder: " + child + " already exists in project: " + project);
        _test.log("Creating subfolder " + child + " under " + parent);
        _test.clickAndWait(Locator.xpath("//a[@title='New Subfolder']"));
        _test.waitForElement(Locator.name("name"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.setFormElement(Locator.name("name"), child);
    }

    @LogMethod
    public void deleteFolder(String project, @LoggedParam String folderName)
    {
        _test.log("Deleting folder " + folderName + " under project " + project);
        _test.clickProject(project);
        _test.clickFolder(folderName);
        _test.ensureAdminMode();
        _test.goToFolderManagement();
        _test.waitForElement(Ext4Helper.Locators.folderManagementTreeNode(folderName));
        _test.clickButton("Delete");
        // confirm delete subfolders if present
        if(_test.isTextPresent("This folder has subfolders."))
            _test.clickButton("Delete All Folders");
        // confirm delete:
        _test.clickButton("Delete");
        // verify that we're not on an error page with a check for a project link:
        _test.assertElementPresent(Locator.currentProject().withText(project));
        _test.hoverFolderBar();
        _test.assertElementNotPresent(Locator.linkWithText(folderName));
    }

    @LogMethod
    public void renameFolder(String project, @LoggedParam String folderName, @LoggedParam String newFolderName, boolean createAlias)
    {
        _test.log("Renaming folder " + folderName + " under project " + project + " -> " + newFolderName);
        _test.clickProject(project);
        _test.clickFolder(folderName);
        _test.ensureAdminMode();
        _test.goToFolderManagement();
        _test.waitForElement(Ext4Helper.Locators.folderManagementTreeNode(folderName).notHidden());
        _test.clickButton("Change Name Settings");
        _test.setFormElement(Locator.name("name"), newFolderName);
        if (createAlias)
            _test.checkCheckbox(Locator.name("addAlias"));
        else
            _test.uncheckCheckbox(Locator.name("addAlias"));
        // confirm rename:
        _test.clickButton("Save");
        _createdFolders.remove(new WebTestHelper.FolderIdentifier(project, folderName));
        _createdFolders.add(new WebTestHelper.FolderIdentifier(project, newFolderName));
        _test.assertElementPresent(Locator.currentProject().withText(project));
        _test.hoverFolderBar();
        _test.waitForElement(Locator.linkWithText(newFolderName));
        _test.assertElementNotPresent(Locator.linkWithText(folderName));
    }

    @LogMethod
    public void moveFolder(@LoggedParam String projectName, @LoggedParam String folderName, @LoggedParam String newParent, boolean createAlias) throws CommandException
    {
        _test.log("Moving folder [" + folderName + "] under project [" + projectName + "] to [" + newParent + "]");
        _test.clickProject(projectName);
        _test.clickFolder(folderName);
        _test.ensureAdminMode();
        _test.goToFolderManagement();
        _test.waitForElement(Ext4Helper.Locators.folderManagementTreeNode(folderName));
        _test.clickButton("Move");
        if (createAlias)
            _test.checkCheckbox(Locator.name("addAlias"));
        else
            _test.uncheckCheckbox(Locator.name("addAlias"));
        // Select Target
        _test.waitForElement(Locator.permissionsTreeNode(newParent), 10000);
        _test.sleep(1000); // TODO: what is the right way to wait for the tree expanding animation to complete?
        _test.selectFolderTreeItem(newParent);
        // move:
        _test.clickButton("Confirm Move");

        // verify that we're not on an error page with a check for folder link:
        _test.assertElementPresent(Locator.currentProject().withText(projectName));
        _test.hoverFolderBar();
        _test.waitForElement(Locator.xpath("//li").withClass("clbl").withPredicate(Locator.xpath("a").withText(newParent)).append("/ul/li/a").withText(folderName));
        String newProject = _test.getText(Locator.currentProject());
        _createdFolders.remove(new WebTestHelper.FolderIdentifier(projectName, folderName));
        _createdFolders.add(new WebTestHelper.FolderIdentifier(newProject, folderName));
    }
}
