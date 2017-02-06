#!/usr/bin/env bash
echo "Pull request: ${TRAVIS_PULL_REQUEST}; Branch: ${TRAVIS_BRANCH}"

if [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_BRANCH" == "develop" ];
then
    if [ "${TRAVIS_SCALA_VERSION}" == "2.11.8" ] && [ "${TRAVIS_JDK_VERSION}" == "oraclejdk8" ];
    then

        git_user="ci@ons.gov.uk"
        echo "Setting git user email to $git_user"
        git config user.email ${git_user}

        echo "Setting git user name to Travis CI"
        git config user.name "Travis CI"

        echo "The current JDK version is ${TRAVIS_JDK_VERSION}"
        echo "The current Scala version is ${TRAVIS_SCALA_VERSION}"

        COMMIT_MSG=$(git log -1 --pretty=%B 2>&1)
        COMMIT_SKIP_MESSAGE="[version skip]"

        echo "Last commit message $COMMIT_MSG"
        echo "Commit skip message $COMMIT_SKIP_MESSAGE"

        if [[ $COMMIT_MSG == *"$COMMIT_SKIP_MESSAGE"* ]]
        then
            echo "Skipping version bump and simply tagging"
            sbt git-tag
        else
            sbt git-tag
        fi

        echo "Pushing tag to GitHub."
        git push --tags "https://${github_token}@${GH_REF}"

        if [[ $COMMIT_MSG == *"$COMMIT_SKIP_MESSAGE"* ]]
        then
            echo "No version bump performed in CI, no GitHub push necessary."
        else
            echo "Publishing version bump information to GitHub"
            git add .
            git commit -m "TravisCI: Bumping version to match CI definition [ci skip]"
            git checkout -b version_branch
            git checkout -B $TRAVIS_BRANCH version_branch
            git push "https://${github_token}@${GH_REF}" $TRAVIS_BRANCH
        fi
    else
        echo "Only tagging version for Scala 2.11.8 develop branch."
    fi
else
    echo "This is either a pull request or the branch is not develop, deployment not necessary"
fi
