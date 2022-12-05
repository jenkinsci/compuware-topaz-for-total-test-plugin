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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import com.compuware.jenkins.common.configuration.HostConnection;
import com.google.common.base.Strings;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;

public class TotalTestCTBuilder extends Builder implements SimpleBuildStep
{
	private static final int MAX_ACCOUNTING_LEN = 52;
	
	private static final String LOGLEVELALL = "ALL"; //$NON-NLS-1$
	private static final String LOGLEVELTRACE = "TRACE"; //$NON-NLS-1$
	private static final String LOGLEVELINFO = "INFO"; //$NON-NLS-1$
	private static final String LOGLEVELDEBUG = "DEBUG"; //$NON-NLS-1$
	private static final String LOGLEVELWARNING = "WARNING"; //$NON-NLS-1$
	private static final String LOGLEVELERROR = "ERROR"; //$NON-NLS-1$

	private static final String SONARVERSION5 = "5"; //$NON-NLS-1$
	private static final String SONARVERSION6 = "6"; //$NON-NLS-1$
	private static final String SONARVERSION5TITLE = "Version 5"; //$NON-NLS-1$
	private static final String SONARVERSION6TITLE = "Version 6"; //$NON-NLS-1$

	public static final String defaultLocalConfigLocation = "./TotalTestConfiguration"; //NOSONAR //$NON-NLS-1$

	/** Host credentials plugin */
	private final String credentialsId;

	/** Environment ID need to used during the execution */
	private final String environmentId;
	/** Folder from which tests should be executed */
	private final String folderPath;
	/** Repository server url */
	private final String serverUrl;
	/** Server credentials plugin */
	private final String serverCredentialsId;

	private boolean localConfig = false;
	private String localConfigLocation = DescriptorImpl.defaultLocalConfigLocation;

	/**
	 * Recursive: true|false - if test cases should be found recursively in the folder
	 */
	private boolean recursive = DescriptorImpl.defaultRecursive;
	/** Stop if test fails or threshold is reached. Defaults to true */
	private boolean stopIfTestFailsOrThresholdReached = DescriptorImpl.defaultStopIfTestFailsOrThresholdReached;
	/**
	 * Upload to server: true|false - If results should be published to the server
	 */
	private boolean uploadToServer = DescriptorImpl.defaultUploadToServer;
	/** Halt the execution when first test case fails */
	private boolean haltAtFailure = DescriptorImpl.defaultHaltAtFailure;
	/** Code coverage threshold */
	private int ccThreshold = DescriptorImpl.defaultCCThreshold;
	/** SonarQube version 5 or 6 */
	private String sonarVersion;
	private String logLevel;
	
	/**
	 * Optional file path to a folder that contains source code of tested programs. Default is COBOL. It is only used to set the
	 * source path.
	 */
	private String sourceFolder = DescriptorImpl.defaultSourceFolder;
	private String reportFolder = DescriptorImpl.defaultReportFolder;
	private String accountInfo = DescriptorImpl.defaultAccountInfo;

	/**
	 * Fields for Reporting.
	 */
	private boolean compareJUnits = DescriptorImpl.defaultCompareJunits;
	private boolean createReport = DescriptorImpl.defaultCreateReport;
	private boolean createResult = DescriptorImpl.defaultCreateResult;
	private boolean createSonarReport = DescriptorImpl.defaultCreateSonarReport;
	private boolean createJUnitReport = DescriptorImpl.defaultCreateJUnitReport;
	
	/**
	 * Fields for selected programs to execute.
	 */
	private String jsonFile = DescriptorImpl.defaultJsonFile;
	private String programList = DescriptorImpl.defaultProgramList;
	private boolean useScenarios = DescriptorImpl.defaultUseScenarios;
	private boolean selectProgramsOption = DescriptorImpl.defaultSelectProgramsOption;
	private String selectProgramsRadio = DescriptorImpl.selectProgramsJsonValue;

	/**
	 * Fields for Code Coverage.
	 */
	private boolean haltPipelineOnFailure = DescriptorImpl.defaultHaltPipelineOnFailure;
	
	private boolean collectCodeCoverage = DescriptorImpl.defaultCollectCodeCoverage;
	private String collectCCRepository = DescriptorImpl.defaultCollectCCRepository;
	private String collectCCSystem = DescriptorImpl.defaultCollectCCSystemy;
	private String collectCCTestID = DescriptorImpl.defaultCollectCCTestID;
	private boolean clearCodeCoverage = DescriptorImpl.defaultClearCodeCoverage;

	/**
	 * Fields for Enterprise Data.
	 */
	private boolean useEnterpriseData = DescriptorImpl.defaultUseEnterpriseData;
	private String enterpriseDataHostPort = DescriptorImpl.defaultEnterpriseDataHostPort;
	private String enterpriseDataWorkspace = DescriptorImpl.defaultEnterpriseDataWorkspace;
	
	private String customerId = DescriptorImpl.defaultCustomerId;
	private String siteId = DescriptorImpl.defaultSiteId;
	
	/**
	 * Fields for JCL
	 */
	private String jclPath = DescriptorImpl.defaultJclPath;
//	private String selectjclConfigRadio = DescriptorImpl.selectLocalConfigValue;
	
	/**
	 * Fields for host and port
	 */
	private String selectEnvironmentRadio = DescriptorImpl.selectEnvironmentIdValue;
	private String connectionId; 
	
	/**
	 * Field for context variables
	 */
	private String contextVariables = DescriptorImpl.defaultContextVariables;
	
	/**
	 * Constructor 
	 * 
	 * @param environmentId
	 * 			The environment id for the selected connection
	 * @param folderPath
	 * 			The folder location for the test execution.
	 * @param serverUrl
	 * 			URL for the TTT Server.
	 * @param serverCredentialsId
	 * 			The server credentials
	 * @param connectionId
	 * 			The Host connection id.
	 * @param credentialsId
	 * 			Host credentials.
	 * @param sonarVersion
	 * 			The Sonar version.
	 * @param logLevel
	 * 			The debug log level.
	 */
	@DataBoundConstructor
	public TotalTestCTBuilder(String environmentId, String folderPath,
							  String serverUrl, String serverCredentialsId,
							  String connectionId, String credentialsId,
							  String sonarVersion, String logLevel)
	{
		super();
		this.environmentId = environmentId;
		this.folderPath = folderPath;
		this.serverUrl = StringUtils.trimToEmpty(serverUrl);
		this.serverCredentialsId = StringUtils.trimToEmpty(serverCredentialsId);
		this.connectionId = StringUtils.trimToEmpty(connectionId);
		this.credentialsId = StringUtils.trimToEmpty(credentialsId);
		
		if (Strings.isNullOrEmpty(sonarVersion))
		{
			this.sonarVersion = DescriptorImpl.defaultSonarVersion;
		}
		else
		{
			this.sonarVersion = sonarVersion;
		}
		
		if (Strings.isNullOrEmpty(sonarVersion))
		{
			this.logLevel = DescriptorImpl.defaultLogLevel;
		}
		else
		{
			this.logLevel = logLevel;
		}
	}

