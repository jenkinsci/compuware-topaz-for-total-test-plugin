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

1. Install the BMC AMI DevX Total Test plugin according to the Jenkins instructions for installing plugins.
2. In the Jenkins system configuration page under the Common Configurations section, supply the path to the CLI installation directory either for the Windows Workbench for Eclipse CLI home or for the Linux Workbench for Eclipse CLI home depending on the requirement. If necessary, change the default values given to match the correct installation location(s)
3. In the Jenkins system configuration page under the Common Configurations section, we can add HCI Host, Port, CES URL, Code page and Encryption protocol etc under the sub section Host Connections. For this you need to click on "+Add Host Connection" button and fill up the fields. Out of the 7 fields, first 2 are mandatory i.e. Description and Host:Port are mandatory fields.
   Note: The BMC AMI DevX Workbench CLI must be installed on the machine that is configured to run the job.
4. On the project Configuration page, in the Build section click Add build step button and select Total Test - Execute Total Test Scenarios. (Avoid using Total Test - Execute Unit Tests(deprecated))
5. In the Host:port field, select the z/OS host and port of your choice out of the available host: port options.
6. In Jenkins, navigate to Manage Jenkins → System → Host Connections → Login Credentials. From this section, you can select the stored credentials that Jenkins should use to log in to the target host.
7. Alternatively, you can add credentials directly at the job level by going to: Job → Configure → Build Steps and then click Add Credentials. This requires the Credentials Plugin, which allows you to securely store and manage authentication details within Jenkins.
8. In the Test Folder Path field, enter the path to the folder containing the BMC AMI DevX Total Test project.
9. Use Scenario files checkbox for selecting or unselecting execution of functional test scenarios only.
10. The path to the JCL file to use when executing. ‘test scenario' files.
11. Optionally click the Code Coverage button to have Code Coverage information generated during the test run.
    1. In the Code Coverage Repository field, enter the dataset name of the Code
    Coverage repository to be used.
    2. In the System name field, enter the system name, If left blank defaults to the test
    scenario or test suite name.
    3. In the Test Id field, enter a name for test test. If left blank defaults to the test
    scenario or test suite name.
12. Test Execution section: Optionally Select Programs to Execute – Choose which programs to run during the test execution.
    1. Selected Programs – Manually pick specific programs for execution - Optional comma separated list of tests to execute. Use the Test list field to enter a comma separated list of program names to be tested. Will only include test scenarios that have component under test defined as one of these
    Note: This field is only used for Total Test CLI version 20.04.01 and later
    2. JSON file – Provide a JSON file containing the list of programs to execute - Optional JSON file containing tests to execute. Use the JSON file field to enter a JSON file containing the tests to execute.
    Note: This field is only used for Total Test CLI version 20.04.01 and later
    3. Job Accounting Information – Enter details for tracking job execution and resource usage.
    4. Optionally Stop if test fails or threshold is reached – Stop if test fails or threshold is reached (default is true)
    5. Optionally Halt at failure – Terminate execution immediately after the first test case fails.
    6. Optionally Halt pipeline if errors occur – Stop the entire pipeline if any error occurs during execution.
    7. Optionally Context Variables – Define variables to pass contextual data into the test execution - Execution context variable in the format "field1=value1, field2=value2".
13. Optional Report and Logging Section Checkbox for Upload to Server - If enabled, results will be published to the Total Test repository server, allowing centralized access and tracking.
    1. Text input field for Source Folder - Users need to supply the path for the program source code.
    2. Drop down field for SonarQube Version - Specifies the version of SonarQube being targeted (Version 6 as default).
    3. Checkbox for Report - Enables the creation of a general report summarizing the test execution or analysis
    4. Checkbox for Result - Specifies whether a detailed result file should be generated, often used for further analysis or archiving.
    5. Checkbox for Sonar Report - Determines if a report compatible with SonarQube should be created. SonarQube is a tool for continuous inspection of code quality.
    6. Checkbox for JUnit Report- Enables generation of a JUnit-style report, commonly used in Java testing frameworks and CI/CD pipelines.
    7. Drop down field for Logging Level - Controls the verbosity of logs. INFO is the default selection.
14. Click Save.

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


