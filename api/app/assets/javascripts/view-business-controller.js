testUi.controller("ViewBusinessController", [
  "$scope",
  "$http",
  "$stateParams",
  function($scope, $http, $stateParams) {

    $scope.businessName = decodeURIComponent($stateParams.id);

    $scope.displayName = $scope.businessName.replace(/"/g, '');


    $http.get('/v1/search', {
      params: {
        "query": "\"" + $scope.businessName + "\""
      }
    }).then(function(response) {
      $scope.item = response.data[0];
    });
  }]);

