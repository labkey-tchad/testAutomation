/*
 * Copyright (c) 2005-2012 LabKey Corporation
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

import org.labkey.test.module.*;
import org.labkey.test.ms2.MS2ClusterTest;
import org.labkey.test.ms2.MascotTest;
import org.labkey.test.ms2.QuantitationTest;
import org.labkey.test.ms2.SequestTest;
import org.labkey.test.tests.*;

import java.util.Arrays;
import java.util.List;

public enum TestSet
{
    DRT(new Class[] {
        BasicTest.class,
        JUnitTest.class,
        SecurityShortTest.class,
        FlowShortTest.class,
        XTandemShortTest.class,
        StudyShortTest.class,
    }),
    //new tests added to the DRTs should be added to BVT as well

    BVT(new Class[] {
        BasicTest.class,
        JUnitTest.class,
        WikiTest.class,
        ExpTest.class,
        AssayTest.class,
        PipelineTest.class,
        FileContentTest.class,
        ClientAPITest.class,
        ReportTest.class,
        MicroarrayTest.class,
        ButtonCustomizationTest.class,
        ReagentTest.class,
        FilterTest.class,
        GroupTest.class,
        StudyRedesignTest.class,
        SecurityTest.class,
        FlowTest.class,
        XTandemTest.class,
        StudyTest.class,
    }),

    MS2(new Class[]
    {
        XTandemTest.class,
        MascotTest.class,
        SequestTest.class,
        MS2Test.class,
        MS2GZTest.class,
        LibraTest.class,
        TargetedMSTest.class,
    }),

    Daily(600000, new Class[]
    {
        BasicTest.class,
        EmbeddedWebPartTest.class,
        ModuleAssayTest.class,
        SpecimenTest.class,
        StudyExtraTest.class,
        NabAssayTest.class,
        FlowJoQueryTest.class,
        FlowImportTest.class,
        FlowNormalizationTest.class,
        FlowCBCTest.class,
        DataRegionTest.class,
        UserPermissionsTest.class,
        SampleSetTest.class,
        AuditLogTest.class,
        FieldValidatorTest.class,
        ProgrammaticQCTest.class,
        SchemaBrowserTest.class,
        StudySecurityTest.class,
        MS1Test.class,
        UniprotAnnotationTest.class, //requires bootstrap
        HTTPApiTest.class,
        MessagesLongTest.class, //do we need both MessagesTest and MessagesLongTest?
        QuantitationTest.class,
        MessagesTest.class,
        MS2Test.class,
        MS2GZTest.class,
        WikiLongTest.class,
        ListTest.class,
        UserTest.class,
        IssuesTest.class,
        NabOldTest.class,
        TimelineTest.class,
        ExternalSchemaTest.class,
        MenuBarTest.class,
        LuminexUploadAndCopyTest.class,
        LuminexExcludableWellsTest.class,
        LuminexMultipleCurvesTest.class,
        LuminexJavaTransformTest.class,
        LuminexRTransformTest.class,
        LuminexEC50Test.class,
        LuminexGuideSetTest.class,
        LuminexAsyncImportTest.class,
        LuminexPositivityTest.class,
        SimpleModuleTest.class,
        JavaClientApiTest.class,
        QuerySnapshotTest.class,
        SCHARPStudyTest.class,
        FormulationsTest.class,
        CohortTest.class,
        RlabkeyTest.class,
        ScriptValidationTest.class,
        SearchTest.class,
        WorkbookTest.class,
        CustomizeViewTest.class,
        ElispotAssayTest.class,
        CreateVialsTest.class,
        SpecimenMergeTest.class,
        TargetStudyTest.class,
        TargetedMSTest.class,
        TimeChartTest.class,
        EHRStudyTest.class,
        GpatAssayTest.class,
        FolderTest.class,
        StudyDemoModeTest.class,
        LibraTest.class,
//        StudyReloadTest.class, //TODO:  don't checkin
        AncillaryStudyTest.class,
        FolderExportTest.class,
        GenotypingTest.class,
        MissingValueIndicatorsTest.class,
        PipelineCancelTest.class,
        StudyExportTest.class,
        ViabilityTest.class,
        ProjectSettingsTest.class,
        CDSTest.class,
        ExtraKeyStudyTest.class,
        CAVDStudyTest.class,
        SampleMindedImportTest.class,
        PivotQueryTest.class,
    }),

    MiniTest(new Class[]
    {
        LuminexUploadAndCopyTest.class,
        LuminexExcludableWellsTest.class,
        LuminexMultipleCurvesTest.class,
        LuminexJavaTransformTest.class,
        LuminexRTransformTest.class,
        LuminexEC50Test.class,
        LuminexGuideSetTest.class,
        LuminexAsyncImportTest.class,
        LuminexPositivityTest.class,
    }),

    IE(new Class[]
    {
        BasicTest.class,
        FlowTest.class,
        SecurityTest.class,
        WikiTest.class,
        GpatAssayTest.class,
        EmbeddedWebPartTest.class,
        AssayTest.class,
        FolderTest.class,
        StudyTest.class
    }),

    Cluster(new Class[]
    {
        MS2ClusterTest.class
    }),

    XTandem(new Class[]
    {
        XTandemTest.class
    }),

    Mascot(new Class[]
    {
        MascotTest.class
    }),

    Sequest(new Class[]
    {
        SequestTest.class
    }),

    Module(new Class[]
    {
        ModuleTest.class
    }),

    Flow(new Class[] {
        FlowTest.class,
        FlowJoQueryTest.class,
        FlowImportTest.class,
        FlowNormalizationTest.class,
        FlowCBCTest.class,
    }),

    // Many (but not all) of the tests that use wiki functionality
    Wiki(new Class[] {
        WikiTest.class,
        WikiLongTest.class,
        ClientAPITest.class,
        ButtonCustomizationTest.class,
        EmbeddedWebPartTest.class,
        TimelineTest.class
    }),

    Study(new Class[] {
        StudyTest.class,
        StudyExportTest.class,
        StudyManualTest.class,
        StudyExtraTest.class,
        CohortTest.class,
        AssayTest.class,
        SpecimenMergeTest.class,
        TargetStudyTest.class,
        ReportTest.class,
        QuerySnapshotTest.class,
        SCHARPStudyTest.class,
        AncillaryStudyTest.class,
        CAVDStudyTest.class,
        ExtraKeyStudyTest.class,
        SampleMindedImportTest.class
    }),

    Assays(new Class[] {
        AssayTest.class,
        MissingValueIndicatorsTest.class,
        ElispotAssayTest.class,
        TargetStudyTest.class,
        NabOldTest.class,
        NabAssayTest.class,
        LuminexUploadAndCopyTest.class,
        LuminexExcludableWellsTest.class,
        LuminexMultipleCurvesTest.class,
        LuminexJavaTransformTest.class,
        LuminexRTransformTest.class,
        LuminexEC50Test.class,
        LuminexGuideSetTest.class,
        LuminexAsyncImportTest.class,
        LuminexPositivityTest.class,
        ViabilityTest.class,
        ModuleAssayTest.class,
        FormulationsTest.class,
    }),

    UnitTests(new Class[] {
        JUnitTest.class
    }),

    Chrome(new Class[] {
        ListExportTest.class
    }),

    Data(new Class[] {
        DataRegionTest.class,
        ExternalSchemaTest.class,
        ListTest.class,
        IssuesTest.class,
        ScriptValidationTest.class,
        FilterTest.class,
        PivotQueryTest.class
    }),

    IDRI(new Class[] {
        FormulationsTest.class
    }),

    BVTnDaily(BVT, Daily.tests),

    Weekly(600000, BVTnDaily, new Class[] {
            // Add special test classes, not in daily or BVT.
            SecurityTestExtended.class,
            NabMigrationTest.class
    }),

    Adaptive(new Class[]{
        AdaptiveTest.class
    }),

    AdaptivePerf(new Class[]{
            AdaptiveVisualizationPerf.class
    }),

    EHR(new Class[]{
            EHRStudyTest.class
    }),

    CDSPopulation(new Class[] {
            CDSPopulation.class
    }),

    CONTINUE(new Class[] {})
    {
        public boolean isSuite()
        {
            return false;
        }
    },

    TEST(new Class[] {})
    {
        public boolean isSuite()
        {
            return false;
        }
    }
    ;


    public Class[] tests;
    private static final int DEFAULT_CRAWLER_TIMEOUT = 90000;
    private int crawlerTimeout;

    TestSet(int timeout, TestSet set, Class... tests)
    {
        Class[] all = new Class[set.tests.length + tests.length];
        System.arraycopy(set.tests, 0, all, 0, set.tests.length);
        System.arraycopy(tests, 0, all, set.tests.length, tests.length);
        setTests(timeout, all);
    }

    TestSet(TestSet set, Class... tests)
    {
        this(DEFAULT_CRAWLER_TIMEOUT, set, tests);
    }

    TestSet(int timeout, Class... tests)
    {
        setTests(timeout, tests);
    }

    TestSet(Class... tests)
    {
        setTests(DEFAULT_CRAWLER_TIMEOUT, tests);
    }

    void setTests(Class... tests)
    {
        setTests(DEFAULT_CRAWLER_TIMEOUT, tests);
    }

    void setTests(int timeout, Class... tests)
    {
        this.tests = tests;
        crawlerTimeout = timeout;
    }

    public boolean isSuite()
    {
        return true;
    }

    public int getCrawlerTimeout()
    {
        return crawlerTimeout;
    }

    public List<Class> getTestList()
    {
        return Arrays.asList(tests);
    }
}
