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

import hudson.Launcher;
import hudson.Util;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import com.compuware.jenkins.common.configuration.HostConnection;

import org.kohsuke.stapler.QueryParameter;

import java.util.Collections;
import java.util.List;

/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link TotalTestRunner} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields to remember
 * remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform} method will be invoked. 
 *
 */
public class TotalTestBuilder extends AbstractTotalTestBuilderMigration implements SimpleBuildStep
{
	private static final String COLON = ":"; //$NON-NLS-1$
	private static final String EQUAL = "="; //$NON-NLS-1$
	private static final String COMMA = ","; //$NON-NLS-1$
	private static final String EMPTY_STRING = ""; //$NON-NLS-1$
	
	private static final String TEST_SCENARIO_SUFFIX = ".testscenario"; //$NON-NLS-1$
	private static final String TEST_SUITE_SUFFIX = ".testsuite"; //$NON-NLS-1$
	private static final String DATASET_HLQ_PATTERN = "^([A-Z#@\\$]{1}[\\w#@\\$\\-]{1,7})"; //$NON-NLS-1$
	private static final String DB2 = "DB2"; //$NON-NLS-1$
	private static final String IMS = "IMS"; //$NON-NLS-1$
	private static final String TOTALTEST = "TOTALTEST"; //$NON-NLS-1$
	
	private String projectFolder;
	private String credentialsId;
	private String testSuite;
	private String jcl;
	private String datasetHLQ;
	private boolean useStubs;
	private boolean deleteTemp;

	private String ccRepo;
	private String ccSystem;
	private String ccTestId;
	private String ccPgmType;
	private boolean ccClearStats;
	
	/**
	 * Constructor 
	 * 
	 * @param connectionId
	 * 			The connection id for the selected connection
	 * @param credentialsId
	 * 			The user's credential id.
	 * @param projectFolder
	 * 			The name of the folder containing the Total Test project.
	 * @param testSuite
	 * 			The name of the test scenario or test suite to run.
	 * @param jcl
	 * 			The name of the JCL file to use.
	 */
	@DataBoundConstructor
	public TotalTestBuilder(String connectionId, String credentialsId, String projectFolder, String testSuite, String jcl)
	{
		super.connectionId = StringUtils.trimToEmpty(connectionId);
		
		this.credentialsId = StringUtils.trimToEmpty(credentialsId);
		this.projectFolder = StringUtils.trimToEmpty(projectFolder);
		this.testSuite = StringUtils.trimToEmpty(testSuite);
		this.jcl = StringUtils.trimToEmpty(jcl);
		this.useStubs = true;
		this.deleteTemp = true;
		this.ccClearStats = true;
	}
	
	/**
	 * Gets the unique identifier of the 'Host connection'.
	 * 
	 * @return <code>String</code> value of m_connectionId
	 */
	public String getConnectionId()
	{
		return connectionId;
	}
	
	/**
	 * Returns the credential id of the user.
	 * 
	 * @return	The user's credential id.
	 */
	public String getCredentialsId()
	{
		return credentialsId;
	}
	
	/**
	 * Returns the folder name containing the Total Test project.
	 * 
	 * @return	The name of the folder containing the Total Test project.
	 */
	public String getProjectFolder()
	{
		return projectFolder;
	}

	/**
	 * Returns the name of the test scenario or test suite to
	 * run.
	 * <p>
	 * The test scenario or test suite must be in the specified
	 * project.
	 * 
	 * @return	The name of the test scenario or test suite to run.
	 */
	public String getTestSuite()
	{
		return testSuite;
	}
	
	/**
	 * Returns the name of the JCL file to use.
	 * <p>
	 * The JCL file must be in the specified project.
	 * 
	 * @return	The name of the JCL file to use.
	 */
	public String getJcl()
	{
		return jcl;
	}
	
	/**
	 * Sets the Code Coverage repository dataset name.
	 * 
	 * @param ccRepo
	 * 			The Code Coverage repository name.
	 */
	@DataBoundSetter
	public void setCcRepo(final String ccRepo)
	{
		this.ccRepo = StringUtils.trimToEmpty(ccRepo);
	}
	
