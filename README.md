# contentvalidatormavenplugin

The main concern in the AEM multi tenant environment is installing the content package with wrong filters - the root path that is more wide or AEM platform specific

This maven plug-in helps to validate the AEM content packages for below restrictions

- Check if the specific sub package is allowed to be deployed
- Check if the specific bundle is allowed to be deployed
- Check if the content package filter.xml contains the root path that is more wide or AEM platform specific that impact the other tenants in the platform

The restricted Sub package and bundle paths can be configured in AbstractValidationMojo.java as a regular expression- blacklistedSubPackagesAndBundles
The platform level filter restrictions can be configured in ContentValidationMojo.java as a regular expression - platformFilterRestrictionPaths

The violations are logged as a Error and also the build is aborted.

##Steps to run the plug-in

- Clone and Install the plug-in
- git clone https://github.com/albinissac/contentvalidatormavenplugin.git
- mvn clean install

##Run in standalone mode 

mvn com.contentpackage.validator:contentpackagevalidator-maven-plugin:validate -Dvalidation.filename=<Content Package path>

This mode can be used for any content packages - packages build through Maven build or packages created through package manager.


##Run as part of Maven build
 Add the below plug-in configurations to the pom.xml of the package

<plugin>
<groupId>com.contentpackage.validator</groupId>
<artifactId>contentpackagevalidator-maven-plugin</artifactId>
<version>1.0-SNAPSHOT</version>
<executions>
<execution>
<goals>
<goal>validate</goal>
</goals>
</execution>
</executions>
</plugin>

