package com.mxstrive.jenkins.plugin.contentreplace;

import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;

public class FileContentReplaceConfig extends AbstractDescribableImpl<FileContentReplaceConfig> {

	private String filePath;
	private List<FileContentReplaceItemConfig> configs;

	@DataBoundConstructor
	public FileContentReplaceConfig(String filePath, List<FileContentReplaceItemConfig> configs) {
		this.filePath = filePath;
		this.configs = configs;
	}

	public String getFilePath() {
		return filePath;
	}

	public List<FileContentReplaceItemConfig> getConfigs() {
		return configs;
	}

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