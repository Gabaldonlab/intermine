package org.intermine.bio.dataconversion;

import java.io.File;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.apache.commons.lang.StringUtils;



import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;


/**
 * 
 * @author
 */
public class CstrainsConverter extends BioFileConverter
{
	
    protected static final Logger LOG = Logger.getLogger(CstrainsConverter.class);

	
    //
    private static final String DATASET_TITLE = "CandidaMine";
    private static final String DATA_SOURCE_NAME = "Strains and Isolates";

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public CstrainsConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     * 
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        File currentFile = getCurrentFile();
        Pattern filePattern = Pattern.compile("^(\\S+).strains");
	Matcher matcher = filePattern.matcher(currentFile.getName());

        if (matcher.find()) {
            String organismTaxonId = matcher.group(1);
        	LOG.info("Loading data from Organims with taxonId " + organismTaxonId + ", From File " + currentFile.getName());

            String organismRefId = getOrganism(organismTaxonId);
            Iterator<?> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
            while (lineIter.hasNext()) {
                String[] lineTokens = (String[]) lineIter.next();
            


		// Strain Info
                String strainName = lineTokens[0];
                String strainTaxonId = lineTokens[1];

                Item strain = createItem("Strain");
		strain.setAttribute("name", strainName);
		if (!StringUtils.isEmpty(strainTaxonId)) { 
                	strain.setAttribute("taxonId", strainTaxonId);
		}
                strain.setReference("organism",organismRefId);
		// 
		// Isolate info
		String hostTaxonId = lineTokens[2];
		String isolateSite = lineTokens[3];
		String isolateCountry = lineTokens[4];
		String isolateMating = lineTokens[5];
		Item isolate = createItem("Isolate");
		if (!StringUtils.isEmpty(isolateCountry))
			isolate.setAttribute("country",isolateCountry);
		if (!StringUtils.isEmpty(isolateMating))
			isolate.setAttribute("mating",isolateMating);	
                try {
                    store(strain);
		    String strainRefId = strain.getIdentifier();
		    isolate.setReference("strain",strainRefId);
		    isolate.setReference("organism",organismRefId);
		    if (!StringUtils.isEmpty(hostTaxonId)) {
		    	String hostRefId = getOrganism(hostTaxonId);
		    	isolate.setReference("host",organismRefId);
		    }
		    store(isolate);
                } catch (ObjectStoreException e) {
                    throw new RuntimeException("failed to store strian " + strainName +" with taxonId : " + strainTaxonId, e);
                }
                
            }
            
            
            
        }
        else
        {
        	// do not know the parent organism of the strains ??
        }
    	
    	
    }
}

