package com.mxstrive.jenkins.plugin.contentreplace;

import java.util.List;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;

public class FileContentReplaceConfig extends AbstractDescribableImpl<FileContentReplaceConfig> {

	private String filePath;
	private String fileEncoding = "UTF-8";
	private String lineSeparator;
	private List<FileContentReplaceItemConfig> configs;

	@DataBoundConstructor
	public FileContentReplaceConfig(String filePath, String fileEncoding, List<FileContentReplaceItemConfig> configs) {
		this.filePath = filePath.trim();
		this.fileEncoding = fileEncoding;
		this.configs = configs;
	}

	public String getFilePath() {
		return filePath;
	}

	public String getFileEncoding() {
		return fileEncoding;
	}

	public String getLineSeparator() {
		String value = null;
		if ("Windows".equals(lineSeparator)) {
			value = "\r\n";
		} else if ("Unix".equals(lineSeparator)) {
			value = "\n";
		} else {
			value = System.getProperty("line.separator");
		}
		return value;
	}

	public List<FileContentReplaceItemConfig> getConfigs() {
		return configs;
	}

	@DataBoundSetter
	public void setLineSeparator(String value) {
		this.lineSeparator = value;
	}

	@Symbol("fileContentReplaceConfig")
	@Extension
	public static class DescriptorImpl extends Descriptor<FileContentReplaceConfig> {

		@Override
		public String getDisplayName() {
			return "";
		}
		
		public FormValidation doCheckFilePath(@QueryParameter StaplerRequest req, @QueryParameter StaplerResponse rsp, @QueryParameter final String value) {
			if(value.length() == 0) {
                return FormValidation.error("Please set a file path.");
			}
			return FormValidation.ok();
        }

	}
}