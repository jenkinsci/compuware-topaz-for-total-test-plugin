/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015-2020 Compuware Corporation
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import com.compuware.jenkins.common.configuration.HostConnection;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.util.ArgumentListBuilder;

public class TotalTestRunner
{
	private static final String TOTAL_TEST_CLI_BAT = "TotalTestFTCLI.bat"; //$NON-NLS-1$
	private static final String TOTAL_TEST_CLI_SH = "TotalTestFTCLI.sh"; //$NON-NLS-1$
	public static final String TOPAZ_CLI_WORKSPACE = "TopazCliWkspc"; //$NON-NLS-1$
	
	private static final String HOST = "-host"; //$NON-NLS-1$
	private static final String PORT = "-port"; //$NON-NLS-1$
	private static final String USER= "-userid"; //$NON-NLS-1$
	private static final String PASSWORD = "-p"; //$NON-NLS-1$ //NOSONAR
	private static final String ENCRYPTION_PROTOCOL = "-encrypt"; //$NON-NLS-1$
	private static final String CODE_PAGE = "-code-page";  //$NON-NLS-1$
	
	private static final String ROOT = "-root-folder";   //$NON-NLS-1$
	private static final String FILE = "-file";   //$NON-NLS-1$
	private static final String PROGRAM_NAMES = "-program-names";   //$NON-NLS-1$
	
	private static final String JCL = "-j"; //$NON-NLS-1$
	private static final String DATA = "-data"; //$NON-NLS-1$
	private static final String DSN_HLQ = "-dsnhlq"; //$NON-NLS-1$
	
	private static final String CODE_COVERAGE_REPO = "-ccrepository"; //$NON-NLS-1$
	private static final String CODE_COVERAGE_SYSTEM = "-ccsystem"; //$NON-NLS-1$
	private static final String CODE_COVERAGE_TESTID = "-cctestid"; //$NON-NLS-1$
	private static final String CODE_COVERAGE_TYPE = "-cctype"; //$NON-NLS-1$
	private static final String CODE_COVERAGE_CLEAR = "-ccclearstats"; //$NON-NLS-1$
	
	private static final String PROPERTY_FILE_SEPARATOR = "file.separator";  //$NON-NLS-1$
	private static final String DEFAULT_CODE_PAGE = "1047";  //$NON-NLS-1$

	private static final String LAUNCHER = "-launcher";  //$NON-NLS-1$
	private static final String JENKINS = "jenkins";  //$NON-NLS-1$
	
	private static final String SCENARIOS_DIR = "Scenarios"; //$NON-NLS-1$
	private static final String SUITES_DIR = "Suites"; //$NON-NLS-1$
	private static final String TESTSCENARIO = ".testscenario"; //$NON-NLS-1$
	
	private static final String JCL_DIR = "JCL"; //$NON-NLS-1$
	
	private static final String LOGLEVEL = "-loglevel";  //$NON-NLS-1$
	private static final String RECURSIVE = "-recursive";  //$NON-NLS-1$

	private final TotalTestBuilder tttBuilder;
    private String remoteFileSeparator = null;
	private TaskListener listener;
	private VirtualChannel vChannel;
	private boolean isLinux;
	
	/**
	 * Constructor
	 * 
	 * @param tttBuilder
	 * 			  An instance of <code>TotalTestBuilder</code> containing the arguments.
	 */
	public TotalTestRunner(TotalTestBuilder tttBuilder)
	{
		this.tttBuilder = tttBuilder;
	}
	
