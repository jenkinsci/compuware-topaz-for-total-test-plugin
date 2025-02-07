# Change Log
### Version 2.4.16

Updated the following

-	the label for Use Scenario Check box as ‘Execute functional test scenarios only’ and
-	text for the help tip for the Use Scenario Check box as This will execute functional test only using the .scenarios 		files instead of the .context files which are meant for both Functional and Unit tests.

### Version 2.4.15

Updated the Add build step option "Total Test - Execute unit tests" to "Total Test - Execute unit tests (deprecated)"

### Version 2.4.14

**This release requires BMC AMI DevX Workbench CLI 20.14.01 or later.**

-    Rebranidng changes for Title.
-    Updated pom.xml for plugin name, description and developer email address.


### Version 2.4.8

**This release requires BMC AMI DevX Workbench CLI 20.14.01 or later.**

-    Updated entire plugin for rebranding changes.
-    Updated compuware-common-configuration version to 1.0.16.
-    Updated org.jenkins-ci.plugins credentials version to 2.6.1.1.

### Version 2.4.7

**This release requires Topaz Workbench CLI 20.09.02 or later.**

-    Updated change log.

### Version 2.4.6

**This release requires Topaz Workbench CLI 20.09.02 or later.**

-    Honor the HCI connection encryption setting

### Version 2.4.5

**This release requires Topaz Workbench CLI 20.06.01 or later.**

-    Updated change log.

### Version 2.4.4

**This release requires Topaz Workbench CLI 20.06.01 or later.**

-    Corrections for failed plugin publish.

### Version 2.4.3

**This release requires Topaz Workbench CLI 20.06.01 or later.**

-    Updated required minimum credentials version to 2.3.19
-    Updated required minimum Jenkins version to 2.222.4
-    Updated required minimum SLF4j version to 1.7.26

### Version 2.4.2

**This release requires  Topaz Workbench CLI 20.06.01 or later.**

-    Corrected error with plug-in not finding results file when an absolute file path specified for the test.

### Version 2.4.1

**This release requires Topaz Workbench CLI 20.06.01 or later.**

-    Corrected error with program-names argument.
-    Removed Report Folder
-    Removed vesion text labels

### Version 2.3.12

-    Corrected error with invalid quoting of parameters.

### Version 2.3.11

-    Changed Enterprise Data connection to host and port.

### Version 2.3.10

-    Added server credentials when using Topaz Workbench CLI version 20.05.01 or later.
-    Added new Functional Test option to supply JCL when executing .testscenario files when using Topaz Workbench CLI version 20.05.01 or later.
-    Added new Functional Test option to supply replaceable Context Variables when using Topaz Workbench CLI version 20.05.01 or later.
-    Added new Functional Test option for Enterprise Data integration when using WorkbenchCLI CLI version 20.05.01 or later.

### Version 2.3.9

- Misc bug fixes.

### Version 2.3.8

##### Non-Virtualized Testing(Functional Test)

-    Redesigned UI for better grouping of options.
-    When running with WorkbenchCLI CLI before version 20.04.01, the Select Programs to Execute options will not be used.
-    Added new Functional Test option to allow pipeline scripts to continue to execute even where failures occur in the tests.
-    Added option execute with configuration either from the Repository Server or from a local configuration directory when using WorkbenchCLI CLI version 20.04.01 or later.

### Version 2.3.7

**This release requires Workbench 19.06.04 or above.**

##### Non-Virtualized Testing(Functional Test)

-    Modified location of output results to be in the "Output" folder for WorkbenchCLI CLI version 20.03.01 and higher.
-    The report folder specification is not used with WorkbenchCLI CLI version 20.03.01 and higher.
-    Added new Functional Test file extensions introduced in WorkbenchCLI 20.02.01.

##### Virtualized Testing(Unit Test)

-   Added ability to recurse a project, containing multiple test folders, and execute test scenarios contained in those test folders.

### Version 2.3.5

-	Fixed Functional test exception when analyzing test results.

### Version 2.3.4

-	Fixed Functional test exception when analyzing test results.
-	Updated Functional test to support WorkbenchCLI 20.02 new result extension .suiteresult

### Version 2.3.2

-   Updated Functional test to execute with Topaz Workbench CLI 19.06.04 and higher.
     
### Version 2.3.1

-   Updated for documentation in GitHub 
    
### Version 2.3.0

-   Updated Functional test to execute in the standard Compuware CLI
    environment.
	
**Functional test requires version 20.01.01 or higher of the Topaz Workbench CLI.**

**Functional Test tests that contain 'SQL Select' or 'SQL Update' palette elements require DB2 jar files on the classpath for using JDBC to access DB2 on the mainframe, and these jar files must be available. By default the Functional Test CLI looks for these jarfiles in <install directory>/dbDrivers.The user must create the directory and copy the user specific DB2 jar files to this directory. Typically there are two jar files, one with the DB2 driver and one with the DB2 license. They are typically named db2jcc.jar and db2jcc_license_cisuz.jar.**

### Version 2.2.3

-   Updated Functional test to generate zAdviser events for Jenkins.

**Support for generating Jenkins zAdviser events from Total Test Functional test requires**
**version 19.06.01 or higher of the Topaz  Workbench CLI.**

### Version 2.2.2

-   Fixed various problems running Unit test and Functional test on
    Jenkins slave.

**Support for executing a Total Test Functional test requires version 19.05.03 or higher of the Topaz Workbench CLI.**

### Version 2.2.1

-   Added support for accounting information when executing a Topaz for
    Total Test Functional Test test.

**Support for Topaz for accounting information when executing a Total Test Functional test requires version 19.05.01** 
**or higher of the Topaz Workbench CLI.**

### Version 2.2.0

-   Added support for executing Topaz for Total Test Functional Test
    test cases.
    -   **Support for Topaz for Total Test Functional test require version 19.04.03 or higher of the Topaz Workbench CLI.**
-   Added support for secure connections when executing Topaz for Total
    Test Unit Test test cases.
    -   **Support for secure connections requires Compuware Common Configuration 1.0.7 or higher.**

### Version 2.1.3

-   Added support for Compuware Common Configuration 1.0.7

**Lower versions of Topaz for Total Test plugins do not support Compuware Common Configuration 1.0.7.**

### Version 2.1.2

-   Fix various spelling errors.

### Version 2.1.1

-   Added "-jenkins" to the generated Topaz Workbench CLL command line for zAdviser support.

**zAdviser support requires Topaz Workbench CLI  version 19.2.1 or higher.**

### Version 2.1.0

**This release requires Topaz Workbench CLI version 18.2.4 or higher.**

-   Support to clear statistics on a Code Coverage repository for a System and Test Id.
-   Allow Test Project Folder to be specified as a relative path from project workspace directory.
-   Checks to verify Topaz Workbench CLI is required minimum release.

### Version 2.0.2

-   Fixed creation of the -data argument for Linux.

### Version 2.0.1

-   Fixed display and processing of CodeCoverage information.

### Version 2.0.0

-   The plugin now integrates with the [Compuware Common Configuration](https://plugins.jenkins.io/compuware-common-configuration)              plugin which allows the Host Connection configurations and Topaz Workbench CLI to be defined centrally for other Compuware Jenkins plugins instead of needing to be specified in each Jenkins project's configuration. Host Connection configuration is now defined in the Jenkins/Manage Jenkins/Configure System screen.

### Version 1.8.2

-   Added display of Jenkins and Compuware Topaz for Total versions in Jenkins Console log.
-   Fix Linux global configuration

### Version 1.8

-   Initial release.
