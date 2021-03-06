package com.sandboni.core.engine.render;

import com.sandboni.core.engine.exception.RendererException;
import com.sandboni.core.engine.render.file.FileOptions;
import com.sandboni.core.engine.render.file.FileType;
import com.sandboni.core.engine.render.file.csv.CSVFileStrategy;
import com.sandboni.core.engine.render.file.csv.RelatedTestsFileRenderer;
import com.sandboni.core.engine.sta.graph.vertex.TestVertex;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;

import static com.sandboni.core.engine.render.file.csv.RelatedTestsFileRenderer.IS_DISCONNECTED_TESTS;

public class PresentationStrategyTest {

    protected static Set<TestVertex> tests = new HashSet<>();

    @BeforeClass
    public static void init() {
        Set<Map<String, String>> set = new HashSet<>();
        set.add(genereteMap("com.sandboni.core.engine.result.ResultGeneratorTest", "testResultWhenEmptyContextAndRunSelectiveModeIsTrue()"));
        set.add(genereteMap("com.sandboni.core.engine.ProcessorTest", "testGetUnreachableChanges()"));
        set.add(genereteMap("com.sandboni.core.engine.result.ResultGeneratorTest", "testResultWhenOnlyJavaContext()"));
        set.add(genereteMap("com.sandboni.core.engine.result.ResultGeneratorTest", "testResultWhenOnlyJavaContextAndRunSelectiveModeIsTrue()"));
        set.add(genereteMap("com.sandboni.core.engine.result.ResultGeneratorTest", "testResultWhenEmptyContext()"));
        set.add(genereteMap("com.sandboni.core.engine.result.ResultGeneratorTest", "testResultWhenBothBuildCnfgAndJavaContext()"));
        set.add(genereteMap("com.sandboni.core.engine.result.ResultGeneratorTest", "testResultWhenBothBuildCnfgAndJavaContextAndRunSelectiveModeIsTrue()"));
        set.add(genereteMap("com.sandboni.core.engine.result.ResultGeneratorTest", "testResultWhenOnlyCnfgContextAndRunSelectiveModeIsTrue()"));
        set.add(genereteMap("com.sandboni.core.engine.result.ResultGeneratorTest", "testResultWhenOnlyCnfgContext()"));
        set.add(genereteMap("com.sandboni.core.engine.ProcessorTest", "testGetChanges()"));
        set.add(genereteMap("com.sandboni.core.engine.finder.bcel.BcelFinderTest", "testPoCDiffChangeDetector()"));
        set.add(genereteMap("com.sandboni.core.engine.ProcessorTest", "testFileRelatedTests()"));
        set.add(genereteMap("com.sandboni.core.engine.ProcessorTest", "testGetDisconnectedVertices()"));
        set.add(genereteMap("com.sandboni.core.engine.ProcessorTest", "testGetAllTestsCount()"));
        set.add(genereteMap("com.sandboni.core.engine.ProcessorTest", "testGetDisconnectedEntryPoints()"));
        set.add(genereteMap("com.sandboni.core.engine.ProcessorTest", "testGetRelatedTests()"));
        set.add(genereteMap("Receive management", " "));

        for (Map<String, String> test : set) {
            TestVertex v = new TestVertex.Builder(test.get("actor"), test.get("action"), null).build();
            tests.add(v);
        }
    }

    private static Map<String, String> genereteMap(
            String actor, String action) {
        Map<String, String> m = new HashMap<>();
        m.put("actor", actor);
        m.put("action", action);
        m.put("isSpecial", "false");
        m.put("filePath", null);
        m.put("filter", null);
        m.put("ignore", "false");
        m.put("externalLocation", "false");
        m.put("lineNumbersEmpty", "true");
        m.put("special", "false");
        return m;
    }

    @Test
    public void testPresentRelatedTests()  {
        FileOptions fileOptions = new FileOptions.FileOptionsBuilder()
                .with(fo -> {
                    fo.name = "test.csv";
                    fo.type = FileType.CSV;
                    fo.attributes = "{"+IS_DISCONNECTED_TESTS +" :true}"; })
                .build();
        String result = new RelatedTestsFileRenderer().renderBody(tests, fileOptions);
        Assert.assertEquals(17 + 1/*for header*/, result.split("\n").length);
        Assert.assertEquals(5, result.split("\n")[17].split(",").length);
    }

    @Test
    public void testIsNotConnected()  {
        FileOptions fileOptions = new FileOptions.FileOptionsBuilder()
                .with(fo -> {
                    fo.name = "test.csv";
                    fo.type = FileType.CSV;
                    fo.attributes = "{"+IS_DISCONNECTED_TESTS +" :true}"; })
                .build();
        String result = new RelatedTestsFileRenderer().renderBody(tests, fileOptions);
        Assert.assertEquals("N", result.split("\n")[17].split(",")[4]);
    }

    @Test
    public void testIstConnected()  {
        FileOptions fileOptions = new FileOptions.FileOptionsBuilder()
                .with(fo -> {
                    fo.name = "test.csv";
                    fo.type = FileType.CSV;
                    fo.attributes = "{"+IS_DISCONNECTED_TESTS +" :false}"; })
                .build();
        String result = new RelatedTestsFileRenderer().renderBody(tests, fileOptions);
        Assert.assertEquals("Y", result.split("\n")[17].split(",")[4]);
    }

    @Test
    public void testIstConnectedNoAttr()  {
        FileOptions fileOptions = new FileOptions.FileOptionsBuilder()
                .with(fo -> {
                    fo.name = "test.csv";
                    fo.type = FileType.CSV; })
                .build();
        String result = new RelatedTestsFileRenderer().renderBody(tests, fileOptions);
        Assert.assertEquals("Y", result.split("\n")[17].split(",")[4]);
    }

    @Test
    public void testPresentRelatedAndDisconnectedTests() throws RendererException {

        String header = new RelatedTestsFileRenderer().renderHeader();
        Assert.assertEquals("type,package,class,method,connected?\n", header);

        String result = new CSVFileStrategy<>(tests, new RelatedTestsFileRenderer(),
                new FileOptions.FileOptionsBuilder()
                        .with(fo -> {
                            fo.name = "test.csv";
                            fo.type = FileType.CSV;
                            fo.attributes = "{key1:val1, key2:val2}"; })
                        .build()).render();
        String[] lines = result.split("\n");
        Assert.assertTrue(lines.length > 1);

        Assert.assertEquals(18, lines.length);
        int relatedCount = (int) Arrays.stream(lines).filter(r -> r.endsWith("Y")).count();
        Assert.assertEquals(17, relatedCount);
        int disconnectedCount = (int) Arrays.stream(lines).filter(r -> r.endsWith("N")).count();
        Assert.assertEquals(0, disconnectedCount);

        boolean isHeader = true;
        for (String line : lines) {
            if (isHeader){
                isHeader = false;
                continue;
            }
            String[] columns = line.split(",");
            Assert.assertEquals(5, columns.length);
            Assert.assertTrue("Y".equals(columns[4].trim()) || "N".equals(columns[4].trim()));
        }

        int integrations = (int) Arrays.stream(lines).filter(l -> l.startsWith("INTEGRATION,")).count();
        int units = (int) Arrays.stream(lines).filter(l -> l.startsWith("UNIT,")).count();
        Assert.assertEquals(1, integrations);
        Assert.assertEquals(16, units);
    }
}
