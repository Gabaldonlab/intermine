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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
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
public class CgdOrthologsConverter extends BioFileConverter
{
	
    protected static final Logger LOG = Logger.getLogger(CgdOrthologsConverter.class);
    
    // keep list here 
    private final Map<String, String> dataSets = new HashMap<String, String>();

	
    private String dataSet = null;    
    //
    private  String dataSetTitle = null;
    private  String dataSourceName = null;
    private  String dataSetDesc = null;

    private static final String DEFAULT_IDENTIFIER_TYPE = "secondaryIdentifier";


    private static final String ORTHOLOGUE = "orthologue";
    private static final String PARALOGUE = "paralogue";

    private static final String EVIDENCE_CODE_ABBR = "AA";
    private static final String EVIDENCE_CODE_NAME = "Amino acid sequence comparison";

    private static String evidenceRefId = null;
    
    private Map<String, String> identifiersToGenes = new HashMap<String, String>();
    
    
    protected IdResolver rslv;

    
    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public CgdOrthologsConverter(ItemWriter writer, Model model) {
        super(writer, model);
        
    }

    
    public void setDataSet(String dataSetTitle)
    {
    	this.dataSetTitle = dataSetTitle;
    }
    public void setDataSetDesc(String dataSetDesc)
    {
    	this.dataSetDesc = dataSetDesc;
    }
    public void setDataSource(String dataSource)
    {
    	this.dataSourceName = dataSource;
    }
    /**
     * Return a DataSet ref with the given details.
     *
     * @param title the DataSet title
     * @param dataSourceRefId the DataSource referenced by the the DataSet
     * @return the DataSet Item
     */
    @Override
    public String getDataSet(String title, String dataSourceRefId) {
        return getDataSet(title,dataSourceRefId,null);
    }
    
    public String getDataSet(String title, String dataSourceRefId , String desc) {
        String refId = dataSets.get(title);
        if (refId == null) {
            Item dataSet = createItem("DataSet");
            dataSet.setAttribute("name", title);
            dataSet.setReference("dataSource", dataSourceRefId);
            if (!StringUtils.isEmpty(desc))
            	dataSet.setAttribute("description", desc);
            try {
                store(dataSet);
            } catch (ObjectStoreException e) {
                throw new RuntimeException("failed to store DataSet with title: " + title, e);
            }
            refId = dataSet.getIdentifier();
            dataSets.put(title, refId);
        }
        return refId;
    }
    
    private void setupDataset()
    {
    	String dataSource = null;
    	if (StringUtils.isNotEmpty(dataSourceName) && StringUtils.isNotEmpty(dataSetTitle)) {
            dataSource = getDataSource(dataSourceName);
            dataSet = getDataSet(dataSetTitle, dataSource, dataSetDesc);
            setStoreHook(new BioStoreHook(getModel(), dataSet, dataSource, getSequenceOntologyRefId()));
        }
        
    }
    