	/**
	 * Returns the name of the Code Coverage repository.
	 * 
	 * @return	The name of the Code Coverage repository.
	 */
	public String getCcRepo()
	{
		return ccRepo;
	}
	
	/**
	 * Sets the Code Coverage system.
	 * 
	 * @param ccSystem
	 * 			The Code Coverage system.
	 */
	@DataBoundSetter
	public void setCcSystem(final String ccSystem)
	{
		this.ccSystem = StringUtils.trimToEmpty(ccSystem);
	}
	
	/**
	 * Returns the Code Coverage system.
	 * 
	 * @return	The Code Coverage system.
	 */
	public String getCcSystem()
	{
		return ccSystem;
	}
	
	/**
	 * Sets the Code Coverage test id.
	 * 
	 * @param ccTestId
	 * 			The Code Coverage test id.
	 */
	@DataBoundSetter
	public void setCcTestId(final String ccTestId)
	{
		this.ccTestId = StringUtils.trimToEmpty(ccTestId);
	}
	
	/**
	 * Returns the Code Coverage test id.
	 * 
	 * @return	The Code Coverage test id.
	 */
	public String getCcTestId()
	{
		return ccTestId;
	}
	
	/**
	 * Set if the main program is DB2.
	 * <p>
	 * This method is only here to support version 1.7 and below when ccDB2 was a checkbox.
	 * 
	 * @param ccDB2
	 * 			<code>true</code> if Code Coverage is for a DB2 program, otherwise <code>false</code>
	 */
	@DataBoundSetter
	public void setCcDB2(final boolean ccDB2)
	{
		ccPgmType = DB2;
	}
	
	/**
	 * Returns whether Code Coverage main program is DB2(IKJEFT01).
	 * 
	 * @return	<code>true</code> indicates program is DB2, otherwise <code>false</code>
	 */
	public boolean getCcDB2()
	{
		return (DB2.equalsIgnoreCase(ccPgmType));
	}
	
	/**
	 * Returns the Code Coverage main program type.
	 * 
	 * @return	The main program type for Code Coverage
	 */
	public String getCcPgmType()
	{
		return (ccPgmType == null || ccPgmType.equals("") ? TOTALTEST : ccPgmType); //$NON-NLS-1$
	}
	
	/**
	 * Sets the Code Coverage main program type.
	 * 
	 * @param ccType
	 * 			The Code Coverage main program type. Should be 'DB2', 'IMS", or 'TOTALTEST'.
	 */
	@DataBoundSetter
	public void setCcPgmType(final String ccType)
	{
		this.ccPgmType = ccType;
	}
	
	/**
	 * Sets whether statistics should be clear before the test.
	 * 
	 * @param ccClearStats
	 * 			<code>true</code> if the statistics should be cleared before test, otherwise <code>false</code>
	 */
	@DataBoundSetter
	public void setCcClearStats(final boolean ccClearStats)
	{
		this.ccClearStats = ccClearStats;
	}
	
	/**
	 * Returns whether statistics should be cleared before the test
	 * 
	 * @return	<code>true</code> indicates stubs should be used, otherwise <code>false</code>
	 */
	public boolean isCcClearStats()
	{
		return ccClearStats;
	}
	
	/**
	 * Sets if stubs are being used.
	 * 
	 * @param useStubs
	 * 			<code>true</code> if stubs are to be used, otherwise <code>false</code>
	 */
	@DataBoundSetter
	public void setUseStubs(final boolean useStubs)
	{
		this.useStubs = useStubs;
	}
	
	/**
	 * Returns whether stubs are to be used.
	 * 
	 * @return	<code>true</code> indicates stubs should be used, otherwise <code>false</code>
	 */
	public boolean isUseStubs()
	{
		return useStubs;
	}
	
	/**
	 * Sets whether temporary file should be deleted.
	 * 
	 * @param deleteTemp
	 * 			<code>true</code> if temporary files should be deleted, otherwise <code>false</code>
	 */
	@DataBoundSetter
	public void setDeleteTemp(final boolean deleteTemp)
	{
		this.deleteTemp = deleteTemp;
	}
	
	/**
	 * Returns whether temporary file should be deleted.
	 * 
	 * @return	<code>true</code> indicates temporary files should be deleted.
	 */
	public boolean isDeleteTemp()
	{
		return deleteTemp;
	}
	