	/**
	 * environment where test is executed
	 * 
	 * @return <code>String</code> value of environmentId
	 */
	public String getEnvironmentId()
	{
		return environmentId;
	}

	/**
	 * environment where test is executed
	 * 
	 * @return <code>String</code> value of folder path
	 */
	public String getFolderPath()
	{
		return folderPath;
	}

	/**
	 * Server URL accessor
	 * 
	 * @return <code>String</code> value of server URL
	 */
	public String getServerUrl()
	{
		return serverUrl;
	}

	/**
	 * Host credentials accessor
	 * 
	 * @return <code>String</code> value of user Id
	 */
	public String getCredentialsId()
	{
		return credentialsId;
	}

	/**
	 * Server credentials accessor
	 * 
	 * @return <code>String</code> value of user Id
	 */
	public String getServerCredentialsId()
	{
		return serverCredentialsId;
	}

	/**
	 * Code Coverage accessor
	 * 
	 * @return <code>int</code> Code Coverage threshold
	 */
	public int getCcThreshold()
	{
		return ccThreshold;
	}

	/**
	 * Recursive search for Functional Test scenarios
	 * 
	 * @return	<code>true</code> if recursive is selected, otherwise <code>false</code>.
	 */
	public boolean getRecursive()
	{
		return recursive;
	}

	/**
	 * Upload results to the server
	 * 
	 * @return	<code>true</code> if uploading results to the server is selected, otherwise <code>false</code>.
	 */
	public boolean getUploadToServer()
	{
		return uploadToServer;
	}

	/**
	 * Should Functional Test scenarios execution stop when an error occurs.
	 * 
	 * @return	<code>true</code> if stop at first error is selected, otherwise <code>false</code>.
	 */
	public boolean getHaltAtFailure()
	{
		return haltAtFailure;
	}

	/**
	 * Returns the Sonar version
	 * 
	 * @return	<code>String</code> Sonar version.
	 */
	public String getSonarVersion()
	{
		return Strings.isNullOrEmpty(sonarVersion) ? DescriptorImpl.defaultSonarVersion : sonarVersion; //$NON-NLS-1$
	}

	/**
	 * The location of ths Source Folder
	 * 
	 * @return	<code>String</code> The value set for the Source folder.
	 */
	public String getSourceFolder()
	{
		return sourceFolder;
	}

	/**
	 * Returns the location of the report folder
	 * 
	 * @return	<code>String</code> The folder to put the Reports.
	 */
	public String getReportFolder()
	{
		return reportFolder;
	}

	/**
	 * When to stop Functional Test suite or scenario.
	 * 
	 * @return	<code>true</code> if the test has failed or the threshold has been reached, otherwise <code>false</code>.
	 */
	public boolean getStopIfTestFailsOrThresholdReached()
	{
		return stopIfTestFailsOrThresholdReached;
	}

	/**
	 * Return the accounting information to be used durring execution of the Functional Test
	 * 
	 * @return	The account information to be used for execution of the Functional Test
	 */
	public String getAccountInfo()
	{
		return accountInfo;
	}

	/**
	 * Set the Source Folder location
	 * 
	 * @param sourceFolder
	 * 			  The Source Folder location.
	 */
	@DataBoundSetter
	public void setSourceFolder(String sourceFolder)
	{
		this.sourceFolder = sourceFolder;
	}

	/**
	 * Set the Sonar Version
	 * 
	 * @param sonarVersion
	 * 			  The version of Sonar.
	 */
	@DataBoundSetter
	public void setSonarVersion(String sonarVersion)
	{
		this.sonarVersion = sonarVersion;
	}

	/**
	 * Set the Report Folder location
	 * 
	 * @param reportFolder
	 * 			  The report folder location.
	 */
	@DataBoundSetter
	public void setReportFolder(String reportFolder)
	{
		this.reportFolder = reportFolder;
	}

	/**
	 * Set the Code Coverage threshold
	 * 
	 * @param ccThreshold
	 * 			  The vale to be set as the Code Coverage threshold.
	 */
	@DataBoundSetter
	public void setCcThreshold(int ccThreshold)
	{
		this.ccThreshold = ccThreshold;
	}

	/**
	 * Set the flag to halt when a failure has been detected.
	 * 
	 * @param haltAtFailure
	 * 			  <code>true</code> if the test should stop at failure, otherwise <code>false</code>.
	 */
	@DataBoundSetter
	public void setHaltAtFailure(boolean haltAtFailure)
	{
		this.haltAtFailure = haltAtFailure;
	}

	/**
	 * Set the flag to look for test scenarios recursively from the sourceFolder.
	 * 
	 * @param recursive
	 * 			  <code>true</code> if files should be searched recursively, otherwise <code>false</code>.
	 */
	@DataBoundSetter
	public void setRecursive(boolean recursive)
	{
		this.recursive = recursive;
	}

	/**
	 * Set the flag to upload the results to the server.
	 * 
	 * @param uploadToServer
	 * 			  <code>true</code> if results should be uploaded to the server, otherwise <code>false</code>.
	 */
	@DataBoundSetter
	public void setUploadToServer(boolean uploadToServer)
	{
		this.uploadToServer = uploadToServer;
	}

	/**
	 * Set the flag to stop execution of the test if a failure or threshold has been reached.
	 * 
	 * @param stopIfTestFailsOrThresholdReached
	 * 			  <code>true</code> if if the test should be stopped with failures, otherwise <code>false</code>.
	 */
	@DataBoundSetter
	public void setStopIfTestFailsOrThresholdReached(boolean stopIfTestFailsOrThresholdReached)
	{
		this.stopIfTestFailsOrThresholdReached = stopIfTestFailsOrThresholdReached;
	}

	/**
	 * Set the accounting information for the job execution.
	 * 
	 * @param accountInfo
	 * 			  The accounting information to be used when this test is executed.
	 */
	@DataBoundSetter
	public void setAccountInfo(String accountInfo)
	{
		this.accountInfo = accountInfo;
	}

	/**
	 * Set the if comparing JUnits.
	 * 
	 * @param compareJUnits
	 * 			  <code>true</code> if JUnits should be compare, otherwise <code>false</code>.
	 */
	@DataBoundSetter
	public void setCompareJUnits(boolean compareJUnits)
	{
		this.compareJUnits = compareJUnits;
	}
	
	/**
	 * Returns if comparing JUnits
	 * 
	 * @return	<code>true</code> indicates comparing JUnits. <code>false</code>
	 * 			indicates not comparing JUnits.
	 */
	public boolean getCompareJUnits()
	{
		return this.compareJUnits;
	}
	
