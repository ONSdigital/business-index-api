testUi.controller("SearchController", [
  "$scope",
  "$http",
  "$log",
  function($scope, $http, $log) {

    $scope.suggest = false;

    var _selected;

    $scope.getResults = function(endpoint, query) {
      return $http.get(endpoint, {
        params: {
          "query": query
        }
      }).then(function(response) {
        return (response.data || []);
      });
    };

    $scope.searchBusiness = function(query) {
      return $scope.getResults("/v1/search", query);
    };

    $scope.suggestBusiness = function(query) {
      return $scope.getResults("/v1/suggest", query);
    };

    $scope.search = function(query) {
      if ($scope.suggest) {
        $log.info("Searching for a business", query);
        return $scope.suggestBusiness(query);
      } else {
        $log.info("Suggesting a business", query);
        return $scope.searchBusiness(query);
      }
    };

    $scope.ngModelOptionsSelected = function(value) {
      if (arguments.length) {
        _selected = value;
      } else {
        return _selected;
      }
    };

    $scope.modelOptions = {
      debounce: {
        default: 500,
        blur: 250
      },
      getterSetter: true
    };

  }
]);
