/*
 * Copyright (c) 2012-2016 LabKey Corporation
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
package org.labkey.test;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.assay.ImportRunResponse;
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.APIAssayHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Maps;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Category({DailyA.class})
public class AssayAPITest extends BaseWebDriverTest
{
    @Override
    protected String getProjectName()
    {
        return "Assay API TEST";
    }

    @BeforeClass
    public static void doSetup() throws Exception
    {
        AssayAPITest initTest = (AssayAPITest)getCurrentTest();
        initTest._containerHelper.createProject(initTest.getProjectName(), "Assay");
    }

    @Test
    public void testImportRun() throws Exception
    {
        goToProjectHome();
        int pipelineCount = 0;
        String runName = "trial01.xls";
        importAssayAndRun(TestFileUtils.getSampleData("AssayAPI/XLS Assay.xar.xml"), ++pipelineCount, "XLS Assay",
                TestFileUtils.getSampleData("GPAT/" + runName), runName, new String[]{"K770K3VY-19"});
        waitForElement(Locator.paginationText(1, 100, 201));

        goToProjectHome();

        //Issue 16073
        importAssayAndRun(TestFileUtils.getSampleData("AssayAPI/BatchPropRequired.xar"), ++pipelineCount, "BatchPropRequired",
                TestFileUtils.getSampleData("GPAT/" + runName), "trial01-1.xls", new String[]{"K770K3VY-19"});
        waitForElement(Locator.paginationText(1, 100, 201));
//        _assayHelper.getCurrentAssayNumber();
    }

    // Issue 30003: support importing assay data relative to pipeline root
    @Test
    public void testImportRun_serverFilePath() throws Exception
    {
        goToProjectHome();

        String assayName = "GPAT-RunFilePath";
        APIAssayHelper assayHelper = new APIAssayHelper(this);
        assayHelper.createAssayWithDefaults("General", assayName);
        int assayId = assayHelper.getIdFromAssayName(assayName, getProjectName());

        // First, simulate file already being uploaded to the server by copying to the pipeline root
        List<String> lines1 = Arrays.asList(
                "ptid\tdate\n",
                "p01\t2017-05-10\n",
                "p02\t2017-05-10\n"
        );
        Path relativePath1 = Paths.get("testImportRunFilePath", "results1.tsv");
        Path pipelinePath1 = createDataFile(relativePath1, lines1);

        // import the file using a relative path
        ImportRunResponse resp = assayHelper.importAssay(assayId, relativePath1.toString(), getProjectName(), Collections.emptyMap());
        beginAt(resp.getSuccessURL());
        assertTextPresent("p01", "p02");

        goToProjectHome();

        List<String> lines2 = Arrays.asList(
                "ptid\tdate\n",
                "p03\t2017-05-10\n",
                "p04\t2017-05-10\n"
        );
        Path relativePath2 = Paths.get("testImportRunFilePath", "results2.tsv");
        Path pipelinePath2 = createDataFile(relativePath2, lines2);

        // import the file using an absolute path
        resp = assayHelper.importAssay(assayId, pipelinePath2.toString(), getProjectName(), Collections.emptyMap());
        beginAt(resp.getSuccessURL());
        assertTextPresent("p03", "p04");

        // attempt to import file outside of pipeline root
        try
        {
            File runFilePath = TestFileUtils.getSampleData("GPAT/trial01.xls");
            assayHelper.importAssay(assayId, runFilePath.toString(), getProjectName(), Collections.emptyMap());
            fail("Expected exception trying to read file outside of pipeline root");
        }
        catch (CommandException ex)
        {
            assertTrue("Expected 'File not found', got: " + ex.getMessage(), ex.getMessage().contains("File not found"));
            assertTrue("Expected 'trial01.xls', got: " + ex.getMessage(), ex.getMessage().contains("trial01.xls"));
        }
    }

    private Path createDataFile(Path relativePath, Iterable<String> lines) throws IOException
    {
        File fileRoot = TestFileUtils.getDefaultFileRoot(getProjectName());
        Path pipelinePath = fileRoot.toPath().resolve(relativePath);
        if (!Files.isRegularFile(pipelinePath))
        {
            Files.createDirectories(pipelinePath.getParent());
            Files.write(pipelinePath, lines);
            if (!Files.isRegularFile(pipelinePath))
                fail("Failed to create file " + pipelinePath);
        }
        return pipelinePath;
    }

    // Issue 21247: Import runs into GPAT assay using LABKEY.Experiment.saveBatch() API
    @Test
    public void testGpatSaveBatch() throws Exception
    {
        goToProjectHome();

        log("create GPAT assay");
        String assayName = "GPAT-SaveBatch";
        _assayHelper.createAssayWithDefaults("General", assayName);

        log("create run via saveBatch");
        String runName = "created-via-saveBatch";
        List<Map<String, Object>> resultRows = new ArrayList<>();
        resultRows.add(Maps.of("ptid", "188438418", "SpecimenID", "K770K3VY-19"));
        resultRows.add(Maps.of("ptid", "188487431", "SpecimenID", "A770K4W1-15"));
        ((APIAssayHelper)_assayHelper).saveBatch(assayName, runName, resultRows, getProjectName());

        log("verify assay saveBatch worked");
        goToManageAssays();
        clickAndWait(Locator.linkContainingText(assayName));
        clickAndWait(Locator.linkContainingText(runName));
        DataRegionTable table = new DataRegionTable("Data", this);
        Assert.assertEquals(Arrays.asList("K770K3VY-19", "A770K4W1-15"), table.getColumnDataAsText("SpecimenID"));
    }

    protected void  importAssayAndRun(File assayPath, int pipelineCount, String assayName, File runPath,
                                      String runName, String[] textToCheck) throws IOException, CommandException
    {
        APIAssayHelper assayHelper = new APIAssayHelper(this);
        assayHelper.uploadXarFileAsAssayDesign(assayPath, pipelineCount);
        assayHelper.importAssay(assayName, runPath, getProjectName(), Collections.singletonMap("ParticipantVisitResolver", "SampleInfo"));

        log("verify import worked");
        goToProjectHome();
        clickAndWait(Locator.linkContainingText(assayName));
        clickAndWait(Locator.linkContainingText(runName));
        assertTextPresent(textToCheck);
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName(), afterTest);
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("assay");
    }
}
