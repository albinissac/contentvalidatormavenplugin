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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;

import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.fs.io.ZipArchive;
import org.apache.jackrabbit.vault.fs.io.ZipStreamArchive;
import org.apache.maven.plugin.AbstractMojo;


import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathConstants;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;

public abstract class AbstractValidationMojo extends AbstractMojo {

    protected static final String TARGET_EXTENSION = ".zip";
    static ArrayList<String> blacklistedSubPackagesAndBundles;
    private static final String SUBPACKAGE_EXPRESSION = "/jcr_root/etc/packages/.*.zip";

    static
    {
        blacklistedSubPackagesAndBundles=new ArrayList<String>();
        blacklistedSubPackagesAndBundles.add("/jcr_root/etc/packages/adobe/.*.zip");
        blacklistedSubPackagesAndBundles.add("/jcr_root/etc/packages/acs-commons/.*.zip");
        blacklistedSubPackagesAndBundles.add("/jcr_root/apps/.*/install/test.*.jar");
    }

    List<ContentPackageFilter> getFileContent (File contentPackage) throws MojoExecutionException{
        ZipArchive archive = new ZipArchive(contentPackage);
        return getArchiveContent (archive, contentPackage.getName());
    }

    /**
     *
     * @param archive
     * @param filename the filename of the archive
     * @return
     */
    List<ContentPackageFilter> getArchiveContent (Archive archive, String archiveFilename) throws MojoExecutionException{
        List<ContentPackageFilter> content = new ArrayList<ContentPackageFilter>();
        try {
            archive.open(true);
            List<PathFilterSet> filterset= archive.getMetaInf().getFilter().getFilterSets();
            ContentPackageFilter contentPackagefilter=new ContentPackageFilter();
            contentPackagefilter.setArchiveFilename(archiveFilename);
            contentPackagefilter.getFilterSets().addAll(filterset);
            content.add(contentPackagefilter);
            Archive.Entry root = archive.getRoot();
            analyzeEntry (root, "", archive, archiveFilename, content);

        } catch (IOException e) {
            getLog().error("Caught exception while analyzing content package", e);
        } finally {
            archive.close();
        }
        return content;

    }

    /**
     *
     * @param entry
     * @param path
     * @param archive
     * @param archiveFilename
     * @param content
     */
    void analyzeEntry(Archive.Entry entry, String path, Archive archive,String archiveFilename, List<ContentPackageFilter> content) throws  MojoExecutionException {
            boolean negativeMatch = blacklistedSubPackagesAndBundles.stream()
                    .filter((String regex) -> path.matches(regex))
                    .findFirst().isPresent();

            if (negativeMatch) {
                String msg = String.format("policy violation detected, Adobe standard package %s is included", path);
                getLog().error(msg);
                throw new MojoExecutionException("Policy violation detected, please review the execution trace.");
            }

        if (path.matches(SUBPACKAGE_EXPRESSION)) {

            ZipStreamArchive subArchive = null;
            try {
                subArchive = new ZipStreamArchive(archive.openInputStream(entry));
                subArchive.open(true);
                List<PathFilterSet> filterset = subArchive.getMetaInf().getFilter().getFilterSets();
                ContentPackageFilter contentPackagefilter = new ContentPackageFilter();
                String filename = String.format("%s:%s", archiveFilename, path);
                contentPackagefilter.setArchiveFilename(filename);
                contentPackagefilter.getFilterSets().addAll(filterset);
                content.add(contentPackagefilter);
                Archive.Entry rootNode = subArchive.getRoot();
                analyzeEntry(rootNode, "", subArchive, filename, content);

            } catch (IOException e1) {
                String msg = String.format("Error while extracting subpackage %s", path);
                getLog().error(msg, e1);
            } finally {
                if (subArchive != null) {
                    subArchive.close();
                }
            }

        }
        // recurse down the tree
        for (Archive.Entry c : entry.getChildren())
        {
            String childPath = path + "/"+ c.getName();
            try
            {
                analyzeEntry(c, childPath, archive, archiveFilename, content);

            } catch (MojoExecutionException e) {
                throw e;
            }
         }


    }


}

