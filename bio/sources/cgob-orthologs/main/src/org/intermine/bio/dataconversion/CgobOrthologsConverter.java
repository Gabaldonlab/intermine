package org.intermine.bio.dataconversion;

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
import java.util.Properties;
import java.util.Set;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.lang.StringUtils;
import org.intermine.bio.dataconversion.HomologeneConverter.GeneRecord;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;


/**
 * 
 * @author
 */
public class CgobOrthologsConverter extends BioFileConverter
{
    //
    private static final String DATASET_TITLE = "CGOB Homology";
    private static final String DATA_SOURCE_NAME = "CGOB";
    private static final String DEFAULT_IDENTIFIER_TYPE = "secondaryIdentifier";

    
    
    
    
    
    
    private static final String ORTHOLOGUE = "orthologue";
    private static final String PARALOGUE = "paralogue";

    private static final String EVIDENCE_CODE_ABBR = "AA";
    private static final String EVIDENCE_CODE_NAME = "Amino acid sequence comparison";

    private static String evidenceRefId = null;
    
    
    private Map<MultiKey, String> identifiersToGenes = new HashMap<MultiKey, String>();

    
    
    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public CgobOrthologsConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     * 
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
    	Iterator<?> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
    	
    	// file format
    	// each clm contains genes from one speices and all othologs genes are in one row
    	
    	
    	// head contains taxonIds of the species
    	
    	// current set of genes
        Set<GeneRecord> genes = null;

    	String[] lineTokens = (String[]) lineIter.next();
    	
    	int numberOfSpecies = lineTokens.length;
    	String[] taxonIds = new String[numberOfSpecies] ;
    	for(int i = 0 ; i < numberOfSpecies;i++ )
    	{
    		taxonIds[i] = lineTokens[i];
    	}
    	
    	while (lineIter.hasNext()) {
    		
    		lineTokens = (String[]) lineIter.next();
            genes = new HashSet<GeneRecord>();
    		for(int i = 0 ; i < numberOfSpecies;i++ )
    		{
    			String geneId = lineTokens[i];
    			String taxonId = taxonIds[i];
    			String gene = getGene(geneId, taxonId);
                genes.add(new GeneRecord(gene, taxonId));
    		}
            processHomologues(genes);

    	}
    }
    
    private void processHomologues(Set<GeneRecord> genes)
            throws ObjectStoreException {
            Set<GeneRecord> notProcessed = new HashSet<GeneRecord>(genes);
            for (GeneRecord gene : genes) {
                notProcessed.remove(gene);
                for (GeneRecord homologue : notProcessed) {
                    createHomologue(gene.geneRefId, gene.taxonId, homologue.geneRefId,
                            homologue.taxonId);
                    createHomologue(homologue.geneRefId, homologue.taxonId, gene.geneRefId,
                            gene.taxonId);
                }
            }
    }
   
    private void createHomologue(String gene1, String taxonId1, String gene2, String taxonId2)
            throws ObjectStoreException {
            Item homologue = createItem("Homologue");
            homologue.setReference("gene", gene1);
            homologue.setReference("homologue", gene2);
            homologue.addToCollection("evidence", getEvidence());
            homologue.setAttribute("type", taxonId1.equals(taxonId2) ? PARALOGUE : ORTHOLOGUE);
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
    
    
    
    
    private String getGene(String geneId, String taxonId)
            throws ObjectStoreException {
    	
    	
    		// TODO :: read and process configuration file
//            String identifierType = config.get(taxonId);
//            if (identifierType == null) {
//                identifierType = DEFAULT_IDENTIFIER_TYPE;
//            }
    		// For now use default
    		String identifierType = DEFAULT_IDENTIFIER_TYPE;
    	
    	
    	
            // No resolver yet 
            // Commnet out this section
//            String resolvedIdentifier = resolveGene(taxonId, ncbiId, symbol);
//            if (resolvedIdentifier == null) {
//                return null;
//            }
    		
    		// For now use the id privided it is the secondary id used in the database
    		
    		String resolvedIdentifier = geneId;
    		
            String refId = identifiersToGenes.get(new MultiKey(taxonId, resolvedIdentifier));
    		
            if (refId == null) {
                Item item = createItem("Gene");
                if (!StringUtils.isEmpty(identifierType)) {
                    item.setAttribute(identifierType, resolvedIdentifier);
                }
                item.setReference("organism", getOrganism(taxonId));
                refId = item.getIdentifier();
                identifiersToGenes.put(new MultiKey(taxonId, resolvedIdentifier), refId);
                store(item);
            }
            return refId;
        }
    
    
    
    
    /**
     * represents a gene record in the data files
     * @author Julie
     */
    protected class GeneRecord
    {
        protected String geneRefId;
        protected String taxonId;
        /**
         * @param geneRefId the reference number of the gene
         * @param taxonId taxon ID
         */
        public GeneRecord(String geneRefId, String taxonId) {
            this.geneRefId = geneRefId;
            this.taxonId = taxonId;
        }
    }
}
