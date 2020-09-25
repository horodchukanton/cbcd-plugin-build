# cbcd-plugin-build

Plugin used in gradle projects of the specification tests in CloudBees CD plugins.

## Features:
 - Adds 'io.qameta.allure plugin' to the project, and configures it for the Spock framework
 - Applies 'groovy' plugin to the project
 - Adds system variables for the 'test' task:
    - variables from the *specs/environments/{envName}/systemtest.env* are added and logged
    - variables from the *specs/environments/{envName}/remote-secrets.env* are added and masked in log
 - Sets up some test verbosity properties
 - Sends JSON files generated by Allure to an internal reports server instance
 
## Usage example:

Here's a working minimal build.gradle file:
```
plugins {
    id "com.cloudbees.cd.plugins.specs" version "1.3.0.0"
}

version = "1.0"
description = "EC-JIRA Specs"
sourceCompatibility = 1.8

defaultTasks 'test'

repositories {
    jcenter()
    mavenCentral()
    maven {
        url 'https://oss.sonatype.org/content/repositories/snapshots'
    }
    maven {
        url "https://dl.bintray.com/ecpluginsdev/maven"
    }
}

dependencies {
    implementation 'org.codehaus.groovy:groovy-all:2.4.5:indy'
    implementation 'org.spockframework:spock-core:1.1-groovy-2.4'
    implementation 'com.electriccloud:ec-specs-plugins-core:1.9.2'
    
    // Add other dependencies here
}

test {
    systemProperties['COMMANDER_SERVER'] = findProperty('server') ?: 'localhost'
    systemProperties['COMMANDER_SECURE'] = findProperty('secure') ?: 1
}

sendAllureReports {
    projectName = 'ec-jira'
}

configureTests {
    environmentName = findProperty('envName') ?: 'default'
    readEnvironmentVariables = true
    readSecrets = true
}
```
 
## Configuration:

Plugin adds two separate tasks:

### sendAllureReports
Finalizes the 'test' task.

Usage:
```
sendAllureReports {
  // Mandatory
  projectName '<plugin-name>'
  reportsBaseUrl 'https://plugin-reports.nimbus.beescloud.com/allure-docker-service/'
  
  // Optional, default value is a special GCP resource location
  serverUrl 'http://reports-server:5050/allure-docker-service'

}
``` 

### configureTests
Before test task starts, environment variables will be set up for the task. 

Usage:
```
configureTests {
  // Optional, rvalues are same as the default values
  environmentName = findProperty('envName') ?: 'default'
  environmentLocation = getProject().getProjectDir()
  
  // Warning will be shown if file does not exist
  readSecrets = true
  readEnvironmentVariables = true
}
``` 

Properties that are set on the task are equal to manually specifying:
```
test {
...
  testLogging {
      showStandardStreams = true
      events 'passed', 'skipped', 'failed'
  }
  outputs.upToDateWhen { false }
  systemProperties['EC_SPECS_CLI'] = true
...
}
```


### Changelog
- 1.6.8
  - Handling of missing environment file/properties

- 1.6.6
  - Project property 'disableAllure' disables Allure injection

- 1.6.3 
  - Cleaner long secrets masking
  
- 1.6.0
  - Ability to specify custom 'environments' folder