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
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;


/**
 * 
 * @author
 */
public class CgdIdentifiersConverter extends BioFileConverter
{
    //
    private static final String DATASET_TITLE = "Candida Genome Database";
    private static final String DATA_SOURCE_NAME = "CGD";
    private String organismTaxonId = null;
    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public CgdIdentifiersConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }


   public void setTaxonId(String taxonId) {
   	this.organismTaxonId = taxonId;	

   }



    /**
     * 
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        Iterator<?> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);

//      Columns within chromosomal_features.tab:
//
//        	1.  Feature name (mandatory); this is the primary systematic name, if available
//        	2.  Gene name (locus name)
//        	3.  Aliases (multiples separated by |)
//        	4.  Feature type
//        	5.  Chromosome
//        	6.  Start Coordinate
//        	7.  Stop Coordinate
//        	8.  Strand 
//        	9.  Primary CGDID
//        	10. Secondary CGDID (if any)
//        	11. Description
//        	12. Date Created
//        	13. Sequence Coordinate Version Date (if any)
//        	14. Blank
//        	15. Blank
//        	16. Date of gene name reservation (if any).
//        	17. Has the reserved gene name become the standard name? (Y/N)
//        	18. Name of S. cerevisiae ortholog(s) (multiples separated by |)

        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            // skip comment line 
            if (line[0].startsWith("!"))
            	continue;
            
            String systematicName = line[0]; // id from the gff file
            String featureType = line[3];
            String primaryidentifier = line[8]; // CGDID
            String secondaryidentifier = line[9]; // CGDID
            String chromosomeName = line[4];
            String name = line[1];
            String shortDesc = line[10] ;
            
            // only process records of type genes - in this file is ORF
            if(featureType.contains("ORF")) {
            
	            Item gene = createItem("Gene");
	            if ( !StringUtils.isEmpty(chromosomeName) ) 
	            {
	                if (!StringUtils.isEmpty(primaryidentifier)) {
	                    gene.setAttribute("primaryIdentifier", primaryidentifier);
	
	                    // if (!StringUtils.isEmpty(systematicName)) {
	                    //    gene.setAttribute("symbol", systematicName);
	                    //}
	                    if (!StringUtils.isEmpty(systematicName)) {
	                        // gene.setAttribute("secondaryIdentifier", secondaryidentifier);
	                    	gene.setAttribute("secondaryIdentifier",systematicName);
	                    }
	                    if (!StringUtils.isEmpty(name)) {
	                        gene.setAttribute("symbol", name);
	                    }
	                    //if (!StringUtils.isEmpty(shortDesc)) {
	                    //    gene.setAttribute("briefDescription", shortDesc); // brief Description 
	                    //}
	                }
	                gene.setReference("organism", getOrganism(organismTaxonId));
	                store(gene);
	            }
            }
        }
    }
}
