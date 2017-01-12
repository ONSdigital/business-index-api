testUi.controller("MatchController", [
  "$scope",
  function($scope) {

    $scope.encode = function(match) {
      console.log(match);
      return match.label;
    };

  }]);

