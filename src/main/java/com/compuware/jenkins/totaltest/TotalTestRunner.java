/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 - 2018 Compuware Corporation
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
import java.util.Properties;

import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import com.compuware.jenkins.common.configuration.HostConnection;
import com.compuware.jenkins.common.utils.CLIVersionUtils;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Plugin;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.util.ArgumentListBuilder;
import jenkins.model.Jenkins;

public class TotalTestRunner
{
	public static final String TTT_MINIMUM_CLI_VERSION = "18.2.4";
	
	private static final String COMMA = ","; //$NON-NLS-1$
	
	private static final String COPY_JUNIT = "copyjunit"; //$NON-NLS-1$
	private static final String COPY_SONAR = "copysonar"; //$NON-NLS-1$
	private static final String RUNTEST = "runtest"; //$NON-NLS-1$

	private static final String TOTAL_TEST_CLI_BAT = "TotalTestCLI.bat"; //$NON-NLS-1$
	private static final String TOTAL_TEST_CLI_SH = "TotalTestCLI.sh"; //$NON-NLS-1$
	public static final String TOPAZ_CLI_WORKSPACE = "TopazCliWkspc"; //$NON-NLS-1$
	
	private static final String COMMAND = "-command"; //$NON-NLS-1$
	private static final String HOST = "-host"; //$NON-NLS-1$
	private static final String PORT = "-port"; //$NON-NLS-1$
	private static final String USER= "-user"; //$NON-NLS-1$
	private static final String PASSWORD = "-pw"; //$NON-NLS-1$ //NOSONAR
	private static final String PROJECT = "-project"; //$NON-NLS-1$
	private static final String TESTSUITE = "-ts"; //$NON-NLS-1$
	private static final String JCL = "-jcl"; //$NON-NLS-1$
	private static final String EXTERNAL_TOOLS_WS = "-externaltoolsws";  //$NON-NLS-1$
	private static final String POST_RUN_COMMANDS = "-postruncommands";  //$NON-NLS-1$
	private static final String TEST_NAME_LIST = "-testsuitelist";  //$NON-NLS-1$
	private static final String DATA = "-data"; //$NON-NLS-1$
	private static final String DSN_HLQ = "-dsnhlq"; //$NON-NLS-1$
	private static final String PROTOCOL = "-encryptprotocol"; //$NON-NLS-1$
	
	private static final String CODE_COVERAGE_REPO = "-ccrepo"; //$NON-NLS-1$
	private static final String CODE_COVERAGE_SYSTEM = "-ccsystem"; //$NON-NLS-1$
	private static final String CODE_COVERAGE_TESTID = "-cctestid"; //$NON-NLS-1$
	private static final String CODE_COVERAGE_TYPE = "-cctype"; //$NON-NLS-1$
	private static final String CODE_COVERAGE_CLEAR = "-ccclearstats"; //$NON-NLS-1$
	
	private static final String USE_STUBS = "-usestubs"; //$NON-NLS-1$
	private static final String DELETE_TEMPORARY = "-deletetemp"; //$NON-NLS-1$
	private static final String TARGET_ENCODING = "-targetencoding"; //$NON-NLS-1$
	
	private static final String PROPERTY_FILE_SEPARATOR = "file.separator";  //$NON-NLS-1$
	private static final String DEFAULT_CODE_PAGE = "1047";  //$NON-NLS-1$

	private static final String JENKINS = "-jenkins";  //$NON-NLS-1$
	
	private final TotalTestBuilder tttBuilder;
	
	public TotalTestRunner(TotalTestBuilder tttBuilder)
	{
		this.tttBuilder = tttBuilder;
	}
	
