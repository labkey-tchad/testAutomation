/*
 * Copyright (c) 2012 LabKey Corporation
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

import org.labkey.test.WebTestHelper;
import org.labkey.test.util.DataRegionTable;

import java.io.File;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 2/21/12
 * Time: 1:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class LuminexPositivityTest extends LuminexTest
{

    private String assayName = "Positivity";
    protected void runUITests()
    {
        addTransformScript(new File(WebTestHelper.getLabKeyRoot(), getAssociatedModuleDirectory() + RTRANSFORM_SCRIPT_FILE1));

        //clickNavButton("Save & Close");
        //TODO: Just 'Save & Close' to avoid timing issues. Blocked
        saveAssay();
        sleep(5000);

        // Test positivity data upload with 3x Fold Change
        uploadPositivityFile(assayName + " 3x Fold Change", 3);
        String[] posWells = new String[] {"A2", "B2", "A6", "B6", "A8", "B8", "A9", "B9"};
        checkPositivityValues("positive", posWells.length, posWells);
        String[] negWells = new String[] {"A3", "B3", "A5", "B5", "A10", "B10"};
        checkPositivityValues("negative", negWells.length, negWells);

        // Test positivity data upload with 5x Fold Change
        uploadPositivityFile(assayName + " 5x Fold Change", 5);
        posWells = new String[] {"A8", "B8", "A9", "B9"};
        checkPositivityValues("positive", posWells.length, posWells);
        negWells = new String[] {"A2", "B2", "A3", "B3", "A5", "B5", "A6", "B6", "A10", "B10"};
        checkPositivityValues("negative", negWells.length, negWells);
    }

    private void uploadPositivityFile(String assayName, int foldChange)
    {
        goToTestAssayHome();
        clickNavButton("Import Data");
        clickNavButton("Next");
        setFormElement("name", assayName);
        checkCheckbox("calculatePositivity");
        setFormElement("baseVisit", "1");
        setFormElement("positivityFoldChange", String.valueOf(foldChange));
        File positivityData = new File(getSampledataPath(), "Luminex/Positivity.xls");
        assertTrue("Positivity Data absent: " + positivityData.toString(), positivityData.exists());
        setFormElement("__primaryFile__", positivityData);
        clickNavButton("Next");
        clickNavButton("Save and Finish");
        clickLinkWithText(assayName);
    }

    private void checkPositivityValues(String type, int numExpected, String[] positivityWells)
    {
        // verify that we are already on the Data results view
        assertTextPresent(TEST_ASSAY_LUM+ " Results");

        assertTextPresent(type, numExpected);

        DataRegionTable drt = new DataRegionTable( TEST_ASSAY_LUM+ " Data", this);
        List<String> posivitiy = drt.getColumnDataAsText("Positivity");
        List<String> wells = drt.getColumnDataAsText("Well");

        for(String well : positivityWells)
        {
            int i = wells.indexOf(well);
            assertEquals(type, posivitiy.get(i));
        }
    }
}
