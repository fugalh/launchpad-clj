#!/bin/bash
function usage {
  echo "usage: $0 <example> [args]"
  echo "Where <example> is the basename of an example in src/examples"
  echo
  echo "e.g. $0 roxanne"
  exit 1
}

test $# -ge 1 || usage
T=$1
shift
test -f "$(dirname "$0")/src/examples/$T.clj" || usage

lein run -m examples.$T "$@"
