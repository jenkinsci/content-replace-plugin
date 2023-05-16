package com.mxstrive.jenkins.plugin.contentreplace;

import hudson.FilePath;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static com.mxstrive.jenkins.plugin.contentreplace.ContentReplaceBuilderUtil.hasTrailingNewline;

public class ContentReplaceBuilderUtilTest {
    @Test
    public void testHasTrailingNewline() throws IOException {
        FilePath filePath1 = new FilePath(new File("src/test/resources/testTrailingNewlineTrue.txt"));
        Assert.assertTrue(hasTrailingNewline(filePath1));
        FilePath filePath2 = new FilePath(new File("src/test/resources/testTrailingNewlineFalse.txt"));
        Assert.assertFalse(hasTrailingNewline(filePath2));
    }

}