	/**
	 * Runs the Total Test CLI
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
	public boolean run(final Run<?,?> build, final Launcher launcher, final FilePath workspaceFilePath, final TaskListener listener) throws IOException, InterruptedException
	{
        ArgumentListBuilder args = new ArgumentListBuilder();
        EnvVars env = build.getEnvironment(listener);

        VirtualChannel vChannel = launcher.getChannel();
        Properties remoteProperties = vChannel.call(new RemoteSystemProperties());
        String remoteFileSeparator = remoteProperties.getProperty(PROPERTY_FILE_SEPARATOR);
        
		boolean isShell = launcher.isUnix();
		String osScriptFile = isShell ? TOTAL_TEST_CLI_SH : TOTAL_TEST_CLI_BAT;
		
		logJenkinsAndPluginVersion(listener);
		
		FilePath cliScriptPath = getCLIScriptPath(launcher, listener, remoteFileSeparator, osScriptFile);
		
		args.add(cliScriptPath.getRemote());
		
		String topazCliWorkspace = workspaceFilePath.getRemote() + remoteFileSeparator + TOPAZ_CLI_WORKSPACE;
		listener.getLogger().println("Topaz for Total Test CLI workspace: " + topazCliWorkspace); //$NON-NLS-1$
		
		addArgument(args, COMMAND, RUNTEST, isShell);
		
		args.add(JENKINS);
		
		addHostArguments(build, args, isShell);
		
		addProjectArguments(launcher, workspaceFilePath.getRemote() + remoteFileSeparator, args, isShell);
	
		addExecutionArguments(args, isShell);
		
		addCodeCoverageArguments(args, isShell);
		
		addExternalToolArguments(workspaceFilePath, args, isShell);
		
		args.add(DATA, topazCliWorkspace);
		
		FilePath workDir = new FilePath (vChannel, workspaceFilePath.getRemote());
		workDir.mkdirs();
		int exitValue = launcher.launch().cmds(args).envs(env).stdout(listener.getLogger()).pwd(workDir).join();

		listener.getLogger().println(osScriptFile + " exited with exit value = " + exitValue); //$NON-NLS-1$ //$NON-NLS-2$

		return exitValue == 0;
	}
	
	/**
	 * Returns the path to the script to execute Total Test CLI
	 * 
	 * @param launcher
	 *            The machine that the files will be checked out.
	 *            
	 * @return	An instance of <code>FilePath</code> for the CLI directory
	 * 
	 * @throws IOException
	 * 			If the CLI directory does not exist.
	 * @throws InterruptedException
	 * 			If unable to get CLI directory.
	 */
	private FilePath getCLIScriptPath(final Launcher launcher, final TaskListener listener, String remoteFileSeparator, String osScriptFile) throws IOException, InterruptedException
	{
		FilePath cliBatchFileRemote = null;
		FilePath globalCLIDirectory = null;
		CpwrGlobalConfiguration globalConfig = CpwrGlobalConfiguration.get();
		if (globalConfig != null)
		{
			String cliDirectoryName = globalConfig.getTopazCLILocation(launcher);
			if (cliDirectoryName != null)
			{
		        VirtualChannel vChannel = launcher.getChannel();
				globalCLIDirectory = new FilePath(vChannel,cliDirectoryName);
			}
		}
		
		if (globalCLIDirectory == null)
		{
        	throw new FileNotFoundException("ERROR: Topaz Workench CLI location was not specified. Check 'Compuware Configuration' section under 'Configure System'"); //$NON-NLS-1$
		}
		else
		{
			if (globalCLIDirectory.exists() == false) //NOSONAR
			{
		       	throw new FileNotFoundException("ERROR: Topaz Workench CLI location does not exist. Location: " + globalCLIDirectory.getRemote() + ". Check 'Compuware Configuration' section under 'Configure System'");  //NOSONAR
			}

			String cliScriptFile = globalCLIDirectory  + remoteFileSeparator + osScriptFile;
			listener.getLogger().println("Topaz for Total Test CLI script file path: " + cliScriptFile); //$NON-NLS-1$
			
	        VirtualChannel vChannel = launcher.getChannel();
			cliBatchFileRemote = new FilePath(vChannel, cliScriptFile);
			listener.getLogger().println("Topaz for Total Test CLI script file remote path: " + cliBatchFileRemote.getRemote()); //$NON-NLS-1$
			
			String cliVersion = CLIVersionUtils.getCLIVersion(globalCLIDirectory, TTT_MINIMUM_CLI_VERSION);
			CLIVersionUtils.checkCLICompatibility(cliVersion, TTT_MINIMUM_CLI_VERSION);
		}
		
		return cliBatchFileRemote;
	}
	
