package com.compuware.jenkins.totaltest;

import java.util.Properties;

import org.jenkinsci.remoting.RoleChecker;

import hudson.remoting.Callable;

/**
 * Get remote system properties
 */
public class RemoteSystemProperties implements Callable<Properties, RuntimeException>
{
	private static final long serialVersionUID = -8859580651709239685L;

	public Properties call()
	{
		return System.getProperties();
	}

	/* (non-Javadoc)
	 * @see org.jenkinsci.remoting.RoleSensitive#checkRoles(org.jenkinsci.remoting.RoleChecker)
	 */
	@Override
	public void checkRoles(RoleChecker checker) throws SecurityException
	{
	}
}
