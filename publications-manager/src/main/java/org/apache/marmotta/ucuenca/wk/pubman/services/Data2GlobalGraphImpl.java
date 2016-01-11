/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.marmotta.ucuenca.wk.pubman.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.apache.marmotta.commons.vocabulary.FOAF;
import org.apache.marmotta.kiwi.model.rdf.KiWiUriResource;
import org.apache.marmotta.platform.core.exception.InvalidArgumentException;
import org.apache.marmotta.platform.core.exception.MarmottaException;
import org.apache.marmotta.platform.sparql.api.sparql.SparqlService;
import org.apache.marmotta.ucuenca.wk.commons.service.PropertyPubService;
import org.apache.marmotta.ucuenca.wk.commons.service.QueriesService;
import org.apache.marmotta.ucuenca.wk.pubman.api.Data2GlobalGraph;
import org.apache.marmotta.ucuenca.wk.pubman.api.SparqlFunctionsService;
import org.openrdf.model.Value;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.UpdateExecutionException;
import org.semarglproject.vocab.OWL;
import org.semarglproject.vocab.RDF;
import org.simmetrics.StringMetric;
import static org.simmetrics.StringMetricBuilder.with;
import org.simmetrics.metrics.CosineSimilarity;
import org.simmetrics.metrics.Levenshtein;
import org.simmetrics.simplifiers.Simplifiers;
import org.simmetrics.tokenizers.Tokenizers;
import org.slf4j.Logger;

/**
 *
 * @author Satellite
 */
@ApplicationScoped
public class Data2GlobalGraphImpl implements Data2GlobalGraph, Runnable {

    @Inject
    private Logger log;

    @Inject
    private QueriesService queriesService;

    @Inject
    private PropertyPubService pubVocabService;

    @Inject
    private SparqlFunctionsService sparqlFunctionsService;

    private String namespaceGraph = "http://ucuenca.edu.ec/";
    private String wkhuskaGraph = namespaceGraph + "wkhuska";
    private String uriPublication = "http://ucuenca.edu.ec/wkhuska/publication/";
    private String bibloTitle = "http://purl.org/dc/terms/title";
    private String publicationOntology = "http://purl.org/ontology/bibo/Article";
    private String uriNewAuthor = "http://ucuenca.edu.ec/resource/";
    private double total = 0;
    private double totalPublicationRecognized = 0;
    private double totalPublicationNotRecognized = 0;
    private double totalPublications = 0;
    private double problemWithTitle = 0;
    private boolean newInsert = false;
    private String bufferTitle = null;
    private int countPublicationAskIngnored = 0;
    private List<String> results = new ArrayList<String>();
    private String authorsGraph = "http://ucuenca.edu.ec/wkhuska/authors";

    private int processpercent = 0;


    /* graphByProvider
     Graph to save publications data by provider
     Example: http://ucuenca.edu.ec/wkhuska/dblp
     */
    private String graphByProviderNS = wkhuskaGraph + "/provider/";

    @Inject
    private SparqlService sparqlService;

    @Override

