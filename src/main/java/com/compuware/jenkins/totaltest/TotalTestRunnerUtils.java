/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015-2020 Compuware Corporation
 * (c) Copyright 2019-2020 & 2020-2022BMC Software, Inc.
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.matchers.IdMatcher;
import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import com.compuware.jenkins.common.utils.CLIVersionUtils;

import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.Plugin;
import hudson.model.Item;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.security.ACL;
import jenkins.MasterToSlaveFileCallable;
import jenkins.model.Jenkins;

public class TotalTestRunnerUtils
{
	public static final String TTT_MINIMUM_CLI_VERSION = "20.09.02"; //$NON-NLS-1$
	public static final String TTT_NEW_EXTENSIONS_CLI_VERSION = "20.02.01"; //$NON-NLS-1$
	public static final String TTT_OUTPUTFOLDER_CLI_VERSION = "20.03.01"; //$NON-NLS-1$
	public static final String TTT_CLI_200401 = "20.04.01"; //$NON-NLS-1$
	public static final String TTT_CLI_200501 = "20.05.01"; //$NON-NLS-1$

	private static final String QUESTION = "?"; //$NON-NLS-1$
	private static final String ASTERISK = "*"; //$NON-NLS-1$
	private static final String COMMA = ","; //$NON-NLS-1$
	private static final String COLON = ":"; //$NON-NLS-1$
	private static final String DOUBLE_QUOTE = "\""; //$NON-NLS-1$
	private static final String DOUBLE_QUOTE_ESCAPED = "\"\""; //$NON-NLS-1$
	
	private static final String ALL_SCENARIOS = "ALL_SCENARIOS"; //$NON-NLS-1$
	private static final String ALL_SUITES = "ALL_SUITES"; //$NON-NLS-1$
	private static final String AUTO_SELECT = "AUTO_SELECT"; //$NON-NLS-1$
	
	/**
	 * Gets the host name;
	 * 
	 * @param hostPort
	 * 			A string containing host:port.
	 * 
	 * @return <code>String</code> the host name
	 */
	public static String getHost(final String hostPort)
	{
		String host = hostPort;

		int index = host.indexOf(':');
		if (index > 0)
		{
			host = host.substring(0, index);
		}

		return host;
	}

	/**
	 * Gets the port for the host connection.
	 * 
	 * @param hostPort
	 * 			A string containing host:port.
	 * 
	 * @return <code>String</code> the port
	 */
	public static String getPort(final String hostPort)
	{
		String port = hostPort;

		int index = port.indexOf(':');
		if (index > 0)
		{
			port = port.substring(index + 1);
		}

		return port;
	}

	/**
	 * Retrieves login information given a credential ID
	 * 
	 * @param project
	 *			the Jenkins project
	 * @param credentialsId
	 * 			  The credendtial id for the user.
	 * 
	 * @return a Jenkins credential with login information
	 */
	public static StandardUsernamePasswordCredentials getLoginInformation(Item project, String credentialsId)
	{
		StandardUsernamePasswordCredentials credential = null;

		List<StandardUsernamePasswordCredentials> credentials = CredentialsProvider
				.lookupCredentials(StandardUsernamePasswordCredentials.class, project, ACL.SYSTEM,
						Collections.<DomainRequirement> emptyList());

		IdMatcher matcher = new IdMatcher(credentialsId);
		for (StandardUsernamePasswordCredentials cred : credentials)
		{
			if (matcher.matches(cred))
			{
				credential = cred;
			}
		}

		return credential;
	}

