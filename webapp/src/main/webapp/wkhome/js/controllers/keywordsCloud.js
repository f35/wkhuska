
wkhomeControllers.controller('keywordsCloud', ['$routeParams', '$scope', 'globalData', 'sparqlQuery', 'searchData', '$window',
    function ($routeParams, $scope, globalData, sparqlQuery, searchData, $window) {
       $('html,body').animate({
            scrollTop: $("#scrollToTop").offset().top
        }, "slow");

        clickonRelatedauthor = function (id_author)
        {
            var getAuthorDataQuery = globalData.PREFIX
                    + ' CONSTRUCT {   <' + id_author + '> foaf:name ?name; a foaf:Person  '
                    + ' }   '
                    + ' WHERE '
                    + ' {'
                    + 'Graph <' + globalData.centralGraph + '>'
                    + '{'
                    + '     <' + id_author + '> a foaf:Person.'
                    + '     <' + id_author + '> foaf:name ?name'
                    + ' } '
                    + '}';
          
            sparqlQuery.querySrv({query: getAuthorDataQuery}, function (rdf) {
                jsonld.compact(rdf, globalData.CONTEXT, function (err, compacted) {
                    $scope.$apply(function () {
                        searchData.authorSearch = compacted;
                        //alert(author);
                        $window.location.hash = "/" + $routeParams.lang + "/w/search?" + id_author;
                    });
                });
            });
        }; //end clickonRelatedauthor


        $scope.todos = [];
        $scope.ctrlFn = function (value)
        {
            $scope.todos = [];
            var model = {};
            _.map(value, function (pub) {
                //var keys = Object.keys(author);

                model["id"] = pub["@id"];
                model["title"] = pub["dct:title"];
                model["abstract"] = pub["bibo:abstract"];
                model["uri"] = pub["bibo:uri"]["@id"];
                if (model["title"] && model["abstract"])
                {
                    $scope.todos.push({id: model["id"], title: model["title"], abstract: model["abstract"], uri: model["uri"]});
                }
            });
            $('html,body').animate({
                scrollTop: $("#scrollToHere").offset().top
            }, "slow");
            $scope.loadData();
        };
        $scope.loadData = function () {
            $scope.$apply(function () {
                $scope.filteredTodos = []
                        , $scope.currentPage = 1
                        , $scope.numPerPage = 10
                        , $scope.maxSize = 5;
                $scope.$watch('currentPage + numPerPage', function () {
                    var begin = (($scope.currentPage - 1) * $scope.numPerPage)
                            , end = begin + $scope.numPerPage;
                    $scope.filteredTodos = $scope.todos.slice(begin, end);
                });
            });
        };
        if (!searchData.allkeywordsCloud) // if no load data by default
        {
            waitingDialog.show();
            var queryKeywords = globalData.PREFIX
                    + ' CONSTRUCT { '
                    + ' ?keyword rdfs:label ?k; '
                    + ' uc:total ?totalPub } '
                    + ' FROM <' + globalData.centralGraph + '> '
                    + ' WHERE { '
                    + '     SELECT ?keyword ?k (COUNT(DISTINCT(?subject)) AS ?totalPub) '
                    + '     WHERE { '
                    + '         ?person foaf:publications ?subject. '
                    + '         ?subject bibo:Quote ?k . '
                    + '         BIND(IRI(?k) AS ?keyword) . '
                    + '     } '
                    + '     GROUP BY ?keyword ?k '
                    + '     HAVING(?totalPub > 2 && ?totalPub < 25) '
                    + ' LIMIT 150'
                    //+'ORDER BY DESC(?totalPub) '
                    + '}';
            sparqlQuery.querySrv({query: queryKeywords}, function (rdf) {
                jsonld.compact(rdf, globalData.CONTEXT, function (err, compacted) {
                    $scope.$apply(function () {
                        $scope.data = {schema: {"context": globalData.CONTEXT, fields: ["rdfs:label", "uc:total"]}, data: compacted};
                        searchData.allkeywordsCloud = {schema: {"context": globalData.CONTEXT, fields: ["rdfs:label", "uc:total"]}, data: compacted};
                        waitingDialog.hide();
                    });
                });
            });
        }
        else
        {
            $scope.data = searchData.allkeywordsCloud;
        } // end if if (!searchData.allkeywordsCloud) 



    }]);