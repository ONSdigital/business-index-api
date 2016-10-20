var testUi = angular.module("test-ui", [
  "ui.router",
  "ui.bootstrap",
  "ui.bootstrap.typeahead"
]);

testUi.config(["$stateProvider", "$urlRouterProvider",
  function($stateProvider, $urlRouterProvider) {

  $urlRouterProvider.otherwise("/");

  $stateProvider.state("view", {
      url: "/view/:id",
      views: {
        "search": {
          controller: "SearchController",
          templateUrl: "/assets/partials/view.html"
        }
      }
    }).state("404", {
      url: "/404",
      templateUrl: "/assets/partials/404.html"
    });
  }]);

testUi.run(["$rootScope", "$state", "$log", function($rootScope, $state, $log) {

  $rootScope.previousState = null;
  $rootScope.currentState = null;

  $rootScope.$on("$stateNotFound", function(event, unfoundState, fromState, fromParams) {
    $log.error(event, unfoundState, fromState, fromParams);
    $state.go("404");
  });

  $rootScope.$on('$stateChangeError',
      function(event, toState, toParams, fromState, fromParams, error) {
        // this is required if you want to prevent the $UrlRouter reverting the URL to the previous valid location
        event.preventDefault();
        $log.error(event, toState, toParams, fromState, fromParams, error);
      });

  $rootScope.$on("$stateChangeSuccess", function(ev, to, toParams, from, fromParams) {
    $rootScope.previousState = from.name;
    $rootScope.currentState = to.name;

    $log.debug("Previous state:" + $rootScope.previousState);
    $log.debug("Current state:" + $rootScope.currentState);
  });

}]);


testUi.controller("MatchController", [
  "$scope",
  function($scope) {

  $scope.encode = function(match) {
    return encodeURIComponent(match.label);
  };

}]);

testUi.controller("ViewBusinessController", [
  "$scope",
  "$http",
  "$stateParams",
  function($scope, $http, $stateParams) {

  $scope.businessName = $stateParams.id;

  $http.get('/search', {
    params: {
      "query": "\"" + decodeURIComponent($scope.businessName) + "\""
    }
  }).then(function(response) {
    $scope.item = response.data[0];
  });
}]);

testUi.controller("SearchController", [
  "$scope",
  "$http",
  function($scope, $http) {

    var _selected;

    $scope.getBusiness = function(query) {
      return $http.get('/v1/search', {
        params: {
          "query": query
        }
      }).then(function(response) {
        return response.data.map(function(el) {
          return el.businessName || "";
        });
      });
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
