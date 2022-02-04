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
	private Boolean verbose;

	@DataBoundConstructor
	public FileContentReplaceItemConfig() {
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
		return verbose == null ? Boolean.TRUE : verbose;
	}

	@DataBoundSetter
	public void setSearch(String search) {
		this.search = search;
	}

	@DataBoundSetter
	public void setReplace(String replace) {
		this.replace = replace;
	}

	@DataBoundSetter
	public void setMatchCount(int matchCount) {
		this.matchCount = matchCount;
	}

	@DataBoundSetter
	public void setVerbose(Boolean verbose) {
		this.verbose = verbose;
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