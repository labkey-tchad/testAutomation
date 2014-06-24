package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.InDevelopment;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PipelineAnalysisHelper;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.RReportHelper;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

@Category({InDevelopment.class, Assays.class})
public class ExpressionMatrixAssayTest extends BaseWebDriverTest
{
    private static final String PIPELINE_NAME = "create-matrix";
    private static final String SAMPLE_SET = "ExpressionMatrix SampleSet";
    private static final String ASSAY_NAME = "Test Expression Matrix";
    private static final String FEATURE_SET_NAME = "Expression Matrix Test Feature Set";
    private static final String FEATURE_SET_VENDOR = "Vendor";
    private static final File FEATURE_SET_DATA = getSampleData("Microarray/expressionMatrix/sample-feature-set.txt");
    private static final File CEL_FILE1 = getSampleData("Affymetrix/CEL_files/sample_file_1.CEL");
    private static final File CEL_FILE2 = getSampleData("Affymetrix/CEL_files/sample_file_2.CEL");
    private static final String PROTOCOL_NAME = "Expression Matrix Protocol";
    private static int featureSetId;
    private static int expectedPipelineJobCount;

    private final PipelineAnalysisHelper pipelineAnalysis = new PipelineAnalysisHelper(this);

    @BeforeClass
    public static void doSetup() throws Exception
    {
        ExpressionMatrixAssayTest initTest = new ExpressionMatrixAssayTest();
        initTest.doCleanup(false);

        initTest.doSetupSteps();
        expectedPipelineJobCount = 0;

        currentTest = initTest;
    }

    private void doSetupSteps()
    {
        RReportHelper rReportHelper = new RReportHelper(this);
        rReportHelper.ensureRConfig();

        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.enableModules(Arrays.asList("pipelinetest", "Microarray"));

        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Sample Sets");
        clickButton("Import Sample Set");
        setFormElement(Locator.id("name"), SAMPLE_SET);
        setFormElement(Locator.name("data"), "KeyCol\n1");
        clickButton("Submit");

        clickProject(getProjectName());
        portalHelper.addWebPart("Feature Annotation Sets");
        featureSetId = createFeatureSet(FEATURE_SET_NAME, FEATURE_SET_VENDOR, FEATURE_SET_DATA);

        clickProject(getProjectName());
        portalHelper.addWebPart("Assay List");
        _assayHelper.createAssayWithDefaults("Expression Matrix", ASSAY_NAME);

        goToModule("FileContent");
        _fileBrowserHelper.uploadFile(CEL_FILE1);
        _fileBrowserHelper.uploadFile(CEL_FILE2);
    }

