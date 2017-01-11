testUi.controller("ImportController", [
  "$scope",
  "$http",
  "$stateParams",
  function($scope, $http, $stateParams) {

  var endpoint = "/api/v1/data/import";

  $scope.debugImportData = function(query, obj) {
    return $http.post(endpoint, {
      params: {
        "query": query
      },
      payload: obj
    });
  };

}]);
