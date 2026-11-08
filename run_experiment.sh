#!/usr/bin/env bash
# Reproduces results/results.csv (about 2-3 minutes). Fixed heap + serial GC = less timing noise.
set -euo pipefail
mvn -q compile
java -Xms2g -Xmx2g -XX:+UseSerialGC -cp target/classes kz.city.search.Experiment results