	/**
	 * Validate that there is a host and port defined.
	 * 
	 * @param listener
	 *            Task listener
	 * @param hostPortValue
	 * 			The host and port. Format(host:port)
	 */
	public static void validateHostPort(final TaskListener listener, final String hostPortValue)
	{
		String trimmedValue =  StringUtils.trimToEmpty(hostPortValue);
		if (trimmedValue.isEmpty())
		{
			throw new IllegalArgumentException(Messages.checkHostPortEmptyError());
		}
		else
		{
			String[] hostPort = trimmedValue.split(COLON);
			if (hostPort.length != 2)
			{
				throw new IllegalArgumentException(Messages.invalidParameterValueError(Messages.hostPort(), hostPort));
			}
			else
			{
				String host = StringUtils.trimToEmpty(hostPort[0]);
				if (host.isEmpty())
				{
					throw new IllegalArgumentException(Messages.invalidParameterValueError(Messages.hostPort(), hostPort));
				}

				String port = StringUtils.trimToEmpty(hostPort[1]);
				if (port.isEmpty())
				{
					throw new IllegalArgumentException(Messages.invalidParameterValueError(Messages.hostPort(), hostPort));
				}
				else if (StringUtils.isNumeric(port) == false) //NOSONAR
				{
					throw new IllegalArgumentException(Messages.checkHostPortInvalidPorttError());
				}
			}
		}
		
		listener.getLogger().println(Messages.hostPort() + " = " + hostPortValue); //$NON-NLS-1$
	}
	
	
	/**
	 * Returns an escaped version of the given input String for a Batch or Shell script.
	 * 
	 * @param input
	 *            the <code>String</code> to escape
	 * 
	 * @return the escaped <code>String</code>
	 */
	public static String escapeForScript(final String input)
	{
		String output = null;

		if (input != null)
		{
			// escape any double quotes (") with another double quote (") for both batch and shell scripts
			output = StringUtils.replace(input, DOUBLE_QUOTE, DOUBLE_QUOTE_ESCAPED);
		}

		return output;
	}
	
	/**
	 * Checks if the specified test name is ALL_SCENARIOS or ALL_SUITES
	 * <p>
	 * The check is case insensitive.
	 * 
	 * @param testName
	 * 			The test name to check.
	 * 
	 * @return	<code>true</code> if either ALL_SCENARIOS or ALL_SUITES was specified, otherwise <code>false</code>.
	 */
	public static boolean isSpecicalTestName(String testName)
	{
		if (ALL_SCENARIOS.equalsIgnoreCase(testName) || ALL_SUITES.equalsIgnoreCase(testName) || AUTO_SELECT.equalsIgnoreCase(testName))
		{ 
			return true;
		}
		
		return false;
	}
	
	/**
	 * Checks if the test suite entry is a list of test scenario/suite names.
	 * 
	 * @param testSuiteEntry
	 * 			The test suite entry to check.
	 * 
	 * @return	<code>true</code> if the entry is a list, otherwise <code>false</code>
	 */
	public static boolean isTestNameList(String testSuiteEntry)
	{
		if (testSuiteEntry.contains(COMMA) || testSuiteEntry.contains(QUESTION) || testSuiteEntry.contains(ASTERISK))
		{
			return true;
		}
		
		return false;

	}
	