	/**
	 * Logs the Jenkins and Total Test Plugin versions
	 * 
	 * @param listener
	 *            Build listener
	 */
	private void logJenkinsAndPluginVersion(final TaskListener listener)
	{
		listener.getLogger().println("Jenkins Version: " + Jenkins.VERSION);
		Jenkins jenkinsInstance = Jenkins.getInstance();
		if (jenkinsInstance != null) //NOSONAR
		{
			Plugin pluginV1 = jenkinsInstance.getPlugin("compuware-topaz-for-total-test"); //$NON-NLS-1$
			if (pluginV1 != null)
			{
				listener.getLogger().println("Topaz for Total Test Jenkins Plugin: " + pluginV1.getWrapper().getShortName() + " Version: " + pluginV1.getWrapper().getVersion());  //$NON-NLS-1$  //$NON-NLS-2$
			}
			else
			{
				Plugin pluginV2 = jenkinsInstance.getPlugin("compuware-totaltest");  //$NON-NLS-1$
				if (pluginV2 != null)
				{
					listener.getLogger().println("Topaz for Total Test Jenkins Plugin: " + pluginV2.getWrapper().getShortName() + " Version: " + pluginV2.getWrapper().getVersion()); //$NON-NLS-1$  //$NON-NLS-2$
				}
			}
		}
	}
	
	/**
	 * Adds to host related arguments to the argument list.
	 * <p>
	 * The following argument are added:
	 * <ul>
	 * <li>Host
	 * <li>Port>
	 * <li>Code Page
	 * <li>User id
	 * <li>Password
	 * </ul>
	 * 
	 * @param build
	 *			  The current running Jenkins build
	 * @param args
	 * 			An instance of <code>ArgumentListBuilder</code> containing the arguments.
	 * @param isShell
	 * 			<code>true</code> if running a shell script, otherwise <code>false</code>.
	 * 
	 * @throws IOException
	 * 			If not host connection defined.
	 */
	@SuppressWarnings("deprecation")
	private void addHostArguments(final Run<?,?> build, final ArgumentListBuilder args, final boolean isShell) throws IOException
	{
		String host = null;
		String port = null;
		String codePage = DEFAULT_CODE_PAGE;
		String protocol = null;
		
		HostConnection connection = null;
		CpwrGlobalConfiguration globalConfig = CpwrGlobalConfiguration.get();
		if (globalConfig != null)
		{
			connection = globalConfig.getHostConnection(tttBuilder.getConnectionId());
		}
		
		if ((connection == null) && (tttBuilder.getHostPort() == null)) //NOSONAR
		{
			throw new IOException("ERROR: No host connection defined. Check project and global configurations to unsure host connection is set.");
		}
		else if (connection != null)
		{
			host = connection.getHost();
			port = connection.getPort();
			codePage = connection.getCodePage();
			protocol = connection.getProtocol();
			if (codePage == null|| codePage.length() == 0)
			{
				codePage = DEFAULT_CODE_PAGE;
			}
			
			if (protocol == null || protocol.isEmpty())
			{
				protocol = "None"; //NOSONAR
			}
		}
		else if (tttBuilder.getHostPort() != null) //NOSONAR
		{
			String[] hostAndPort = tttBuilder.getHostPort().split(":"); //NOSONAR
			if (hostAndPort.length == 2)
			{
				host = hostAndPort[0];
				port = hostAndPort[1];
			}
			else
			{
				throw new IOException("ERROR: Invalid host information. Check project and global configurations to unsure host connection is set.");
			}
		}
		
		addArgument(args, HOST, host, isShell);
		addArgument(args, PORT, port, isShell);
		addArgument(args, TARGET_ENCODING, codePage, isShell);
		addArgument(args, PROTOCOL, protocol, isShell);
		addArgument(args, USER, TotalTestRunnerUtils.getLoginInformation(build.getParent(), tttBuilder.getCredentialsId()).getUsername(), isShell);
		addArgument(args, PASSWORD, TotalTestRunnerUtils.getLoginInformation(build.getParent(), tttBuilder.getCredentialsId()).getPassword().getPlainText(), isShell, true);
	}
	