	/**
	 * Sets the dataset high level qualifier,
	 * 
	 * @param hlq
	 * 			High level qualifier for allocating datasets. Can be <code>null</code>
	 */
	@DataBoundSetter
	public void setHlq(final String hlq)
	{
		this.datasetHLQ = StringUtils.trimToEmpty(hlq);
	}
	
	/**
	 * Returns the dataset high level qualifier
	 * 
	 * @return	The high level qualifier for allocating datasets. <code>null</code> will be
	 * 			returned if no high level qualifier was specified.
	 */
	public String getHlq()
	{
		return datasetHLQ;
	}
	
	/*
	 * (non-Javadoc)
	 * @see jenkins.tasks.SimpleBuildStep#perform(hudson.model.Run, hudson.FilePath, hudson.Launcher, hudson.model.TaskListener)
	 */
    @Override
    public void perform(final Run<?,?> build, final FilePath workspaceFilePath, final Launcher launcher, final TaskListener listener) throws AbortException
    {
        // This is where you 'build' the project.
        // Since this is a dummy, we just say 'hello world' and call that a build.

        // This also shows how you can consult the global configuration of the builder
    	listener.getLogger().println(Messages.displayNameTotalTest());
    	
		try
		{
			validateParameters(launcher, listener, build.getParent());
			
			TotalTestRunner runner = new TotalTestRunner(this);
			boolean success = runner.run(build, launcher, workspaceFilePath, listener);
			if (success == false) //NOSONAR
			{
				throw new AbortException(Messages.totalTestFailure());
			}
			else
			{
				listener.getLogger().println(Messages.totalTestSuccess());
			}
					
		}
		catch (Exception e)
		{
			listener.getLogger().println(e.getMessage());
			throw new AbortException();
		}
    }
    
    /*
     * (non-Javadoc)
     * @see hudson.tasks.Builder#getDescriptor()
     */
    @Override
    public DescriptorImpl getDescriptor()
    {
        return (DescriptorImpl)super.getDescriptor();
    }

	/**
	 * Validates the configuration parameters.
	 * 
	 * @param launcher
	 *         An instance of <code>Launcher</code> for launching the plugin.
	 * @param listener
	 *          An instance of <code>TaskListener</code> for the build listener.
	 * @param project
	 * 			An instance of <code>Item</code> for the Jenkins project.
	 */
	public void validateParameters(final Launcher launcher, final TaskListener listener, final Item project)
	{
		if (TotalTestRunnerUtils.getLoginInformation(project, getCredentialsId()) != null)
		{
			listener.getLogger().println(Messages.username() + EQUAL + TotalTestRunnerUtils.getLoginInformation(project, getCredentialsId()).getUsername());
		}
		else
		{
			throw new IllegalArgumentException(Messages.missingParameterError(Messages.loginCredentials()));
		}

		if (getProjectFolder().isEmpty() == false) // NOSONAR
		{
			listener.getLogger().println(Messages.project() + EQUAL + getProjectFolder());		
		}
		else
		{
			throw new IllegalArgumentException(Messages.missingParameterError(Messages.project()));
		}
		
		if (getTestSuite().isEmpty() == false) // NOSONAR
		{
			String [] nameList = getTestSuite().split(COMMA);
			if ((nameList.length == 1) && (TotalTestRunnerUtils.isAllTestScenariosOrSuites(nameList[0].trim())))
			{
				listener.getLogger().println(Messages.testsuite() + EQUAL + getTestSuite());	
				return;
			}
			
			for (String name : nameList)
			{
				String lcName = name.trim().toLowerCase();
				
				if (TotalTestRunnerUtils.isAllTestScenariosOrSuites(lcName))
				{
					throw new IllegalArgumentException(Messages.testSuiteAllScenariosSuitesError(name));
				}
				
				if ((lcName.endsWith(TEST_SUITE_SUFFIX) == false) && (lcName.endsWith(TEST_SCENARIO_SUFFIX) == false)) //NOSONAR
				{
					throw new IllegalArgumentException(Messages.testSuiteError(name));
				}
			}
			
			listener.getLogger().println(Messages.testsuite() + EQUAL + getTestSuite());		
		}
		else
		{
			throw new IllegalArgumentException(Messages.missingParameterError(Messages.testsuite()));
		}
		
		if (getJcl().isEmpty() == false) // NOSONAR
		{
			listener.getLogger().println(Messages.jcl() + EQUAL + getJcl());			
		}
		else
		{
			throw new IllegalArgumentException(Messages.missingParameterError(Messages.jcl()));
		}
		
		if (getHlq().isEmpty() == false) // NOSONAR
		{
			listener.getLogger().println(Messages.hlq() + EQUAL + getHlq().isEmpty());			
		}
	}
	
