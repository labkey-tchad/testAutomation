/*
 * Copyright (c) 2007-2008 LabKey Corporation
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

package org.labkey.test.drt;

import org.labkey.test.BaseSeleniumWebTest;

import java.io.File;

/**
 * User: brittp
 * Date: May 13, 2006
 * Time: 2:53:13 PM
 */
public class NabTest extends BaseSeleniumWebTest
{
    private static final String PROJECT_NAME = "NabVerifyProject";
    private static final String FOLDER_NAME = "NabFolder";
    private static final String TEST_FILE_NAME = "m0902051;3997.xls";
    private static final String TEST_FILE_PATH = "/sampledata/Nab/" + TEST_FILE_NAME;


    public String getAssociatedModuleDirectory()
    {
        return "nab";
    }


    @Override
    protected boolean isFileUploadTest()
    {
        return true;
    }

    protected void doCleanup() throws Exception
    {
        try {deleteFolder(PROJECT_NAME, FOLDER_NAME); } catch (Throwable t) {}
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
    }

    protected void doTestSteps()
    {
        if (!isFileUploadAvailable())
        {
            log("\n\n====\nNAB Test requires file upload for any testing. Test is being skipped in this browser\n\n===");
            return;
        }
        
        createProject(PROJECT_NAME);
        createSubfolder(PROJECT_NAME, FOLDER_NAME, new String[]{"Nab", "Study"});

        // click the study tab to force creation of the study table row:
        clickTab("Study");
        clickNavButton("Create Study");
        clickNavButton("Create Study");
        clickTab("Nab");
        File testFile = new File(getLabKeyRoot() + TEST_FILE_PATH);
        setFormElement("dataFile", testFile);
        setFormElement("metadata.virusName", "First run");
        uncheckCheckbox("runSettings.inferFromFile");
        clickNavButton("Calculate", longWaitForPage);
        assertTextPresent(TEST_FILE_NAME);
        assertTextPresent("1547");
        assertTextPresent("109935");

        clickTab("Nab");
        setFormElement("dataFile", testFile);
        setFormElement("metadata.virusName", "Second run");
        clickNavButton("Calculate");
        assertTextPresent(TEST_FILE_NAME);
        assertTextPresent("1353");
        clickTab("Nab");

        log("Verify saved run");
        clickLinkWithText("Previous Runs");
        assertTextPresent("First run");
        assertTextPresent("Second run");
        assertTextPresent(TEST_FILE_NAME);

        log("Verify saved sample results");
        clickLinkWithText("Previous Runs By Sample");
        assertTextPresent("Specimen 1");

        log("Verify EC50 Values");
        assertTextPresent("1546.78");
        assertTextPresent("1353.14");

        log("Verify EC80 Values");
        assertTextPresent("357.04");
        assertTextPresent("353.65");

        clickLinkWithText("details");
        assertTextPresent(TEST_FILE_NAME);

        clickLinkWithText("Previous Runs By Sample");
        checkCheckbox(".select", 0, false);
        clickNavButton("Copy Selected to Study");
        selectOptionByText("targetContainerId", "/NabVerifyProject/NabFolder (NabFolder Study)");
        clickNavButton("Next");
        setFormElement("sequenceNums", "100.1");
        setFormElement("participantIds", "Participant001");
        clickNavButton("Copy to Study");

        assertTextPresent("Participant001");
        assertTextPresent("100.1");

        clickTab("Nab");
        clickLinkWithText("Previous Runs");
        log("Delete run");
        clickImgButtonNoNav("Select All");
        clickNavButton("Delete Selected");

        log("Verify deleted run");
        assertTextPresent("Incubation Time");
        assertTextNotPresent(TEST_FILE_NAME);
    }
}
