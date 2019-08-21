/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2019 Compuware Corporation
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions: The above copyright notice and this permission notice
 * shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.compuware.jenkins.totaltest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import com.google.common.base.Strings;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.util.ArgumentListBuilder;

public class TotalTestCTRunner
{
	private  static final String TTT_MINIMUM_CLI_VERSION = "19.5.1";
	private static final String TOTAL_TEST_CLI_BAT = "TotalTestFTCLI.bat";
	private static final String TOTAL_TEST_CLI_SH = "TotalTestFTCLI.sh";
	private static final String TOTAL_TEST_WEBAPP = "totaltestapi";

	private final TotalTestCTBuilder tttBuilder;

	private Properties remoteProperties;
	private String remoteFileSeparator;
	private VirtualChannel vChannel;
	private TaskListener listener;
	private FilePath workspaceFilePath;
	private Run<?, ?> build;

	public TotalTestCTRunner(TotalTestCTBuilder tttBuilder)
	{
		this.tttBuilder = tttBuilder;
	}

	public boolean run(final Run<?, ?> build, final Launcher launcher, final FilePath workspaceFilePath,
			final TaskListener listener) throws IOException, InterruptedException
	{
		// initialization
		ArgumentListBuilder args = new ArgumentListBuilder();
		EnvVars env = build.getEnvironment(listener);
		vChannel = launcher.getChannel();
		if (vChannel == null)
		{
			listener.getLogger().println("Error: No channel could be retrieved");
			return false;
		}
		this.listener = listener;
		this.workspaceFilePath = workspaceFilePath;
		this.build = build;
		remoteProperties = vChannel.call(new RemoteSystemProperties());
		remoteFileSeparator = remoteProperties.getProperty("file.separator");

		boolean isLinux = launcher.isUnix();
		String osScriptFile = isLinux ? TOTAL_TEST_CLI_SH : TOTAL_TEST_CLI_BAT;

		TotalTestRunnerUtils.logJenkinsAndPluginVersion(listener);

		FilePath cliScriptPath = TotalTestRunnerUtils.getCLIScriptPath(launcher, listener, remoteFileSeparator, osScriptFile, TTT_MINIMUM_CLI_VERSION);
		args.add(cliScriptPath.getRemote());
		addArguments(args, listener);

		FilePath fRootPath = new FilePath(new File(TotalTestRunnerUtils.getTopaWorkbenchCLIPath(launcher)));
		FilePath workDir = new FilePath(vChannel, fRootPath.getRemote());
		workDir.mkdirs();

		listener.getLogger().println("----------------------------------");
		listener.getLogger().println("Now executing Total Test Testing CLI and printing out the execution log...");
		listener.getLogger().println("----------------------------------\n\n");

		int exitValue = launcher.launch().cmds(args).envs(env).stdout(listener.getLogger()).pwd(workDir).join();
		listener.getLogger().println(osScriptFile + " exited with exit value = " + exitValue);

		if (exitValue == 0)
		{
			listener.getLogger().println("\n\n----------------------------------");
			listener.getLogger().println("Total Test Testing CLI finished executing, now analysing the result...");
			listener.getLogger().println("----------------------------------\n\n");
			exitValue = readTestResult(launcher);

			if (exitValue != 0)
			{
				boolean isStopIfTestFailsOrThresholdReached = tttBuilder.getStopIfTestFailsOrThresholdReached();

				if (!isStopIfTestFailsOrThresholdReached)
				{
					listener.getLogger()
							.println("Test result failed but build continues (isStopIfTestFailsOrThresholdReached is false)");
					exitValue = 0;
				}
			}
		}
		else
		{
			listener.getLogger().println(
					"Something went wrong when executing the Total Test Testing CLI, and therefore there is no test results to analyze");
		}

		return exitValue == 0;
	}

