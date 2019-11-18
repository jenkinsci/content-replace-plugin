package com.mxstrive.jenkins.plugin.contentreplace;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class ContentReplaceBuilder extends Builder {

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
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
			throws InterruptedException, IOException {
		EnvVars envVars = new EnvVars(build.getEnvironment(listener));
		boolean rc = true;
		for (FileContentReplaceConfig config : configs) {
			rc &= replaceFileContent(config, envVars, build, listener);
			if (!rc) {
				break;
			}
		}
		build.setResult(rc ? Result.SUCCESS : Result.FAILURE);
		return rc;
	}

	private boolean replaceFileContent(FileContentReplaceConfig config, EnvVars envVars, AbstractBuild<?, ?> build,
			TaskListener listener) throws InterruptedException, IOException {
		String[] paths = config.getFilePath().split(",");
		boolean rc = true;
		for (String path : paths) {
			rc &= replaceFileContent(path, config, envVars, build, listener);
			if (!rc) {
				break;
			}
		}
		return rc;
	}

	private boolean replaceFileContent(String path, FileContentReplaceConfig config, EnvVars envVars,
			AbstractBuild<?, ?> build, TaskListener listener) throws InterruptedException, IOException {
		PrintStream log = listener.getLogger();
		FilePath filePath = ensureFileExisted(envVars.expand(path), build.getWorkspace(), listener);
		if (filePath == null) {
			return false;
		}
		InputStream is = filePath.read();
		List<String> lines = IOUtils.readLines(is, Charset.forName(config.getFileEncoding()));
		is.close();
		String content = StringUtils.join(lines, IOUtils.LINE_SEPARATOR);
		listener.getLogger().println("replace content of file: " + filePath);
		for (FileContentReplaceItemConfig cfg : config.getConfigs()) {
			String replace = envVars.expand(cfg.getReplace());
			if (!assertEnvVarsExpanded(replace, listener)) {
				return false;
			}
			Pattern pattern = Pattern.compile(cfg.getSearch());
			int matchCount = 0;
			for (int i = 0, size = lines.size(); i < size; i++) {
				Matcher matcher = pattern.matcher(lines.get(i));
				while (matcher.find()) {
					++matchCount;
				}
			}
			if (cfg.getMatchCount() != 0 && matchCount != cfg.getMatchCount()) {
				listener.getLogger().println("[" + cfg.getSearch() + "]" + " match count is " + matchCount
						+ " not equals " + cfg.getMatchCount() + "(in config)");
				return false;
			}
			log.println("replace times: " + matchCount + ", [" + cfg.getSearch() + "] => [" + replace + "]");
			content = pattern.matcher(content).replaceAll(replace);
		}
		filePath.write(content, config.getFileEncoding());
		return true;
	}

	private boolean assertEnvVarsExpanded(String replace, TaskListener listener) {
		List<String> evs = findUnexpandEnvVars(replace);
		if (!evs.isEmpty()) {
			listener.getLogger().println("can't find envVars: " + evs);
			return false;
		}
		return true;
	}

	private FilePath ensureFileExisted(String path, FilePath workspace, TaskListener listener)
			throws InterruptedException, IOException {
		FilePath filePath = workspace.child(path);
		if (!filePath.exists()) {
			listener.getLogger().println(path + " " + Messages.Message_errors_fileNotFound());
			return null;
		} else if (filePath.isDirectory()) {
			listener.getLogger().println(path + " " + Messages.Message_errors_isNotAFile());
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
