# Change Log 

### Version 2.3.1

-   Updated for documentation in GitHub
    
### Version 2.3.0

-   Updated Functional test to execute in the standard Compuware CLI
    environment.
	
**Functional test requires version 20.01.01 or higher of the Topaz
Workbench CLI.**

**Functional Test tests that contain 'SQL Select' or 'SQL Update'
palette elements require DB2 jar files on the classpath for using
JDBC to access DB2 on the mainframe, and these jar files must be 
available. By default the Functional Test CLI looks for these jar
files in <install directory>/dbDrivers. The user must create the
directory and copy the user specific DB2 jar files to this directory.
Typically there are two jar files, one with the DB2 driver and one 
with the DB2 license. They are typically named db2jcc.jar and 
db2jcc_license_cisuz.jar.**

### Version 2.2.3

-   Updated Functional test to generate zAdviser events for Jenkins.

**Support for generating Jenkins zAdviser events from Total Test
Functional test requires version 19.06.01 or higher of the Topaz
Workbench CLI.**

### Version 2.2.2

-   Fixed various problems running Unit test and Functional test on
    Jenkins slave.

**Support for executing a Total Test Functional test requires version
19.05.03 or higher of the Topaz Workbench CLI.**

### Version 2.2.1

-   Added support for accounting information when executing a Topaz for
    Total Test Functional Test test.

**Support for Topaz for accounting information when executing a Total
Test Functional test requires version 19.05.01 or higher of the Topaz
Workbench CLI.**

### Version 2.2.0

-   Added support for executing Topaz for Total Test Functional Test
    test cases.
    -   **Support for Topaz for Total Test Functional test requires
        version 19.04.03 or higher of the Topaz Workbench CLI.**
-   Added support for secure connections when executing Topaz for Total
    Test Unit Test test cases.
    -   **Support for secure connections requires Compuware Common
        Configuration 1.0.7 or higher.**

### Version 2.1.3

-   Added support for Compuware Common Configuration 1.0.7

** Lower versions of Topaz for Total Test plugins do not support
Compuware Common Configuration 1.0.7.**

### Version 2.1.2

-   Fix various spelling errors.

### Version 2.1.1

-   Added "-jenkins" to the generated Topaz Workbench CLI  command line
    for zAdviser support.

** zAdviser support requires **Topaz Workbench CLI **version 19.2.1 or
higher.**

### Version 2.1.0

**This release requires Topaz Workbench CLI version 18.2.4 or higher.**

-   Support to clear statistics on a Code Coverage repository for a
    System and Test Id.
-   Allow Test Project Folder to be specified as a relative path from
    project workspace directory.
-   Checks to verify Topaz Workbench CLI is required minimum release.

### Version 2.0.2

-   Fixed creation of the -data argument for Linux.

### Version 2.0.1

-   Fixed display and processing of CodeCoverage information.

### Version 2.0.0

-   The plugin now integrates with the [Compuware Common
    Configuration](https://plugins.jenkins.io/compuware-common-configuration){.external-link} plugin
    which allows the Host Connection configurations and Topaz Workbench
    CLI to be defined centrally for other Compuware Jenkins plugins
    instead of needing to be specified in each Jenkins project's
    configuration.  Host Connection configuration is now defined in the
    Jenkins/Manage Jenkins/Configure System screen. 

### Version 1.8.2

-   Added display of Jenkins and Compuware Topaz for Total versions in
    Jenkins Console log.
-   Fix Linux global configuration

### Version 1.8

-   Initial release.