	/**
	 * Adds the project arguments to the list of arguments.
	 * <p>
	 * The following arguments are added:
	 * <ul>
	 * <li>Project folder
	 * <li>Test suite or test suite list
	 * <li>JCL
	 * </ul>
	 * 
	 * @param args
	 * 			An instance of <code>ArgumentListBuilder</code> containing the arguments.
	 * @param isShell
	 * 			<code>true</code> if running a shell script, otherwise <code>false</code>.
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	private void addProjectArguments(final Launcher launcher, final String workspaceFilePath, final ArgumentListBuilder args, final boolean isShell) throws IOException, InterruptedException
	{
		FilePath projectPath = null;
		
		if (tttBuilder.getProjectFolder() != null)
		{
			File projectFile = new File(tttBuilder.getProjectFolder());
			if (projectFile.isAbsolute())
			{
				VirtualChannel vChannel = launcher.getChannel();
				FilePath absProjectPath = new FilePath(vChannel, projectFile.getAbsolutePath());
				if (absProjectPath.exists() && absProjectPath.isDirectory())
				{
					projectPath = absProjectPath;
				}
				else
				{
					throw new IOException("ERROR: Test Project Folder '" + projectFile.getAbsolutePath() + "' does not exist or is not a directory.");
				}
			}
			else
			{
				VirtualChannel vChannel = launcher.getChannel();
				File workspaceProjecFile = new File(workspaceFilePath + tttBuilder.getProjectFolder());
				FilePath workspaceProjectPath = new FilePath(vChannel, workspaceProjecFile.getAbsolutePath());
				if (workspaceProjectPath.exists() && workspaceProjectPath.isDirectory())
				{
					projectPath = workspaceProjectPath;
				}
				else
				{
					throw new IOException("ERROR: Test Project Folder '" + workspaceProjecFile + "' does not exist or is not a directory.");
				}
			}
		}
		else
		{
			throw new IOException("ERROR: 'Test Project Folder' was not specified.");
		}
		
		addArgument(args,PROJECT, projectPath.getRemote(), isShell);
		 
		String testSuiteEntry = tttBuilder.getTestSuite();
		if (TotalTestRunnerUtils.isAllTestScenariosOrSuites(testSuiteEntry) || TotalTestRunnerUtils.isTestNameList(testSuiteEntry))
		{
			addArgument(args, TEST_NAME_LIST, testSuiteEntry, isShell);
		}
		else
		{
			addArgument(args, TESTSUITE, testSuiteEntry, isShell);
		}
		
		addArgument(args, JCL, tttBuilder.getJcl(), isShell);
	}
	
	/**
	 * Adds the Code Coverage arguments to the list of arguments.
	 * 
	 * The following arguments are added:
	 * <ul>
	 * <li>Repository name
	 * <li>System
	 * <li>Test Id
	 * <li>Program type
	 * <li>Clear statistics
	 * </ul>
	 * 
	 * @param args
	 * 			An instance of <code>ArgumentListBuilder</code> containing the arguments.
	 * @param isShell
	 * 			<code>true</code> if running a shell script, otherwise <code>false</code>.
	 */
	private void addCodeCoverageArguments(final ArgumentListBuilder args, final boolean isShell)
	{
		String ccRepo = tttBuilder.getCcRepo();
		if ((ccRepo != null) && (ccRepo.length() != 0))
		{
			addArgument(args, CODE_COVERAGE_REPO, ccRepo.toUpperCase(), isShell);
			
			String ccSystem = (tttBuilder.getCcSystem() != null ? tttBuilder.getCcSystem().toUpperCase() : null);
			if ((ccSystem != null) && (ccSystem.length() != 0))
			{
				addArgument(args, CODE_COVERAGE_SYSTEM, ccSystem, isShell);
			}
			
			String ccTestId = (tttBuilder.getCcTestId() != null ? tttBuilder.getCcTestId().toUpperCase() : null);
			if ((ccTestId != null) && (ccTestId.length() != 0))
			{
				addArgument(args, CODE_COVERAGE_TESTID, ccTestId, isShell);
			}
			
			addArgument(args, CODE_COVERAGE_TYPE, tttBuilder.getCcPgmType(), isShell);

			
			addArgument(args, CODE_COVERAGE_CLEAR, Boolean.toString(tttBuilder.isCcClearStats()), isShell);
		}
	}

