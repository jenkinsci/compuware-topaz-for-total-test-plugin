package com.compuware.jenkins.totaltest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import com.compuware.jenkins.common.configuration.HostConnection;

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
	private static final String COMMA = ","; //$NON-NLS-1$
	private static final String COPY_JUNIT = "copyjunit"; //$NON-NLS-1$
	private static final String COPY_SONAR = "copysonar"; //$NON-NLS-1$

	private static final String TOTAL_TEST_CLI_BAT = "TotalTestCLI.bat"; //$NON-NLS-1$
	private static final String TOTAL_TEST_CLI_SH = "TotalTestCLI.sh"; //$NON-NLS-1$
	public static final String TOPAZ_CLI_WORKSPACE = "TopazCliWkspc"; //$NON-NLS-1$
	
	private static final String COMMAND_PARM = "-cmd=runtest"; //$NON-NLS-1$
	private static final String HOST_PARM = "-host="; //$NON-NLS-1$
	private static final String PORT_PARM = "-port="; //$NON-NLS-1$
	private static final String USER_PARM = "-user="; //$NON-NLS-1$
	private static final String PASSWORD_PARM = "-pw="; //$NON-NLS-1$ //NOSONAR
	private static final String PROJECT_PARM = "-project="; //$NON-NLS-1$
	private static final String TESTSUITE_PARM = "-ts="; //$NON-NLS-1$
	private static final String JCL_PARM = "-jcl="; //$NON-NLS-1$
	private static final String EXTERNAL_TOOLS_WS_PARM = "-externaltoolsws=";  //$NON-NLS-1$
	private static final String POST_RUN_COMMANDS = "-postruncommands=";  //$NON-NLS-1$
	private static final String TEST_NAME_LIST_PARM = "-testsuitelist=";  //$NON-NLS-1$
	private static final String DATA_PARM = "-data"; //$NON-NLS-1$
	private static final String DSN_HLQ_PARM = "-dsnhlq="; //$NON-NLS-1$
	
	private static final String CODE_COVERAGE_REPO = "-ccrepo="; //$NON-NLS-1$
	private static final String CODE_COVERAGE_SYSTEM = "-ccsystem="; //$NON-NLS-1$
	private static final String CODE_COVERAGE_TESTID = "-cctestid="; //$NON-NLS-1$
	private static final String CODE_COVERAGE_DB2 = "-ccdb2="; //$NON-NLS-1$
	
	private static final String USE_STUBS = "-usestubs="; //$NON-NLS-1$
	private static final String DELETE_TEMPORARY = "-deletetemp="; //$NON-NLS-1$
	private static final String TARGET_ENCODING = "-targetencoding="; //$NON-NLS-1$
	
	private static final String PROPERTY_FILE_SEPARATOR = "file.separator";
	private static final String DEFAULT_CODE_PAGE = "1047";  //$NON-NLS-1$
	
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
		String osFile = isShell ? TOTAL_TEST_CLI_SH : TOTAL_TEST_CLI_BAT;
		
		logJenkinsAndPluginVersion(listener);
        
		FilePath cliDirectory = getCLIDirectory(launcher);
		
		String cliScriptFile = cliDirectory  + remoteFileSeparator + osFile;
		listener.getLogger().println("Topaz for Total Test CLI script file path: " + cliScriptFile); //$NON-NLS-1$
		
		FilePath cliBatchFileRemote = new FilePath(vChannel, cliScriptFile);
		listener.getLogger().println("Topaz for Total Test CLI script file remote path: " + cliBatchFileRemote.getRemote()); //$NON-NLS-1$
		
		args.add(cliBatchFileRemote.getRemote());
		
		String topazCliWorkspace = workspaceFilePath.getRemote() + remoteFileSeparator + TOPAZ_CLI_WORKSPACE;
		listener.getLogger().println("Topaz for Total Test CLI workspace: " + topazCliWorkspace); //$NON-NLS-1$
		
		args.add(COMMAND_PARM);
		
		addHostArguments(build, args, isShell);
		
		addProjectArguments(args,isShell);
		
		addExecutionArguments(args, isShell);
		
		addCodeCoverageArguments(args, isShell);
		
		addExternalToolArguments(workspaceFilePath, args, isShell);
		
		String data = TotalTestRunnerUtils.escapeForScript(topazCliWorkspace, isShell);
		args.add(DATA_PARM, data);
		
		FilePath workDir = new FilePath (vChannel, workspaceFilePath.getRemote());
		workDir.mkdirs();
		int exitValue = launcher.launch().cmds(args).envs(env).stdout(listener.getLogger()).pwd(workDir).join();

		listener.getLogger().println(osFile + " exited with exit value = " + exitValue); //$NON-NLS-1$ //$NON-NLS-2$

		return exitValue == 0;
	}
	
	/**
	 * Returns the Total Test Workbench CLI directory.
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
	private FilePath getCLIDirectory(final Launcher launcher) throws IOException, InterruptedException
	{
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
		}
		
		return globalCLIDirectory;
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
			host = TotalTestRunnerUtils.escapeForScript(HOST_PARM + connection.getHost(), isShell);
			port = TotalTestRunnerUtils.escapeForScript(PORT_PARM + connection.getPort(), isShell);
			codePage = connection.getCodePage();
			if ((codePage == null) || (codePage.length() == 0))
			{
				codePage = DEFAULT_CODE_PAGE;
			}
		}
		else if (tttBuilder.getHostPort() != null) //NOSONAR
		{
			String[] hostAndPort = tttBuilder.getHostPort().split(":"); //NOSONAR
			if (hostAndPort.length == 2)
			{
				host = TotalTestRunnerUtils.escapeForScript(HOST_PARM + hostAndPort[0], isShell);
				port = TotalTestRunnerUtils.escapeForScript(PORT_PARM + hostAndPort[1], isShell);
			}
			else
			{
				throw new IOException("ERROR: Invalid host information. Check project and global configurations to unsure host connection is set.");
			}
		}
		
		String targetEncoding = TotalTestRunnerUtils.escapeForScript(TARGET_ENCODING + codePage, isShell);
		String user = TotalTestRunnerUtils.escapeForScript(USER_PARM + TotalTestRunnerUtils.getLoginInformation(build.getParent(), tttBuilder.getCredentialsId()).getUsername(), isShell);
		String password = TotalTestRunnerUtils.escapeForScript(PASSWORD_PARM + TotalTestRunnerUtils.getLoginInformation(build.getParent(), tttBuilder.getCredentialsId()).getPassword().getPlainText(), isShell);

		args.add(host);
		args.add(port);
		args.add(targetEncoding);
		args.add(user);
		args.add(password, true);
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
	 * @param args
	 * 			An instance of <code>ArgumentListBuilder</code> containing the arguments.
	 * @param isShell
	 * 			<code>true</code> if running a shell script, otherwise <code>false</code>.
	 */
	private void addProjectArguments(final ArgumentListBuilder args, final boolean isShell)
	{
		String projectFolder = TotalTestRunnerUtils.escapeForScript(PROJECT_PARM + tttBuilder.getProjectFolder(), isShell);
		 
		String testSuiteInfo;
		String testSuiteEntry = tttBuilder.getTestSuite();
		if (TotalTestRunnerUtils.isAllTestScenariosOrSuites(testSuiteEntry) || TotalTestRunnerUtils.isTestNameList(testSuiteEntry))
		{
			testSuiteInfo = TotalTestRunnerUtils.escapeForScript(TEST_NAME_LIST_PARM + testSuiteEntry, isShell);
		}
		else
		{
			testSuiteInfo = TotalTestRunnerUtils.escapeForScript(TESTSUITE_PARM + testSuiteEntry, isShell);
		}
		
		String jcl = TotalTestRunnerUtils.escapeForScript(JCL_PARM + tttBuilder.getJcl(), isShell);

		args.add(projectFolder);
		args.add(testSuiteInfo);
		args.add(jcl);
	}
	
	/**
	 * Adds the Code Coverage arguments to the list of arguments.
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
			args.add(TotalTestRunnerUtils.escapeForScript(CODE_COVERAGE_REPO + ccRepo.toUpperCase(), isShell));
			
			String ccSystem = tttBuilder.getCcSystem();
			if ((ccSystem != null) && (ccSystem.length() != 0))
			{
				args.add(TotalTestRunnerUtils.escapeForScript(CODE_COVERAGE_SYSTEM + ccSystem, isShell)); //$NON-NLS-1$
			}
			
			String ccTestId = tttBuilder.getCcTestId();
			if ((ccTestId != null) && (ccTestId.length() != 0))
			{
				args.add(TotalTestRunnerUtils.escapeForScript(CODE_COVERAGE_TESTID + ccTestId, isShell)); //$NON-NLS-1$
			}
			
			if (tttBuilder.getCcDB2())
			{
				args.add(TotalTestRunnerUtils.escapeForScript(CODE_COVERAGE_DB2 + Boolean.toString(tttBuilder.getCcDB2()), isShell)); //$NON-NLS-1$
			}
			
		}
	}

	/**
	 * Adds the Execution arguments to the list of arguments.
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
			args.add(TotalTestRunnerUtils.escapeForScript(DSN_HLQ_PARM + dsnhlq.toUpperCase(), isShell));
		}
		
		if (tttBuilder.isUseStubs() == false) //NOSONAR
		{
			args.add(TotalTestRunnerUtils.escapeForScript(USE_STUBS + "false", isShell)); //$NON-NLS-1$
		}
		
		if (tttBuilder.isDeleteTemp() == false) //NOSONAR
		{
			args.add(TotalTestRunnerUtils.escapeForScript(DELETE_TEMPORARY + "false", isShell)); //$NON-NLS-1$
		}
	}
	
	/**
	 * Adds the Execution arguments to the list of arguments.
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
		String externalToolsWS = TotalTestRunnerUtils.escapeForScript(EXTERNAL_TOOLS_WS_PARM + workspaceFilePath.getRemote(), isShell);
		String externalTool = TotalTestRunnerUtils.escapeForScript(POST_RUN_COMMANDS + COPY_JUNIT + COMMA + COPY_SONAR, isShell);
		args.add(externalToolsWS);
		args.add(externalTool);
	}
}
