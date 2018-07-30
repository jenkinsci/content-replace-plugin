package com.mxstrive.jenkins.plugin.contentreplace;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;

public class ContentReplaceBuilder extends Builder implements SimpleBuildStep {

	private List<FileContentReplaceConfig> configs;
	
	@DataBoundConstructor
	public ContentReplaceBuilder(List<FileContentReplaceConfig> configs) {
		this.configs = configs;
	}

    @DataBoundSetter
    public void setConfigs(List<FileContentReplaceConfig> configs) {
        this.configs = configs;
    }
    
	public List<FileContentReplaceConfig> getConfigs() {
		return configs;
	}
    
	@Override
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
			throws InterruptedException, IOException {
		EnvVars envVars = new EnvVars(run.getEnvironment(listener));
		for (FileContentReplaceConfig config : configs) {
			replaceFileContent(config, envVars, run, workspace, listener);
		}
	}

	private void replaceFileContent(FileContentReplaceConfig config, EnvVars envVars, Run<?, ?> run, FilePath workspace, TaskListener listener) throws InterruptedException, IOException {
		PrintStream log = listener.getLogger();
		FilePath filePath = workspace.child(config.getFilePath());
		if (!filePath.exists()) {
			log.println(Messages.Message_errors_fileNotFound());
			run.setResult(Result.FAILURE);
			return;
		}
		if (filePath.isDirectory()) {
			log.println(config.getFilePath() + " " + Messages.Message_errors_isNotAFile());
			run.setResult(Result.FAILURE);
			return;
		}
		File file = new File(filePath.toURI());
		String content = FileUtils.readFileToString(file);
		listener.getLogger().println("replace file content: " + config.getFilePath());
		for (FileContentReplaceItemConfig cfg : config.getConfigs()) {
			String replace = envVars.expand(cfg.getReplace());
			content = content.replaceAll(cfg.getSearch(), replace);
			log.println(cfg.getSearch() + " => " + replace);
		}
		FileUtils.write(file, content);
	}
		
	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
		public DescriptorImpl() {
			super(ContentReplaceBuilder.class);
			load();
		}

		@SuppressWarnings("rawtypes")
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return Messages.ContentReplaceBuilder_DescriptorImpl_DisplayName();
		}
	}

}
