package org.intermine.bio.dataconversion;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.util.PropertiesUtil;

public class CgdIdentifiersResolverFactory extends IdResolverFactory {
	 protected static final Logger LOG = Logger.getLogger(CgdIdentifiersResolverFactory.class);
	
    private final String propKey = "resolver.file.rootpath";
    private final String resolverFileSymbo = "cgd";

	// Just construct the resolver for the Gene class.
	// this is what currently in the ids file.
	public CgdIdentifiersResolverFactory()
	{
        this.clsCol = this.defaultClsCol;

	}
	
	@Override
	protected void createIdResolver() {
		if (resolver == null) {
            resolver = new IdResolver(clsCol.iterator().next());
		}
		
		// read the id from the file the format is exactly what is written and read by the cache file
		try {
            boolean isCachedIdResolverRestored = restoreFromFile();
            if (!isCachedIdResolverRestored ) {
                String resolverFileRoot =
                        PropertiesUtil.getProperties().getProperty(propKey);
                if (StringUtils.isBlank(resolverFileRoot)) {
                    String message = "Resolver data file root path is not specified";
                    LOG.warn(message);
                    return;
                }
                
                String resolverFileName = resolverFileRoot.trim() + "/" + resolverFileSymbo;
                LOG.info("Creating id resolver from data file and caching it.");
                File f = new File(resolverFileName);
                if (f.exists()) {
                	restoreFromFile(f);
                    resolver.writeToFile(new File(idResolverCachedFileName));
                } else {
                    LOG.warn("Resolver file does not exist: " + resolverFileName);
                }
            }else {
                LOG.info("Using previously cached id resolver file: " + idResolverCachedFileName);
            }
		} catch (Exception e) {
	        throw new RuntimeException(e);
	    }
	}

}
