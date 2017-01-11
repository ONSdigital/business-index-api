testUi.controller("MatchController", [
  "$scope",
  function($scope) {

    $scope.encode = function(match) {
      return encodeURIComponent(match.label);
    };

  }]);

