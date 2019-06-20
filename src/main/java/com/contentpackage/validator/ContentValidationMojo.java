package com.contentpackage.validator;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;

/**
 * Goal which validate the AEM content package.
 * @goal validate1
 * @phase VERIFY
 */
@Mojo (name="validate", defaultPhase=LifecyclePhase.VERIFY, requiresProject=false )
public class ContentValidationMojo extends AbstractValidationMojo
{
    static ArrayList<String> platformConfigurationPaths;
    @Parameter (property="validation.filename", defaultValue="${project.build.directory}/${project.build.finalName}")
    private File target;
    static
    {
        platformConfigurationPaths=new ArrayList<String>();
        platformConfigurationPaths.add("/etc/cloudservices[/]?(dynamictagmanagement|search-promote|gtm)[/]?");
        platformConfigurationPaths.add("/apps/sling.*");
        platformConfigurationPaths.add("/etc[/]?(rep:policy)?");
        platformConfigurationPaths.add("/etc/key.*");
        platformConfigurationPaths.add("/apps/system/config.*");
        platformConfigurationPaths.add("/apps/cq.*");
        platformConfigurationPaths.add("/libs.*");
        platformConfigurationPaths.add("/etc/tags[/]?");
        platformConfigurationPaths.add("/content[/]?(rep:policy)?");
        platformConfigurationPaths.add("/content/dam[/]?(rep:policy)?");
        platformConfigurationPaths.add("/etc/designs[/]?");
        platformConfigurationPaths.add("/etc/clientlibs[/]?");
        platformConfigurationPaths.add("/apps/granite.*");
        platformConfigurationPaths.add("/rep:policy");
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        if (!target.getName().endsWith(TARGET_EXTENSION)) {
            target = new File (target.getAbsolutePath() + TARGET_EXTENSION);
        }
        if (!target.exists()) {
           getLog().error(String.format("File %s does not exist", target.getAbsolutePath()));
          return;
        }
        reportViolations(validatePackage(target));
    }

    /**
     * Validate the content package
     * @param zipFile the input file
     * @return the policy violations
     */
    private List<String> validatePackage(File zipFile) throws MojoExecutionException{
        List<String> policyViolations = new ArrayList<>();
        List<ContentPackageFilter> violations = getFileContent(zipFile).stream()
                .filter (cpe -> filterPathViolations(cpe,policyViolations))
                .collect(Collectors.toList());
        return policyViolations;
    }
    /**
     * report policy violations
     * @param policyViolations
     * @throws MojoExecutionException
     */
    private void reportViolations(List<String> policyViolations) throws MojoExecutionException {
        if (policyViolations.size() > 0) {
            policyViolations.forEach(s -> getLog().error(s));
            throw new MojoExecutionException("Policy violation detected, please review the execution trace.");
        }
    }
    /**
     * validate a content package entry against the path filter rules
     * @param cpe
     * @param policyViolations the list of violations
     * @return false if the violation is deteced, true otherwise
     */
    boolean filterPathViolations (ContentPackageFilter cpe, List<String> policyViolations){
        boolean isComplient=true;
        List<PathFilterSet> filters=cpe.getFilterSets();
        for(PathFilterSet filter: filters)
        {
            boolean positiveMatch = platformConfigurationPaths.stream()
                    .filter((String regex) -> filter.getRoot().matches(regex))
                    .findFirst().isPresent();
            if (positiveMatch) {
                isComplient=false;
                String msg = String.format("[%s] detected violation of path rule: [%s]", cpe.getArchiveFilename(),filter.getRoot());
                policyViolations.add(msg);
            }
        }
        return isComplient;
    }
}