package org.intermine.bio.dataconversion;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.log4j.Logger;


import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.metadata.TypeUtil;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;



/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


/**
 * 
 * @author
 */
public class CgdPhenotypeConverter extends BioFileConverter
{
    //
    private static final String DATASET_TITLE = "Add DataSet.title here";
    private static final String DATA_SOURCE_NAME = "Add DataSource.name here";

    
    protected static final Logger LOG = Logger.getLogger(CgdPhenotypeConverter.class);

    private Map<String, String> identifiersToFeatures = new HashMap<String, String>();
    private Map<String, String> strians = new HashMap<String, String>();
    private Map<MultiKey, String> phenotypes = new HashMap<MultiKey, String>();
    private Map<String, String> experimentTypes = new HashMap<String, String>();
    private Map<String, String> mutantTypes = new HashMap<String, String>();
    
    private Map<String, String> apoTerms = new HashMap<String, String>();

    
    private Map<String, String> alleles = new HashMap<String, String>();
    private Map<String, String> pubmeds = new HashMap<String, String>();
    private Map<String, String> conditions = new HashMap<String, String>();
    private Map<String, String> chemicals = new HashMap<String, String>();


    protected IdResolver rslv;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public CgdPhenotypeConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    
    
    
    
    private String taxonId  = null;
    private String refOragism = null;
    public void setTaxonId(String taxonId)
    {
    	this.taxonId = taxonId;
    }
    
    
    /**
     * 
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
    	if (rslv == null) {
    		rslv = IdResolverService.getCgdIdResolver();
    	}
    	
    	refOragism = getOrganism(taxonId);
    	
    	
    	Iterator<?> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);	
    	while (lineIter.hasNext()) {
    	
    	// File Format
//    	1) Feature Name (Mandatory)		-The feature name of the gene
//    	2) Feature Type (Mandatory)		-The feature type of the gene	
//    	3) Gene Name (Optional)			-The standard name of the gene
//    	4) CGDID (Mandatory)			-The CGDID of the gene
//    	5) Reference (CGD_REF Required,         -PMID: ####|CGD_REF: #### (separated by pipe)(one reference per row)
//    	              PMID optional)				
//    	6) Experiment Type (Mandatory)		-The method used to detect and analyze the phenotype
//    	7) Mutant Type (Mandatory) 		-Description of the impact of the mutation on activity of the gene product
//    	8) Allele (Optional)			-Allele name and description, if applicable
//    	9) Strain background (mandatory) 	-Genetic background in which the phenotype was analyzed
//    	10) Phenotype (Mandatory)		-The feature observed and the direction of change relative to wild type
//    	11) Chemical (Optional)			-Any chemicals relevant to the phenotype
//    	12) Condition (Optional)		-Condition under which the phenotype was observed
//    	13) Details (Optional)			-Details about the phenotype
//    	14) Reporter (Optional)			-The protein(s) or RNA(s) used in an experiment to track a process
//    	15) Anatomical Structure (Optional)     -The Fungal Anatomy Ontology term
//    	                                         that denotes the affected structure for an anatomical phenotype
//    	16) Virulence Model (Optional)		-The model system used to assess the virulence of a mutant
//    	17) Species
    		
    		
    		
    		String[] lineTokens = (String[]) lineIter.next();
    		process(lineTokens);
    	}
    }
    
    
    void process(String[] tokens) throws ObjectStoreException  {
    	
    	// get Feature
    	String featureId  = tokens[3];
    	String featureType = tokens[1];
    	String featureItemRef = getFeature(featureId,featureType);
    	

    	
    	// get experiment type and details
    	String experimentStr = tokens[5];
    	String experimentTypeRef = getExperimentType(experimentStr);
    	String experimentDetails = getExperimentDetails(experimentStr);

    	
    	// mutant type
    	String mutantRef = getAPOTerm(tokens[6],"MutantType");
    	
    	
    	String strainName = tokens[8];
    	String strainRef = null;
    	if(!"not recorded".equals(strainName.toLowerCase()))
    	{
    		strainRef = getStrain(strainName);
    	}
    	
    	String condition = tokens[11];
    	String observableStr = tokens[9];
    	String chemicalDetails = null;
    	
    	String chemicalRef =  null;
    	
    	if(!StringUtils.isEmpty(tokens[10]))
    	{
    		String[] chParts = tokens[10].split(Pattern.quote(" ("));
    		chemicalRef = getChemical(chParts[0]);
    		if(chParts.length > 1 )
    		{
    			chemicalDetails = tokens[10];
    		}
    	}
    	
    	
    	String phenotypeRef = getPhenotype(observableStr, chemicalRef);
    	
    	// Create and set the phenotype annotation
    	
    	Item phenotypeAnnotation = createItem("PhenotypeAnnotation");

    	// required fields
    	phenotypeAnnotation.setReference("phenotype", phenotypeRef); 
    	phenotypeAnnotation.setReference("experimentType", experimentTypeRef); 
    	phenotypeAnnotation.setReference("feature", featureItemRef); 
    	phenotypeAnnotation.setReference("mutantType", mutantRef); 
    	phenotypeAnnotation.setReference("organism", refOragism); 

    	
    	if(! StringUtils.isEmpty(experimentDetails))
    	{
    		phenotypeAnnotation.setAttribute("experimentDetails", experimentDetails);
    	}
    	if(! StringUtils.isEmpty(chemicalDetails))
    	{
    		phenotypeAnnotation.setAttribute("chemicalDetails", chemicalDetails);
    	}
    	if(! StringUtils.isEmpty(strainRef))
    	{
    		phenotypeAnnotation.setReference("strain", strainRef);
    	}
    	
    	if(! StringUtils.isEmpty(condition))
    	{
    		phenotypeAnnotation.setAttribute("condition", condition);
    	}
    	
    	
    	
    	// get publication
    	String publicationStr = tokens[4];
    	addPublication(phenotypeAnnotation, publicationStr);
    	
    	store(phenotypeAnnotation);
    }
    
    
    private String getPhenotype(String observableStr,String chemicalRef) throws ObjectStoreException {
    	
    	String[] phenotypeTerms = observableStr.split(":");
    	
    	String observableRef = getAPOTerm(phenotypeTerms[0],"Observable");
    	String qualifierRef =  null;
    	
    	if(phenotypeTerms.length > 1)
    	{
    		qualifierRef = getAPOTerm(phenotypeTerms[1],"Qualifier");
    	}
    	
    	
    	MultiKey phenotypeKey = new MultiKey(observableRef,qualifierRef,chemicalRef);
    	
    	String phenotypeRef = phenotypes.get(phenotypeKey);
    	if(phenotypeRef == null)
    	{
    		Item item = createItem("Phenotype");
    		// observable is required
            item.setReference("observable", observableRef);
            if (! StringUtils.isEmpty(qualifierRef))
            	item.setReference("qualifier", qualifierRef);
            if (! StringUtils.isEmpty(chemicalRef))
            	item.setReference("chemical", chemicalRef);
            phenotypeRef = item.getIdentifier();
            phenotypes.put(phenotypeKey, phenotypeRef);
            store(item);
    	}
    	
    	return phenotypeRef;
    	
    }
    
    private String getAPOTerm(String termName,String termType ) throws ObjectStoreException {
    	
    	
    	termName = termName.trim();
    	
    	String tremRef = apoTerms.get(termName);
    	if(tremRef == null)
    	{
    		if (!StringUtils.isEmpty(termName)) {
            	Item item = createItem(termType);
                item.setAttribute("name", termName);
                tremRef = item.getIdentifier();
                apoTerms.put(termName, tremRef);
                store(item);
            }
    	}
    	return tremRef;
    }
    
    private String getChemical(String chemical ) throws ObjectStoreException {
    	
    	String chemicalRef = chemicals.get(chemical);
    	if(chemicalRef == null)
    	{
    		if (!StringUtils.isEmpty(chemical)) {
            	Item item = createItem("Chemical");
                item.setAttribute("name", chemical);
                chemicalRef = item.getIdentifier();
                chemicals.put(chemical, chemicalRef);
                store(item);
            }
    	}
    	return chemicalRef;
    }
    
    private String getStrain(String strain ) throws ObjectStoreException {
    	String strainRef = strians.get(strain);
    	if(strainRef == null)
    	{
    		if (!StringUtils.isEmpty(strain)) {
            	Item item = createItem("Strain");
                item.setAttribute("name", strain);
                item.setReference("organism", refOragism);
                strainRef = item.getIdentifier();
                strians.put(strain, strainRef);
                store(item);
            }
    	}
    	return strainRef;
    }
    
    
    private String getExperimentType(String experimentStr ) throws ObjectStoreException {
    	
    	String[] expTokens = experimentStr.split(Pattern.quote(" ("));
    	
    	String expType = expTokens[0];
    	String expTypeRef = experimentTypes.get(expType);
    	if(expTypeRef == null)
    	{
    		if (!StringUtils.isEmpty(expType)) {
            	Item item = createItem("ExperimentType");
                item.setAttribute("name", expType);
                expTypeRef = item.getIdentifier();
                experimentTypes.put(expType, expTypeRef);
                store(item);
            }
    	}
    	return expTypeRef;
    }
    private String getExperimentDetails(String experimentStr ) throws ObjectStoreException {
    	
    	
    	// simple split to get the detail desc of the experiment
    	String[] expTokens = experimentStr.split(Pattern.quote(" ("));
    	if(expTokens.length > 1 )
    	{
    		return  expTokens[1].replaceAll(Pattern.quote(")"), "");
    	}
    	return null;
    }
    
    /**
     *  get feature 
     * @param featureId
     * @param featureType
     * @return
     * @throws ObjectStoreException
     */
    private String getFeature(String featureId , String featureType) throws ObjectStoreException 
    {
    	// replace ORF with gene
    	if(featureType.equals("ORF"))
    	{
    		featureType =  "gene";
    	}
    	
		// For now use the id privided it is the primary id used in the database
		String resolvedIdentifier = featureId;
		// if rslv exit and has taxond of this organims try to resolve the primary id.
		// If not then use the provided id in the file as primary
		if (rslv!= null &&  taxonId != null && rslv.hasTaxonAndClassName(taxonId, featureType) ) {
			int resCount = rslv.countResolutions(taxonId, featureType, featureId);
			if (resCount == 1) {
				resolvedIdentifier = rslv.resolveId(taxonId, featureType, featureId).iterator().next();
			}
		}
		
		
		String className = TypeUtil.javaiseClassName(featureType);
		
        String refId = identifiersToFeatures.get(resolvedIdentifier);
		
        if (refId == null  ) {
            
            if (!StringUtils.isEmpty(resolvedIdentifier)) {
            	Item item = createItem(className);
                item.setAttribute("primaryIdentifier", resolvedIdentifier);
                // item.setReference("organism", refOragism);
                refId = item.getIdentifier();
                identifiersToFeatures.put(resolvedIdentifier, refId);
                store(item);
            }
           
            
        }
        return refId;
     }
    
    /**
     * Add Publication to item
     * @param refItem
     * @param publicationStr
     * @throws ObjectStoreException
     */
    private void addPublication(Item refItem,String publicationStr) throws ObjectStoreException
    {
    	
    	String[] pmidIds = null;
    	if(StringUtils.contains(publicationStr, "|"))
    	{
    		pmidIds = publicationStr.split("|");
    	}
    	else
    	{
    		pmidIds = new String[] {publicationStr};
    	}
    	
    	for(String pmidId : pmidIds )
    	{
    		
    		// if not PMID id ignore
    		if(!pmidId.contains("PMID"))
    		{
    			continue;
    		}
    		
    		String pmidRefId = null;
    		if(pubmeds.containsKey(pmidId))
    		{
    			pmidRefId = pubmeds.get(pmidId);
    		}
    		else
    		{
    			Item pubItem = createItem("Publication");
    			pubItem.setAttribute("pubMedId", pmidId);
    			store(pubItem);
    			pmidRefId = pubItem.getIdentifier();
    			pubmeds.put(pmidId, pmidRefId);
    		}
    		
    		refItem.addToCollection("publications", pmidRefId);
    	}
    	    	
    }
    
}
