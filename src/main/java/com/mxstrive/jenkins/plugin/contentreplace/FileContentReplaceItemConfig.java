package com.mxstrive.jenkins.plugin.contentreplace;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;

public class FileContentReplaceItemConfig extends AbstractDescribableImpl<FileContentReplaceItemConfig> {

	private String search;
	private String replace;
	private int matchCount;
	private boolean verbose;

	@DataBoundConstructor
	public FileContentReplaceItemConfig(String search, String replace, int matchCount) {
		this.search = StringUtils.strip(search);
		this.replace = StringUtils.strip(replace);
		this.matchCount = matchCount;
	}

	@DataBoundSetter
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public String getSearch() {
		return search;
	}

	public String getReplace() {
		return replace;
	}
    
	public int getMatchCount() {
		return matchCount;
	}

	public boolean isVerbose() {
		return verbose;
	}

	@Symbol("fileContentReplaceItemConfig")
	@Extension
	public static class DescriptorImpl extends Descriptor<FileContentReplaceItemConfig> {

		@Override
		public String getDisplayName() {
			return "";
		}

		public FormValidation doCheckSearch(@QueryParameter final String value) {
			if(value.length()==0)
                return FormValidation.error("Please set a regex expression.");
			try {
				Pattern.compile(value);	
			} catch (PatternSyntaxException e) {
				return FormValidation.error("Syntax error: " + e.getMessage());
			}
			return FormValidation.ok();
        }
	}
}