testUi.controller("ViewBusinessController", [
  "$scope",
  "business",
  function($scope, business) {

  console.log(business);
  $scope.item = business;
}]);