	/**
	 * Runs the Total Test Unit Test CLI
	 * 
	 * @param build
	 *			  The current running Jenkins build
	 * @param launcher
	 *            The machine that the files will be checked out.
	 * @param workspaceFilePath
	 *            a directory to check out the source code.
	 *            
	 * @return <code>boolean</code> if the build was successful
	 * 
	 * @throws IOException
	 * 			If an error occurred execute Total Test run.
	 * @throws InterruptedException
	 * 			If the Total Test run was interrupted.
	 */
	public boolean run(final Run<?,?> build, final Launcher launcher, final FilePath workspaceFilePath, final TaskListener taskListener) throws IOException, InterruptedException
	{
		listener = taskListener;
        ArgumentListBuilder args = new ArgumentListBuilder();
        EnvVars env = build.getEnvironment(listener);

        vChannel = launcher.getChannel();
        if (vChannel != null)
        {
        	Properties remoteProperties = vChannel.call(new RemoteSystemProperties());
            remoteFileSeparator = remoteProperties.getProperty(PROPERTY_FILE_SEPARATOR);
        }
        else
        {
        	remoteFileSeparator = File.separator;
        }
        
		isLinux = launcher.isUnix();
		String osScriptFile = isLinux ? TOTAL_TEST_CLI_SH : TOTAL_TEST_CLI_BAT;
		
		TotalTestRunnerUtils.logJenkinsAndPluginVersion(listener);
		
		FilePath cliScriptPath = TotalTestRunnerUtils.getCLIScriptPath(launcher, listener, remoteFileSeparator, osScriptFile);
		
		args.add(normalizeSlashes(cliScriptPath.getRemote()));
		
		String topazCliWorkspace = workspaceFilePath.getRemote() + remoteFileSeparator + TOPAZ_CLI_WORKSPACE;
		topazCliWorkspace = normalizeSlashes(topazCliWorkspace);
		listener.getLogger().println("Topaz for Total Test CLI workspace: " + topazCliWorkspace); //$NON-NLS-1$
		
		addArgument(args, LAUNCHER, JENKINS);
		
		addHostArguments(build, args);
		
		addProjectArguments(launcher, workspaceFilePath.getRemote() + remoteFileSeparator, args);
	
		addExecutionArguments(args);
		
		addCodeCoverageArguments(args);
				
		addArgument(args, DATA, topazCliWorkspace);
		
		FilePath workDir = new FilePath (vChannel, workspaceFilePath.getRemote());
		workDir.mkdirs();
		int exitValue = launcher.launch().cmds(args).envs(env).stdout(listener.getLogger()).pwd(workDir).join();

		listener.getLogger().println(osScriptFile + " exited with exit value = " + exitValue); //$NON-NLS-1$

		return exitValue == 0;
	}
	