	/**
	 * Logs the Jenkins and Total Test Plugin versions
	 * 
	 * @param listener
	 *          An instance of <code>TaskListener</code> for the task.
	 */
	public static void logJenkinsAndPluginVersion(final TaskListener listener)
	{
		listener.getLogger().println("Jenkins Version: " + Jenkins.VERSION); //$NON-NLS-1$
		Jenkins jenkinsInstance = Jenkins.getInstanceOrNull();
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
	 * Returns the path of the Topaz Workbench CLI, as defined in the global
	 * Jenkins' System settings.
	 * 
	 * @param launcher
	 *          An instance <code>Launcher</code> for launching the script.
	 *          
	 * @return	The path to The Topaz Workbench CLI
	 */
	public static String getTopaWorkbenchCLIPath(final Launcher launcher)
	{
		String cliDirectoryName = null;
		CpwrGlobalConfiguration globalConfig = CpwrGlobalConfiguration.get();
		if (globalConfig != null)
		{
			cliDirectoryName = globalConfig.getTopazCLILocation(launcher);
		}
		
		return cliDirectoryName;
	}

	/**
	 * Returns the path to the script to execute Total Test CLI
	 * 
	 * @param launcher
	 *          An instance <code>Launcher</code> for launching the script.
	 * @param listener
	 * 			An instance of <code>TaskListener</code> for the task.
	 * @param fileSeparator
	 * 			The file separator for the system on which the script will run.
	 * @param osScriptFile
	 * 			The name of the operating system dependent script file to run.
	 *            
	 * @return	An instance of <code>FilePath</code> for the CLI directory
	 * 
	 * @throws IOException
	 * 			If the CLI directory does not exist.
	 * @throws InterruptedException
	 * 			If unable to get CLI directory.
	 */
	public static FilePath getCLIScriptPath(final Launcher launcher, final TaskListener listener, final String fileSeparator, 
			final String osScriptFile) throws IOException, InterruptedException
	{
		return getCLIScriptPath(launcher, listener, fileSeparator, osScriptFile, TTT_MINIMUM_CLI_VERSION);
	}
	
	/**
	 * Returns the path to the script to execute Total Test CLI
	 * 
	 * @param launcher
	 *          An instance <code>Launcher</code> for launching the script.
	 * @param listener
	 * 			An instance of <code>TaskListener</code> for the task.
	 * @param fileSeparator
	 * 			The file separator for the system on which the script will run.
	 * @param osScriptFile
	 * 			The name of the operating system dependent script file to run.
	 * @param minCLIRelease
	 * 			The minimum CLI release required to run the script.
	 *            
	 * @return	An instance of <code>FilePath</code> for the CLI directory
	 * 
	 * @throws IOException
	 * 			If the CLI directory does not exist.
	 * @throws InterruptedException
	 * 			If unable to get CLI directory.
	 */
	public static FilePath getCLIScriptPath(final Launcher launcher, final TaskListener listener, final String fileSeparator, 
			final String osScriptFile, final String minCLIRelease) throws IOException, InterruptedException
	{
		FilePath topazWorkbenchCLIPath = null;
		FilePath cliScriptPath = null;
		
        VirtualChannel vChannel = launcher.getChannel();

		String cliDirectoryName = getTopaWorkbenchCLIPath(launcher);
		if (cliDirectoryName != null)
		{
			topazWorkbenchCLIPath = new FilePath(vChannel,cliDirectoryName);
		}

		
		if (topazWorkbenchCLIPath == null)
		{
        	throw new FileNotFoundException("ERROR: Topaz Workench CLI location was not specified. Check 'Compuware Configuration' section under 'Configure System'"); //$NON-NLS-1$
		}
		else
		{
			if (topazWorkbenchCLIPath.exists() == false) //NOSONAR
			{
		       	throw new FileNotFoundException("ERROR: Topaz Workench CLI location does not exist. Location: " + topazWorkbenchCLIPath.getRemote() + ". Check 'Compuware Configuration' section under 'Configure System'");  //NOSONAR //$NON-NLS-1$  //$NON-NLS-2$
			}
			
			String cliScriptFile = topazWorkbenchCLIPath.getRemote()  + fileSeparator + osScriptFile;
			cliScriptPath = new FilePath(vChannel, cliScriptFile);
			listener.getLogger().println("Topaz for Total Test CLI script path: " + cliScriptPath.getRemote()); //$NON-NLS-1$
			
			String cliVersion = getCLIVersion(launcher, fileSeparator);
			CLIVersionUtils.checkCLICompatibility(cliVersion, minCLIRelease);
		}
		
		return cliScriptPath;
	}
	
	/**
	 * Returns the version of the Total Test CLI
	 * 
	 * @param launcher
	 *            The machine that the files will be checked out.
	 * @param remoteFileSeparator
	 * 			  The remote file separator
	 *            
	 * @return	An instance of <code>FilePath</code> for the CLI directory
	 * 
	 * @throws IOException
	 * 			If the CLI directory does not exist.
	 * @throws InterruptedException
	 * 			If unable to get CLI directory.
	 */
	public static String getCLIVersion(final Launcher launcher, String remoteFileSeparator) throws IOException, InterruptedException
	{
		String cliVersion = null;
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
				throw new FileNotFoundException("ERROR: Topaz Workench CLI location does not exist. Location: " + globalCLIDirectory.getRemote() + ". Check 'Compuware Configuration' section under 'Configure System'");  //NOSONAR //$NON-NLS-1$ //$NON-NLS-2$
			}
			else
			{
				cliVersion = CLIVersionUtils.getCLIVersion(globalCLIDirectory, TTT_MINIMUM_CLI_VERSION);
			}
		}
		
