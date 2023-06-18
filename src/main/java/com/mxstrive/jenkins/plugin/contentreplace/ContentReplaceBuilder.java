package com.mxstrive.jenkins.plugin.contentreplace;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

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
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
			throws InterruptedException, IOException {
		FilePath workspace = build.getWorkspace();
		if (workspace == null) {
			listener.getLogger().println("workspace can't be null");
			return false;
		}
		this.perform(build, workspace, launcher, listener);
		Result result = build.getResult();
		return result == null || !result.equals(Result.FAILURE);
	}

	@Override
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
			throws InterruptedException, IOException {
		EnvVars envVars = new EnvVars(run.getEnvironment(listener));
		PrintStream log = listener.getLogger();
		log.println("content-replace start");
		boolean rc = true;
		for (FileContentReplaceConfig config : configs) {
			rc &= replaceFileContent(config, envVars, run, workspace, listener);
			if (!rc) {
				break;
			}
		}
		log.println("content-replace end");
		if (!rc) {
			run.setResult(Result.FAILURE);
		}
	}

	private boolean replaceFileContent(FileContentReplaceConfig config, EnvVars envVars, Run<?, ?> run,
			FilePath workspace, TaskListener listener) throws InterruptedException, IOException {
		String[] paths = config.getFilePath().split(",");
		boolean rc = true;
		for (String path : paths) {
			rc &= replaceFileContent(path, config, envVars, run, workspace, listener);
			if (!rc) {
				break;
			}
		}
		return rc;
	}

	private boolean replaceFileContent(String path, FileContentReplaceConfig config, EnvVars envVars, Run<?, ?> run,
			FilePath workspace, TaskListener listener) throws InterruptedException, IOException {
		if (workspace == null) {
			return false;
		}
		List<FilePath> filePaths = getTargetFiles(envVars.expand(path), workspace, listener);
		if (filePaths.isEmpty()) {
			return false;
		}
		boolean result = true;
		for (int i = 0, size = filePaths.size(); result && i < size; i++) {
			result = replaceFileContent(filePaths.get(i), config, envVars, workspace, listener);
		}
		return result;
	}

	private boolean replaceFileContent(FilePath filePath, FileContentReplaceConfig config, EnvVars envVars,
			FilePath workspace, TaskListener listener) throws InterruptedException, IOException {
		PrintStream log = listener.getLogger();
		InputStream is = filePath.read();
		List<String> lines = readLines(is, Charset.forName(config.getFileEncoding()));
		is.close();
		listener.getLogger().println(" > replace content of file: " + filePath);
		for (FileContentReplaceItemConfig cfg : config.getConfigs()) {
			String replace = envVars.expand(cfg.getReplace());
			if (!assertEnvVarsExpanded(replace, listener)) {
				return false;
			}
			Pattern pattern = Pattern.compile(cfg.getSearch(), Pattern.MULTILINE);
			List<Integer> matchedLineIndexs = new ArrayList<Integer>();
			for (int i = 0, size = lines.size(); i < size; i++) {
				Matcher matcher = pattern.matcher(lines.get(i));
				while (matcher.find()) {
					matchedLineIndexs.add(i);
				}
			}
			if (cfg.getMatchCount() != 0 && matchedLineIndexs.size() != cfg.getMatchCount()) {
				listener.getLogger().println("   > [" + cfg.getSearch() + "]" + " match count is "
						+ matchedLineIndexs.size() + " not equals " + cfg.getMatchCount() + "(in config)");
				return false;
			}
			for (Integer i : matchedLineIndexs) {
				String line = lines.get(i);
				String newLine = pattern.matcher(line).replaceFirst(replace);
				lines.set(i, newLine);
				if (cfg.isVerbose()) {
					log.println("   > replace : [" + line + "] => [" + newLine + "]");
				}
			}
			if (cfg.isVerbose()) {
				log.println("   > replace times: " + matchedLineIndexs.size() + ", [" + cfg.getSearch() + "] => ["
						+ replace + "]");
			}
		}
		String content = String.join(config.getLineSeparatorString(), lines);
		filePath.write(content, config.getFileEncoding());
		return true;
	}

	private List<String> readLines(InputStream is, Charset charset) throws IOException {
		List<String> ss = new ArrayList<>();
		InputStreamReader isr = new InputStreamReader(is, charset);
		StringBuilder sb = new StringBuilder();
		char cr = 0;
		while (isr.ready()) {
			cr = (char)isr.read();
			if (cr == '\r') {
				continue;
			} else if (cr == '\n') {
				ss.add(sb.toString());
				sb.delete(0, sb.length());
			} else {
				sb.append(cr);
			}
		}
		if (sb.length() > 0) {
			ss.add(sb.toString());
		} else {				
			ss.add("");
		}
		return ss;
	}

	private boolean assertEnvVarsExpanded(String replace, TaskListener listener) {
		List<String> evs = findUnexpandEnvVars(replace);
		if (!evs.isEmpty()) {
			listener.getLogger().println("   > can't find envVars: " + evs);
			return false;
		}
		return true;
	}

	private List<FilePath> getTargetFiles(String path, FilePath workspace, TaskListener listener)
			throws InterruptedException, IOException {
		List<FilePath> result = new ArrayList<FilePath>();
		Matcher matcher = Pattern.compile("(.*?)\\*(.*)").matcher(path);
		if (!matcher.matches()) {
			FilePath filePath = workspace.child(path);
			if (ensureFileExisted(filePath, listener)) {
				result.add(filePath);
			}
			return result;
		}
		String basePath = matcher.group(1);
		int idx = Math.max(basePath.lastIndexOf("\\"), basePath.lastIndexOf("/"));
		if (idx != -1) {
			basePath = basePath.substring(0, idx);
		} else {
			basePath = "";
		}
		FilePath[] matchedFilePaths = null;
		try {
			matchedFilePaths = workspace.child(basePath).list(path.substring(basePath.equals("") ? 0 : basePath.length() + 1));
		} catch (Exception e) {
			e.printStackTrace();
			listener.getLogger().println("   > " + path + " list file fail");
			return result;
		}
		for (int i = 0; i < matchedFilePaths.length; i++) {
			FilePath filePath = matchedFilePaths[i];
			if (ensureFileExisted(filePath, listener)) {
				result.add(filePath);
			}
		}
		return result;
	}

	private boolean ensureFileExisted(FilePath filePath, TaskListener listener)
			throws IOException, InterruptedException {
		if (!filePath.exists()) {
			listener.getLogger().println("   > " + filePath + " " + Messages.Message_errors_fileNotFound());
			return false;
		} else if (filePath.isDirectory()) {
			listener.getLogger().println("   > " + filePath + " " + Messages.Message_errors_isNotAFile());
			return false;
		}
		return true;
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
