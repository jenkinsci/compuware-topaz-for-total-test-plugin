/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2019-2020 Compuware Corporation
 * (c) Copyright 2019-2020 BMC Software, Inc.
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
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
	private static final String TOTAL_TEST_CLI_BAT = "TotalTestFTCLI.bat"; //$NON-NLS-1$
	private static final String TOTAL_TEST_CLI_SH = "TotalTestFTCLI.sh"; //$NON-NLS-1$
	private static final String TOTAL_TEST_WEBAPP = "totaltestapi"; //$NON-NLS-1$
	private static final String TOPAZ_CLI_WORKSPACE = "TopazCliWkspc"; //$NON-NLS-1$
	private static final String DATA = "-data"; //$NON-NLS-1$
	private static final String FOLDER_OUTPUT = "Output"; //$NON-NLS-1$
	private static final String GENERATED_SUITE_RESULT_FILE_NAME = "generated.cli.suiteresult";  //$NON-NLS-1$
	private static final String GENERATED_SUITE_RESULT_FILE_NAME_OLD = "generated.cli.xasuiteres"; //$NON-NLS-1$
	private static final String FILE_EXT_XAUNIT ="scenario"; //$NON-NLS-1$
	private static final String FILE_EXT_XAUNIT_OLD = "xaunit"; //$NON-NLS-1$
	private static final String FILE_EXT_XASUITE = "suite"; //$NON-NLS-1$
	private static final String FILE_EXT_RESULT = "result"; //$NON-NLS-1$
	private static final String FILE_EXT_RESULT_OLD = "xares"; //$NON-NLS-1$
	private static final String FILE_EXT_XASUITE_RESULT = "suiteresult"; //$NON-NLS-1$
	private static final String FILE_EXT_XASUITE_RESULT_OLD ="xasuiteres"; //$NON-NLS-1$
	private static final String FILE_EXT_CONTEXT="context"; //$NON-NLS-1$
  	private static final String FILE_EXT_CONTEXT_OLD="xactx"; //$NON-NLS-1$

	private final TotalTestCTBuilder tttBuilder;

	private TaskListener listener;
	private FilePath workspaceFilePath;
	private Run<?, ?> build;
	private String remoteFileSeparator;

	/**
	 * Constructor
	 * 
	 * @param tttBuilder
	 * 			  An instance of <code>TotalTestCTBuilder</code> containing the arguments.
	 */
	public TotalTestCTRunner(TotalTestCTBuilder tttBuilder)
	{
		this.tttBuilder = tttBuilder;
	}

	/**
	 * Runs the Total Test Functional Test CLI
	 * 
	 * @param build
	 *			  The current running Jenkins build
	 * @param launcher
	 *            The machine that the files will be checked out.
	 * @param workspaceFilePath
	 *            a directory to check out the source code.
	 * @param listener
	 *            Build listener
	 *            
	 * @return <code>boolean</code> if the build was successful
	 * 
	 * @throws IOException
	 * 			If an error occurred execute Total Test run.
	 * @throws InterruptedException
	 * 			If the Total Test run was interrupted.
	 */
	public boolean run(final Run<?, ?> build, final Launcher launcher, final FilePath workspaceFilePath,
			final TaskListener listener) throws IOException, InterruptedException
	{
		// initialization
		ArgumentListBuilder args = new ArgumentListBuilder();
		EnvVars env = build.getEnvironment(listener);
		VirtualChannel vChannel = launcher.getChannel();
		if (vChannel == null)
		{
			listener.getLogger().println("Error: No channel could be retrieved"); //$NON-NLS-1$
			return false;
		}
		this.listener = listener;
		this.workspaceFilePath = workspaceFilePath;
		this.build = build;
		Properties remoteProperties = vChannel.call(new RemoteSystemProperties());
		remoteFileSeparator = remoteProperties.getProperty("file.separator"); //$NON-NLS-1$

		boolean isLinux = launcher.isUnix();
		String osScriptFile = isLinux ? TOTAL_TEST_CLI_SH : TOTAL_TEST_CLI_BAT;

		TotalTestRunnerUtils.logJenkinsAndPluginVersion(listener);

		FilePath cliScriptPath = TotalTestRunnerUtils.getCLIScriptPath(launcher, listener, remoteFileSeparator, osScriptFile);
		args.add(cliScriptPath.getRemote());
		
		String topazCliWorkspace = workspaceFilePath.getRemote() + remoteFileSeparator + TOPAZ_CLI_WORKSPACE;
		args.add(DATA, TotalTestRunnerUtils.escapeForScript(topazCliWorkspace));
		
		addArguments(args, launcher, listener, remoteFileSeparator);

		FilePath workDir = new FilePath (vChannel, workspaceFilePath.getRemote());
		workDir.mkdirs();

		listener.getLogger().println("----------------------------------"); //$NON-NLS-1$
		listener.getLogger().println("Now executing Total Test Testing CLI and printing out the execution log..."); //$NON-NLS-1$
		listener.getLogger().println("----------------------------------\n\n"); //$NON-NLS-1$

		int exitValue = launcher.launch().cmds(args).envs(env).stdout(listener.getLogger()).pwd(workDir).join();
		listener.getLogger().println(osScriptFile + " exited with exit value = " + exitValue); //$NON-NLS-1$

		if (exitValue == 0)
		{
			listener.getLogger().println("\n\n----------------------------------"); //$NON-NLS-1$
			listener.getLogger().println("Total Test Testing CLI finished executing, now analysing the result..."); //$NON-NLS-1$
			listener.getLogger().println("----------------------------------\n\n"); //$NON-NLS-1$
			exitValue = readTestResult(launcher);

			if (exitValue != 0)
			{
				if (!tttBuilder.getStopIfTestFailsOrThresholdReached())
				{
					listener.getLogger()
							.println("Test result failed but build continues (Stop If Test Fails Or Threshold Reached is false)"); //$NON-NLS-1$
					exitValue = 0;
				}
				else if (!tttBuilder.getHaltPipelineOnFailure())
				{
					// TODO: Should check if this build is for a pipeline or a Free style project?

					// Don't fail the build so the pipeline can continue.
					listener.getLogger()
					.println("Test result failed but build continues (\"" + tttBuilder.getHaltPipelineTitle() + "\" is false)"); //$NON-NLS-1$
					exitValue = 0;
				}
			}
		}
		else
		{
			listener.getLogger().println(
					"Something went wrong when executing the Total Test Testing CLI, and therefore there is no test results to analyze"); //$NON-NLS-1$
		}

		return exitValue == 0;
	}

	/**
	 * Read the test results
	 * 
	 * @param launcher
	 *              The machine that the files will be checked out.
	 *            
	 * @return		<code>int</code> 0 if the readTestRestult successful, otherwise -1
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private int readTestResult(final Launcher launcher) throws IOException, InterruptedException
	{
		int result = 0;
		try
		{
			if (tttBuilder.getCreateResult())
			{
				VirtualChannel vChannel = launcher.getChannel();
				FilePath testFolder = new FilePath(vChannel, tttBuilder.getFolderPath());
				boolean usesNewExtension = TotalTestRunnerUtils.usesNewFileExtensions(launcher, listener, remoteFileSeparator);
				boolean isSuite = true;
				String resultFileName = null;
				
				if (testFolder.exists() && testFolder.isDirectory() == false) //NOSONAR
				{
					// This most likely is a fully pathed test scenario.
					String fileName = testFolder.getName();
					int idx = fileName.indexOf('.');
					if (idx != -1)
					{
						String extension = fileName.substring(idx + 1);
						if (extension.compareTo(FILE_EXT_XASUITE) == 0)
						{
							isSuite = true;
							resultFileName =
									String.format("%s.%s", fileName.substring(0, idx), //$NON-NLS-1$
												  usesNewExtension ? FILE_EXT_XASUITE_RESULT : FILE_EXT_XASUITE_RESULT_OLD);
						}
						else if (extension.compareTo(FILE_EXT_XAUNIT) == 0 ||
							extension.compareTo(FILE_EXT_XAUNIT_OLD) == 0 ||
							extension.compareTo(FILE_EXT_CONTEXT) == 0 ||
							extension.compareTo(FILE_EXT_CONTEXT_OLD) == 0)
						{
							isSuite = false;
							resultFileName = String.format("%s.%s", fileName.substring(0, idx), //$NON-NLS-1$
														   usesNewExtension ? FILE_EXT_RESULT : FILE_EXT_RESULT_OLD);
						}
					}
				}
				else
				{
					isSuite = true;
					resultFileName = usesNewExtension ? GENERATED_SUITE_RESULT_FILE_NAME : GENERATED_SUITE_RESULT_FILE_NAME_OLD;
				}
				
				FilePath testSuiteResultPath = getOutputFilePath(launcher, listener, resultFileName);
				
				if (testSuiteResultPath != null)
				{
					listener.getLogger().println("Found file path: " + testSuiteResultPath.getRemote()); //$NON-NLS-1$
				}
				else
				{
					FilePath workDir = new FilePath(vChannel, workspaceFilePath.getRemote());
					testSuiteResultPath = new FilePath(workDir, resultFileName).absolutize();
					listener.getLogger().println("The file path: " + testSuiteResultPath.getRemote() + " is missing."); //$NON-NLS-1$ //$NON-NLS-2$
				}
		
				listener.getLogger().println("TotalTest  CLI script file remote path: " + testSuiteResultPath.getRemote()); //$NON-NLS-1$
		
				listener.getLogger().println("Reading suite result from file: " + testSuiteResultPath.getRemote()); //$NON-NLS-1$
			
				String content = null;
	
				// For performance reasons we will create the content String from the testSuiteResultPath if the file is remote
				// (running on a slave) otherwise we use the better performing Files.readAllBytes.
				// The performance difference is approximately 2x faster with Files.readAllBytes than
				// TotalTestRunnerUtils.GetRemoteUTF8FileContents() when running locally.
				if (testSuiteResultPath.isRemote())
				{
					content = testSuiteResultPath.act(new TotalTestRunnerUtils.GetRemoteUTF8FileContents());
				}
				else
				{
					content = new String(Files.readAllBytes(Paths.get(testSuiteResultPath.getRemote())), StandardCharsets.UTF_8);
				}
		
				listener.getLogger().println("Result content:"); //$NON-NLS-1$
				listener.getLogger().println(content);
	
				Document document = getXaScenarioSuiteResultAsDocument(content);
				String xaScenarioSuiteResult = getXaScenarioSuiteResult(document, isSuite);
				String logMessage = String.format("Result state from %s: %s", isSuite ? FILE_EXT_XASUITE : FILE_EXT_XAUNIT , xaScenarioSuiteResult);  //$NON-NLS-1$
				listener.getLogger().println(logMessage);
	
				if (!xaScenarioSuiteResult.equalsIgnoreCase("SUCCESS")) //$NON-NLS-1$
				{
					result = -1;
				}
	
				if (isSuite && result != -1 && tttBuilder.getCcThreshold() > 0)
				{
					listener.getLogger().println(
							"The suite executed successfully, now checking that code coverage level is higher than the threshold on " //$NON-NLS-1$
									+ tttBuilder.getCcThreshold() + " %"); //$NON-NLS-1$
					boolean isCCThresholdOk = getXaScenarioSuiteCodeCoverage(document, isSuite);
					if (!isCCThresholdOk)
					{
						listener.getLogger().println("Code coverage threshold not reached"); //$NON-NLS-1$
						result = -1;
					}
				}
			}
		}
		catch (Exception e)
		{
			listener.getLogger().println("Exception in parsing XaSuiteResult. " + e.getMessage()); //$NON-NLS-1$
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			listener.getLogger().println(sw.toString());
		}
		return result;
	}

	/**
	 * Return the results Document.
	 * 
	 * @param xml
	 * 			  The results xml to create results document.
	 * 
	 * @return	  <code>Document</code> that is generaged from the result xml
	 * 
	 * @throws Exception
	 */
	private Document getXaScenarioSuiteResultAsDocument(String xml) throws Exception //NOSONAR
	{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Reader r = new StringReader(xml);
		return db.parse(new InputSource(r));
	}

	/**
	 * Return the Scenario or Suite results Document.
	 * 
	 * @param document
	 * 			The results document to get the status from.
	 * @param isSuite
	 * 			<code>true</code> if a suite result otherwise <code>false</code>
	 * 
	 * @return	 <code>String</code> representing the results.
	 * 
	 * @throws Exception
	 */
	private String getXaScenarioSuiteResult(Document document, boolean isSuite) throws Exception	//NOSONAR
	{
		String resultType = null;

		XPathFactory xpf = XPathFactory.newInstance();
		XPath xpath = xpf.newXPath();
		String resultTag = isSuite ? "/XaSuiteResult" : "/XaUnitResult"; //$NON-NLS-1$ //$NON-NLS-2$
		Element xaSuiteResultElement = (Element) xpath.evaluate(resultTag, document, XPathConstants.NODE); //NOSONAR
		resultType = xaSuiteResultElement.getAttribute("resultType"); //$NON-NLS-1$

		return resultType;
	}

	/**
	 * Return if there is Code Doverage data.
	 * 
	 * @param document
	 * 			Document to look for Code Coverage data.
	 * @param isSuite
	 * 			<code>true</code> if a suite result otherwise <code>false</code>
	 * 
	 * @return <code>boolean</code> if the document has Code Coverage data.
	 * 
	 * @throws Exception
	 */
	private boolean getXaScenarioSuiteCodeCoverage(Document document, boolean isSuite) throws Exception // NOSONAR
	{
		boolean isCCThresholdOk = true;

		XPathFactory xpf = XPathFactory.newInstance();
		XPath xpath = xpf.newXPath();
		String resultPathName = isSuite ? "XaSuiteResult" : "XaUnitResult";  //$NON-NLS-1$  //$NON-NLS-2$
		Element percentageElement = (Element) xpath.evaluate(String.format("/%s/CC/data", resultPathName), document, XPathConstants.NODE); //$NON-NLS-1$

		if (percentageElement != null)
		{
			String sPercentage = percentageElement.getAttribute("percentage"); //$NON-NLS-1$

			int percentage = Integer.parseInt(sPercentage);

			if (percentage < tttBuilder.getCcThreshold())
			{
				listener.getLogger().println(resultPathName + " percentage on " + sPercentage //$NON-NLS-1$
						+ " is less than Code Coverage threshold on " + tttBuilder.getCcThreshold() + ". Aborting build."); //$NON-NLS-1$ //$NON-NLS-2$
				isCCThresholdOk = false;
			}

			if (isCCThresholdOk)
			{
				listener.getLogger().println(resultPathName + " Code Coverage threshold is " //$NON-NLS-1$
						+ tttBuilder.getCcThreshold() + " which is below the result on " + sPercentage); //$NON-NLS-1$
			}
		}

		return isCCThresholdOk;
	}

	
	/**
	 * Adds an arguments to the argument list.
	 * 
	 * @param args
	 *		  The argument list to add to.
	 * @param launcher
	 *            The machine that the files will be checked out.
	 * @param listener
	 * 		  Build listener
	 * @param remoteFileSeparator
	 * 			  The remote file separator
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	private void addArguments(final ArgumentListBuilder args, final Launcher launcher, final TaskListener listener, final String remoteFileSeparator) throws IOException, InterruptedException
	{
		args.add("-e").add(TotalTestRunnerUtils.escapeForScript(tttBuilder.getEnvironmentId()), false); //$NON-NLS-1$

		String tttServerUrl = tttBuilder.getServerUrl();

		if (!tttServerUrl.endsWith("/")) //$NON-NLS-1$
		{
			tttServerUrl += "/"; //$NON-NLS-1$
		}

		tttServerUrl += TOTAL_TEST_WEBAPP + "/"; //$NON-NLS-1$
		listener.getLogger().println("Set the repository URL : " + tttServerUrl); //$NON-NLS-1$

		if (!TotalTestRunnerUtils.isMinimumRelease(launcher, listener, remoteFileSeparator, TotalTestRunnerUtils.TTT_CLI_200401) || !tttBuilder.getLocalConfig())
		{
			args.add("-s").add(TotalTestRunnerUtils.escapeForScript(tttServerUrl), false); //$NON-NLS-1$
		}

		
		args.add("-u").add( //$NON-NLS-1$
				TotalTestRunnerUtils.getLoginInformation(build.getParent(), tttBuilder.getCredentialsId()).getUsername(),
				false);
		args.add("-p").add( //$NON-NLS-1$
				TotalTestRunnerUtils.getLoginInformation(build.getParent(), tttBuilder.getCredentialsId()).getPassword(), true);

		String folder = tttBuilder.getFolderPath();
		if (Strings.isNullOrEmpty(folder) || folder.trim().isEmpty())
		{
			folder = "."; //$NON-NLS-1$
		}

		listener.getLogger().println("The folder path: " + folder); //$NON-NLS-1$
		args.add("-f").add(TotalTestRunnerUtils.escapeForScript(folder), false); //$NON-NLS-1$

		String workDir = workspaceFilePath.getRemote();
		if (!Strings.isNullOrEmpty(workDir))
		{
			if (workDir.compareTo(workspaceFilePath.absolutize().getRemote()) != 0)
			{
				args.add("-r").add(TotalTestRunnerUtils.escapeForScript(workDir)); //$NON-NLS-1$
				listener.getLogger().println("Set the root folder : " + workDir); //$NON-NLS-1$
			}
			else
			{
				listener.getLogger().println("Absolute folder used for the Root folder : " + workDir); //$NON-NLS-1$
			}
		}

		if (tttBuilder.getRecursive())
		{
			args.add("-R"); //$NON-NLS-1$
		}

		if (tttBuilder.getUploadToServer())
		{
			args.add("-x"); //$NON-NLS-1$
		}

		if (tttBuilder.getHaltAtFailure())
		{
			args.add("-h"); //$NON-NLS-1$
		}

		if (!Strings.isNullOrEmpty(tttBuilder.getSourceFolder()) && !tttBuilder.getSourceFolder().equalsIgnoreCase("COBOL")) //$NON-NLS-1$
		{
			args.add("-S").add(TotalTestRunnerUtils.escapeForScript(tttBuilder.getSourceFolder())); //$NON-NLS-1$
		}

		if (TotalTestRunnerUtils.usesDefaultOutputFolder(launcher, listener, remoteFileSeparator) == true) //NOSONAR
		{
			args.add("-G"); //$NON-NLS-1$
		}
		else
		{
			if (!Strings.isNullOrEmpty(tttBuilder.getReportFolder()))
			{
				args.add("-g").add(TotalTestRunnerUtils.escapeForScript(tttBuilder.getReportFolder())); //$NON-NLS-1$
				args.add("-G"); //$NON-NLS-1$
			}
		}

		if (TotalTestRunnerUtils.isMinimumRelease(launcher, listener, remoteFileSeparator, TotalTestRunnerUtils.TTT_CLI_200401))
		{
			if (tttBuilder.isLocalConfig())
			{
				args.add("-cfgdir"); //$NON-NLS-1$
				if (Strings.isNullOrEmpty(tttBuilder.getLocalConfigLocation()))
				{
					args.add(TotalTestRunnerUtils.escapeForScript(TotalTestCTBuilder.DescriptorImpl.defaultLocalConfigLocation)); //$NON-NLS-1$
				}
				else
				{
					args.add(TotalTestRunnerUtils.escapeForScript(tttBuilder.getLocalConfigLocation())); //$NON-NLS-1$
				}
			}
		}

		if (!Strings.isNullOrEmpty(tttBuilder.getSonarVersion()))
		{
			args.add("-v").add(tttBuilder.getSonarVersion()); //$NON-NLS-1$
		}

		if (!Strings.isNullOrEmpty(tttBuilder.getAccountInfo()))
		{
			args.add("-a").add(tttBuilder.getAccountInfo()); //$NON-NLS-1$
		}
		
		args.add("-l").add("jenkins"); //$NON-NLS-1$ //$NON-NLS-2$
		
		if (tttBuilder.getCompareJUnits())
		{
			args.add("-cju"); //$NON-NLS-1$
		}
		
		if (!Strings.isNullOrEmpty(tttBuilder.getAccountInfo()))
		{
			args.add("-a").add(tttBuilder.getAccountInfo()); //$NON-NLS-1$
		}
		
		if (TotalTestRunnerUtils.isMinimumRelease(launcher, listener, remoteFileSeparator, TotalTestRunnerUtils.TTT_CLI_200401))
		{
			if (tttBuilder.getSelectProgramsOption())
			{
				String selectProgramsRadio = tttBuilder.getselectProgramsRadioValue();
				String selectProgramsText = tttBuilder.getselectProgramsText();
				
				if (!Strings.isNullOrEmpty(selectProgramsRadio))
				{
					args.add(selectProgramsRadio);
					
					if (!Strings.isNullOrEmpty(selectProgramsText))
					{
						args.add(selectProgramsText);
					}
				}
			}

			if (tttBuilder.getUseScenarios())
			{
				args.add("-U"); //$NON-NLS-1$
			}
			
			args.add("-loglevel").add(tttBuilder.getLogLevel());
		
			if (!tttBuilder.getCreateReport())
			{
				args.add("-norep"); //$NON-NLS-1$
			}
			if (!tttBuilder.getCreateResult())
			{
				args.add("-nores"); //$NON-NLS-1$
			}
			if (!tttBuilder.getCreateSonarReport())
			{
				args.add("-nosq"); //$NON-NLS-1$
			}
			if (!tttBuilder.getCreateJUnitReport())
			{
				args.add("-noju"); //$NON-NLS-1$
			}

			if (tttBuilder.getCollectCodeCoverage())
			{
				boolean isCodeCoverageValid = true;
				
				if (!Strings.isNullOrEmpty(tttBuilder.getCollectCCRepository()))
				{
					args.add("-ccrepo").add(tttBuilder.getCollectCCRepository()); //$NON-NLS-1$
				}
				else
				{
					isCodeCoverageValid = false;
				}
				if (!Strings.isNullOrEmpty(tttBuilder.getCollectCCSystem()))
				{
					args.add("-ccsys").add(tttBuilder.getCollectCCSystem()); //$NON-NLS-1$
				}
				else
				{
					isCodeCoverageValid = false;
				}
				if (!Strings.isNullOrEmpty(tttBuilder.getCollectCCTestID()))
				{
					args.add("-cctid").add(tttBuilder.getCollectCCTestID()); //$NON-NLS-1$
				}
				else
				{
					isCodeCoverageValid = false;
				}
				if (isCodeCoverageValid)
				{
					args.add("-ccclear").add(tttBuilder.getClearCodeCoverage()); //$NON-NLS-1$
				}
			}
		}
	}

	/**
	 * Returns the path to the Output directory.
	 * 
	 * @param launcher
	 *            The machine that the files will be checked out.
	 * @param listener
	 *            Build listener
	 * @param osFile
	 * 			  The file name of the file on the remote system
	 *            
	 * @return	  An instance of <code>FilePath</code> for the CLI directory
	 * 
	 * @throws IOException
	 *             If the CLI directory does not exist.
	 * @throws InterruptedException
	 *             If unable to get CLI directory.
	 */
	private FilePath getOutputFilePath(final Launcher launcher, final TaskListener listener, String osFile) throws IOException, InterruptedException
	{
		VirtualChannel vChannel = launcher.getChannel();
		boolean useDefaultOutput = TotalTestRunnerUtils.usesDefaultOutputFolder(launcher, listener, remoteFileSeparator);
		FilePath workDir = new FilePath(vChannel, workspaceFilePath.getRemote());
		
		String folderPathString = tttBuilder.getFolderPath();
		if (folderPathString != null && !folderPathString.isEmpty() && !".".equals(folderPathString)) //$NON-NLS-1$
		{
			FilePath absoluteFolder = new FilePath (vChannel, folderPathString).absolutize();
			if (absoluteFolder.isDirectory())
			{
				workDir = absoluteFolder;
			}
			else
			{
				if (useDefaultOutput == true) //NOSONAR
				{
					if (absoluteFolder.exists())
					{
						// This is an absolute path file, so go back to the parent directory.
						absoluteFolder = new FilePath (vChannel, absoluteFolder + remoteFileSeparator + "..").absolutize(); //$NON-NLS-1$
					}
					else
					{
						if (folderPathString.endsWith("\\") || folderPathString.endsWith( "/")) //$NON-NLS-1$ //$NON-NLS-2$
						{
							absoluteFolder = new FilePath (vChannel, workDir + folderPathString);
						}
						else
						{
							absoluteFolder = new FilePath (vChannel, workDir + remoteFileSeparator + folderPathString).absolutize();
						}
					}
					if (absoluteFolder.isDirectory())
					{
						workDir = absoluteFolder;
					}
				}
			}
		}

		if (!workDir.exists())
		{
			throw new FileNotFoundException("workDir location does not exist. Location: " + workDir.getRemote()); //$NON-NLS-1$
		}

		listener.getLogger().println("workspace path: " + workDir.getRemote()); //$NON-NLS-1$
		
		String outputFolder = null;
		
		if (useDefaultOutput == false && folderPathString != null) // NOSONAR
		{
			FilePath tempAbsoluteFolder = new FilePath (vChannel, folderPathString);
			FilePath absoluteFolder = null;
			if (null != tempAbsoluteFolder)
			{
				absoluteFolder = tempAbsoluteFolder.absolutize();
				if (!folderPathString.isEmpty() && !".".equals(folderPathString)) //$NON-NLS-1$
				{
					if (absoluteFolder.exists())
					{
						if (absoluteFolder.isDirectory() == true) // NOSONAR
						{
							absoluteFolder = new FilePath (absoluteFolder, tttBuilder.getReportFolder().trim());
						}
					}
					else
					{
						// Strip off the scenario/suite name, add the report folder and add the difference between working and absolute path
						String reportFolder = tttBuilder.getReportFolder().trim();
						String folderPath = tttBuilder.getFolderPath().trim();
						
						if (reportFolder.endsWith("/") || reportFolder.endsWith("\\")) //$NON-NLS-1$ //$NON-NLS-2$
						{
							// Relative Path with trailing seperator
							absoluteFolder = new FilePath (vChannel, reportFolder + folderPath);
						}
						else
						{
							// Relative path with no trailing separator.
							absoluteFolder = new FilePath (vChannel, reportFolder + remoteFileSeparator + folderPath);
						}
					}
				}
			}

			if (absoluteFolder.exists() && absoluteFolder.isDirectory())
			{
				// Absolute Path to a folder
				outputFolder = absoluteFolder.getRemote();
			}
			else
			{
				// Absolute Path to file (Suite, Context or Scenario) in the working directory
				FilePath absoluteReportFolderPath = null;
				absoluteReportFolderPath = new FilePath(workDir, absoluteFolder.getRemote()).absolutize();
				
				if (absoluteReportFolderPath.exists() && absoluteReportFolderPath.isDirectory())
				{
					// Relative Path exists in the working directory
					outputFolder = absoluteFolder.getRemote();
				}
				else
				{
					FilePath parentPath = absoluteReportFolderPath.getParent();
					
					if (parentPath.exists())
					{
						// Absolute path to suite/context/scenario
						outputFolder = new FilePath(parentPath, tttBuilder.getReportFolder().trim()).getRemote();
					}
					else
					{
						outputFolder = tttBuilder.getReportFolder().trim();
					}
				}
			}
		}
		
		if (outputFolder == null || outputFolder.isEmpty())
		{
			outputFolder = FOLDER_OUTPUT;
		}
		
		FilePath absoluteReportFolderPath = null;
		absoluteReportFolderPath = new FilePath(workDir, outputFolder).absolutize();
		
		listener.getLogger().println("Search " + osFile + " from the folder path: " + absoluteReportFolderPath.getRemote()); //$NON-NLS-1$ //$NON-NLS-2$
		FilePath fileFound = searchFileFromDir(absoluteReportFolderPath, osFile, listener);
		
		return fileFound;
	}

	/**
	 * find a file by name in the folder
	 * 
	 * @param directoryPath
	 * 			  The folder where we should searc.
	 * @param search
	 * 			  The file to search for.
	 * @param listener
	 *            Build listener
	 * 
	 * @return	  <code>String</code> The absolute path to the file.
	 */
	private static FilePath searchFileFromDir(FilePath directoryPath, String search, final TaskListener listener)
	{
		FilePath returnFile = null;
		
		if (!Strings.isNullOrEmpty(search))
		{
			try {
				for (FilePath childPath : directoryPath.list())
				{
					if (childPath.isDirectory())
					{
						// Recurse into this directory
						returnFile = searchFileFromDir(childPath, search, listener);
					}
					else if (search.equals(childPath.getName()))
					{
						returnFile = childPath.absolutize();
					}
					
					if (null != returnFile)
					{
						break;
					}
				}
			} catch (IOException e) {
				listener.getLogger().println("Exception locating " + search + " from " + directoryPath.getRemote() + ":" + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			} catch (InterruptedException e) { //NOSONAR
				listener.getLogger().println("Exception locating " + search + " from " + directoryPath.getRemote() + ":" + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
		
		return returnFile;
	}
}