	private int readTestResult(final Launcher launcher) throws IOException, InterruptedException
	{
		int result = 0;
		FilePath testSuiteResultPath = getRemoteFilePath(launcher, listener, remoteFileSeparator, "generated.cli.xasuiteres");
		listener.getLogger().println("Reading suite result from file: " + testSuiteResultPath.getRemote());
		String content = new String(Files.readAllBytes(Paths.get(testSuiteResultPath.getRemote())), "UTF-8");

		listener.getLogger().println("Result content:");
		listener.getLogger().println(content);

		String xaSuiteResult = null;

		try
		{
			Document document = getXaSuiteResultAsDocument(content);
			xaSuiteResult = getXaSuiteResult(document);
			listener.getLogger().println("Result state from suite: " + xaSuiteResult);

			if (!xaSuiteResult.equalsIgnoreCase("SUCCESS"))
			{
				result = -1;
			}

			if (result != -1 && tttBuilder.getCcThreshhold() > 0)
			{
				listener.getLogger().println(
						"The suite executed successfully, now checking that code coverage level is higher than the treshhold on "
								+ tttBuilder.getCcThreshhold() + " %");
				boolean isCCThresholdOk = getXaSuiteCodeCoverage(document);
				if (!isCCThresholdOk)
				{
					listener.getLogger().println("Code coverage treshhold not reached");
					result = -1;
				}
			}
		}
		catch (Exception e)
		{
			listener.getLogger().println("Exception in parsing XaSuiteResult. " + e.getMessage());
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			listener.getLogger().println(sw.toString());
		}
		return result;
	}

	private Document getXaSuiteResultAsDocument(String xml) throws Exception
	{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Reader r = new StringReader(xml);
		Document document = db.parse(new InputSource(r));

		return document;
	}

	private String getXaSuiteResult(Document document) throws Exception
	{
		String resultType = null;

		XPathFactory xpf = XPathFactory.newInstance();
		XPath xpath = xpf.newXPath();
		Element xaSuiteResultElement = (Element) xpath.evaluate("/XaSuiteResult", document, XPathConstants.NODE);
		resultType = xaSuiteResultElement.getAttribute("resultType");

		return resultType;
	}

	private boolean getXaSuiteCodeCoverage(Document document) throws Exception
	{
		boolean isCCThresholdOk = true;

		XPathFactory xpf = XPathFactory.newInstance();
		XPath xpath = xpf.newXPath();
		Element percentageElement = (Element) xpath.evaluate("/XaSuiteResult/CC/data", document, XPathConstants.NODE);

		if (percentageElement != null)
		{
			String sPercentage = percentageElement.getAttribute("percentage");

			int percentage = Integer.parseInt(sPercentage);

			if (percentage < tttBuilder.getCcThreshhold())
			{
				listener.getLogger().println("XaUnitResult percentage on " + sPercentage
						+ " is less than Code Coverage threshold on " + tttBuilder.getCcThreshhold() + ". Aborting build.");
				isCCThresholdOk = false;
			}

			if (isCCThresholdOk)
			{
				listener.getLogger().println("XaUnitResults Code Coverage threshold is " + tttBuilder.getCcThreshhold()
						+ " which is below the result on " + sPercentage);
			}
		}

		return isCCThresholdOk;
	}
	
	private void addArguments(final ArgumentListBuilder args, final TaskListener listener)
	{
		args.add("-e").add(tttBuilder.getEnvironmentId(), false);

		String tttServerUrl = tttBuilder.getServerUrl();

		if (!tttServerUrl.endsWith("/"))
		{
			tttServerUrl += "/";
		}

		tttServerUrl += TOTAL_TEST_WEBAPP + "/";
		listener.getLogger().println("Set the repository URL : " + tttServerUrl);

		args.add("-s").add(tttServerUrl, false);
		args.add("-u").add(
				TotalTestRunnerUtils.getLoginInformation(build.getParent(), tttBuilder.getCredentialsId()).getUsername(),
				false);
		args.add("-p").add(
				TotalTestRunnerUtils.getLoginInformation(build.getParent(), tttBuilder.getCredentialsId()).getPassword(), true);

		String folder = tttBuilder.getFolderPath();
		if (folder == null || folder.isEmpty() || folder.trim().isEmpty())
		{
			folder = ".";
		}

		listener.getLogger().println("The folder path: " + folder);
		args.add("-f").add(folder, false);

		String rootFolder = workspaceFilePath.getRemote();

		if (!".".equals(folder))
		{
			File fFolderPath = new File(tttBuilder.getFolderPath());

			if (!fFolderPath.isAbsolute())
			{
				args.add("-r").add(rootFolder);
				listener.getLogger().println("Set the root folder : " + rootFolder);
			}
		}
		else
		{
			args.add("-r").add(workspaceFilePath.getRemote());
			listener.getLogger().println("Set the root folder : " + rootFolder);
		}

		if (tttBuilder.getRecursive())
		{
			args.add("-R");
		}

		if (tttBuilder.getUploadToServer())
		{
			args.add("-x");
		}

		if (tttBuilder.getHaltAtFailure())
		{
			args.add("-h");
		}

		if (!Strings.isNullOrEmpty(tttBuilder.getSourceFolder()) && !tttBuilder.getSourceFolder().equalsIgnoreCase("COBOL"))
		{
			args.add("-S").add(tttBuilder.getSourceFolder());
		}

		if (!Strings.isNullOrEmpty(tttBuilder.getReportFolder()))
		{
			args.add("-g").add(tttBuilder.getReportFolder());
			args.add("-G");
		}

		if (!Strings.isNullOrEmpty(tttBuilder.getSonarVersion()))
		{
			args.add("-v").add(tttBuilder.getSonarVersion());
		}

		if (!Strings.isNullOrEmpty(tttBuilder.getAccountInfo()))
		{
			args.add("-a").add(tttBuilder.getAccountInfo());
		}

	}

