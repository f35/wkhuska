/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.marmotta.ucuenca.wk.commons.impl;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.marmotta.ucuenca.wk.commons.service.QueriesService;

/**
 *
 * @author Satellite
 */
public class Queries implements QueriesService {

    @Override
    public String getAuthorsQuery(String datagraph) {
        return "SELECT DISTINCT ?s WHERE { GRAPH <" + datagraph + "> { ?s rdf:type foaf:Person }}";
    }

    @Override
    public String getRetrieveResourceQuery() {
        return "SELECT ?x ?y ?z WHERE { ?x ?y ?z }";
    }

    /**
     * Return a INSERT QUERY when object is a LITERAL
     *
     * @param varargs
     * @return
     */
    @Override
    public String getInsertDataLiteralQuery(String... varargs) {
        String graphSentence = "GRAPH <" + varargs[0] + ">";
        String subjectSentence = "<" + varargs[1] + ">";
        String object = null;
        if (varargs[3].contains("^^")) {
            object = "\"" + StringEscapeUtils.escapeJava(varargs[3].substring(1, varargs[3].indexOf("^^") - 1)) + "\"" + varargs[3].substring(varargs[3].indexOf("^^"));
        } else {
            object = "\"" + StringEscapeUtils.escapeJava(varargs[3].substring(1, varargs[3].length() - 1)) + "\"";
        }

        return "INSERT DATA { " + graphSentence + "  { " + subjectSentence + " <" + varargs[2] + "> " + object + " }}";

    }

    /**
     * Return a INSERT QUERY when object is a URI
     *
     * @param varargs
     * @return
     */
    @Override
    public String getInsertDataUriQuery(String... varargs) {
        String graphSentence = "GRAPH <" + varargs[0] + ">";
        String subjectSentence = "<" + varargs[1] + ">";
        return "INSERT DATA { " + graphSentence + " { " + subjectSentence + " <" + varargs[2] + "> <" + varargs[3] + "> }}";

    }

    /**
     * Return true or false if object is a URI
     *
     * @param object
     * @return
     */
    @Override
    public Boolean isURI(String object) {
        URL url = null;
        try {
            url = new URL(object);
        } catch (Exception e1) {
            return false;
        }
        Pattern pat = Pattern.compile("^[hH]ttp(s?)");
        Matcher mat = pat.matcher(url.getProtocol());
        return mat.matches();

        // return "http".equals(url.getProtocol()) || "https".equals(url.getProtocol()) ;
    }

    /**
     * Return ASK query for a resource
     *
     * @param resource
     * @return
     */
    @Override
    public String getAskResourceQuery(String graph, String resource) {
        return "ASK FROM <" + graph + "> {  <" + resource + "> ?p ?o }";
    }

    @Override
    public String getEndpointNameQuery(String endpointsGraph, String name, String resourceHash) {
        return "INSERT DATA { GRAPH <" + endpointsGraph + "> { <http://ucuenca.edu.ec/wkhuska/endpoint/" + resourceHash + ">  <http://ucuenca.edu.ec/wkhuska/resource/name>  \"" + name + "\" }}";
    }

    @Override
    public String getEndpointUrlQuery(String endpointsGraph, String url, String resourceHash) {
        return "INSERT DATA { GRAPH <" + endpointsGraph + "> { <http://ucuenca.edu.ec/wkhuska/endpoint/" + resourceHash + ">  <http://ucuenca.edu.ec/wkhuska/resource/url>  <" + url + "> }}";
    }

    @Override
    public String getEndpointGraphQuery(String endpointsGraph, String graphUri, String resourceHash) {
        return "INSERT DATA { GRAPH <" + endpointsGraph + "> { <http://ucuenca.edu.ec/wkhuska/endpoint/" + resourceHash + ">  <http://ucuenca.edu.ec/wkhuska/resource/graph>  <" + graphUri + "> }}";
    }

    @Override
    public String getlisEndpointsQuery(String endpointsGraph) {
        return "SELECT DISTINCT ?id ?name ?url ?graph  WHERE {  "
                + " GRAPH <" + endpointsGraph + ">"
                + " {"
                + " ?id <http://ucuenca.edu.ec/wkhuska/resource/name> ?name ."
                + " ?id <http://ucuenca.edu.ec/wkhuska/resource/url> ?url."
                + " ?id <http://ucuenca.edu.ec/wkhuska/resource/graph> ?graph."
                + " }"
                + " }";
    }

