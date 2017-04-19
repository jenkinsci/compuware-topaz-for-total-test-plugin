package com.compuware.jenkins.totaltest;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.matchers.IdMatcher;

import hudson.model.Item;
import hudson.model.TaskListener;
import hudson.security.ACL;

public class TotalTestRunnerUtils
{
	private static final String COLON = ":"; //$NON-NLS-1$
	private static final String DOUBLE_QUOTE = "\"";
	private static final String DOUBLE_QUOTE_ESCAPED = "\"\"";
	
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
	 * @param isShell
	 *            <code>true</code> if the script is a Shell script, <code>false</code> if it is a Batch script
	 * 
	 * @return the escaped <code>String</code>
	 */
	public static String escapeForScript(final String input, final boolean isShell)
	{
		String output = null;

		if (input != null)
		{
			// escape any double quotes (") with another double quote (") for both batch and shell scripts
			output = StringUtils.replace(input, DOUBLE_QUOTE, DOUBLE_QUOTE_ESCAPED);

			// wrap the input in quotes
			output = wrapInQuotes(output, isShell);
		}

		return output;
	}
	
	
	/**
	 * Wrap a string in quotes.
	 * 
	 * @param text
	 *            the string to wrap in quotes
	 * @return the quoted string
	 */
	private static String wrapInQuotes(final String text, final boolean isShell)
	{
		String quotedValue = text;
		if (text != null)
		{
			quotedValue = String.format("\"%s\"", text); //$NON-NLS-1$
		}
		
		return quotedValue;
	}
}
