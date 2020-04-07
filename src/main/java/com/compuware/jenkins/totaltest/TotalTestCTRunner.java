/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2019,2020 Compuware Corporation
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
	private  static final String TTT_MINIMUM_CLI_VERSION = "19.6.4"; //$NON-NLS-1$
	private static final String TOTAL_TEST_CLI_BAT = "TotalTestFTCLI.bat"; //$NON-NLS-1$
	private static final String TOTAL_TEST_CLI_SH = "TotalTestFTCLI.sh"; //$NON-NLS-1$
	private static final String TOTAL_TEST_WEBAPP = "totaltestapi"; //$NON-NLS-1$
	private static final String TOPAZ_CLI_WORKSPACE = "TopazCliWkspc"; //$NON-NLS-1$
	private static final String DATA = "-data"; //$NON-NLS-1$

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

		FilePath cliScriptPath = TotalTestRunnerUtils.getCLIScriptPath(launcher, listener, remoteFileSeparator, osScriptFile, TTT_MINIMUM_CLI_VERSION);
		args.add(cliScriptPath.getRemote());
		
		String topazCliWorkspace = workspaceFilePath.getRemote() + remoteFileSeparator + TOPAZ_CLI_WORKSPACE;
		args.add(DATA, TotalTestRunnerUtils.escapeForScript(topazCliWorkspace));
		
		addArguments(args, listener);

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
				boolean isStopIfTestFailsOrThresholdReached = tttBuilder.getStopIfTestFailsOrThresholdReached();

				if (!isStopIfTestFailsOrThresholdReached)
				{
					listener.getLogger()
							.println("Test result failed but build continues (isStopIfTestFailsOrThresholdReached is false)"); //$NON-NLS-1$
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
	 *              The machine that the files will be checked out..
	 *            
	 * @return		<code>int</code> 0 if the readTestRestult successful, otherwise -1
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private int readTestResult(final Launcher launcher) throws IOException, InterruptedException
	{
		int result = 0;
		String resultFileName = "generated.cli.xasuiteres"; //$NON-NLS-1$
		FilePath testSuiteResultPath = getRemoteFilePath(launcher, listener, resultFileName);
		if (testSuiteResultPath == null)
		{
			resultFileName = "generated.cli.suiteresult"; //$NON-NLS-1$
			testSuiteResultPath = getRemoteFilePath(launcher, listener, resultFileName);
		}
		
		
		if (testSuiteResultPath != null)
		{
			listener.getLogger().println("Found file path: " + testSuiteResultPath.getRemote()); //$NON-NLS-1$
		}
		else
		{
			VirtualChannel vChannel = launcher.getChannel();
			FilePath workDir = new FilePath(vChannel, workspaceFilePath.getRemote());
			testSuiteResultPath = new FilePath(workDir, resultFileName).absolutize();
			listener.getLogger().println("The file path: " + testSuiteResultPath.getRemote() + " is missing."); //$NON-NLS-1$ //$NON-NLS-2$
		}

		listener.getLogger().println("TotalTest  CLI script file remote path: " + testSuiteResultPath.getRemote()); //$NON-NLS-1$

		listener.getLogger().println("Reading suite result from file: " + testSuiteResultPath.getRemote()); //$NON-NLS-1$
		
		try
		{
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

			Document document = getXaSuiteResultAsDocument(content);
			String xaSuiteResult = getXaSuiteResult(document);
			listener.getLogger().println("Result state from suite: " + xaSuiteResult); //$NON-NLS-1$

			if (!xaSuiteResult.equalsIgnoreCase("SUCCESS")) //$NON-NLS-1$
			{
				result = -1;
			}

			if (result != -1 && tttBuilder.getCcThreshhold() > 0)
			{
				listener.getLogger().println(
						"The suite executed successfully, now checking that code coverage level is higher than the treshhold on " //$NON-NLS-1$
								+ tttBuilder.getCcThreshhold() + " %"); //$NON-NLS-1$
				boolean isCCThresholdOk = getXaSuiteCodeCoverage(document);
				if (!isCCThresholdOk)
				{
					listener.getLogger().println("Code coverage treshhold not reached"); //$NON-NLS-1$
					result = -1;
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
	private Document getXaSuiteResultAsDocument(String xml) throws Exception //NOSONAR
	{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Reader r = new StringReader(xml);
		return db.parse(new InputSource(r));
	}

	/**
	 * Return the Suite results Document.
	 * 
	 * @param document
	 * 			The results document to get the status from.
	 * 
	 * @return	 <code>String</code> representing the results.
	 * 
	 * @throws Exception
	 */
	private String getXaSuiteResult(Document document) throws Exception	//NOSONAR
	{
		String resultType = null;

		XPathFactory xpf = XPathFactory.newInstance();
		XPath xpath = xpf.newXPath();
		Element xaSuiteResultElement = (Element) xpath.evaluate("/XaSuiteResult", document, XPathConstants.NODE); //NOSONAR //$NON-NLS-1$
		resultType = xaSuiteResultElement.getAttribute("resultType"); //$NON-NLS-1$

		return resultType;
	}

	/**
	 * Return if there is Code Doverage data.
	 * 
	 * @param document
	 * 			Document to look for Code Coverage data.
	 * 
	 * @return <code>boolean</code> if the document has Code Coverage data.
	 * 
	 * @throws Exception
	 */
	private boolean getXaSuiteCodeCoverage(Document document) throws Exception // NOSONAR
	{
		boolean isCCThresholdOk = true;

		XPathFactory xpf = XPathFactory.newInstance();
		XPath xpath = xpf.newXPath();
		Element percentageElement = (Element) xpath.evaluate("/XaSuiteResult/CC/data", document, XPathConstants.NODE); //$NON-NLS-1$

		if (percentageElement != null)
		{
			String sPercentage = percentageElement.getAttribute("percentage"); //$NON-NLS-1$

			int percentage = Integer.parseInt(sPercentage);

			if (percentage < tttBuilder.getCcThreshhold())
			{
				listener.getLogger().println("XaUnitResult percentage on " + sPercentage //$NON-NLS-1$
						+ " is less than Code Coverage threshold on " + tttBuilder.getCcThreshhold() + ". Aborting build."); //$NON-NLS-1$ //$NON-NLS-2$
				isCCThresholdOk = false;
			}

			if (isCCThresholdOk)
			{
				listener.getLogger().println("XaUnitResults Code Coverage threshold is " + tttBuilder.getCcThreshhold() //$NON-NLS-1$
						+ " which is below the result on " + sPercentage); //$NON-NLS-1$
			}
		}

		return isCCThresholdOk;
	}

	
	/**
	 * Adds an arguments to the argument list.
	 * 
	 * @param args
	 *		  The argument list to add to.
	 * @param listener
	 * 		  Build listener
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	private void addArguments(final ArgumentListBuilder args, final TaskListener listener) throws IOException, InterruptedException
	{
		args.add("-e").add(TotalTestRunnerUtils.escapeForScript(tttBuilder.getEnvironmentId()), false); //$NON-NLS-1$

		String tttServerUrl = tttBuilder.getServerUrl();

		if (!tttServerUrl.endsWith("/")) //$NON-NLS-1$
		{
			tttServerUrl += "/"; //$NON-NLS-1$
		}

		tttServerUrl += TOTAL_TEST_WEBAPP + "/"; //$NON-NLS-1$
		listener.getLogger().println("Set the repository URL : " + tttServerUrl); //$NON-NLS-1$

		args.add("-s").add(TotalTestRunnerUtils.escapeForScript(tttServerUrl), false); //$NON-NLS-1$
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

		if (!Strings.isNullOrEmpty(tttBuilder.getReportFolder()))
		{
			args.add("-g").add(TotalTestRunnerUtils.escapeForScript(tttBuilder.getReportFolder())); //$NON-NLS-1$
			args.add("-G"); //$NON-NLS-1$
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

	}

	/**
	 * Returns the path to the remote file
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
	private FilePath getRemoteFilePath(final Launcher launcher, final TaskListener listener, String osFile) throws IOException, InterruptedException
	{
		VirtualChannel vChannel = launcher.getChannel();
		FilePath workDir = new FilePath(vChannel, workspaceFilePath.getRemote());

		String folderPathString = tttBuilder.getFolderPath();

		if (folderPathString != null && !folderPathString.isEmpty() && !".".equals(folderPathString)) //$NON-NLS-1$
		{
			FilePath absoluteFolder = new FilePath (vChannel, folderPathString).absolutize();
			if (absoluteFolder.isDirectory())
			{
				workDir = absoluteFolder;
			}
		}

		if (!workDir.exists())
		{
			throw new FileNotFoundException("workDir location does not exist. Location: " + workDir.getRemote()); //$NON-NLS-1$
		}

		listener.getLogger().println("workspace path: " + workDir.getRemote()); //$NON-NLS-1$
		
		String reportFolder = tttBuilder.getReportFolder().trim();
		FilePath absoluteReportFolderPath = null;

		if (reportFolder != null && !reportFolder.isEmpty())
		{
			FilePath reportFolderPath = new FilePath(vChannel, reportFolder);
			absoluteReportFolderPath = reportFolderPath.absolutize();

			if (absoluteReportFolderPath != null && absoluteReportFolderPath.isDirectory())
			{
				absoluteReportFolderPath = new FilePath(workDir, reportFolder).absolutize();
			}
			else
			{
//				absoluteReportFolderPath = new FilePath(workDir, folderPathString + remoteFileSeparator + reportFolder).absolutize();
				absoluteReportFolderPath = new FilePath(workDir, reportFolder + remoteFileSeparator + folderPathString).absolutize();
			}
		}
		else
		{
			absoluteReportFolderPath = new FilePath(vChannel, reportFolder).absolutize();
		}
		
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