	/**
	 * Returns if the report file should be created.
	 * 
	 * @return	<code>true</code> indicates a report file should be created.
	 * 			<code>false</code> indicates no report file will be created.
	 */
	
	public boolean getCreateReport()
	{
		return createReport;
	}

	/**
	 * Sets if the report file should be created.
	 * 
	 * @param createReport
	 * 			<code>true</code> indicates a report file should be created.
	 * 			<code>false</code> indicates no report file will be created.
	 */
	@DataBoundSetter
	public void setCreateReport(boolean createReport)
	{
		this.createReport = createReport;
	}

	/**
	 * Returns if the result file should be created.
	 * 
	 * @return	<code>true</code> indicates a result file should be created.
	 * 			<code>false</code> indicates no result file will be created.
	 */
	public boolean getCreateResult()
	{
		return createResult;
	}

	/**
	 * Set if the result file should be created.
	 * 
	 * @param createResult
	 * 			<code>true</code> indicates a result file should be created.
	 * 			<code>false</code> indicates no report file will be created.
	 */
	@DataBoundSetter
	public void setCreateResult(boolean createResult)
	{
		this.createResult = createResult;
	}
	
	/**
	 * Returns if the Sonar report file should be created.
	 * 
	 * @return	<code>true</code> indicates a Sonar report file should be created.
	 * 			<code>false</code> indicates no Sonar report file will be created.
	 */
	
	public boolean getCreateSonarReport()
	{
		return createSonarReport;
	}

	/**
	 * Sets if the Sonar report file should be created.
	 * 
	 * @param createSonarReport
	 * 			<code>true</code> indicates a Sonar report file should be created.
	 * 			<code>false</code> indicates no Sonar report file will be created.
	 */
	@DataBoundSetter
	public void setCreateSonarReport(boolean createSonarReport)
	{
		this.createSonarReport = createSonarReport;
	}

	/**
	 * Returns if the JUnit report file should be created.
	 * 
	 * @return	<code>true</code> indicates a JUnit result file should be created.
	 * 			<code>false</code>indicates no JUnit result file will be created.
	 */
	
	public boolean getCreateJUnitReport()
	{
		return createJUnitReport;
	}

	/**
	 * Sets if the JUnit report file should be created.
	 * 
	 * @param createJUnitReport
	 * 			<code>true</code> indicates a JUnit report file should be created.
	 * 			<code>false</code> indicates no JUnit report file will be created.
	 */
	@DataBoundSetter
	public void setCreateJUnitReport(boolean createJUnitReport)
	{
		this.createJUnitReport = createJUnitReport;
	}
	
	/**
	 * Returns the logging level.
	 * 
	 * @return	<code>String</code> The logging level
	 */
	public String getLogLevel()
	{
		return Strings.isNullOrEmpty(logLevel) ? DescriptorImpl.defaultLogLevel : logLevel; //$NON-NLS-1$
	}
	
	/**
	 * Sets the logging level.
	 * 
	 * @param logLevel
	 * 			the log level. Should be 'ALL', 'TRACE", 'DEBUG', 'INFO', 'WARNING' or 'ERROR'.
	 */
	@DataBoundSetter
	public void setLogLevel(final String logLevel)
	{
		this.logLevel = logLevel;
	}

	/**
	 * Set the if comparing using .scenario files instead of .context files.
	 * 
	 * @param useScenarios
	 * 			  <code>true</code> if .scenario files should be used, otherwise
	 * 			  <code>false</code> indicates .context files will be used.
	 */
	@DataBoundSetter
	public void setUseScenarios(boolean useScenarios)
	{
		this.useScenarios = useScenarios;
	}
	
	/**
	 * Returns if using .scenario files instead of .context files
	 * 
	 * @return	<code>true</code> indicates using .scenario files.
	 * 			<code>false</code> indicates using .context files.
	 */
	public boolean getUseScenarios()
	{
		return this.useScenarios;
	}

	/**
	 * Sets the selectProgramsOption radio button.
	 * 
	 * @param selectProgramsOption
	 * 			  Should the selectedOption check box be selected.
	 */
	@DataBoundSetter
	public void setSelectProgramsOption(boolean selectProgramsOption)
	{
		this.selectProgramsOption = selectProgramsOption;
	}
	
	/**
	 * Sets the selectProgramsOption radio button.
	 * 
	 * @return	<code>true</code> if selectedOption check box is selected, otherwise <code>false</code>.
	 */
	public boolean getSelectProgramsOption()
	{
		return selectProgramsOption;
	}

	/**
	 * Sets the selected selectProgramsRadio radio button.
	 * 
	 * @param selectProgramsRadio
	 * 			  Selected programs option to set.
	 */
	@DataBoundSetter
	public void setSelectProgramsRadio(String selectProgramsRadio)
	{
		this.selectProgramsRadio = selectProgramsRadio;
	}
	
	/**
	 * Returns the selected selectProgramsRadio radio button.
	 * 
	 * @return	<code>String</code> value of the selectProgramsRadio option.
	 */
	public String getSelectProgramsRadio()
	{
		String selectedProgramsRadioSelection = null;
		
		if (Strings.isNullOrEmpty(selectProgramsRadio) || isSelectProgramsJSON())
		{
			selectedProgramsRadioSelection =  DescriptorImpl.selectProgramsJsonValue;
		}
		else
		{
			selectedProgramsRadioSelection =  DescriptorImpl.selectProgramsListValue;
		}
		
		return selectedProgramsRadioSelection;
	}
	
	/**
	 * Returns if the Select JSON option is selected.
	 * 
	 * @return	<code>true</code> if Select JSON option is selected, otherwise <code>false</code>.
	 */
	public boolean isSelectProgramsJSON()
    {
		return Strings.isNullOrEmpty(selectProgramsRadio) || selectProgramsRadio.compareTo(DescriptorImpl.selectProgramsJsonValue) == 0;
    }

	/**
	 * Returns if the Select Programs option is selected.
	 * 
	 * @return	<code>true</code> if Select Programs option is selected, otherwise <code>false</code>.
	 */
	public boolean isSelectProgramsList()
    {
		return !Strings.isNullOrEmpty(selectProgramsRadio) && selectProgramsRadio.compareTo(DescriptorImpl.selectProgramsListValue) == 0;
   }

	/**
	 * Set the JSON file of tests to execute.
	 * 
	 * @param jsonFile
	 * 			  The JSON to be used when this test is executed.
	 */
	@DataBoundSetter
	public void setJsonFile(String jsonFile)
	{
		this.jsonFile = jsonFile;
	}

	/**
	 * Returns the JSON file
	 * 
	 * @return	<code>String</code> The JSON file.
	 */
	public String getJsonFile()
	{
		return jsonFile;
	}