    /**
     * 
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
    	
//    	// create data set
    	if(dataSet == null)
    	{
    		setupDataset();
    	}    	
    	if (rslv == null) {
    		rslv = IdResolverService.getCgdIdResolver();
    	}
    	
    	
    	
    	
    	
    	
    	
    	
    	Iterator<?> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
    	
    	// file format
    	// each clm contains genes from one speices and all othologs genes are in one row
    	
    	File currentFile = getCurrentFile();
    	LOG.info("Loading data from File " + currentFile.getName());
    	// String fileName = FilenameUtils.getBaseName(currentFile.getName());
    	String fileName = currentFile.getName()	;

    	System.out.println("Loading data from File " + fileName );

    	// FileName should be like this taxonId1.taxonId2
    	String[] taxonIds = fileName.split("\\.");
    	String organism1RefId = getOrganism(taxonIds[0]);
    	String organism2RefId = getOrganism(taxonIds[1]);
    	// 6 clms
        // 0- second id 
        // 1- symbol
        // 2- primary id
        // 0- second id 
        // 1- symbol
        // 2- primary id
        
    	
    	while (lineIter.hasNext()) {
    		
    		String[] lineTokens = (String[]) lineIter.next();
            String gene1_2ndId = lineTokens[0];
            String gene1_symbol = lineTokens[1];
            String gene1_1stId = lineTokens[2];
            String gene2_2ndId = lineTokens[3];
            String gene2_symbol = lineTokens[4];
            String gene2_1stsId = lineTokens[5];
    		
            String gene1RefId = getGene(gene1_1stId, gene1_2ndId,gene1_symbol,taxonIds[0],organism1RefId);
			String gene2RefId = getGene(gene2_1stsId, gene2_2ndId,gene2_symbol,taxonIds[1],organism2RefId);
    		
			if (gene1RefId != null && gene2RefId != null ) {
				createHomologue(gene1RefId,gene2RefId);
				createHomologue(gene2RefId,gene1RefId);
			}

    	}
    }
    

    private void createHomologue(String gene1RefId,  String gene2RefId)
            throws ObjectStoreException {
            Item homologue = createItem("Homologue");
            homologue.setReference("gene", gene1RefId);
            homologue.setReference("homologue", gene2RefId);
            homologue.addToCollection("evidence", getEvidence());
            homologue.setAttribute("type", ORTHOLOGUE);
            store(homologue);
        }
    
    
    
    
    // Not sure if this is correct for cgob dataset
    private String getEvidence() throws ObjectStoreException {
        if (evidenceRefId == null) {
            Item item = createItem("OrthologueEvidenceCode");
            item.setAttribute("abbreviation", EVIDENCE_CODE_ABBR);
            item.setAttribute("name", EVIDENCE_CODE_NAME);
            try {
                store(item);
            } catch (ObjectStoreException e) {
                throw new ObjectStoreException(e);
            }
            String refId = item.getIdentifier();

            item = createItem("OrthologueEvidence");
            item.setReference("evidenceCode", refId);
            try {
                store(item);
            } catch (ObjectStoreException e) {
                throw new ObjectStoreException(e);
            }

            evidenceRefId = item.getIdentifier();
        }
        return evidenceRefId;
    }
    
    
    
    
    private String getGene(String gene1stId, String gene2ndId,String geneSymbol,String taxonId,  String organismRefId)
            throws ObjectStoreException {
    	

    		// For now use default
    		String identifierType1 = "primaryIdentifier";
    		String identifierType2 = "secondaryIdentifier";


    		// For now use the id privided it is the primary id used in the database
    		String resolvedIdentifier = gene1stId;
    		// if rslv exit and has taxond of this organims try to resolve the primary id.
    		// If not then use the provided id in the file as primary
    		if (rslv!= null && rslv.hasTaxon(taxonId)) {
    			int resCount = rslv.countResolutions(taxonId, "gene", gene1stId);
    			if (resCount == 1) {
    				resolvedIdentifier = rslv.resolveId(taxonId, "gene", gene1stId).iterator().next();
    			}
    			else {
    				// can not resolve the id
    				// this mean that this gene might no exit in the current version or no infomation is provided about it so do not add
    				// resolvedIdentifier = null;
    				return null;
    			}
    		}
    		
    		
    		
    		
            String refId = identifiersToGenes.get(resolvedIdentifier);
    		
            if (refId == null) {
                Item item = createItem("Gene");
                if (!StringUtils.isEmpty(resolvedIdentifier)) {
                    item.setAttribute(identifierType1, resolvedIdentifier);
                }
                
                // if blank do not update
                if (!StringUtils.isEmpty(gene2ndId)) {
                    item.setAttribute(identifierType2, gene2ndId);
                }
                
                // If it is the same do not add the symbol ?? Maybe it would be better to add if so 
                // TODO ::Revise this conidtion 
                if (!StringUtils.isEmpty(geneSymbol) && !geneSymbol.equals(gene2ndId)) {
                    item.setAttribute("symbol", geneSymbol);
                }
                item.setReference("organism", organismRefId);
                refId = item.getIdentifier();
                identifiersToGenes.put(resolvedIdentifier, refId);
                store(item);
            }
            return refId;
        }
    
    
    
    
   
}
