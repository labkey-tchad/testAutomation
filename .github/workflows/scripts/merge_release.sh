#!/bin/bash

if ! [ -x "$(command -v hub)" ]; then
  echo 'Error: GitHub command line tool is not installed.' >&2
  exit 1
fi

TRIAGE_ALIAS='labkey-teamcity' # 'LabKey/Triage'

# TARGET_BRANCH=release19.3
# MERGE_BRANCH=ff_19.3.11
# PR_NUMBER=9

if [ -z $TARGET_BRANCH ] || [ -z $MERGE_BRANCH ] || [ -z $PR_NUMBER ]; then
	echo "PR info not specified" >&2
	exit 1
fi

git config user.name "github-actions[bot]"

echo "Merge approved PR from $MERGE_BRANCH to $TARGET_BRANCH."
git fetch --unshallow
git checkout $TARGET_BRANCH
git merge origin/$MERGE_BRANCH -m "Merge $MERGE_BRANCH to $TARGET_BRANCH" && git push || {
	echo "Failed to merge!" >&2
	hub api repos/{owner}/{repo}/issues/$PR_NUMBER/comments --raw-field 'body=@'$TRIAGE_ALIAS' __ERROR__ Automatic merge failed!'
	exit 1
}