	/**
     * Descriptor for {@link TotalTestRunner}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See {@code src/main/resources/hudson/plugins/totatest/TotalTestRunner/*.jelly}
     * for the actual HTML fragment for the configuration screen.
     */
	@Symbol("totaltestUT")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> 
    {
        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public DescriptorImpl()
        {
            load();
         }

		/**
		 * Validates the 'hostPort' text field
		 * 
		 * @param value
		 *            Value passed from the config.jelly "hostPort" field
		 *            
		 * @return	An instance of <code>FormValidation</code> containing the validation status.
		 */
		public FormValidation doCheckHostPort(@QueryParameter final String value)
		{
			String trimmedValue =  StringUtils.trimToEmpty(value);
			if (trimmedValue.isEmpty())
			{
				return FormValidation.error(Messages.checkHostPortEmptyError());
			}
			else
			{
				String[] hostPort = trimmedValue.split(COLON);
				if (hostPort.length != 2)
				{
					return FormValidation.error(Messages.checkHostPortFormatError());
				}
				else
				{
					String host = StringUtils.trimToEmpty(hostPort[0]);
					if (host.isEmpty())
					{
						return FormValidation.error(Messages.checkHostPortMissingHostError());
					}

					String port = StringUtils.trimToEmpty(hostPort[1]);
					if (port.isEmpty())
					{
						return FormValidation.error(Messages.checkHostPortMissingPortError());
					}
					else if (StringUtils.isNumeric(port) == false) //NOSONAR
					{
						return FormValidation.error(Messages.checkHostPortInvalidPorttError());
					}
				}
			}

			return FormValidation.ok();
		}

		/**
		 * Validates the 'projectFolder' text field
		 * 
		 * @param value
		 *            Value passed from the config.jelly "projectFolder" field
		 *            
		 * @return	An instance of <code>FormValidation</code> containing the validation status.
		 */
		public FormValidation doCheckProjectFolder(@QueryParameter final String value)
		{
			String trimmedValue = value.trim();
			if (trimmedValue.isEmpty() || trimmedValue.length() == 0)
			{
				return FormValidation.error(Messages.checkProjectEmptyError());
			}

			return FormValidation.ok();
		}

		/**
		 * Validates the 'testSuite' text field
		 * 
		 * @param value
		 *            Value passed from the config.jelly "testSuite" field
		 *            
		 * @return	An instance of <code>FormValidation</code> containing the validation status.
		 */
		public FormValidation doCheckTestSuite(@QueryParameter final String value)
		{
			String trimmedValue = value.trim();
			if (trimmedValue.isEmpty() || trimmedValue.length() == 0)
			{
				return FormValidation.error(Messages.checkTestSuiteEmptyError());
			}
			
			String [] nameList = trimmedValue.split(COMMA);
			if ((nameList.length == 1) && (TotalTestRunnerUtils.isAllTestScenariosOrSuites(nameList[0].trim())))
			{
				return FormValidation.ok();
			}
			
			for (String name : nameList)
			{
				String lcName = name.trim().toLowerCase();
				if (TotalTestRunnerUtils.isAllTestScenariosOrSuites(lcName))
				{
					return FormValidation.error(Messages.testSuiteAllScenariosSuitesError(name));
				}
				
				if ((lcName.endsWith(TEST_SUITE_SUFFIX) == false) && (lcName.endsWith(TEST_SCENARIO_SUFFIX) == false)) //NOSONAR
				{
					return FormValidation.error(Messages.testSuiteError(name));
				}
			}
			
			return FormValidation.ok();
		}
		
		/**
		 * Validates for the 'JCL' text field
		 * 
		 * @param value
		 *            Value passed from the config.jelly "jcl" field
		 *            
		 * @return	An instance of <code>FormValidation</code> containing the validation status.
		 */
		public FormValidation doCheckJcl(@QueryParameter final String value)
		{
			String trimmedValue = value.trim();
			if (trimmedValue.isEmpty() || trimmedValue.length() == 0)
			{
				return FormValidation.error(Messages.checkJCLEmptyError());
			}

			return FormValidation.ok();
		}
		
		/**
		 * Validates for the 'hlsq' field
		 * 
		 * @param value
		 *            Value passed from the config.jelly "hlq" field
		 *            
		 * @return	An instance of <code>FormValidation</code> containing the validation status.
		 */
		public FormValidation doCheckHlq(@QueryParameter final String value)
		{
			String trimmedValue = value.trim().toUpperCase();
			if (trimmedValue.isEmpty() == false) //NOSONAR
			{
				if (trimmedValue.length() >= 8) 
				{		
					return FormValidation.error(Messages.checkHLQLengthError());
				}
				else if (trimmedValue.contains(" ")) //$NON-NLS-1$
				{
					return FormValidation.error(Messages.checkHLQSpacesError());
				}
				else
				{
					if (trimmedValue.matches(DATASET_HLQ_PATTERN) == false) //NOSONAR
					{
						return FormValidation.error(Messages.checkHLQInvalidError());
					}
				}
				
			}
			
			return FormValidation.ok();
		}
		
		/**
		 * Validates the 'ccRepo' field.
		 * 
		 * @param value
		 *            Value passed from the config.jelly "ccRepo" field
		 * 
		 * @return	An instance of <code>FormValidation</code> containing the validation status.
		 */
		public FormValidation doCheckCcRepo(@QueryParameter final String value)
		{
			String trimmedValue = value.trim();
			if (trimmedValue.length() > 44)
			{
				return FormValidation.error(Messages.checkCCRepoLengthError());
			}
			else if (trimmedValue.contains(" ")) //$NON-NLS-1$
			{
				return FormValidation.error(Messages.checkCCRepoSpacesError());
			}

			return FormValidation.ok();
		}
		
		/**
		 * Validates the 'ccTestId' field.
		 * 
		 * @param value
		 *            Value passed from the config.jelly "ccRepo" field
		 * 
		 * @return	An instance of <code>FormValidation</code> containing the validation status.
		 */
		public FormValidation doCheckCcSystem(@QueryParameter final String value)
		{
			String trimmedValue = value.trim();
			if (trimmedValue.length() > 16)
			{
				return FormValidation.error(Messages.checkCCSystemLengthError());
			}
			
			return FormValidation.ok();
		}

		/**
		 * Validates the 'ccTestId' field.
		 * 
		 * @param value
		 *            Value passed from the config.jelly "ccRepo" field
		 * 
		 * @return	An instance of <code>FormValidation</code> containing the validation status.
		 */
		public FormValidation doCheckCcTestId(@QueryParameter final String value)
		{
			String trimmedValue = value.trim();
			if (trimmedValue.length() > 16)
			{
				return FormValidation.error(Messages.checkCCTestIdLengthError());
			}
			
			return FormValidation.ok();
		}
		
		/**
		 * Validates for the 'Login Credential' field
		 * 
		 * @param value
		 *            Value passed from the config.jelly "fileExtension" field
		 * @return validation message
		 */
		public FormValidation doCheckCredentialsId(@QueryParameter final String value)
		{
			if (value.equalsIgnoreCase(EMPTY_STRING))
			{
				return FormValidation.error(Messages.checkLoginCredentialError());
			}

			return FormValidation.ok();
		}

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
			CpwrGlobalConfiguration globalConfig = CpwrGlobalConfiguration.get();
			HostConnection[] hostConnections = globalConfig.getHostConnections();

			ListBoxModel model = new ListBoxModel();
			model.add(new Option(StringUtils.EMPTY, StringUtils.EMPTY, false));

			for (HostConnection connection : hostConnections)
			{
				boolean isSelected = false;
				if (connectionId != null)
				{
					isSelected = connectionId.matches(connection.getConnectionId());
				}

				model.add(new Option(connection.getDescription() + " [" + connection.getHostPort() + ']', //$NON-NLS-1$
						connection.getConnectionId(), isSelected));
			}

			return model;
		}

