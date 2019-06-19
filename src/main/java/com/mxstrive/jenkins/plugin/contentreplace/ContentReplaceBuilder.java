package com.mxstrive.jenkins.plugin.contentreplace;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
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
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
		EnvVars envVars = new EnvVars(run.getEnvironment(listener));
		for (FileContentReplaceConfig config : configs) {
			replaceFileContent(config, envVars, run, workspace, listener);
		}
	}

	private void replaceFileContent(FileContentReplaceConfig config, EnvVars envVars, Run<?, ?> run, FilePath workspace, TaskListener listener) throws InterruptedException, IOException {
		String[] paths = config.getFilePath().split(",");
		for (String path : paths) {
			replaceFileContent(path, config, envVars, run, workspace, listener);
		}
	}
	
	private void replaceFileContent(String path, FileContentReplaceConfig config, EnvVars envVars, Run<?, ?> run, FilePath workspace, TaskListener listener) throws InterruptedException, IOException {
		PrintStream log = listener.getLogger();
		Path filePath = ensureFileExisted(envVars.expand(path), run, workspace, listener);
		if (filePath == null) {
			return;
		}

		listener.getLogger().println("replace content of file: " + filePath);

		Stream<String> lines = Files.lines(filePath);
		String content = String.join("/n", lines.collect(Collectors.toList()));

		for (FileContentReplaceItemConfig cfg : config.getConfigs()) {
			String replace = envVars.expand(cfg.getReplace());
			if (!assertEnvVarsExpanded(replace, run, listener)) {
				return;
			}

			Stream<String> countLines = Files.lines(filePath);
			Matcher matcher = Pattern.compile(cfg.getSearch()).matcher(String.join("\n", countLines.collect(Collectors.toList())));
			int count = matcher.groupCount();
			countLines.close();
			if (cfg.getMatchCount() != 0 && count != cfg.getMatchCount()) {
				listener.getLogger().println("[" + cfg.getSearch() + "]"+ " match count is " + count + " not equals " + cfg.getMatchCount() + "(in config)");
				run.setResult(Result.FAILURE);
				return;
			}

			content = matcher.replaceAll(replace);
			log.println("replace times: " + count + ", [" + cfg.getSearch() + "] => [" + replace + "]");
		}

		Files.write(filePath, Collections.singleton(content));
		lines.close();

	}

	private boolean assertEnvVarsExpanded(String replace, Run<?, ?> run, TaskListener listener) {
		List<String> evs = findUnexpandEnvVars(replace);
		if (!evs.isEmpty()) {
			listener.getLogger().println("can't find envVars: " + evs);
			run.setResult(Result.FAILURE);
			return false;
		}
		return true;
	}
	
	private Path ensureFileExisted(String path, Run<?, ?> run, FilePath workspace, TaskListener listener) throws InterruptedException, IOException {

		Path filePath = Paths.get(workspace.child(path).getRemote());
		if (filePath == null || filePath.toFile() == null) {
			listener.getLogger().println(path + " " + Messages.Message_errors_fileNotFound());
			run.setResult(Result.FAILURE);
			return null;
		}
		if(filePath.toFile().isDirectory()) {
			listener.getLogger().println(path + " " + Messages.Message_errors_isNotAFile());
			run.setResult(Result.FAILURE);
			return null;
		}
		return filePath;
	}
	
	private List<String> findUnexpandEnvVars(String src) {
		List<String> evs = new ArrayList<>();
		Matcher matcher = Pattern.compile("\\$\\{(.+?)\\}").matcher(src);
		while (matcher.find()) {
			evs.add(matcher.group(1));
		}
		return evs;
	}
	
	@Symbol("contentReplace")
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