	/**
	 * Set the list of tests to execute.
	 * 
	 * @param testList
	 * 			  The comma separated list of tests to be executed.
	 */
	@DataBoundSetter
	public void setProgramList(String testList)
	{
		this.programList = testList;
	}

	/**

	 * Returns the list of tests to execute.
	 * 
	 * @return	<code>String</code> The list of tests to execute.
	 */
	public String getProgramList()
	{
		return programList;
	}

	/**
	 * Set if the pipeline execution continues when an error occurs.
	 * 
	 * @param haltPipelineOnFailure
	 * 			<code>true</code> indicates pipeline should stop error is detected.
	 * 			<code>false</code> indicates pipeline should not stop error is detected.
	 */
	@DataBoundSetter
	public void setHaltPipelineOnFailure(boolean haltPipelineOnFailure)
	{
		this.haltPipelineOnFailure = haltPipelineOnFailure;
	}

	/**
	 * Should the pipeline execution continues when an error occurs.
	 * 
	 * @return	<code>true</code> indicates pipeline should stop error is detected.
	 * 			<code>false</code> indicates pipeline should not stop error is detected.
	 */
	public boolean getHaltPipelineOnFailure()
	{
		return haltPipelineOnFailure;
	}

	/**
	 * Set if using local configuration
	 * 
	 * @param localConfig
	 * 			Using local configuration.
	 */
	@DataBoundSetter
	public void setLocalConfig(boolean localConfig)
	{
		this.localConfig = localConfig && !Strings.isNullOrEmpty(localConfigLocation);
	}

	/**
	 * Return if using local configuration.
	 * 
	 * @return	<code>true</code> If using local configuration, otherwise <code>false</code>
	 */
	public boolean getLocalConfig()
	{
		return localConfig;
	}

	/**
	 * Is local configuration.
	 * 
	 * @return	<code>true</code> if the local configuration button is selected, otherwise <code>false</code>.
	 */
	public boolean isLocalConfig()
	{
		return localConfig;
	}
	
	/**
	 * Set local configuration location.
	 * 
	 * @param localConfigLocation
	 * 			The local configuration location.
	 */
	@DataBoundSetter
	public void setLocalConfigLocation(String localConfigLocation)
	{
		this.localConfigLocation = localConfigLocation;
	}

	/**
	 * Return the local configuration location.
	 * 
	 * @return	<code>String</code> the local configuration location.
	 */
	public String getLocalConfigLocation()
	{
		return localConfigLocation;
	}

	/**
	 * Set if Code Coverage should be executed.
	 * 
	 * @param collectCodeCoverage
	 * 			<code>true</code> indicates code coverage is to be executed.
	 * 			<code>false</code> indicates code coverage is not to be executed.
	 */
	@DataBoundSetter
	public void setCollectCodeCoverage(boolean collectCodeCoverage)
	{
		this.collectCodeCoverage = collectCodeCoverage;
	}

	/**
	 * Should Code Coverage should be executed.
	 * 
	 * @return	<code>true</code> indicates code coverage is to be executed.
	 * 			<code>false</code> indicates code coverage is not to be executed.
	 */
	public boolean getCollectCodeCoverage()
	{
		return collectCodeCoverage;
	}

	/**
	 * Set the Code Coverage Repository Server.
	 * 
	 * @param collectCCRepository
	 * 			The Code Coverage Repository Server.
	 */
	@DataBoundSetter
	public void setCollectCCRepository(String collectCCRepository)
	{
		this.collectCCRepository = collectCCRepository;
	}

	/**
	 * Returns the Code Coverage Repository Server.
	 * 
	 * @return	The Code Coverage Repository Server.
	 */
	public String getCollectCCRepository()
	{
		return collectCCRepository;
	}

	/**
	 * Set the Code Coverage System.
	 * 
	 * @param collectCCSystem
	 * 			The Code Coverage System.
	 */
	@DataBoundSetter
	public void setCollectCCSystem(String collectCCSystem)
	{
		this.collectCCSystem = collectCCSystem;
	}

	/**
	 * Returns the Code Coverage System.
	 * 
	 * @return	The Code Coverage System.
	 */
	public String getCollectCCSystem()
	{
		return collectCCSystem;
	}
	
	/**
	 * Set the Code Coverage Test ID.
	 * 
	 * @param collectCCTestID
	 * 			The Code Coverage Test ID.
	 */
	@DataBoundSetter
	public void setCollectCCTestID(String collectCCTestID)
	{
		this.collectCCTestID = collectCCTestID;
	}

	/**
	 * Returns the Code Coverage Test ID.
	 * 
	 * @return	The Code Coverage Test ID.
	 */
	public String getCollectCCTestID()
	{
		return collectCCTestID;
	}
	
	/**
	 * Set if Code Coverage results should be cleared at start of execution.
	 * 
	 * @param clearCodeCoverage
	 * 			<code>true</code> indicates code coverage should be cleared at the start of execution.
	 * 			<code>false</code> indicates code coverage should not be cleared at the start of execution.
	 */
	@DataBoundSetter
	public void setClearCodeCoverage(boolean clearCodeCoverage)
	{
		this.clearCodeCoverage = clearCodeCoverage;
	}
	/**
	 * Set if Code Coverage results should be cleared at start of execution.
	 * 
	 * @return	<code>true</code> indicates code coverage is to be executed.
	 * 			<code>false</code> indicates code coverage is not to be executed.
	 */
	public boolean getClearCodeCoverage()
	{
		return clearCodeCoverage;
	}
	
	@DataBoundSetter
	public void setUseEnterpriseData(boolean collectEnterpriseData)
	{
		this.useEnterpriseData = collectEnterpriseData;
	}
	
	public boolean getUseEnterpriseData()
	{
		return useEnterpriseData;
	}

	/**
	 * Sets the Enterprise Data Communication Manager host and port.
	 *
	 * @param enterproseDataHostPort
	 *            The Enterprise Data Communication Manager host and port.
	 */
	@DataBoundSetter
	public void setEnterpriseDataHostPort(String enterproseDataHostPort)
	{
		this.enterpriseDataHostPort = enterproseDataHostPort;
	}

	/**
	 * Gets the Enterprise Data Communication Manager host and port.
	 * 
	 * @return The Enterprise Data Communication Manager host and port.
	 */
	public String getEnterpriseDataHostPort()
	{
		return enterpriseDataHostPort;
	}

	/**
	 * Sets the Enterprise Data Communication Manager workspace.
	 *
	 * @param enterpriseDataWorkspace
	 *            The Enterprise Data Communication Manager workspace.
	 */
	@DataBoundSetter
	public void setEnterpriseDataWorkspace(String enterpriseDataWorkspace)
	{
		this.enterpriseDataWorkspace = enterpriseDataWorkspace;
	}
	