		/**
		 * Fill in the Code Coverage program types.
		 * 
		 * @param context
		 * 		An instance of <code>context</code> for the Jenkin's context
		 * @param ccPgmType
		 * 		The type of program to test.
		 * @param project
		 * 		An instance of <code>Item</code> for the project.
		 * 
		 * @return	A <code>ListBoxModel</code> instance contain the Code Coverage program types.
		 */
		public ListBoxModel doFillCcPgmTypeItems(@AncestorInPath Jenkins context, @QueryParameter String ccPgmType, @AncestorInPath Item project)
		{
			ListBoxModel ccPgmTypeModel = new ListBoxModel();
			ccPgmTypeModel.add(new Option("Live DB2 - IKJEFT01", DB2, (DB2.equalsIgnoreCase(ccPgmType) ? true : false))); //NOSONAR //$NON-NLS-1$
			ccPgmTypeModel.add(new Option("Live IMS - DFSRCC00", IMS, (IMS.equalsIgnoreCase(ccPgmType) ? true : false))); //NOSONAR //$NON-NLS-1$
			ccPgmTypeModel.add(new Option("TotalTest - TTTRUNNR", TOTALTEST, (ccPgmType == null || ccPgmType.equals("") || TOTALTEST.equalsIgnoreCase(ccPgmType) ? true : false))); //NOSONAR //$NON-NLS-1$ //$NON-NLS-2$

			return ccPgmTypeModel;
		}
		