    @Override
    public String getEndpointByIdQuery(String endpointsGraph, String id) {
        return "SELECT DISTINCT ?id ?name ?url ?graph  WHERE {  "
                + " GRAPH <" + endpointsGraph + ">"
                + " {"
                + " ?id <http://ucuenca.edu.ec/wkhuska/resource/name> ?name ."
                + " ?id <http://ucuenca.edu.ec/wkhuska/resource/url> ?url."
                + " ?id <http://ucuenca.edu.ec/wkhuska/resource/graph> ?graph."
                + " FILTER(?id = <" + id + ">)"
                + " }"
                + " }";
    }

    @Override
    public String getEndpointDeleteQuery(String endpointsGraph, String id) {

        return "DELETE { ?id ?p ?o } "
                + "WHERE"
                + " { "
                + " GRAPH <" + endpointsGraph + ">"
                + " { "
                + " ?id ?p ?o . "
                + " FILTER(?id = <" + id + ">) "
                + " } "
                + " }";
    }

    @Override
    public String getWkhuskaGraph() {
        return "http://ucuenca.edu.ec/wkhuska";
    }

    @Override
    public String getCountPersonQuery(String graph) {
        return " PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT (COUNT(?s) as ?count) WHERE { GRAPH <" + graph + "> { ?s rdf:type foaf:Person. }}";
    }

    @Override
    public String getLimit(String limit) {
        return " Limit " + limit;
    }

    @Override
    public String getOffset(String offset) {
        return " offset " + offset;
    }

    @Override
    public String getProvenanceProperty() {
        return "http://purl.org/dc/terms/provenance";
    }

    @Override
    public String getAuthorsDataQuery(String graph) {
        return " PREFIX foaf: <http://xmlns.com/foaf/0.1/> "
                + " SELECT * "
                + " WHERE { GRAPH <" + graph + "> { "
                + " ?subject a foaf:Person. "
                + " ?subject foaf:name ?name."
                + " ?subject foaf:firstName ?fname."
                + " ?subject foaf:lastName ?lname."
                //                + " {"
                //                + " FILTER (regex(?name,\"Saquicela Galarza\"))"
                //                + " } UNION {"
                //                + " FILTER (regex(?name,\"Espinoza Mejia\"))"
                //                + " }"
                + " }}";
    }

    /**
     * get list of graphs query
     *
     * @return
     */
    @Override
    public String getGraphsQuery() {
        return " SELECT DISTINCT ?grafo WHERE { "
                + " graph ?grafo {?x ?y ?z } "
                + " } ";
    }

    /**
     * Return ASK query for triplet
     *
     * @param subject
     * @param predicate
     * @param object
     * @return
     */
    @Override
    public String getAskQuery(String... varargs) {

        String graphSentence = "GRAPH <" + varargs[0] + ">";

        return "ASK { " + graphSentence + "{ <" + varargs[1] + "> <" + varargs[2] + "> <" + varargs[3] + "> } }";
    }

    @Override
    public String getPublicationsQuery(String providerGraph) {
        return " SELECT DISTINCT ?authorResource ?pubproperty ?publicationResource WHERE { "
                + " graph  <" + providerGraph + "> "
                + " {  "
                + " ?authorResource owl:sameAs   ?authorNative. "
                + " ?authorNative ?pubproperty ?publicationResource. "
                + " { FILTER (regex(?pubproperty,\"authorOf\")) } "
                + " UNION"
                + " { FILTER (regex(?pubproperty,\"pub\")) } "
                + " }}";
    }

    @Override
    public String getPublicationsMAQuery(String providerGraph) {
        return " SELECT DISTINCT ?authorResource ?pubproperty ?publicationResource WHERE { "
                + " graph <" + providerGraph + "> "
                + " {  "
                + " ?authorResource owl:sameAs   ?authorNative. "
                + " ?authorNative ?pubproperty ?publicationResource. "
                + " filter (regex(?pubproperty,\"pub\")) "
                + " }  "
                + " }  ";
    }