	/**
	 * Returns the path to the remote file
	 * 
	 * @param launcher
	 *            The machine that the files will be checked out.
	 * 
	 * @return An instance of <code>FilePath</code> for the CLI directory
	 * 
	 * @throws IOException
	 *             If the CLI directory does not exist.
	 * @throws InterruptedException
	 *             If unable to get CLI directory.
	 */
	private FilePath getRemoteFilePath(final Launcher launcher, final TaskListener listener, String remoteFileSeparator,
			String osFile) throws IOException, InterruptedException
	{
		FilePath remoteFile = null;
		FilePath workDir = new FilePath(vChannel, workspaceFilePath.getRemote());

		String ffolder = tttBuilder.getFolderPath();

		if (ffolder != null && !ffolder.isEmpty() && !".".equals(ffolder))
		{
			File theFolder = new File(ffolder);
			if (theFolder.isAbsolute() && theFolder.isDirectory())
			{
				workDir = new FilePath(vChannel, ffolder);
			}
		}

		if (!workDir.exists())
		{
			throw new FileNotFoundException("workDir location does not exist. Location: " + workDir.getRemote());
		}

		String filenameAndPath = workDir.getRemote();
		listener.getLogger().println("worspace path: " + filenameAndPath);

		if (tttBuilder.getReportFolder() != null && tttBuilder.getReportFolder().trim().length() > 0)
		{
			File reportFolder = new File(tttBuilder.getReportFolder().trim());
			if (reportFolder != null && reportFolder.isAbsolute() && reportFolder.isDirectory())
			{
				filenameAndPath = new FilePath(vChannel, tttBuilder.getReportFolder().trim()).getRemote();
			}
			else
			{
				filenameAndPath = filenameAndPath + remoteFileSeparator + tttBuilder.getReportFolder().trim();
			}
		}
		listener.getLogger().println("Search " + osFile + " from the folder path: " + filenameAndPath);
		File theFolder = new File(filenameAndPath);
		String fileFound = searchFileFromDir(theFolder, osFile);

		if (fileFound != null)
		{
			filenameAndPath = fileFound;
			listener.getLogger().println("Founded file path: " + filenameAndPath);
		}
		else
		{
			filenameAndPath = filenameAndPath + remoteFileSeparator + osFile;
			listener.getLogger().println("The file path: " + filenameAndPath + " is missing.");
		}

		VirtualChannel vChannel = launcher.getChannel();
		remoteFile = new FilePath(vChannel, filenameAndPath);
		listener.getLogger().println("TotalTest  CLI script file remote path: " + remoteFile.getRemote());

		return remoteFile;
	}

	/**
	 * find a file by name in the folder
	 * 
	 * @param file
	 * @param search
	 * @return
	 */
	private static String searchFileFromDir(File file, String search)
	{
		if (file != null && search != null)
		{
			if (file.isDirectory())
			{
				File[] files = file.listFiles();
				if (files != null)
				{
					for (File f : files)
					{
						String fName = searchFileFromDir(f, search);
						if (fName != null)
							return fName;
					}
				}
			}
			else
			{
				if (search.equals(file.getName()))
				{
					return file.getAbsolutePath();
				}
			}
		}
		return null;
	}
}