	/**
	 * Gets the Enterprise Data Communication Manager workspace.
	 * 
	 * @return The Enterprise Data Communication Manager workspace.
	 */
	public String getEnterpriseDataWorkspace()
	{
		return enterpriseDataWorkspace;
	}
	
	/**
	 * Sets the Customer ID.
	 *
	 * @param customerId
	 *            The customer's ID.
	 */
	@DataBoundSetter
	public void setCustomerId(String customerId)
	{
		this.customerId = customerId;
	}
	
	/**
	 * Gets the Customer ID.
	 * 
	 * @return The Enterprise Data Customer ID.
	 */
	public String getCustomerId()
	{
		return customerId;
	}

	/**
	 * Sets the Site ID.
	 *
	 * @param siteId
	 * 			The site id.
	 */
	@DataBoundSetter
	public void setSiteId(String siteId)
	{
		this.siteId = siteId;
	}
	
	/**
	 * Gets the Site ID.
	 * 
	 * @return The site id.
	 */
	public String getSiteId()
	{
		return siteId;
	}
	/**
	 * Sets the JCL path when executing '.testscenarios' files.
	 * 
	 * @param jclPath
	 * 			The JCL path.
	 */
	@DataBoundSetter
	public void setJclPath(String jclPath)
	{
		this.jclPath = jclPath;
	}
	
	/**
	 * Gets the JCL path.
	 * 
	 * @return	The JCL path.
	 */
	public String getJclPath()
	{
		return jclPath;
	}
	
	/**
	 * Gets the unique identifier of the 'Host connection'.
	 * 
	 * @return <code>String</code> value of connectionId
	 */
	public String getConnectionId()
	{
		return connectionId;
	}
	
	/**
	 * Sets the context variables
	 * 
	 * @param contextVariables
	 * 			The context variables
	 */
	@DataBoundSetter
	public void setContextVariables(String contextVariables)
	{
		this.contextVariables = contextVariables;
	}
	
	/**
	 * Gets the context variables.
	 * 
	 * @return	The context variables.
	 */
	public String getContextVariables()
	{
		return contextVariables;
	}
	
	/**
	 * Sets the selected environment radio button.
	 * 
	 * @param selectEnvironmenRadio
	 * 			The selected environment radio button.
	 */
	@DataBoundSetter
	public void setSelectEnvironmentRadio(String selectEnvironmenRadio)
	{
		this.selectEnvironmentRadio = selectEnvironmenRadio;
	}
	
	/**
	 * Returns the selected selectProgramsRadio radio button.
	 * 
	 * @return	<code>String</code> value of the selectProgramsRadio option.
	 */
	public String getSelectEnvironmentRadio()
	{
		String selectedEnvironmentSelection = null;
		
		if (isSelectEnvironmentId())
		{
			selectedEnvironmentSelection =  DescriptorImpl.selectEnvironmentIdValue;
		}
		else if (isSelectHostConnection())
		{
			selectedEnvironmentSelection =  DescriptorImpl.selectHostConnectionValue;
		}
		else if (isSelectHostPort())
		{
			selectedEnvironmentSelection =  DescriptorImpl.selectHostPortValue;
		}
		
		return selectedEnvironmentSelection;
	}
	
	/**
	 * Returns if the Select Environment option is selected.
	 * 
	 * @return	<code>true</code> if Select JSON option is selected, otherwise <code>false</code>.
	 */
	public boolean isSelectEnvironmentId()
    {
		return Strings.isNullOrEmpty(selectEnvironmentRadio) ||
				selectEnvironmentRadio.compareTo(DescriptorImpl.selectEnvironmentIdValue) == 0;
    }

	/**
	 * Returns if the select host connection option is selected.
	 * 
	 * @return	<code>true</code> if Select Programs option is selected, otherwise <code>false</code>.
	 */
	public boolean isSelectHostConnection()
    {
		return selectEnvironmentRadio.compareTo(DescriptorImpl.selectHostConnectionValue) == 0;
    }
	
	/**
	 * Returns if the select host/port option is selected.
	 * 
	 * @return	<code>true</code> if Select Programs option is selected, otherwise <code>false</code>.
	 */
	public boolean isSelectHostPort()
    {
		return selectEnvironmentRadio.compareTo(DescriptorImpl.selectHostPortValue) == 0;
    }
	
	/**
	 * Returns the command line parameter for the selected "Select Programs" radio button
	 * 
	 * @return the command line parameter for the selected Program option.
	 */
	public final String getselectProgramsRadioValue()
	{
		String selectedProgramsRadioValue = null;
		
		if (selectProgramsOption)
		{
			if (Strings.isNullOrEmpty(selectProgramsRadio))
			{
				selectedProgramsRadioValue = DescriptorImpl.selectProgramsJsonValue;
			}
			else
			{
				selectedProgramsRadioValue = selectProgramsRadio;
			}
		}
		return selectedProgramsRadioValue;
	}
	
	/**
	 * Return the associated text for the selected "Select Programs" radio button
	 * 
	 * @return the command line parameter for the selected Program option.
	 */
	public final String getselectProgramsRadioText()
	{
		String selectedProgramsRadioText = null;
		
		if (selectProgramsOption)
		{
			if (isSelectProgramsJSON())
			{
				selectedProgramsRadioText = jsonFile;
			}
			else if (isSelectProgramsList())
			{
				selectedProgramsRadioText = programList;
			}
		}
		
		return selectedProgramsRadioText;
	}
	
	/**
	 * Return the associated text for the "Halt pipeline if errors occur" field's title
	 * 
	 * @return <code>String</code> The "Halt pipeline if errors occur" field title
	 */
	public final String getHaltPipelineTitle()
	{
		return DescriptorImpl.haltPipelineTitle;
	}
	
