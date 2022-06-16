### Overview

Compuware's Topaz for Total Test is a testing product with automation to speed testing development and provide higher confidence during program deployment process. The plugin allows Jenkins users to run test scenarios and test suites.
-    Note: The same Topaz Workbench CLI version is required to execute as the Topaz Workbench that supports the functionality of the tests to be executed.
           For example, Functional Test option for Enterprise Data integration requires Workbench version 20.05.01 or later, so any tests that contain Enterprise Data requires Topaz Workbench CLI 20.05.01 or later.

### Change Log

To access the change log, go to
[Total Test Change log](https://github.com/jenkinsci/compuware-topaz-for-total-test-plugin/blob/master/CHANGELOG.md)

### Prerequisites

The following are required to use this plugin:

-   Jenkins
-   Jenkins Credentials Plugin
-   Topaz Workbench CLI. Requires a minimum CLI level of 20.09.02. Refer to the [Topaz Workbench Install
    Guide](http://frontline.compuware.com/Doc/KB/KB1802/PDF/Topaz_Workbench_Install_Guide.pdf) for
    instructions.
-   Topaz for Total Test license.
-   Host Communications Interface

### Installing in a Jenkins Instance

1.  Install the Compuware Topaz for Total Test plugin according to the Jenkins instructions for installing plugins. Dependent plugins will automatically be installed.
2.  Install the Topaz Workbench CLI on the Jenkins instances that will execute the plugin. The Topaz Workbench CLI is available on the Topaz Workbench installation package. If you do not have the installation package, please  visit [go.compuware.com](http://go.compuware.com/). For Topaz Workbench CLI installation instructions, please refer to the[Topaz Workbench Install Guide](http://frontline.compuware.com/Doc/KB/KB1802/PDF/Topaz_Workbench_Install_Guide.pdf)

### Configuring Host Connections
	In order to use Topaz for Total Test you will need to point to an installed Topaz Workbench Command Line Interface (CLI). The Topaz Workbench CLI will work with host connection(s) you also need to configure Topaz for Total Test members.
    - See [Configuring for Topaz Workbench CLI & Host Connections](https://github.com/jenkinsci/compuware-common-configuration-plugin/blob/master/README.md#configuring-for-topaz-workbench-cli--host-connections)
    
### Executing Unit tests

1.  Install the Compuware Topaz for Total Test plugin according to the Jenkins instructions for installing plugins.

2.  In the Jenkins system configuration page's **Topaz Workbench CLI**, point to the Windows and/or Linux installation    location(s) of the CLI. If necessary, change the default values given to match the correct installation location(s).

    **Note**: The Topaz Workbench CLI must be installed on the machine that is configured to run the job.

3.  On the project Configuration page, in the **Build** section click **Add build step** button and select **Topaz for Total Test**.

4.  In the **Host:port** field, enter the z/OS host to connect to.

5.  In the **Login credentials**, select the stored credentials to use for logging onto the host. Alternatively, click **Add** add
    credentials using the Credentials Plugin. Refer to the Jenkins documentation for the Credentials Plugin.

6.  In the **Test Project Folder**, enter the path to the folder containing the Topaz for Total Test project.

7.  In the **Test scenario/suite**, enter the name of the test scenario or test suite to be executed.

    **Note**: Wildcards can be used to select multiple test scenarios or test suites.  
    **Note**: ALL\_SCENARIOS or ALL\_SUITES can be used to select all test.

8.  In the **JCL** field, enter the name of the JCL file to use.

9.  Optionally click the **Code Coverage** button to have Code Coverage information generate during the test run.
    1.  In the Code Coverage Repository field, enter the dataset name of the Code Coverage repository to be used.
    2.  In the **System name** field, enter the system name, If left blank defaults to the test scenario or test suite name.
    3.  In the **Test Id** field, enter a name for test test. If left blank defaults to the test scenario or test suite name.
10. Optionally click the **Execute Options** button to change execution options.
    1.  Check the **Use Stubs** checkbox if the test is to be run using stubs. if Stubs are not going to be used uncheck the checkbox. The default is to use Stubs.
    2.  Check the **Delete temporary files** checkbox if temporary files are to be deleted after the test runs. Uncheck the checkbox if temporary files are to be saved. The default is to delete temporary files.
    3.  In the **High Level Qualifier** field, enter the high level qualifier to be used to allocate z/OS datasets. If specified not defaults to the user id specified in the **Login Credentials**
11. Click **Save**.

# Product Assistance

Compuware provides assistance for customers with its documentation, the Compuware Support Center web site, and telephone customer support.

## Compuware Support Center

You can access online information for Compuware products via our Support Center site at [https://go.compuware.com](https://go.compuware.com/) Support Center provides access to critical information about your Compuware products. You can review frequently asked questions, read or download documentation, access product fixes, or e-mail your questions or comments. The first time you access Support Center, you must register and obtain a password. Registration is free.

Compuware also offers User Communities, online forums to collaborate, network, and exchange best practices with other Compuware solution users worldwide. Go to <http://groups.compuware.com/> to join.

## Contacting Customer Support

At Compuware, we strive to make our products and documentation the best in the industry. Feedback from our customers helps us maintain our quality standards. If you need support services, please obtain the following information before calling Compuware's 24-hour telephone support:

-   The name, release number, and build number of your product. This information is displayed in the **About** dialog box.

-   Installation information including installed options, whether the product uses local or network databases, whether it is installed in the default directories, whether it is a standalone or network installation, and whether it is a client or server installation.

-   Environment information, such as the operating system and release on which the product is installed, memory, hardware and network specification, and the names and releases of other applications that were running when the problem occurred.

-   The location of the problem within the running application and the user actions taken before the problem occurred.

-   The exact application, licensing, or operating system error messages, if any.

You can contact Compuware in one of the following ways:

### Phone

-   USA and Canada: 1-800-538-7822 or 1-313-227-5444.

-   All other countries: Contact your local Compuware office. Contact information is available at [https://go.compuware.com](https://go.compuware.com/)

### Web

You can report issues via Compuware Support Center.

**Note:** Please report all high-priority issues by phone.

### Mail

Customer Support  
Compuware Corporation  
One Campus Martius  
Detroit, MI 48226-5099

## Corporate Web Site

To access Compuware's site on the Web, go to [https://www.compuware.com](https://www.compuware.com/). The Compuware site provides a variety of product and support information.