		/**
		 * Fills in the Login Credential selection box with applicable Jenkins credentials
		 * 
		 * @param context
		 * 		An instance of <code>context</code> for the Jenkin's context
		 * @param credentialsId
		 * 			  The credendtial id for the user.
		 * @param project
		 * 		An instance of <code>Item</code> for the project.
		 * 
		 * @return credential selections
		 * 
		 */
		public ListBoxModel doFillCredentialsIdItems(@AncestorInPath final Jenkins context, @QueryParameter final String credentialsId, @AncestorInPath final Item project)
		{
			List<StandardUsernamePasswordCredentials> creds = CredentialsProvider
					.lookupCredentials(StandardUsernamePasswordCredentials.class, project, ACL.SYSTEM,
							Collections.<DomainRequirement> emptyList());

			StandardListBoxModel model = new StandardListBoxModel();

			model.add(new Option(EMPTY_STRING, EMPTY_STRING, false));

			for (StandardUsernamePasswordCredentials c : creds)
			{
				boolean isSelected = false;

				if (credentialsId != null)
				{
					isSelected = credentialsId.matches(c.getId());
				}

				String description = Util.fixEmptyAndTrim(c.getDescription());
				model.add(new Option(c.getUsername()
						+ (description != null ? " (" + description + ")" : EMPTY_STRING), c.getId(), isSelected)); //$NON-NLS-1$ //$NON-NLS-2$
			}

			return model;
		}
		
		/*
		 * (non-Javadoc)
		 * @see hudson.tasks.BuildStepDescriptor#isApplicable(java.lang.Class)
		 */
		@SuppressWarnings("rawtypes")
		@Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass)
        {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

		/*
		 * (non-Javadoc)
		 * @see hudson.model.Descriptor#getDisplayName()
		 */
		@Override
        public String getDisplayName()
        {
            return Messages.displayNameTotalTest();
        }

		/**
		 * The method is called when the global configuration page is submitted. In the method the data in the web form should
		 * be copied to the Descriptor's fields. To persist the fields to the global configuration XML file, the
		 * <code>save()</code> method must be called. Data is defined in the global.jelly page.
		 * 
		 * @param req
		 *            Stapler request
		 * @param formData
		 *            Form data
		 * @return TRUE if able to configure and continue to next page
		 * 
		 * @throws FormException
		 * 			If an error occurred processing configuration.
		 */
        @Override
        public boolean configure(final StaplerRequest req, final JSONObject formData) throws FormException
        {
            save();
            return super.configure(req,formData);
        }
    }
}
