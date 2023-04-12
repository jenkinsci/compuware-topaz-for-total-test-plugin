# <img src="images/bmc_brandmark.png" width="45" height="45">BMC AMI DevX Total Test

## Overview

BMC AMI DevX Total Test is a testing product with automation to speed testing development and provide higher confidence during program deployment process. The plugin allows Jenkins users to run test scenarios and test suites.
-    Note: The same BMC AMI DevX Workbench CLI version is required to execute as the BMC AMI DevX Workbench that supports the functionality of the tests to be executed.
           For example, Functional Test option for BMC AMI DevX Data Studio (Previously called Enterprise Data) integration requires Workbench version 20.05.01 or later, so any tests that contain DevX Data requires BMC AMI DevX Workbench CLI 20.05.01 or later.

### Change Log

To access the change log, go to
[Total Test Change log](https://github.com/jenkinsci/compuware-topaz-for-total-test-plugin/blob/master/CHANGELOG.md)

### Prerequisites

The following are required to use this plugin:

-   Jenkins
-   Jenkins Credentials Plugin
-   BMC AMI DevX Workbench CLI.Refer to the [BMC AMI DevX Workbench for Eclipse Installation Guide](https://docs.bmc.com/docs/x/Lk5QRw) for
    instructions.
-   BMC AMI DevX Total Test license.
-   Host Communications Interface

### Installing in a Jenkins Instance

1.  Install the BMC AMI DevX Total Test plugin according to the Jenkins instructions for installing plugins. Dependent plugins will automatically be installed.
2.  Install the BMC AMI DevX Workbench CLI on the Jenkins instances that will execute the plugin. The BMC AMI DevX Workbench CLI is available on the Workbench installation package. If you do not have the installation package, please  visit [bmc.com](http://bmc.com/). For BMC AMI DevX Workbench CLI installation instructions, please refer to the[BMC AMI DevX Workbench for Eclipse Installation Guide](https://docs.bmc.com/docs/x/Lk5QRw)

### Configuring Host Connections
	In order to use BMC AMI DevX Total Test you will need to point to an installed BMC AMI DevX Workbench Command Line Interface (CLI). The BMC AMI DevX Workbench CLI will work with host connection(s) you also need to configure Total Test members.
    - See [Configuring for BMC AMI DevX Workbench CLI & Host Connections](https://github.com/jenkinsci/compuware-common-configuration-plugin/blob/master/README.md#configuring-for-topaz-workbench-cli--host-connections)
    
### Executing Unit tests

1.  Install the BMC AMI DevX Total Test plugin according to the Jenkins instructions for installing plugins.

2.  In the Jenkins system configuration page's **BMC AMI DevX Workbench CLI**, point to the Windows and/or Linux installation    location(s) of the CLI. If necessary, change the default values given to match the correct installation location(s).

    **Note**: The BMC AMI DevX Workbench CLI must be installed on the machine that is configured to run the job.

3.  On the project Configuration page, in the **Build** section click **Add build step** button and select **BMC AMI DevX Total Test**.

4.  In the **Host:port** field, enter the z/OS host to connect to.

5.  In the **Login credentials**, select the stored credentials to use for logging onto the host. Alternatively, click **Add** add
    credentials using the Credentials Plugin. Refer to the Jenkins documentation for the Credentials Plugin.

6.  In the **Test Project Folder**, enter the path to the folder containing the BMC AMI DevX Total Test project.

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

BMC provides assistance for customers with its documentation, the BMC Support Center web site, and telephone customer support.

## BMC Support Center

You can access online information for BMC products via our Support Center site at [https://support.bmc.com](https://support.bmc.com/) Support Center provides access to critical information about your BMC products. You can review frequently asked questions, read or download documentation, access product fixes, or e-mail your questions or comments. The first time you access Support Center, you must register and obtain a password. Registration is free.

BMC also offers User Communities, online forums to collaborate, network, and exchange best practices with other BMC solution users worldwide.

## Contacting Customer Support

At BMC, we strive to make our products and documentation the best in the industry. Feedback from our customers helps us maintain our quality standards. If you need support services, please obtain the following information before calling BMC's 24-hour telephone support:

-   The name, release number, and build number of your product. This information is displayed in the **About** dialog box.

-   Installation information including installed options, whether the product uses local or network databases, whether it is installed in the default directories, whether it is a standalone or network installation, and whether it is a client or server installation.

-   Environment information, such as the operating system and release on which the product is installed, memory, hardware and network specification, and the names and releases of other applications that were running when the problem occurred.

-   The location of the problem within the running application and the user actions taken before the problem occurred.

-   The exact application, licensing, or operating system error messages, if any.

You can contact BMC in one of the following ways:

### Web

You can report issues via the BMC Support web site: [BMC Support](https://support.bmc.com/).
-   All other countries: Contact your local BMC office. Contact information is available at [Contact BMC](https://www.bmc.com/contacts-locations/united-states.html)


## Corporate Web Site

To access BMC site on the Web, go to [https://www.bmc.com/](https://www.bmc.com/). The BMC site provides a variety of product and support information.


