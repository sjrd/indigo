#!/usr/bin/env bash

set -e

# Indigo
echo ">>> Indigo Workers"
cd indigo
sbt exportWorkers
cp -u indigo-scene-worker/target/scala-3.0.0-M3/indigo-scene-worker-opt.js ../indigo-plugin/indigo-plugin/resources/workers/
cp -u indigo-scene-worker/target/scala-3.0.0-M3/indigo-scene-worker-opt.js.map ../indigo-plugin/indigo-plugin/resources/workers/
cp -u indigo-render-worker/target/scala-3.0.0-M3/indigo-render-worker-opt.js ../indigo-plugin/indigo-plugin/resources/workers/
cp -u indigo-render-worker/target/scala-3.0.0-M3/indigo-render-worker-opt.js.map ../indigo-plugin/indigo-plugin/resources/workers/
cd ..

# Indigo Plugin
echo ">>> Indigo Plugin"
cd indigo-plugin
bash build.sh
cd ..

# SBT Indigo
echo ">>> SBT-Indigo"
cd sbt-indigo
bash build.sh
cd ..

# Mill Indigo
echo ">>> Mill-Indigo"
cd mill-indigo
bash build.sh
cd ..