	/**
	 * Adds the Execution arguments to the list of arguments.
	 * 
	 * The following arguments are added:
	 * <ul>
	 * <li>Dataset high-level qualifier
	 * <li>User stubs
	 * <li>Delete temporary datasest/files.
	 * </ul>
	 * 
	 * @param args
	 * 			An instance of <code>ArgumentListBuilder</code> containing the arguments.
	 * @param isShell
	 * 			<code>true</code> if running a shell script, otherwise <code>false</code>.
	 */	
	private void addExecutionArguments(final ArgumentListBuilder args, final boolean isShell)
	{
		String dsnhlq =  tttBuilder.getHlq();
		if ((dsnhlq != null) && (dsnhlq.length() != 0))
		{
			addArgument(args, DSN_HLQ , dsnhlq.toUpperCase(), isShell);
		}
		
		addArgument(args, USE_STUBS, Boolean.toString(tttBuilder.isUseStubs()), isShell);
		
		addArgument(args, DELETE_TEMPORARY, Boolean.toString(tttBuilder.isDeleteTemp()), isShell);
	}
	
	/**
	 * Adds the Execution arguments to the list of arguments.
	 * 
	 * The following arguments are added:
	 * <ul>
	 * <li>External tools
	 * <li>Post Run Commands
	 * </ul>
	 * 
	 * @param workspaceFilePath
	 * 			An instance of <code>FilePath</code> for the workspace directory
	 * @param args
	 * 			An instance of <code>ArgumentListBuilder</code> containing the arguments.
	 * @param isShell
	 * 			<code>true</code> if running a shell script, otherwise <code>false</code>.
	 */	
	private void addExternalToolArguments(final FilePath workspaceFilePath, final ArgumentListBuilder args, final boolean isShell)
	{
		addArgument(args, EXTERNAL_TOOLS_WS, workspaceFilePath.getRemote(), isShell);
		addArgument(args, POST_RUN_COMMANDS, COPY_JUNIT + COMMA + COPY_SONAR, isShell);
	}
	
	/**
	 * Adds an argument to the argument list.
	 * 
	 * @param args
	 * 			An instance of <code>ArgumentListBuilder</code> for building the argument list.
	 * @param argument
	 * 			The argument name.
	 * @param argumentValue
	 * 			The argument value.
	 * @param isShell
	 * 			<code>true</code> if running a shell script, otherwise <code>false</code>.
	 * @param mask
	 * 			<code>true</code> to mask value when output, <code>true</code> to display normally
	 */
	private void addArgument(final ArgumentListBuilder args, final String argument, final String argumentValue, final boolean isShell)
	{
		addArgument(args, argument, argumentValue, isShell, false);
	}
	
	/**
	 * Adds an argument to the argument list.
	 * 
	 * @param args
	 * 			An instance of <code>ArgumentListBuilder</code> for building the argument list.
	 * @param argument
	 * 			The argument name.
	 * @param argumentValue
	 * 			The argument value.
	 * @param isShell
	 * 			<code>true</code> if running a shell script, otherwise <code>false</code>.
	 * @param mask
	 * 			<code>true</code> to mask value when output, <code>true</code> to display normally
	 */
	private void addArgument(final ArgumentListBuilder args, final String argument, final String argumentValue, final boolean isShell, boolean mask)
	{
		args.add(TotalTestRunnerUtils.escapeForScript(argument + "=" + argumentValue, isShell), mask); //$NON-NLS
	}
}

