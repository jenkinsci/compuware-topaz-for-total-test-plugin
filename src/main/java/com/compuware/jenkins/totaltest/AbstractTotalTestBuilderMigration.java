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

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundSetter;

import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import com.compuware.jenkins.common.configuration.HostConnection;

import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Project;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;

public abstract class AbstractTotalTestBuilderMigration extends Builder
{
	private static Logger logger = Logger.getLogger("hudson.AbstractConfiguration"); //$NON-NLS-1$
	private static final String DEFAULT_CODEPAGE = "1047"; //$NON-NLS-1$
	
	protected String connectionId; 

	// Backward compatibility
	protected transient @Deprecated String hostPort; //NOSONAR

	protected transient boolean isMigrated = false; //NOSONAR

	private static final Object lock = new Object();

	/**
	 * Return true if the configuration is migrated.
	 * 
	 * @return true if the configuration is migrated
	 */
	protected boolean isMigrated()
	{
		return isMigrated;
	}

	/**
	 * Called when object has been deserialized from a stream.
	 *
	 * <p>
	 * Data migration:
	 * 
	 * <pre>
	 * In 2.0 "hostPort" was were removed and replaced by a list of host connections. This list is a global and
	 * created with the Global Configuration page. If old hostPort property exist, then a an attempt is made to
	 * create a new host connection with these properties and add it to the list of global host connections, as long as there is
	 * no other host connection already existing with the same properties.
	 * </pre>
	 * 
	 * @return {@code this}, or a replacement for {@code this}.
	 */
	protected Object readResolve()
	{
		logger.fine("Checking host and port for TotalTest project."); //$NON-NLS-1$
		
		// Migrate from 1.X to 2.0
		if (hostPort != null) //NOSONAR
		{
			migrateConnectionInfo();
		}

		return this;
	}

	/**
	 * Migrate configuration information from 1.X to 2.0.
	 * 
	 * <p>
	 * Data migration:
	 * 
	 * <pre>
	 * In 2.0 "hostPort" and "codePage" were removed and replaced by a list of host connections. This list is a global and
	 * created with the Global Configuration page. If old hostPort and codePage properties exist, then a an attempt is made to
	 * create a new host connection with these properties and add it to the list of global host connections, as long as there is
	 * no other host connection already existing with the same properties.
	 * </pre>
	 * 
	 */
	protected void migrateConnectionInfo()
	{
		logger.info("Migrating Total Test Builder to version 2.0 compatibility."); //$NON-NLS-1$
		
		synchronized (lock)
		{
			try
			{
				CpwrGlobalConfiguration globalConfig = CpwrGlobalConfiguration.get();
				if (globalConfig != null)
				{
					HostConnection connection = globalConfig.getHostConnection(hostPort, DEFAULT_CODEPAGE); //NOSONAR
					if (connection == null)
					{
						String description = hostPort + " " + DEFAULT_CODEPAGE; //$NON-NLS-1$ //NOSONAR
						logger.info(String.format("Create new connection for: %s", description));  //$NON-NLS-1$ //NOSONAR
						connection = new HostConnection(description, hostPort, DEFAULT_CODEPAGE, null, null); //NOSONAR
						globalConfig.addHostConnection(connection);

					}
					
					logger.info(String.format("Created new connection for: %s id: %s",connection.getDescription(), connection.getConnectionId()));  //$NON-NLS-1$ //NOSONAR
					connectionId = connection.getConnectionId();
					isMigrated = true;

				}
				else
				{
					logger.severe(String.format("No instance of CpwrGlobalConfiguration"));  //$NON-NLS-1$ //NOSONAR
				}
			}
			catch (Exception e)
			{
				logger.log(Level.SEVERE, String.format("Exception creating host connection. Exception: %s", e.toString()), e);  //$NON-NLS-1$ //NOSONAR
			}
		}
	}
	
	/**
	 * Sets the host and port.
	 * <p>
	 * This method is used in version 1.x pipeline projects to set the host and port(host:port). When 2.0 projects are 
	 * created, they will use the connection id's to identify the connection.
	 * 
	 * @param hostPort
	 * 			The host and port
	 */
	@DataBoundSetter
	@Deprecated
	public void setHostPort(final String hostPort) //NOSONAR
	{
		this.hostPort = hostPort;
	}
	
	/**
	 * Returns the host and port
	 * <p>
	 * This method is used in version 1.x pipeline projects to retrieve the host and port(host:port). When 2.0 project are
	 * created, they will use the connection id's to identify the connection.
	 * 
	 * @return	The host and port.
	 */
	@Deprecated
	public String getHostPort() //NOSONAR
	{
		return hostPort;
	}

	@Initializer(after = InitMilestone.JOB_LOADED, before = InitMilestone.COMPLETED)
	public static void jobLoaded() throws IOException
	{
		logger.fine("Initialization milestone: All jobs have been loaded"); //$NON-NLS-1$
		Jenkins jenkins = Jenkins.getInstance();

		for (Project<?, ?> project : jenkins.getItems(Project.class))
		{
			logger.fine("Name: " + project.getName() + " Display Name: " + project.getDisplayName());
			boolean projectNeedsSave = projectNeedsSave(project);
			if (projectNeedsSave)
			{
				try
				{
					project.save();
					logger.info(String.format("Project %s has been migrated.", project.getFullName())); //$NON-NLS-1$ //NOSONAR
				}
				catch (IOException e)
				{
					logger.log(Level.SEVERE, String.format("Failed to upgrade job %s", project.getFullName()), e); //$NON-NLS-1$
				}
			}
		}
	}
	
	/**
	 * Determines if a project needs to be saved.
	 * <p>
	 * A project will need to be save if the project contains a TotalTest build step and the project
	 * has been migrated to a new release.
	 * 
	 * @param project
	 * 			An instance of <code>Project</code> for nthe project.
	 * 
	 * @return	<code>true</code> if the project needs to be saved, otherwise <code>false</code>.
	 */
	private static boolean projectNeedsSave(Project<?, ?> project)
	{
		boolean projectNeedsSave = false;
		
		List<Builder> builders = project.getBuilders();
		if (builders != null)
		{
			logger.fine(String.format("Project %s has %s builders.", project.getFullName(), Integer.toString(builders.size()))); //$NON-NLS-1$ //NOSONAR
			for (Builder builder : builders)
			{
				logger.fine(String.format("Processing builder: %s", builder.getClass().getName())); //$NON-NLS-1$ //NOSONAR
				if ((builder instanceof  AbstractTotalTestBuilderMigration) &&  ((AbstractTotalTestBuilderMigration)builder).isMigrated())
				{
					projectNeedsSave = true;
					break;
				}
			}
		}
		
		return projectNeedsSave; 
		
	}
}