		return cliVersion;
	}

	/**
	 * Returns if the Total Test CLI will use the default Output folder
	 * 
	 * @param launcher
	 *            The machine that the files will be checked out.
	 * @param listener
	 * 			An instance of <code>TaskListener</code> for the task.
	 * @param remoteFileSeparator
	 * 			  The remote file separator
	 *            
	 * @return	true if this uses the default folder
	 */
	public static boolean usesDefaultOutputFolder (final Launcher launcher, final TaskListener listener, String remoteFileSeparator)
	{
		return isMinimumRelease(launcher, listener, remoteFileSeparator, TotalTestRunnerUtils.TTT_OUTPUTFOLDER_CLI_VERSION);
	}

	/**
	 * Returns if the Total Test CLI will use the defulat Output folder
	 * 
	 * @param launcher
	 *            The machine that the files will be checked out.
	 * @param listener
	 * 			An instance of <code>TaskListener</code> for the task.
	 * @param remoteFileSeparator
	 * 			  The remote file separator
	 *            
	 * @return	true if this uses the default folder
	 */
	public static boolean usesNewFileExtensions (final Launcher launcher, final TaskListener listener, String remoteFileSeparator)
	{
		return isMinimumRelease(launcher, listener, remoteFileSeparator, TotalTestRunnerUtils.TTT_NEW_EXTENSIONS_CLI_VERSION);
	}


	/**
	 * Returns if the Total Test CLI is greater or equal to the passed version.
	 * 
	 * @param launcher
	 *            The machine that the files will be checked out.
	 * @param listener
	 * 			An instance of <code>TaskListener</code> for the task.
	 * @param remoteFileSeparator
	 * 			  The remote file separator
	 * @param versionNumber
	 * 			  The minimum Total Test CLI version number to check against.
	 *            
	 * @return	<code>true</code> if this node supports using the local configuration directory, otherwise <code>false</code>.
	 */
	public static boolean isMinimumRelease (final Launcher launcher, final TaskListener listener, String remoteFileSeparator, final String versionNumber)
	{
		boolean isminimumRelease = true;
		try
		{
			String cliVersion = getCLIVersion(launcher, remoteFileSeparator);
			
			try
			{
				CLIVersionUtils.checkCLICompatibility(cliVersion, versionNumber);
			}
			catch (Exception e)
			{
				isminimumRelease = false;
			}
		}
		catch (Exception e)
		{
			isminimumRelease = false;
		}

		return isminimumRelease;
	}
	
	/**
	 * Returns a UTF8 string of the remote file.
	 * 		hudson.VirtualChannel vChannel = new hudson.VirtualChannel(......);
	 *		hudson.FilePath filePath = new hudson.FilePath(vChannel, "somefile");
	 *		filePath.act(new TotalTestRunnerUtils.GetRemoteUTF8FileContents());
	 * 
	 * @see hudson.FilePath#act(FileCallable)
	 * 
	 */
	public static class GetRemoteUTF8FileContents extends MasterToSlaveFileCallable<String> {
		private static final long serialVersionUID = 1L;
		
		@Override
		public String invoke(File file, VirtualChannel channel) throws IOException, InterruptedException {
			String retVal = null;
			
			if (file.getAbsoluteFile().exists()){
				retVal = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())), StandardCharsets.UTF_8);
			}
			
			return retVal;
		}

	}

}
