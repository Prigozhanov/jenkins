/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Erik Ramfelt, Xavier Le Vourch, Yahoo!, Inc.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.tasks.junit;

import java.io.File;
import java.util.List;
import java.net.URISyntaxException;

import junit.framework.TestCase;

import hudson.XmlFile;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * Test cases for parsing JUnit report XML files.
 * As there are no XML schema for JUnit xml files, Hudson needs to handle
 * varied xml files.
 * 
 * @author Erik Ramfelt
 */
public class SuiteResultTest extends TestCase {

    private File getDataFile(String name) throws URISyntaxException {
        return new File(SuiteResultTest.class.getResource(name).toURI());
    }

    private SuiteResult parseOne(File file) throws Exception {
        List<SuiteResult> results = SuiteResult.parse(file, false);
        assertEquals(1,results.size());
        return results.get(0);
    }

    /**
     * Verifying fix for issue #1233.
     * https://hudson.dev.java.net/issues/show_bug.cgi?id=1233
     */
    public void testIssue1233() throws Exception {
        SuiteResult result = parseOne(getDataFile("junit-report-1233.xml"));
        
        List<CaseResult> cases = result.getCases();
        assertEquals("Class name is incorrect", "test.foo.bar.DefaultIntegrationTest", cases.get(0).getClassName());
        assertEquals("Class name is incorrect", "test.foo.bar.BundleResolverIntegrationTest", cases.get(1).getClassName());
        assertEquals("Class name is incorrect", "test.foo.bar.BundleResolverIntegrationTest", cases.get(2).getClassName());
        assertEquals("Class name is incorrect", "test.foo.bar.ProjectSettingsTest", cases.get(3).getClassName());
        assertEquals("Class name is incorrect", "test.foo.bar.ProjectSettingsTest", cases.get(4).getClassName());
    }
    /**
     * Verifying fix for issue #1463.
     * JUnit report file is generated by SoapUI Pro 1.7.6
     * https://hudson.dev.java.net/issues/show_bug.cgi?id=1463
     */
    public void testIssue1463() throws Exception {
        SuiteResult result = parseOne(getDataFile("junit-report-1463.xml"));

        List<CaseResult> cases = result.getCases();
        for (CaseResult caseResult : cases) {
            assertEquals("Test class name is incorrect in " + caseResult.getDisplayName(), "WLI-FI-Tests-Fake", caseResult.getClassName());            
        }
        assertEquals("Test name is incorrect", "IF_importTradeConfirmationToDwh", cases.get(0).getName());
        assertEquals("Test name is incorrect", "IF_getAmartaDisbursements", cases.get(1).getName());
        assertEquals("Test name is incorrect", "IF_importGLReconDataToDwh", cases.get(2).getName());
        assertEquals("Test name is incorrect", "IF_importTradeInstructionsToDwh", cases.get(3).getName());
        assertEquals("Test name is incorrect", "IF_getDeviationTradeInstructions", cases.get(4).getName());
        assertEquals("Test name is incorrect", "IF_getDwhGLData", cases.get(5).getName());
    }

    /**
     * Verifying fix for issue #1472.
     * JUnit report produced by TAP (Test Anything Protocol)
     * https://hudson.dev.java.net/issues/show_bug.cgi?id=1472
     */
    public void testIssue1472() throws Exception {
        List<SuiteResult> results = SuiteResult.parse(getDataFile("junit-report-1472.xml"), false);
        assertTrue(results.size()>20); // lots of data here

        SuiteResult sr0 = results.get(0);
        SuiteResult sr1 = results.get(1);
        assertEquals("make_test.t_basic_lint_t",sr0.getName());
        assertEquals("make_test.t_basic_meta_t",sr1.getName());
        assertTrue(!sr0.getStdout().equals(sr1.getStdout()));
    }

    /**
     * Verifying fix for issue #2874.
     * https://hudson.dev.java.net/issues/show_bug.cgi?id=2874
     */
    public void testIssue2874() throws Exception {
        SuiteResult result = parseOne(getDataFile("junit-report-2874.xml"));

        assertEquals("test suite name", "DummyTest", result.getName());
    }

    public void testErrorDetails() throws Exception {
        SuiteResult result = parseOne(getDataFile("junit-report-errror-details.xml"));

        List<CaseResult> cases = result.getCases();
        for (CaseResult caseResult : cases) {
            assertEquals("Test class name is incorrect in " + caseResult.getDisplayName(), "some.package.somewhere.WhooHoo", caseResult.getClassName());
        }
        assertEquals("this normally has the string like, expected mullet, but got bream", cases.get(0).getErrorDetails());
    }

    public void testSuiteResultPersistence() throws Exception {
        SuiteResult source = parseOne(getDataFile("junit-report-1233.xml"));

        File dest = File.createTempFile("testSuiteResultPersistence", ".xml");
        try {
            XmlFile xmlFile = new XmlFile(dest);
            xmlFile.write(source);

            SuiteResult result = (SuiteResult)xmlFile.read();
            assertNotNull(result);

            assertEquals(source.getName(), result.getName());
            assertEquals(source.getTimestamp(), result.getTimestamp());
            assertEquals(source.getDuration(), result.getDuration());
            assertEquals(source.getStderr(), result.getStderr());
            assertEquals(source.getStdout(), result.getStdout());
            assertEquals(source.getCases().size(), result.getCases().size());
            assertNotNull(result.getCase("testGetBundle"));
        } finally {
            dest.delete();
}
    }

    //@Bug(6516)
    public void testSuiteStdioTrimming() throws Exception {
        File data = File.createTempFile("testSuiteStdioTrimming", ".xml");
        try {
            Writer w = new FileWriter(data);
            try {
                PrintWriter pw = new PrintWriter(w);
                pw.println("<testsuites name='x'>");
                pw.println("<testsuite failures='0' errors='0' tests='1' name='x'>");
                pw.println("<testcase name='x' classname='x'/>");
                pw.println("<system-out/>");
                pw.print("<system-err><![CDATA[");
                pw.println("First line is intact.");
                for (int i = 0; i < 100; i++) {
                    pw.println("Line #" + i + " might be elided.");
                }
                pw.println("Last line is intact.");
                pw.println("]]></system-err>");
                pw.println("</testsuite>");
                pw.println("</testsuites>");
                pw.flush();
            } finally {
                w.close();
            }
            SuiteResult sr = parseOne(data);
            assertEquals(sr.getStderr(), 1028, sr.getStderr().length());
        } finally {
            data.delete();
        }
    }

}
