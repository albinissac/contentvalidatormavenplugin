package com.contentpackage.validator;

import java.util.List;
import java.util.ArrayList;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;

public class ContentPackageFilter
{
    private List<PathFilterSet> filterSets=new ArrayList<PathFilterSet>();
    private String archiveFilename;

    public List<PathFilterSet> getFilterSets()
    {
        return filterSets;
    }

    public String getArchiveFilename()
    {
        return archiveFilename;
    }

    public void setArchiveFilename(String archiveFilename)
    {
        this.archiveFilename=archiveFilename;
    }

}