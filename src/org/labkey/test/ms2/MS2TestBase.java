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

package org.labkey.test.ms2;

import org.labkey.test.BaseSeleniumWebTest;

import java.io.IOException;
import java.io.File;

/**
 * User: brittp
 * Date: Nov 28, 2005
 * Time: 2:47:27 PM
 */
abstract public class MS2TestBase extends BaseSeleniumWebTest
{
    protected static final String PROJECT_NAME = "MS2VerifyProject";
    protected static final String FOLDER_NAME = "ms2folder";
    protected static final String SAMPLE_BASE_NAME = "CAexample_mini";
    protected static final String VIEW = "filterView";
    protected static final String LOG_BASE_NAME = "CAexample_mini";
    protected static final String DATABASE = "Bovine_mini.fasta";
    protected static final String INPUT_XML =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" ?> \n" +
        "<bioml>\n" +
            "  <note label=\"pipeline, protocol name\" type=\"input\">test2</note> \n" +
            "  <note label=\"pipeline, protocol description\" type=\"input\">This is a test protocol using the defaults.</note> \n" +
            "  <note label=\"pipeline prophet, min probability\" type=\"input\">0</note> \n" +
        "  </bioml>";
    protected static final int MAX_WAIT_SECONDS = 60*5;

    protected final String _pipelinePath = getLabKeyRoot() + "/sampledata/xarfiles/ms2pipe";

    public String getAssociatedModuleDirectory()
    {
        return "ms2";
    }

    protected void doCleanup() throws IOException
    {
        cleanPipe(_pipelinePath);
        try {deleteFolder(PROJECT_NAME, FOLDER_NAME); } catch (Throwable t) {}
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
    }

    protected void doTestSteps()
    {
        log("Set cluster checkbox");
        clickLinkWithText("Admin Console");
        clickLinkWithText("site settings");
        uncheckCheckbox("pipelineCluster");
        clickNavButton("Save");

        createProject(PROJECT_NAME);
        createSubfolder(PROJECT_NAME, PROJECT_NAME, FOLDER_NAME, "MS2", new String[] { });

        log("Setup pipeline.");
        clickNavButton("Setup");

        log("Set bad pipeline root.");
        setFormElement("path", "/bogus");
        submit();
        assertTextPresent("does not exist");

        log("Set good pipeline root.");
        setFormElement("path", _pipelinePath);
        submit();
    }

    protected void deleteViews(String... viewNames)
    {
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        if (isLinkPresentWithImage(getContextPath() + "/MS2/images/runIcon.gif"))
        {
            clickLinkWithImage(getContextPath() + "/MS2/images/runIcon.gif");
            clickNavButton("Manage Views");
            for (String viewName : viewNames)
            {
                log("Deleting View " + viewName);
                if (isTextPresent(viewName))
                {
                    checkCheckbox("viewsToDelete", viewName, false);
                }
            }
            clickNavButton("OK");
        }
    }

    protected void deleteRuns()
    {
        log("Delete runs.");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);

        clickLinkWithText("MS2 Runs");
        selectOptionByText("experimentRunFilter", "All Runs");
        waitForPageToLoad();
        if (!isTextPresent("No data to show"))
        {
            checkCheckbox(".toggle");
            clickNavButton("Delete");

            log("Confirm deletion");
            clickNavButton("Confirm Delete");
        }

        log("Make sure all the data got deleted");
        assertTextNotPresent(SAMPLE_BASE_NAME);
    }

    protected void delete(File file) throws IOException
    {
        if (file.isDirectory())
        {
            for (File child : file.listFiles())
            {
                delete(child);
            }
        }
        System.out.println("Deleting " + file.getPath());
        file.delete();
    }

    protected void cleanPipe(String search_type) throws IOException
    {
        if (_pipelinePath == null)
            return;

        File rootDir = new File(_pipelinePath);
        delete(new File(rootDir, "bov_sample/xars"));
        delete(new File(rootDir, "bov_sample/"+search_type+"/test1/CAexample_mini.log"));
        delete(new File(rootDir, "bov_sample/"+search_type+"/test2"));
        delete(new File(rootDir, ".labkey/protocols/mass_spec/TestMS2Protocol.xml"));
        delete(new File(rootDir, ".labkey/protocols/"+search_type+"/default.xml"));
        delete(new File(rootDir, ".labkey/protocols/"+search_type+"/test2.xml"));
    }
}
