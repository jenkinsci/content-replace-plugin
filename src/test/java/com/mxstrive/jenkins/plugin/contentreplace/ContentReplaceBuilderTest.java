package com.mxstrive.jenkins.plugin.contentreplace;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;

public class ContentReplaceBuilderTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private String fileEncoding = "UTF-8";
	private String content = "Version=0.0.0";
	private File file;
	private List<FileContentReplaceConfig> configs;

    @Before
    public void init() throws IOException {
    	file = new File(getClass().getResource(".").getPath() + "tmp.txt");
    	configs = new ArrayList<>();
    	List<FileContentReplaceItemConfig> cfgs = new ArrayList<>();
    	cfgs.add(new FileContentReplaceItemConfig("(Version=)\\d+.\\d+.\\d+", "$11.0.${BUILD_ID}", 0));
    	FileUtils.write(file, content, Charset.forName(fileEncoding));
    	configs.add(new FileContentReplaceConfig(file.getAbsolutePath(), fileEncoding, cfgs));
    }

    @After
    public void clean() throws IOException {
    	FileUtils.forceDelete(file);
    }

    @Test
    public void testBuild() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        ContentReplaceBuilder builder = new ContentReplaceBuilder(configs);
        project.getBuildersList().add(builder);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("   > replace times: 1, [(Version=)\\d+.\\d+.\\d+] => [$11.0." + build.getNumber() + "]", build);
        Assert.assertEquals(FileUtils.readFileToString(file, Charset.forName(fileEncoding)), "Version=1.0." + build.getNumber());
    }

}