	/**
	 * (non-Javadoc)
	 * @see jenkins.tasks.SimpleBuildStep#perform(hudson.model.Run, hudson.FilePath, hudson.Launcher, hudson.model.TaskListener)
	 */
	@Override
	public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener)
			throws InterruptedException, IOException
	{
		listener.getLogger().println("Running " + Messages.displayName() + "\n"); //$NON-NLS-1$ //$NON-NLS-2$

		try
		{
			validateParameters(launcher, listener, build.getParent());

			TotalTestCTRunner runner = new TotalTestCTRunner(this);
			boolean success = runner.run(build, launcher, workspace, listener);
			if (success == false) //NOSONAR
			{
				listener.error("Test failure"); //$NON-NLS-1$
				throw new AbortException("Test failure"); //$NON-NLS-1$
			}
			else
			{
				listener.getLogger().println("Test Success..."); //$NON-NLS-1$
			}

		}
		catch (Exception e)
		{
			listener.getLogger().println(e.getMessage());
			throw new AbortException();
		}
	}

	/**
	 * Validates the configuration parameters.
	 * 
	 * @param launcher
	 *            An instance of <code>Launcher</code> for launching the plugin.
	 * @param listener
	 *            An instance of <code>TaskListener</code> for the build listener.
	 * @param project
	 *            An instance of <code>Item</code> for the Jenkins project.
	 */
	public void validateParameters(final Launcher launcher, final TaskListener listener, final Item project)
	{
		if (isSelectEnvironmentId())
		{
			if (!getEnvironmentId().isEmpty())
			{
				listener.getLogger().println("environmentId = " + environmentId); //$NON-NLS-1$
			}
			else
			{
				throw new IllegalArgumentException(
						"Missing parameter Environment Id - please get the environment ID from the repository server."); //$NON-NLS-1$
			}
		}
		
		if (isSelectEnvironmentId())
		{
			if (Strings.isNullOrEmpty(this.environmentId))
			{
				throw new IllegalArgumentException("No environment defined.  Enter an Environment Id or select a host connection."); //$NON-NLS-1$
			}
		}
		else if (isSelectHostConnection())
		{
			HostConnection connection = null;
			CpwrGlobalConfiguration globalConfig = CpwrGlobalConfiguration.get();
		
			if (globalConfig != null)
			{
				connection = globalConfig.getHostConnection(getConnectionId());
			}
			
			if (connection == null) //NOSONAR
			{
				if (Strings.isNullOrEmpty(getEnvironmentId()))
				{
					throw new IllegalArgumentException("No host connection defined. Either define an Environment Id or a select a host connection.  Check project and global configurations to unsure host connection is set."); //$NON-NLS-1$
				}
			}
		}
		else
		{
			throw new IllegalArgumentException(
					"Missing environment id, host connection or host and port."); //$NON-NLS-1$
		}

		if (isLocalConfig())
		{
			if (!getLocalConfigLocation().isEmpty())
			{
				listener.getLogger().println("Local configuration directory = " + localConfigLocation); //$NON-NLS-1$
			}
			else
			{
				throw new IllegalArgumentException(
						"Missing parameter Local configuration directory. - please enter the local configuration location."); //$NON-NLS-1$
			}
		}
		else
		{
			if (!getServerUrl().isEmpty())
			{
				listener.getLogger().println("serverUrl = " + serverUrl); //$NON-NLS-1$
			}
			else
			{
				throw new IllegalArgumentException(
						"Missing parameter CES server URL - please use the Compuware configuration tool to configure"); //$NON-NLS-1$
			}
		}

		if (!getCredentialsId().isEmpty())
		{

			if (TotalTestRunnerUtils.getLoginInformation(project, getCredentialsId()) != null)
			{
				listener.getLogger().println("Credentials entered..."); //$NON-NLS-1$
			}
			else
			{
				throw new IllegalArgumentException(
						"Credential ID entered is not valid - enter valid ID from Jenkins Credentials plugin"); //$NON-NLS-1$
			}
		}
		else
		{
			throw new IllegalArgumentException("Missing Credentials ID - configure plugin correctly"); //$NON-NLS-1$
		}
		
		if (!getAccountInfo().isEmpty() && getAccountInfo().length() > MAX_ACCOUNTING_LEN)
		{
			throw new IllegalArgumentException("Entered accounting information is greater than 52 characters."); //$NON-NLS-1$
		}

		listener.getLogger().println("ccThreshold = " + ccThreshold); //$NON-NLS-1$
	}

	@Symbol("totaltest")
	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder>
	{
		public static final String defaultFolderPath = ""; //NOSONAR //$NON-NLS-1$
		public static final int defaultCCThreshold = 0; //NOSONAR
		public static final String defaultSourceFolder = "COBOL"; //NOSONAR //$NON-NLS-1$
		public static final String defaultReportFolder = "TTTReport"; //NOSONAR //$NON-NLS-1$
		public static final Boolean defaultRecursive = true; //NOSONAR
		public static final Boolean defaultStopIfTestFailsOrThresholdReached = true; //NOSONAR
		public static final Boolean defaultUploadToServer = Boolean.FALSE; //NOSONAR
		public static final Boolean defaultHaltAtFailure = Boolean.FALSE; //NOSONAR
		public static final String defaultAccountInfo = ""; //NOSONAR //$NON-NLS-1$
		public static final Boolean defaultCompareJunits = false; //NOSONAR
		public static final Boolean defaultCreateReport = true; //NOSONAR
		public static final Boolean defaultCreateResult = true; //NOSONAR
		public static final Boolean defaultCreateSonarReport = true; //NOSONAR
		public static final Boolean defaultCreateJUnitReport = true; //NOSONAR
		public static final String defaultSonarVersion = SONARVERSION6; //NOSONAR //$NON-NLS-1$
		public static final String defaultJsonFile = "changedPrograms.json"; //NOSONAR //$NON-NLS-1$
		public static final String defaultProgramList = ""; //NOSONAR //$NON-NLS-1$
		public static final Boolean defaultUseScenarios = false; //NOSONAR
		public static final String selectJsonFile = "JSON file"; //NOSONAR //$NON-NLS-1$
		public static final String selectProgramList = "Selected Programs"; //NOSONAR //$NON-NLS-1$
		public static final String selectProgramsJsonValue = "-pnf"; //NOSONAR  //$NON-NLS-1$
		public static final String selectProgramsListValue = "-pn"; //NOSONAR  //$NON-NLS-1$
		public static final String defaultSelectProgramsJsonValue = selectProgramsJsonValue; //NOSONAR
		public static final Boolean defaultSelectProgramsOption = false;
		public static final Boolean defaultHaltPipelineOnFailure = true; //NOSONAR
		public static final Boolean defaultCollectCodeCoverage = false; //NOSONAR
		public static final String defaultCollectCCRepository = ""; //NOSONAR //$NON-NLS-1$
		public static final String defaultCollectCCSystemy = ""; //NOSONAR //$NON-NLS-1$
		public static final String defaultCollectCCTestID = ""; //NOSONAR //$NON-NLS-1$
		public static final Boolean defaultClearCodeCoverage = false; //NOSONAR
		public static final String haltPipelineTitle = "Halt pipeline if errors occur"; //NOSONAR //$NON-NLS-1$
		public static final String defaultLocalConfigLocation = TotalTestCTBuilder.defaultLocalConfigLocation; //NOSONAR //$NON-NLS-1$
		public static final String defaultLogLevel = LOGLEVELINFO; //NOSONAR
		public static final Boolean defaultUseEnterpriseData = false; //NOSONAR
		public static final String defaultEnterpriseDataHostPort = ""; // NOSONAR //$NON-NLS-1$
		public static final String defaultEnterpriseDataWorkspace = ""; //NOSONAR //$NON-NLS-1$
		public static final String defaultJclPath = ""; //NOSONAR //$NON-NLS-1$
		public static final String  selectLocalConfigValue = "-cfgdir"; //NOSONAR  //$NON-NLS-1$
		public static final String selectJclPathValue = "-jcl"; //NOSONAR  //$NON-NLS-1$

		public static final String  selectEnvironmentIdValue = "-e"; //NOSONAR  //$NON-NLS-1$
		public static final String selectHostConnectionValue = "-hci"; //NOSONAR  //$NON-NLS-1$
		public static final String selectHostPortValue = "-hostport"; //NOSONAR  //$NON-NLS-1$
		public static final String defaultContextVariables = ""; //NOSONAR  //$NON-NLS-1$
		
		public static final String defaultCustomerId = ""; //NOSONAR  //$NON-NLS-1$
		public static final String defaultSiteId = ""; //NOSONAR  //$NON-NLS-1$

		/**
		 * Fill in the Sonar versions.
		 * 
		 * @param context
		 * 		An instance of <code>context</code> for the Jenkin's context
		 * @param sonarVersion
		 * 		The sonar version to set.
		 * @param project
		 * 		An instance of <code>Item</code> for the project.
		 * 
		 * @return	A <code>ListBoxModel</code> instance contain the Code Coverage program types.
		 */
		public ListBoxModel doFillSonarVersionItems(@AncestorInPath Jenkins context, @QueryParameter String sonarVersion, @AncestorInPath Item project)
		{
			ListBoxModel sonarVersionModel = new ListBoxModel();
			boolean isDefault5 = defaultSonarVersion == SONARVERSION5;
			boolean isDefault6 = defaultSonarVersion == SONARVERSION6;
			sonarVersionModel.add(new Option(SONARVERSION6TITLE, SONARVERSION6,	((sonarVersion == null && isDefault6) || SONARVERSION6.equals(sonarVersion) ? true : false))); //NOSONAR
			sonarVersionModel.add(new Option(SONARVERSION5TITLE, SONARVERSION5,	((sonarVersion == null && isDefault5) || SONARVERSION5.equals(sonarVersion) ? true : false))); //NOSONAR

			return sonarVersionModel;
		}

		/**
		 * Fill in the logging levels.
		 * 
		 * @param context
		 * 		An instance of <code>context</code> for the Jenkin's context
		 * @param logLevel
		 * 		The logging level to check.
		 * @param project
		 * 		An instance of <code>Item</code> for the project.
		 * 
		 * @return	A <code>ListBoxModel</code> instance contain the Code Coverage program types.
		 */
		public ListBoxModel doFillLogLevelItems(@AncestorInPath Jenkins context, @QueryParameter String logLevel, @AncestorInPath Item project)
		{
			ListBoxModel logLevelModel = new ListBoxModel();
			logLevelModel.add(new Option(LOGLEVELALL,		LOGLEVELALL,		(logLevel == null || !LOGLEVELALL.equals(logLevel)		? false : true))); //NOSONAR
			logLevelModel.add(new Option(LOGLEVELTRACE,		LOGLEVELTRACE, 		(logLevel == null || !LOGLEVELTRACE.equals(logLevel)	? false : true))); //NOSONAR
			logLevelModel.add(new Option(LOGLEVELINFO, 		LOGLEVELINFO, 		(logLevel == null || !LOGLEVELINFO.equals(logLevel)		? false : true))); //NOSONAR
			logLevelModel.add(new Option(LOGLEVELDEBUG,		LOGLEVELDEBUG, 		(logLevel == null || !LOGLEVELDEBUG.equals(logLevel)	? false : true))); //NOSONAR
			logLevelModel.add(new Option(LOGLEVELWARNING,	LOGLEVELWARNING,	(logLevel == null || !LOGLEVELWARNING.equals(logLevel)	? false : true))); //NOSONAR
			logLevelModel.add(new Option(LOGLEVELERROR,		LOGLEVELERROR, 		(logLevel == null || !LOGLEVELERROR.equals(logLevel)	? false : true))); //NOSONAR

			return logLevelModel;
		}	

		/**
		 * Validates for the 'CcThreshold' field
		 * 
		 * @param value
		 * 		The code coverage threshold.
		 * @return validation message
		 */
		public FormValidation doCheckCcThreshold(@QueryParameter String value)
		{
			if (value.length() == 0)
			{
				return FormValidation.error(Messages.errors_missingCcThreshold());
			}

			try
			{
				int iValue = Integer.parseInt(value);

				if (iValue < 0 || iValue > 100)
				{
					return FormValidation.error(Messages.errors_missingCcThreshold());
				}
			}
			catch (NumberFormatException e)
			{
				return FormValidation.error(Messages.errors_missingCcThreshold());
			}

			return FormValidation.ok();
		}
		
		public FormValidation doCheckEnvironmentRadio(@QueryParameter String value)
		{
			return FormValidation.ok();
		}
		
//		/**
//		 * Validates for the 'EnvironmentId' field
//		 * 
//		 * @param value
//		 * 		The environment id.
//		 * @return validation message
//		 */
//		public FormValidation doCheckEnvironmentId(@QueryParameter String value)
//		{
//			if (value == null || value.isEmpty() || value.trim().length() == 0)
//			{
//				return FormValidation.error(Messages.errors_missingEnvironmentId());
//			}
//
//			return FormValidation.ok();
//		}

		/**
		 * Fills in the Host Connection selection box with applicable connections.
		 * 
		 * @param context
		 * 		An instance of <code>context</code> for the Jenkin's context
		 * @param connectionId
		 *            an existing host connection identifier; can be null
		 * @param project
		 * 		An instance of <code>Item</code> for the project.
		 * 
		 * @return host connection selections
		 */
		public ListBoxModel doFillConnectionIdItems(@AncestorInPath Jenkins context, @QueryParameter String connectionId,
				@AncestorInPath Item project)
		{
			if (project == null) {
				Jenkins.get().checkPermission(Jenkins.ADMINISTER);
			} else {
				project.checkPermission(Item.CONFIGURE);
			}
			
			CpwrGlobalConfiguration globalConfig = CpwrGlobalConfiguration.get();
			HostConnection[] hostConnections = globalConfig.getHostConnections();

			ListBoxModel model = new ListBoxModel();
			model.add(new Option(StringUtils.EMPTY, StringUtils.EMPTY, false));

			for (HostConnection hostConnection : hostConnections)
			{
				boolean isSelected = false;
				if (connectionId != null)
				{
					isSelected = connectionId.matches(hostConnection.getConnectionId());
				}

				model.add(new Option(hostConnection.getDescription() + " [" + hostConnection.getHostPort() + ']', //$NON-NLS-1$
						hostConnection.getConnectionId(), isSelected));
			}

			return model;
		}

		/**
		 * Validates for the 'Login Credentials' field
		 * 
		 * @param value
		 *            Value passed from the config.jelly "fileExtension" field
		 * @return validation message
		 */
		public FormValidation doCheckCredentialsId(@QueryParameter final String value)
		{
			if (value == null || value.isEmpty() || value.trim().length() == 0)
			{
				return FormValidation.error(Messages.checkLoginCredentialError());
			}

			return FormValidation.ok();
		}

		/**
		 * Validates for the 'reportFolder' field
		 * 
		 * @param value
		 *            Value passed from the config.jelly "fileExtension" field
		 * @return validation message
		 */
		public FormValidation doCheckReportFolder(@QueryParameter final String value)
		{
			if (value == null || value.isEmpty() || value.trim().length() == 0)
			{
				return FormValidation.error(Messages.errors_missingReportFolder());
			}

			File theFolder = new File(value);
			if (theFolder.isFile())
			{
				return FormValidation.error(Messages.errors_wrongReportFolder());
			}

			return FormValidation.ok();
		}

		/**
		 * Validates for the 'folderPath' field
		 * 
		 * @param value
		 *            Value passed from the config.jelly "fileExtension" field
		 * @return validation message
		 */
		public FormValidation doCheckFolderPath(@QueryParameter final String value)
		{
			if (value != null && value.trim().length() > 0)
			{
				File theFolder = new File(value);
				if (theFolder.isFile())
				{
					return FormValidation.error(Messages.errors_missingFolderPath());
				}
			}

			return FormValidation.ok();
		}
		
		public FormValidation doCheckAccountInfo(@QueryParameter final String value)
		{
			if (value != null && value.trim().length() > 0)
			{
				if (value.length() > 52) //NOSONAR
				{
					return FormValidation.error(Messages.errors_invalidAccountingLength());
				}
			}
			return FormValidation.ok();
		}

		/**
		 * Fills in the Login Credential selection box with applicable Jenkins credentials
		 * 
		 * @param context
		 *            Jenkins context.
		 * @param credentialsId
		 *            The host credential id for the user.
		 * @param project
		 *            The Jenkins project.
		 * 
		 * @return credential selections
		 * 
		 */
		public ListBoxModel doFillCredentialsIdItems(@AncestorInPath final Jenkins context, //NOSONAR
				@QueryParameter final String credentialsId, @AncestorInPath final Item project)
		{
			if (project == null) {
				Jenkins.get().checkPermission(Jenkins.ADMINISTER);
			} else {
				project.checkPermission(Item.CONFIGURE);
			}
			List<StandardUsernamePasswordCredentials> creds = CredentialsProvider.lookupCredentials(
					StandardUsernamePasswordCredentials.class, project, ACL.SYSTEM,
					Collections.<DomainRequirement> emptyList());

			StandardListBoxModel model = new StandardListBoxModel();

			model.add(new Option("", "", false)); //$NON-NLS-1$ //$NON-NLS-2$

			for (StandardUsernamePasswordCredentials c : creds)
			{
				boolean isSelected = false;

				if (credentialsId != null)
				{
					isSelected = credentialsId.matches(c.getId());
				}

				String description = Util.fixEmptyAndTrim(c.getDescription());
				model.add(new Option(c.getUsername() + (description != null ? " (" + description + ")" : ""), c.getId(), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						isSelected));
			}

			return model;
		}

		/**
		 * Fills in the CES server URL selection box with applicable Jenkins credentials
		 * 
		 * @param serverUrl
		 *            The serverUrl id for the user.
		 * 
		 * @return serverUrl selections
		 * 
		 */
		public ListBoxModel doFillServerUrlItems(@QueryParameter String serverUrl,@AncestorInPath final Item project)
		{
			if (project == null) {
				Jenkins.get().checkPermission(Jenkins.ADMINISTER);
			} else {
				project.checkPermission(Item.CONFIGURE);
			}
			
			ListBoxModel model = new ListBoxModel();
			model.add(new Option("", "", false)); //$NON-NLS-1$ //$NON-NLS-2$
			CpwrGlobalConfiguration globalConfig = CpwrGlobalConfiguration.get();
			if (globalConfig != null)
			{
				HostConnection[] hostConnections = globalConfig.getHostConnections();

				for (HostConnection connection : hostConnections)
				{
					String cesServerURL = connection.getCesUrl();
					if (cesServerURL != null && !cesServerURL.isEmpty())
					{
						boolean isSelected = false;
						if (serverUrl != null)
						{
							isSelected = serverUrl.equalsIgnoreCase(cesServerURL);
						}

						String cesValue = cesServerURL;
						Option opt = new Option(cesValue, cesServerURL, isSelected);
						boolean exists = false;
						
						ListIterator<Option> li = model.listIterator();
						while (li.hasNext())
						{
							if (li.next().value.equalsIgnoreCase(cesServerURL))
							{
								exists = true;
								break;
							}
						}
						
						if (!exists)
						{
							model.add(opt);
						}
					}
				}
			}

			return model;
		}
		
		/**
		 * Fills in the Login Credential selection box with applicable Jenkins credentials
		 * 
		 * @param context
		 *            Jenkins context.
		 * @param serverCredentialsId
		 *            The server credential id for the user.
		 * @param project
		 *            The Jenkins project.
		 * 
		 * @return credential selections
		 * 
		 */
		public ListBoxModel doFillServerCredentialsIdItems(@AncestorInPath final Jenkins context, //NOSONAR
				@QueryParameter final String serverCredentialsId, @AncestorInPath final Item project)
		{
			if (project == null) {
				Jenkins.get().checkPermission(Jenkins.ADMINISTER);
			} else {
				project.checkPermission(Item.CONFIGURE);
			}
			
			List<StandardUsernamePasswordCredentials> creds = CredentialsProvider.lookupCredentials(
					StandardUsernamePasswordCredentials.class, project, ACL.SYSTEM,
					Collections.<DomainRequirement> emptyList());

			StandardListBoxModel model = new StandardListBoxModel();

			model.add(new Option("", "", false)); //$NON-NLS-1$ //$NON-NLS-2$

			for (StandardUsernamePasswordCredentials c : creds)
			{
				boolean isSelected = false;

				if (serverCredentialsId != null)
				{
					isSelected = serverCredentialsId.matches(c.getId());
				}

				String description = Util.fixEmptyAndTrim(c.getDescription());
				model.add(new Option(c.getUsername() + (description != null ? " (" + description + ")" : ""), c.getId(), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						isSelected));
			}

			return model;
		}

		/**
		 * (non-Javadoc)
		 * @see hudson.tasks.BuildStepDescriptor#isApplicable(java.lang.Class)
		 */
		@SuppressWarnings("rawtypes")
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> aClass)
		{
			return true;
		}

		/**
		 * (non-Javadoc)
		 * @see hudson.model.Descriptor#getDisplayName()
		 */
		@Override
		public String getDisplayName()
		{
			return Messages.displayName();
		}
	}
}
