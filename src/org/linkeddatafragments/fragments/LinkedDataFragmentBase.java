package org.linkeddatafragments.fragments;

import java.net.URISyntaxException;

import org.apache.http.client.utils.URIBuilder;
import org.linkeddatafragments.util.CommonResources;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;

/**
 * Base class of any implementation of {@link ILinkedDataFragment} that uses
 * paging.
 *
 * @author <a href="http://olafhartig.de">Olaf Hartig</a>
 */
public abstract class LinkedDataFragmentBase implements ILinkedDataFragment
{
    public final String fragmentURL;
    public final String datasetURL;
    public final long pageNumber;
    public final boolean isLastPage;

    protected LinkedDataFragmentBase( final String fragmentURL,
                                      final String datasetURL,
                                      final long pageNumber,
                                      final boolean isLastPage )
    {
        this.fragmentURL = fragmentURL;
        this.datasetURL = datasetURL;
        this.pageNumber = pageNumber;
        this.isLastPage = isLastPage;
    }

    @Override
    public boolean isPageOnly() {
        return true;
    }

    @Override
    public long getPageNumber() {
        return pageNumber;
    }

    @Override
    public boolean isLastPage() {
        return isLastPage;
    }

    @Override
    public long getMaxPageSize() {
        return LinkedDataFragmentRequest.TRIPLESPERPAGE;
    }

    /**
     * This implementation uses {@link #addMetadata(Model)}, which should be
     * overridden in subclasses (instead of overriding this method). 
     */
    @Override
    public StmtIterator getMetadata()
    {
        final Model output = ModelFactory.createDefaultModel();
        addMetadata( output );
        return output.listStatements();
    }

    /**
     * This implementation uses {@link #addControls(Model)}, which should be
     * overridden in subclasses (instead of overriding this method). 
     */
    @Override
    public StmtIterator getControls()
    {
        final Model output = ModelFactory.createDefaultModel();
        addControls( output );
        return output.listStatements();
    }

    /**
     * Adds some basic metadata to the given RDF model.
     * This method may be overridden in subclasses.
     */
    public void addMetadata( final Model model )
    {
        final Resource datasetId = model.createResource( getDatasetURI() );
        final Resource fragmentId = model.createResource( fragmentURL );

        datasetId.addProperty( CommonResources.RDF_TYPE, CommonResources.VOID_DATASET );
        datasetId.addProperty( CommonResources.RDF_TYPE, CommonResources.HYDRA_COLLECTION );
        datasetId.addProperty( CommonResources.VOID_SUBSET, fragmentId );

        fragmentId.addProperty( CommonResources.RDF_TYPE, CommonResources.HYDRA_COLLECTION );
        fragmentId.addProperty( CommonResources.RDF_TYPE, CommonResources.HYDRA_PAGEDCOLLECTION );
    }

    /**
     * Adds an RDF description of page links to the given RDF model.
     * This method may be overridden in subclasses.
     */
    public void addControls( final Model model )
    {
        final URIBuilder pagedURL;
        try {
            pagedURL = new URIBuilder( fragmentURL );
        }
        catch ( URISyntaxException e ) {
            throw new IllegalArgumentException( e );
        }

        final Resource fragmentId = model.createResource( fragmentURL );

        final Resource firstPageId =
                model.createResource(
                        pagedURL.setParameter(LinkedDataFragmentRequest.PARAMETERNAME_PAGE,
                                              "1").toString() );

        fragmentId.addProperty( CommonResources.HYDRA_FIRSTPAGE, firstPageId );

        if ( pageNumber > 1) {
            final String prevPageNumber = Long.toString( pageNumber - 1 );
            final Resource prevPageId =
                    model.createResource(
                            pagedURL.setParameter(LinkedDataFragmentRequest.PARAMETERNAME_PAGE,
                                                  prevPageNumber).toString() );

            fragmentId.addProperty( CommonResources.HYDRA_PREVIOUSPAGE, prevPageId );
        }

        if ( ! isLastPage ) {
            final String nextPageNumber = Long.toString( pageNumber + 1 );
            final Resource nextPageId =
                    model.createResource(
                            pagedURL.setParameter(LinkedDataFragmentRequest.PARAMETERNAME_PAGE,
                                                  nextPageNumber).toString() );

            fragmentId.addProperty( CommonResources.HYDRA_NEXTPAGE, nextPageId );
        }
    }

    public String getDatasetURI() {
        return datasetURL + "#dataset";
    }

}