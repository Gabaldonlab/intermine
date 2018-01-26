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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;

import au.com.bytecode.opencsv.CSVReader;


/**
 * 
 * @author
 */
public class CgrVcfConverter extends BioFileConverter
{
	
	private int MAX_LINES_TEST = 1000;
	
    //
    private static final String DATASET_TITLE = "CRG";
    private static final String DATA_SOURCE_NAME = "CRG";
    private static final String SCORE_TYPE = "phred-scaled quality score";
    // main organims/strain of the data set
    // In this case it candida glabrata
    // each variant will be an isolate of a certain strain of the main organism
    // private Item organism = null ;
    private String organismRefId =  null;
    // taxon id of the main organism
    
    private String taxonId =  null;

    // chromosomes seq    
    private Map<String, Item> seqs = new HashMap<String, Item>();
    
    // 
    private Map<Integer, Item > isolates =  new HashMap<Integer, Item>();
    
    private int currentLine = 0;
    
    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public CgrVcfConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     * Sets the taxon ID of the main organim.
     *
     * @param TaxonId a single taxon Id
     */
    public void setTaxonId(String taxonId) {
        this.taxonId = taxonId;
    }
    
    
    /**
     * 
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {

    //  System.out.println("taxonId = " + taxonId);
        organismRefId = getOrganism(taxonId);
    	//System.out.println("organismRefId = " + organismRefId);

    	
    	
    	
        final CSVReader bufferedReader = new CSVReader(reader, '\t', '"');
    	
        
        while (true) {
            String[] line = bufferedReader.readNext();
            if (line == null) {
                // EOF
                break;
            }
            
            // This is for testing
//            if( currentLine > MAX_LINES_TEST)
//            	break;
            
            
            
            if (line[0].startsWith("##"))
	        {
	        	// Skip it is a comment Line
            	currentLine ++;
	        	continue; 
	        }
            
            // if header line process it to git isolates information
            if (line[0].equals("#CHROM"))
            {
            	// System.out.println("Header Info");
            	// if (true)
            	//	throw new RuntimeException("Header Info : " + currentLine);
            	processHeader(line);
            }
            else
            {
            	// process the record and create snp data
            	processRecord(line);
            }
            currentLine ++;
        }
    	
    }
    
    final int isolates_CLM_Start = 9 ;
    private void processHeader(String[] line) {
    	
    	// start index in the line array for the strains info
    	try
    	{
	    	for (int i = isolates_CLM_Start ; i <  line.length ; i ++ )
	    	{
	    		String isolateName = line[i];
	    		Item isolate = createItem("Isolate");
	    		isolate.setAttribute("name", isolateName);
	    		isolates.put(i, isolate);
	    		store(isolate);
	    	}
    	}
    	catch (ObjectStoreException e)
    	{
            throw new RuntimeException("failed to store Isolates at line : " + currentLine, e);

    	}
    }
    
    
    private Map<Integer,List<String>> processIsolates(String[] line) {
	    	
    	String format = line[8];
    	// GT value will be always the first one no need to inspect format Now
    	Map<Integer,List<String>> snps_isolates = new HashMap<Integer,List<String>>();
    	for (int i = isolates_CLM_Start ; i <  line.length ; i ++ )
	    	{
	    		String isolateValues = line[i];
	    		if(!isolateValues.equals("0")) // if not empty just add it to the SNP
	    		{
	    			String[] formatValues = isolateValues.split(":");
	    			int gtIndex = new Integer(formatValues[0]);
	    			
	    			if (!snps_isolates.containsKey(gtIndex))
	    			{
	    				snps_isolates.put(gtIndex, new ArrayList<String>());
	    			}
	    			
	    			String isolateRefId = isolates.get(i).getIdentifier();
	    			snps_isolates.get(gtIndex).add(isolateRefId);
	    		}
	    	}
    	return snps_isolates;
    	
    }
    
    
    private void processRecord(String[] line) {
    	
    	try {
    		
    		
    		// File Format 
    		// #CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT
    		
    		
    		
    		
    		
    		// #CHROM
	        String chromosomeIdentifier = line[0];
	        
	        // POS
	        String start = line[1];
	        // ID -- 
	        String identifier = line[2];
	        // REF
	        String referenceSeq = line[3];
	        // ALT
	        String variantSeq = line[4];
	        // QUAL
	        String qualityScore = line[5];
	        // FILTER
	        String filter =  line[6];
	        
	        // INFO
	        String info = line[7];
	
	        // Format
	        String format = line[8];
	        
	        // If bad quality do not add this line
	        if(  filter.contains("Bad_DepthofQuality"))
	        {
	        	// skip the feature
	        	return;
	        }
	        
	        
	        Map<Integer,List<String>> snps_isolates = processIsolates(line);
	        // which isolate have the mutation 
	        
	        
	        // FIXME :: replace seqid with the correct one from the gff file.
	    	
	    	String[] nameParts = chromosomeIdentifier.split("chr");
	    	// TEMP Fix for the name :: just for Testing
	    	if(chromosomeIdentifier.contains("mitoc"))
	    	{
	    		chromosomeIdentifier = "mito_C_glabrata_CBS138";
	    	}
	    	else
	    	{
	    		chromosomeIdentifier = "Chr"+nameParts[1] + "_C_glabrata_CBS138";
	    	}
	        
	        
	        // create SNV by default?
	        
	        
	        
	        
	        
	        
	        
	        // create chromosme seq
//	        Item chromosome = getChromosome(chromosomeIdentifier);
	        String[] variants = null ;
	        if(StringUtils.contains(variantSeq, ","))
	        {
	        	variants = variantSeq.split(",");
	        }
	        else
	        {
	        	variants = new String[] {variantSeq};
	        }
	        // Which alt index
	        int gtIndex = 1;
	        for(      String variant : variants)
	        {
	        	String type = "SequenceAlteration";

		        String idPrefix = "SNP_";
		        identifier = idPrefix +  chromosomeIdentifier   + "_" +start + "_"+    referenceSeq + "_" + variant;
	        	Item snp =  createRecord(identifier,start,referenceSeq,variant  , chromosomeIdentifier);
	        	snp.setCollection("isolates", snps_isolates.get(gtIndex));
	        	gtIndex++;
	        	store(snp);
	        }
	        
	        
	        
    	}
    	catch(ObjectStoreException e)
    	{
            throw new RuntimeException("failed to store SequenceAlteration at line : " + currentLine, e);

    	}
    	
    }
    
    
    
    
    
    private Item createRecord(String identifier, String start,String referenceSeq,String variantSeq , String chromosomeIdentifier)
    		throws ObjectStoreException {
    	String type = "SequenceAlteration";
    	
    	// TODO :: Just for not create base class
    	// Types have to be check to create it correct
    	
//	    if(referenceSeq.length() == variantSeq.length()  &&  referenceSeq.length() == 1)
//	    	type= "SNV";
//	    if(referenceSeq.length() != variantSeq.length()  &&  referenceSeq.length() > 1 &&  variantSeq.length() > 1)
//	    	type= "Substitution";
//	    if(referenceSeq.length() > variantSeq.length())
//	    	type= "Deletion";
//	    if(referenceSeq.length() < variantSeq.length())
//	    	type= "Insertion";
	    
	    	
    	
    	
	        Item chromosome = getChromosome(chromosomeIdentifier);
    		Item snp =  createItem(type);
	    	snp.setAttribute("primaryIdentifier", identifier);
	        snp.setAttribute("referenceSequence", referenceSeq);
	        snp.setAttribute("variantSequence", variantSeq);
	        snp.setAttribute("type", type);
	     
	      
	        
	        int length = referenceSeq.length();
	        int startPos = new Integer(start);
	        
	        int endPos = startPos + length - 1;
	        Item location = getLocation(chromosome,snp,startPos,endPos);
	        
	        snp.setReference("chromosome", chromosome);
	        snp.setReference("chromosomeLocation",location);
			snp.setReference("organism", organismRefId);
	        return snp;
    }
    
    
    
    
    private Item getChromosome(String chromosomeIdentifier)
    		throws ObjectStoreException {
    	
    	
    	String identifier = chromosomeIdentifier;
    	
    	
    	
    	Item seq = seqs.get(identifier);
    	if (seq == null) {
    		seq = createItem("Chromosome");
    		seq.setAttribute("primaryIdentifier", identifier);
    		seq.setReference("organism", organismRefId);
    		store(seq);
    		seqs.put(identifier, seq);
    	}
    	return seq;
    }
    
    private Item getLocation(Item seq ,Item feature, int start , int end) 
    		throws ObjectStoreException {
    	Item location = createItem("Location");
    	location.setAttribute("start", String.valueOf(start));
        location.setAttribute("end", String.valueOf(end));
        location.setAttribute("strand", "0");
        location.setReference("locatedOn", seq.getIdentifier());
        location.setReference("feature", feature.getIdentifier());

        // TODO :: add ref to the dataset
        //location.addToCollection("dataSets", dataSet);
        
        store(location);
        	

    	return location;
    	
    }
}