    @LogMethod
    private int createFeatureSet(@LoggedParam String name, String vendor, File featureSetData)
    {
        clickButton("Import Feature Annotation Set");
        setFormElement(Locator.name("name"), name);
        setFormElement(Locator.name("vendor"), vendor);
        setFormElement(Locator.name("annotationFile"), featureSetData);
        clickButton("upload");
        clickAndWait(Locator.linkWithText(name));
        return Integer.parseInt(getUrlParam("rowId"));
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testExpressionMatrixAssay()
    {
        final String importAction = "Use R to generate a dummy matrix tsv output file with two samples and two features.";
        final String protocolName = "CreateMatrix";
        final String[] targetFiles = {CEL_FILE1.getName(), CEL_FILE2.getName()};
        final String parameterXml = getParameterXml(ASSAY_NAME, protocolName, featureSetId, true);
        final Map<String, String> protocolProperties = Maps.of(
                "protocolName", protocolName,
                "xmlParameters", parameterXml,
                "saveProtocol", "false");
        pipelineAnalysis.runPipelineAnalysis(importAction, targetFiles, protocolProperties);

        final String pipelineName = PIPELINE_NAME;
        final File fileRoot = getDefaultFileRoot(getProjectName());
        final Map<String, Set<String>> outputFiles = Maps.of(
                pipelineName + ".xml", Collections.<String>emptySet(),
                protocolName + "-taskInfo.tsv", Collections.<String>emptySet(),
                protocolName + ".log", Collections.<String>emptySet(),
                protocolName + ".tsv", Collections.<String>emptySet());
        PipelineAnalysisHelper.setExpectedJobCount(++expectedPipelineJobCount);
        pipelineAnalysis.verifyPipelineAnalysis(pipelineName, protocolName, fileRoot, outputFiles);

        goToProjectHome();
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        assertElementPresent(Locator.linkWithText(FEATURE_SET_NAME));
        clickAndWait(Locator.linkWithText(protocolName));

        DataRegionTable resultTable = new DataRegionTable("Data", this);
        assertEquals(Arrays.asList("10.000000", "30.000000", "20.000000", "40.000000"),
                resultTable.getColumnDataAsText("Value"));
        assertEquals(Arrays.asList("78495_at", "78495_at", "78383_at", "78383_at"),
                resultTable.getColumnDataAsText("Probe Id"));
        assertEquals(Arrays.asList("SampleA", "SampleB", "SampleA", "SampleB"),
                resultTable.getColumnDataAsText("Sample Id"));
        assertEquals(Arrays.asList(protocolName, protocolName, protocolName, protocolName),
                resultTable.getColumnDataAsText("Run"));

        goToSchemaBrowser();
        selectQuery("assay.ExpressionMatrix." + ASSAY_NAME, "FeatureDataBySample");
    }

    @Test
    public void testExpressionMatrixAssayNoValues()
    {
        final String importAction = "Use R to generate a dummy matrix tsv output file with two samples and two features.";
        final String protocolName = "CreateMatrixNoValues";
        final String[] targetFiles = {CEL_FILE1.getName(), CEL_FILE2.getName()};
        final String parameterXml = getParameterXml(ASSAY_NAME, protocolName, featureSetId, false);
        final Map<String, String> protocolProperties = Maps.of(
                "protocolName", protocolName,
                "xmlParameters", parameterXml,
                "saveProtocol", "false");
        pipelineAnalysis.runPipelineAnalysis(importAction, targetFiles, protocolProperties);

        final String pipelineName = PIPELINE_NAME;
        final File fileRoot = getDefaultFileRoot(getProjectName());
        final Map<String, Set<String>> outputFiles = Maps.of(
                pipelineName + ".xml", Collections.<String>emptySet(),
                protocolName + "-taskInfo.tsv", Collections.<String>emptySet(),
                protocolName + ".log", Collections.<String>emptySet(),
                protocolName + ".tsv", Collections.<String>emptySet());
        PipelineAnalysisHelper.setExpectedJobCount(++expectedPipelineJobCount);
        pipelineAnalysis.verifyPipelineAnalysis(pipelineName, protocolName, fileRoot, outputFiles);

        goToProjectHome();
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        assertElementPresent(Locator.linkWithText(FEATURE_SET_NAME));
        clickAndWait(Locator.linkWithText(protocolName));

        DataRegionTable resultTable = new DataRegionTable("Data", this);
        assertEquals(0, resultTable.getDataRowCount());

        goToSchemaBrowser();
        selectQuery("assay.ExpressionMatrix." + ASSAY_NAME, "FeatureDataBySample");
    }

    private String getParameterXml(String assayName, String protocolName, int featureSetId, boolean importValues)
    {
        return String.format("<?xml version='1.0' encoding='UTF-8'?>\n" +
                "<bioml>\n" +
                "  <note label='protocolName' type='input'>%s</note>\n" +
                "  <note label='assay name' type='input'>%s</note>\n" +
                "  <note label='assay run property, featureSet' type='input'>%d</note>\n" +
                "  <note label='assay run property, importValues' type='input'>%s</note>\n" +
                "</bioml>", assayName, protocolName, featureSetId, importValues);

    }

    @Nullable
    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + "Project";
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/microarray";
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