	/**
	 * Adds to host related arguments to the argument list.
	 * <p>
	 * The following argument are added:
	 * <ul>
	 * <li>Host
	 * <li>Port
	 * <li>User id
	 * <li>Password
	 * <li>Code Page
	 * <li>Encryption Protocol
	 * </ul>
	 * 
	 * @param build
	 *			  The current running Jenkins build
	 * @param args
	 * 			An instance of <code>ArgumentListBuilder</code> containing the arguments.
	 * 
	 * @throws IOException
	 * 			If not host connection defined.
	 */
	@SuppressWarnings("deprecation")
	private void addHostArguments(final Run<?,?> build, final ArgumentListBuilder args) throws IOException
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
			throw new IOException("ERROR: No host connection defined. Check project and global configurations to unsure host connection is set."); //$NON-NLS-1$
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
				protocol = "None"; //NOSONAR //$NON-NLS-1$
			}
		}
		else if (tttBuilder.getHostPort() != null) //NOSONAR
		{
			String[] hostAndPort = tttBuilder.getHostPort().split(":"); //NOSONAR //$NON-NLS-1$
			if (hostAndPort.length == 2)
			{
				host = hostAndPort[0];
				port = hostAndPort[1];
			}
			else
			{
				throw new IOException("ERROR: No host connection defined. Check project and global configurations to unsure host connection is set."); //$NON-NLS-1$
			}
		}
		
		addArgument(args, HOST, host);
		addArgument(args, PORT, port);
		addArgument(args, CODE_PAGE, codePage);
		addArgument(args, ENCRYPTION_PROTOCOL, protocol);
		addArgument(args, USER, TotalTestRunnerUtils.getLoginInformation(build.getParent(), tttBuilder.getCredentialsId()).getUsername());
		addArgument(args, PASSWORD, TotalTestRunnerUtils.getLoginInformation(build.getParent(), tttBuilder.getCredentialsId()).getPassword().getPlainText(), true);
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
	 * @param launcher
	 *            The machine that the files will be checked out.
	 * @param workspaceFilePath
	 *            a directory to check out the source code.
	 * @param args
	 * 			An instance of <code>ArgumentListBuilder</code> containing the arguments.
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	private void addProjectArguments(final Launcher launcher, final String workspaceFilePath, final ArgumentListBuilder args) throws IOException, InterruptedException
	{
		FilePath projectPath = null;
		String projectFolder = tttBuilder.getProjectFolder();
		projectFolder = normalizeSlashes(projectFolder);
		
		if (projectFolder != null)
		{
			FilePath remoteProjectFolder = new FilePath(vChannel, projectFolder);
			boolean isAbsolute = remoteProjectFolder.absolutize().getRemote().equalsIgnoreCase(remoteProjectFolder.getRemote());
			
			if (isAbsolute)
			{
				if (remoteProjectFolder.exists() && remoteProjectFolder.isDirectory())
				{
					projectPath = remoteProjectFolder;
				}
				else
				{
					throw new IOException("ERROR: Test Project Folder '" + remoteProjectFolder.getRemote() + "' does not exist or is not a directory.");  //$NON-NLS-1$//$NON-NLS-2$
				}
			}
			else
			{
				FilePath workspaceProjectPath = new FilePath(new FilePath(vChannel, workspaceFilePath).absolutize(), projectFolder);
				FilePath absolutizeWorkspaceProjectPath = workspaceProjectPath.absolutize();
				if (absolutizeWorkspaceProjectPath.exists() && absolutizeWorkspaceProjectPath.isDirectory())
				{
					projectPath = absolutizeWorkspaceProjectPath;
				}
				else
				{
					throw new IOException("ERROR: Test Project Folder '" + absolutizeWorkspaceProjectPath.getRemote() + "' does not exist or is not a directory."); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
		else
		{
			throw new IOException("ERROR: 'Test Project Folder' was not specified."); //$NON-NLS-1$
		}
		
		listener.getLogger().println("Project Folder: " + projectPath.getRemote()); //$NON-NLS-1$
		List<FilePath> testProjects = new ArrayList<>();
		locateTestFolders(projectPath, testProjects);
		if (testProjects.size() == 0)
		{
			throw new IOException("ERROR: Test Project '" + projectPath.getRemote() + "' does not contain any test folders."); //$NON-NLS-1$ //$NON-NLS-2$
		}
		else if (testProjects.size() == 1)
		{
			 projectPath = testProjects.get(0);
		}
		else if (!tttBuilder.isRecursive())
		{
			throw new IOException("ERROR: Test Project '" + projectPath.getRemote() + "' containw any multiple test folders. Specify a test folder or set recursion."); //$NON-NLS-1$ //$NON-NLS-2$
		}

		
		listener.getLogger().println("Test Folder: " + projectPath.getRemote()); //$NON-NLS-1$
		String testSuiteEntry = tttBuilder.getTestSuite();
		if (TotalTestRunnerUtils.isSpecicalTestName(testSuiteEntry) || TotalTestRunnerUtils.isTestNameList(testSuiteEntry))
		{
			addArgument(args,FILE, projectPath.getRemote());
			 
			addArgument(args, PROGRAM_NAMES, testSuiteEntry);
			tttBuilder.setRecursive(true);
		}
		else
		{
			addArgument(args,ROOT, projectPath.getRemote());
			
			String testPath = null;
			if (testSuiteEntry.endsWith(TESTSCENARIO))
			{
				testPath = SCENARIOS_DIR + remoteFileSeparator + testSuiteEntry;
			}
			else
			{
				testPath = SUITES_DIR + remoteFileSeparator + testSuiteEntry;
			}
			
			addArgument(args, FILE, testPath);
		}
		
		String jcl = tttBuilder.getJcl();
		jcl = normalizeSlashes(jcl);
		FilePath jclFilePath = new FilePath(vChannel, jcl);
		boolean isJclAbsolute = jclFilePath.absolutize().getRemote().equalsIgnoreCase(jclFilePath.getRemote());
		if (isJclAbsolute)
		{
			addArgument(args, JCL, jcl);
		}
		else
		{
			String jclPath = JCL_DIR + remoteFileSeparator + tttBuilder.getJcl();
			addArgument(args, JCL, jclPath);
		}
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
	 */
	private void addCodeCoverageArguments(final ArgumentListBuilder args)
	{
		String ccRepo = tttBuilder.getCcRepo();
		if ((ccRepo != null) && (ccRepo.length() != 0))
		{
			addArgument(args, CODE_COVERAGE_REPO, ccRepo.toUpperCase());
			
			String ccSystem = (tttBuilder.getCcSystem() != null ? tttBuilder.getCcSystem().toUpperCase() : null);
			if ((ccSystem != null) && (ccSystem.length() != 0))
			{
				addArgument(args, CODE_COVERAGE_SYSTEM, ccSystem);
			}
			
			String ccTestId = (tttBuilder.getCcTestId() != null ? tttBuilder.getCcTestId().toUpperCase() : null);
			if ((ccTestId != null) && (ccTestId.length() != 0))
			{
				addArgument(args, CODE_COVERAGE_TESTID, ccTestId);
			}
			
			addArgument(args, CODE_COVERAGE_TYPE, tttBuilder.getCcPgmType());
			
			addArgument(args, CODE_COVERAGE_CLEAR, Boolean.toString(tttBuilder.isCcClearStats()));
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
	 */	
	private void addExecutionArguments(final ArgumentListBuilder args)
	{
		String dsnhlq =  tttBuilder.getHlq();
		if ((dsnhlq != null) && (dsnhlq.length() != 0))
		{
			addArgument(args, DSN_HLQ , dsnhlq.toUpperCase());
		}
		
//ftcli		addArgument(args, USE_STUBS, Boolean.toString(tttBuilder.isUseStubs()));
//ftcli		addArgument(args, DELETE_TEMPORARY, Boolean.toString(tttBuilder.isDeleteTemp()));
		
		if (tttBuilder.isDeleteTemp())
		{
			addArgument(args, LOGLEVEL, "INFO");
		}
		else
		{
			addArgument(args, LOGLEVEL, "DEBUG");
		}
		
		if (tttBuilder.isRecursive())
		{
			args.add(RECURSIVE);
		}
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
	 */
	private void addArgument(final ArgumentListBuilder args, final String argument, final String argumentValue)
	{
		addArgument(args, argument, argumentValue, false);
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
	 * @param mask
	 * 			<code>true</code> to mask value when output, <code>true</code> to display normally
	 */
	private void addArgument(final ArgumentListBuilder args, final String argument, final String argumentValue, boolean mask)
	{
		args.add(argument).add(TotalTestRunnerUtils.escapeForScript(argumentValue), mask); //$NON-NLS //$NON-NLS-1$
	}
	
	/**
	 * Scans the specified directory and all sub-directories to locate Unit Test test folders.
	 * 
	 * @param directory
	 * 			An instance of <code>FilePath</code> for the directory to be scanned
	 * @param testProjects
	 * 			A list of <code>File</code> instance for each found Unit Test test folder.
	 */
	private void locateTestFolders(final FilePath directory, final List<FilePath> testProjects)
	{
		try
		{
			List<FilePath> fileList =  directory.list();
			if (fileList != null)
			{
				for (FilePath file : fileList)
				{
					if (file.isDirectory())
					{
						if (isTestFolder(file))
						{
							testProjects.add(file);
						}
						else
						{
							locateTestFolders(file, testProjects);
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			listener.getLogger().println("Exception: " + e.toString()); //$NON-NLS-1$
		}
	}

	/**
	 * Returns whether the specified folder is a Unit Test folder.
	 * 
	 * @param folderPath
	 * 			An instance of <codeFilePath</code> for the specified folder.
	 * 
	 * @return	<code>true</code> if the folder is a Unit Test folder, otherwise <code>false</code>.
	 */
	private boolean isTestFolder(final FilePath folderPath)
	{
		boolean isTestFolder = false;
		try
		{
			List<FilePath> subFolders =  folderPath.list();
			if (subFolders != null && !subFolders.isEmpty() && subFolders.size() >= 3)
			{
				String structuresDirName = folderPath + remoteFileSeparator + "Structures";
				boolean structuresDirExist = doesDirectoryExist(structuresDirName);
				if (structuresDirExist)
				{
					String interfacesDirName = folderPath + remoteFileSeparator + "Interfaces";
					boolean interfacesDirExist = doesDirectoryExist(interfacesDirName);
					if (interfacesDirExist)
					{
						String scenariosDirName = folderPath + remoteFileSeparator + "Scenarios";
						boolean scenariosDirExist = doesDirectoryExist(scenariosDirName);
						if (scenariosDirExist)
						{
							isTestFolder = true;
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			listener.getLogger().println("Exception: " + e.toString()); //$NON-NLS-1$
			isTestFolder = false;
		}
		
		return isTestFolder;
				
	}
	
	/**
	 * Returns whether a directory exists.
	 * 
	 * @param directoryName
	 * 			The name of the directory to check.
	 * 
	 * @return	<code>true</code> if the directory exists, otherwise <code>false</code>.
	 */
	private boolean doesDirectoryExist(String directoryName)
	{
		boolean directoryExists = false;
		
		FilePath directoryFilePath = new FilePath(vChannel, directoryName);
		try
		{
			if (directoryFilePath != null && directoryFilePath.exists() && directoryFilePath.isDirectory())
			{
				directoryExists = true;
			}
		}
		catch (IOException | InterruptedException e)
		{
			listener.getLogger().println("Exception: " + e.toString()); //$NON-NLS-1$
			directoryExists = false;
		}
		
		return directoryExists;
	}
	
	/**
	 * Normalizes the slash in a file path.
	 * 
	 * @param filepath
	 * 			The file path to be mormallized.
	 * 
	 * @return	The normalized file path.
	 */
	private String normalizeSlashes(final String filePath)
	{
		if (!isLinux)
		{
			return filePath.replace("/", remoteFileSeparator);
		}
		else
		{
			return filePath.replace("\\", remoteFileSeparator);
		}
	}
}

