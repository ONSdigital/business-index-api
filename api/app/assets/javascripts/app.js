var testUi = angular.module("test-ui", [
  "ui.router",
  "ui.bootstrap",
  "ui.bootstrap.typeahead",
  'toggle-switch'
]);

testUi.config(["$stateProvider", "$urlRouterProvider",
  function($stateProvider, $urlRouterProvider) {

  $urlRouterProvider.otherwise("/");

  $stateProvider.state("view", {
      url: "/view/:id",
      views: {
        "content": {
          controller: "ViewBusinessController",
          templateUrl: "/assets/partials/view.html"
        }
      }
    }).state("import", {
      url: "/import",
      views: {
        "content": {
          controller: "ImportDataController",
          templateUrl: "/assets/partials/importer.html"
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