    public String LoadData2GlobalGraph() {
        try {

            String providerGraph = "";
            //String getAuthorsQuery = queriesService.getAuthorsQuery();
            String getGraphsListQuery = queriesService.getGraphsQuery();
            List<Map<String, Value>> resultGraph = sparqlService.query(QueryLanguage.SPARQL, getGraphsListQuery);
            /* FOR EACH GRAPH*/

            for (Map<String, Value> map : resultGraph) {
                providerGraph = map.get("grafo").toString();
                KiWiUriResource providerGraphResource = new KiWiUriResource(providerGraph);

                if (providerGraph.contains("provider")) {
                    String prefixTitle = "";

                    Properties propiedades = new Properties();
                    InputStream entrada = null;
                    Map<String, String> mapping = new HashMap<String, String>();
                    try {
                        ClassLoader classLoader = getClass().getClassLoader();
                        //File file = new File(classLoader.getResource("DBLPProvider.properties").getFile());
                        entrada = classLoader.getResourceAsStream(providerGraphResource.getLocalName() + ".properties");
                        // cargamos el archivo de propiedades
                        propiedades.load(entrada);
                        for (String source : propiedades.stringPropertyNames()) {
                            String target = propiedades.getProperty(source);
                            if (target.contains("title")) {
                                prefixTitle = source.replace("..", ":");
                            }
                            mapping.put(source.replace("..", ":"), target.replace("..", ":"));

                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    } finally {
                        if (entrada != null) {
                            try {
                                entrada.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    List<Map<String, Value>> resultPublications = sparqlService.query(QueryLanguage.SPARQL, queriesService.getPublicationsTitleQuery(providerGraph, prefixTitle));
                    results.add(providerGraph + " :size :" + resultPublications.size());
                    for (Map<String, Value> pubresource : resultPublications) {
                        String authorResource = pubresource.get("authorResource").stringValue();
                        String publicationResource = pubresource.get("publicationResource").stringValue();
                        String publicationTitle = cleanPublicationTitle(pubresource.get("title").stringValue());
                        String publicationProperty = pubVocabService.getPubProperty();
                        totalPublications += 1;

                        //verificar existencia de la publicacion y su author sobre el grafo general
                        String newUriAuthorCentral = buildNewUri(authorResource, providerGraph);
                        String askTripletQuery = queriesService.getAskQuery(wkhuskaGraph, newUriAuthorCentral, publicationProperty, uriPublication + publicationTitle);
                        boolean ask = false;
                        try {

                            ask = sparqlService.ask(QueryLanguage.SPARQL, askTripletQuery);
                        } catch (Exception ex) {
                            log.error("Marmotta Exception:  " + askTripletQuery);

                            problemWithTitle += 1;
                            continue;

                        }

                        if (!ask) {

                            List<Map<String, Value>> resultPublicationsAuthor = sparqlService.query(QueryLanguage.SPARQL, queriesService.getAuthorPublicationsQuery(wkhuskaGraph, newUriAuthorCentral, "http://purl.org/dc/terms/title"));
                            List<Map<String, Value>> resultPublicationsAuthorOfProvider = sparqlService.query(QueryLanguage.SPARQL, queriesService.getAuthorPublicationsQueryFromProvider(providerGraph, authorResource, prefixTitle));
                            boolean flagPublicationAlreadyExist = false;
                            String authorResourceBuilding = searchAuthorOfpublication(resultPublicationsAuthorOfProvider, authorResource, newUriAuthorCentral);
                            String authorResourceCentral = authorResourceBuilding == null ? newUriAuthorCentral : authorResourceBuilding;
                            for (Map<String, Value> publicacion : resultPublicationsAuthor) {
                                if (compareTitlePublicationWithSimmetrics(publicationTitle, cleanPublicationTitle(publicacion.get("title").stringValue()))) {
                                    flagPublicationAlreadyExist = true;
                                    bufferTitle = publicacion.get("publicationResource").stringValue();

                                    String insertPublicationPropertyQuery = buildInsertQuery(wkhuskaGraph, bufferTitle, "http://purl.org/dc/terms/contributor", authorResourceCentral);

                                    try {
                                        sparqlService.update(QueryLanguage.SPARQL, insertPublicationPropertyQuery);
                                    } catch (MalformedQueryException ex) {
                                        log.error("Malformed Query:  " + insertPublicationPropertyQuery);
                                    } catch (UpdateExecutionException ex) {
                                        log.error("Update Query:  " + insertPublicationPropertyQuery);
                                    } catch (MarmottaException ex) {
                                        log.error("Marmotta Exception:  " + insertPublicationPropertyQuery);
                                    }

                                }

                            }
                            if (!flagPublicationAlreadyExist || resultPublicationsAuthor.isEmpty()) {
                                insertPublicationToCentralGraph(authorResourceCentral, publicationProperty, uriPublication + publicationTitle);
                                newInsert = true;
                            }

                        } else {
                            countPublicationAskIngnored += 1;

                        }
                        int countNumProperties = 0;
                        List<Map<String, Value>> resultPubProperties = sparqlService.query(QueryLanguage.SPARQL, queriesService.getPublicationsPropertiesQuery(providerGraph, publicationResource));

                        for (Map<String, Value> pubproperty : resultPubProperties) {
                            String nativeProperty = pubproperty.get("publicationProperties").toString();
                            if (mapping.get(nativeProperty) != null) {
                                countNumProperties += 1;
                                if (countNumProperties > 150) {
                                    continue;
                                }

                                String newPublicationProperty = mapping.get(nativeProperty);
                                String publicacionPropertyValue = pubproperty.get("publicationPropertyValue").toString();

                                String insertPublicationPropertyQuery = buildInsertQuery(wkhuskaGraph, newInsert ? (uriPublication + publicationTitle) : bufferTitle == null ? (uriPublication + publicationTitle) : bufferTitle, newPublicationProperty, publicacionPropertyValue);

                                try {
                                    sparqlService.update(QueryLanguage.SPARQL, insertPublicationPropertyQuery);
                                } catch (MalformedQueryException ex) {
                                    log.error("Malformed Query:  " + insertPublicationPropertyQuery);
                                } catch (UpdateExecutionException ex) {
                                    log.error("Update Query:  " + insertPublicationPropertyQuery);
                                } catch (MarmottaException ex) {
                                    log.error("Marmotta Exception:  " + insertPublicationPropertyQuery);
                                }
                            }
                        }
                        //compare properties with the mapping and insert new properties
                        //mapping.get(map)
                        newInsert = false;
                        bufferTitle = null;
                    }
                }
                //in this part, for each graph
            }
            for (String aux : results) {
                log.info(aux);
            }
            log.info("Publication ignored total: " + problemWithTitle);
            log.info("Publication repository total: " + totalPublications);
            log.info("Publication process recognition total: " + total);
            log.info("Publication total Recognized: " + totalPublicationRecognized);
            log.info("Publication total Not Recognized: " + totalPublicationNotRecognized);
            log.info("Publication total ASK ignored: " + countPublicationAskIngnored);

            return "Los datos de las publicaciones se han cargado exitosamente.";
        } catch (InvalidArgumentException ex) {
            return "error:  " + ex;
        } catch (MarmottaException ex) {
            return "error:  " + ex;
        }
    }

    //construyendo sparql query insert 
    public String buildInsertQuery(String grapfhProv, String sujeto, String predicado, String objeto) {
        if (queriesService.isURI(objeto)) {
            return queriesService.getInsertDataUriQuery(grapfhProv, sujeto, predicado, objeto);
        } else {
            return queriesService.getInsertDataLiteralQuery(grapfhProv, sujeto, predicado, objeto);
        }
    }

    @Override
    public void run() {
        LoadData2GlobalGraph();
    }

    public void insertPublicationToCentralGraph(String authorResource, String publicationProperty, String publicationResource) {
        String insertPubQuery = buildInsertQuery(wkhuskaGraph, authorResource, publicationProperty, publicationResource);
        try {
            sparqlService.update(QueryLanguage.SPARQL, insertPubQuery);
        } catch (MalformedQueryException ex) {
            log.error("Malformed Query:  " + insertPubQuery);
        } catch (UpdateExecutionException ex) {
            log.error("Update Query :  " + insertPubQuery);
        } catch (MarmottaException ex) {
            log.error("Marmotta Exception:  " + insertPubQuery);
        }
    }

    private boolean compareTitlePublicationWithSimmetrics(String publicationResourceOne, String publicationResourceTwo) {

        String a = publicationResourceOne;
        String b = publicationResourceTwo;

        StringMetric metric
                = with(new CosineSimilarity<String>())
                .simplify(Simplifiers.toLowerCase())
                .simplify(Simplifiers.removeNonWord()).simplifierCache()
                .tokenize(Tokenizers.qGram(3)).tokenizerCache().build();
        float compare = metric.compare(a, b);

        StringMetric metric2
                = with(new Levenshtein())
                .simplify(Simplifiers.removeDiacritics())
                .simplify(Simplifiers.toLowerCase()).build();

        float compare2 = metric2.compare(a, b);

        total += 1;

        float similarity = (float) ((compare + compare2) / 2.0);
        log.info("Titulos " + publicationResourceOne + "," + publicationResourceTwo + ": similaridad " + similarity * 100 + "%");

        if (similarity > 0.9) {
            totalPublicationRecognized += 1;
        } else {
            totalPublicationNotRecognized += 1;
        }
        return similarity > 0.9; // 0.8131
    }

    private void addPropertiesToPublication(String authorResource, String publicationProperty, String publicationResource) {

    }

    public String cleanPublicationTitle(String title) {
        return title.replaceAll("\"", "").replaceAll(" ", "_").replaceAll("\\{", "").replaceAll("}", "")
                .replaceAll("<", "").replaceAll(">", "").replaceAll("\\\\", "").replaceAll("\\^", "");

    }

    public String searchAuthorOfpublication(List<Map<String, Value>> publications, String authorNativeResource, String newUriAuthorCentral) {
        try {
            List<Map<String, Value>> resultPublicationsTitle = sparqlService.query(QueryLanguage.SPARQL, queriesService.getTitlePublications(wkhuskaGraph));
            for (Map<String, Value> publicacion : resultPublicationsTitle) {
                String authorResource = publicacion.get("authorResource").stringValue();
                String publicationResource = publicacion.get("publicationResource").stringValue();
                String title = publicacion.get("title").stringValue();

                for (Map<String, Value> publicacionParam : publications) {
                    if (compareTitlePublicationWithSimmetrics(publicacionParam.get("title").stringValue(), title)) {
                        String sameAsInsertQuery = buildInsertQuery(wkhuskaGraph, authorNativeResource, OWL.SAME_AS, newUriAuthorCentral);
                        sparqlService.update(QueryLanguage.SPARQL, sameAsInsertQuery);
                        log.info("publication that coinside between authors: 1:" + publicationResource + "2: " + publicacionParam + ", author: " + authorResource);

                        return authorResource;
                    }
                }
            }

        } catch (MarmottaException ex) {
            java.util.logging.Logger.getLogger(Data2GlobalGraphImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidArgumentException ex) {
            java.util.logging.Logger.getLogger(Data2GlobalGraphImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MalformedQueryException ex) {
            java.util.logging.Logger.getLogger(Data2GlobalGraphImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UpdateExecutionException ex) {
            java.util.logging.Logger.getLogger(Data2GlobalGraphImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;

    }

    private String buildNewUri(String authorResource, String providerGraph) {
        try {
            List<Map<String, Value>> resultAuthorName = sparqlService.query(QueryLanguage.SPARQL, queriesService.getFirstNameLastNameAuhor(authorsGraph, authorResource));
            for (Map<String, Value> publicacion : resultAuthorName) {
                String fisrtName = publicacion.get("fname").stringValue();
                String lastName = publicacion.get("lname").stringValue();
                String newuri = uriNewAuthor + fisrtName.replace(" ", "_") + "_" + lastName.replace(" ", "_");
                String askTripletQuery = queriesService.getAskQuery(wkhuskaGraph, newuri, RDF.TYPE, FOAF.NAMESPACE + "Person");
                boolean askNewAuthor = sparqlService.ask(QueryLanguage.SPARQL, askTripletQuery);
                if (!askNewAuthor) {

                    List<Map<String, Value>> resultAuthorProperties = sparqlService.query(QueryLanguage.SPARQL, queriesService.authorDetailsOfProvenance(authorsGraph, authorResource));
                    for (Map<String, Value> property : resultAuthorProperties) {
                        String insertPubQuery = buildInsertQuery(wkhuskaGraph, newuri, property.get("property").stringValue(), queriesService.isURI(property.get("hasValue").stringValue()) ? property.get("hasValue").stringValue() : " " + property.get("hasValue").stringValue() + " ");
                        try {
                            sparqlService.update(QueryLanguage.SPARQL, insertPubQuery);
                        } catch (MalformedQueryException ex) {
                            log.error("Malformed Query:  " + insertPubQuery);
                        } catch (UpdateExecutionException ex) {
                            log.error("Update Query :  " + insertPubQuery);
                        } catch (MarmottaException ex) {
                            log.error("Marmotta Exception:  " + insertPubQuery);
                        }
                    }
                }
                return newuri;

            }

        } catch (MarmottaException ex) {
            java.util.logging.Logger.getLogger(Data2GlobalGraphImpl.class.getName()).log(Level.SEVERE, null, ex);
        }

        return authorResource;
    }
}
