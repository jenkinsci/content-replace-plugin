package com.mxstrive.jenkins.plugin.contentreplace;

import java.nio.charset.StandardCharsets;
import java.nio.charset.spi.CharsetProvider;
import java.util.Arrays;
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
import hudson.util.ListBoxModel;

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
		return lineSeparator;
	}

	public List<FileContentReplaceItemConfig> getConfigs() {
		return configs;
	}

	@DataBoundSetter
	public void setLineSeparator(String value) {
		this.lineSeparator = value;
	}

	public String getLineSeparatorString() {
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

	@Symbol("fileContentReplaceConfig")
	@Extension
	public static class DescriptorImpl extends Descriptor<FileContentReplaceConfig> {
		private static final String[] LINE_SEPARATORS = { "Unix", "Windows", "System" };
		private static final String[] FILE_ENCODINGS = { "UTF-8", "GBK", "ASCII", "ISO-8859-1", "UTF-16" };

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

		public ListBoxModel doFillLineSeparatorItems() {
			return fillListBoxModelOptions(new ListBoxModel(), LINE_SEPARATORS);
		}

		public FormValidation doCheckLineSeparator(@QueryParameter StaplerRequest req, @QueryParameter StaplerResponse rsp, @QueryParameter final String value) {
			return checkOptionValue(value, LINE_SEPARATORS, "line separator.");
		}

		public ListBoxModel doFillFileEncodingItems() {
			return fillListBoxModelOptions(new ListBoxModel(), FILE_ENCODINGS);
		}

		public FormValidation doCheckFileEncoding(@QueryParameter StaplerRequest req, @QueryParameter StaplerResponse rsp, @QueryParameter final String value) {
			return checkOptionValue(value, FILE_ENCODINGS, "file encoding");
		}

		private FormValidation checkOptionValue(final String value, String[] items, String name) {
			if(value.length() == 0) {
				return FormValidation.error(String.format("Please set %s.", name));
			} else if (!Arrays.asList(items).contains(value)) {
				return FormValidation.error(String.format("Please set a vaild %s.", name));
			}
			return FormValidation.ok();
		}

		private ListBoxModel fillListBoxModelOptions(ListBoxModel model, String[] items) {
			for (String e : items) {
				model.add(e, e);
			}
			return model;
		}
	}
}