    @Override
    public String getPublicationsPropertiesQuery(String providerGraph, String publicationResource) {
        return "PREFIX owl: <http://www.w3.org/2002/07/owl#> "
                + " PREFIX foaf: <http://xmlns.com/foaf/0.1/> "
                + " SELECT DISTINCT ?publicationProperties ?publicationPropertyValue WHERE { "
                + " graph <" + providerGraph + "> "
                + " {"
                + " <" + publicationResource + ">  ?publicationProperties ?publicationPropertyValue. "
                + " }} ";
    }

    @Override
    public String getMembersQuery() {
        return "SELECT DISTINCT ?members"
                + " WHERE { ?x <http://xmlns.com/foaf/0.1/member> ?members. } ";
    }

    @Override
    public String getPublicationFromProviderQuery() {
        return "SELECT DISTINCT ?authorResource ?publicationProperty  ?publicationResource "
                + " WHERE {  ?authorResource <http://www.w3.org/2002/07/owl#sameAs> ?authorOtherResource. "
                + " ?authorOtherResource <http://dblp.uni-trier.de/rdf/schema-2015-01-26#authorOf> ?publicationResource. "
                + " ?authorOtherResource ?publicationProperty ?publicationResource. }";
    }
    
    @Override
    public String getPublicationForExternalAuthorFromProviderQuery(String property) {
        return "SELECT DISTINCT ?authorResource ?publicationProperty  ?publicationResource "
                + " WHERE { ?authorResource <"+property+"> ?publicationResource. "
                + " ?authorOtherResource ?publicationProperty ?publicationResource. }";
    }

    @Override
    public String getPublicationFromMAProviderQuery() {
        return "SELECT DISTINCT ?authorResource  ?publicationResource "
                + " WHERE {  ?authorResource <http://xmlns.com/foaf/0.1/publications> ?publicationResource}";
    }

    @Override
    public String getPublicationPropertiesQuery() {
        return "SELECT DISTINCT ?publicationResource ?publicationProperties ?publicationPropertiesValue "
                + " WHERE { ?authorResource <http://dblp.uni-trier.de/rdf/schema-2015-01-26#authorOf> ?publicationResource. ?publicationResource ?publicationProperties ?publicationPropertiesValue }";

    }

    @Override
    public String getPublicationMAPropertiesQuery() {
        return "SELECT DISTINCT ?publicationResource ?publicationProperties ?publicationPropertiesValue "
                + " WHERE { ?authorResource <http://xmlns.com/foaf/0.1/publications> ?publicationResource. ?publicationResource ?publicationProperties ?publicationPropertiesValue }";

    }

    @Override
    public String getAuthorPublicationsQuery(String providerGraph, String author, String prefix) {
        return " SELECT DISTINCT  ?authorResource  ?pubproperty  ?publicationResource "
                + "?title WHERE { "
                + " graph   <" + providerGraph + "> "
                    + " { <" + author + "> <http://xmlns.com/foaf/0.1/publications> "
                + "?publicationResource.  ?publicationResource "
                + "<"+prefix+"> "
                + "?title } }";
    }

    @Override
    public String getPublicationDetails(String publicationResource) {

        return "SELECT DISTINCT ?property ?hasValue  WHERE {\n"
                + "  { <" + publicationResource + "> ?property ?hasValue }\n"
                + "UNION\n"
                + "  { ?isValueOf ?property <" + publicationResource + "> }\n"
                + "}\n"
                + "ORDER BY ?property ?hasValue ?isValueOf";
    }

    @Override
    public String getPublicationsTitleQuery(String providerGraph, String prefix) {
        return ""
                + " SELECT DISTINCT ?authorResource ?pubproperty ?publicationResource ?title "
                + "WHERE {  graph <" + providerGraph + ">  "
                + "{   ?authorResource owl:sameAs   ?authorNative.  "
                + "?authorNative ?pubproperty ?publicationResource.  "
                + "?publicationResource <" + prefix + ">  ?title\n"
                + "\n" + "{ FILTER (regex(?pubproperty,\"authorOf\")) }  "
                + "UNION { FILTER (regex(?pubproperty,\"pub\")) }                                                                                        }} ";
    }

}
