#!/bin/bash

CLASSPATH=/Core/home/kkoning/Dropbox/git/BroadbandGame/BroadbandGame/bin/:/Core/home/kkoning/Dropbox/git/Agency/Agency/bin:/Core/home/kkoning/Dropbox/git/Agency/Agency/lib/ecj-21.jar:/Core/home/kkoning/Dropbox/git/Agency/Agency/lib/mason.16.jar:/Core/home/kkoning/Dropbox/git/Agency/Agency/lib/commons-cli-1.2.jar:/Core/home/kkoning/Dropbox/git/Agency/Agency/lib/commons-math3-3.1.jar 

DIR=`pwd`

cd /Core/home/kkoning/Dropbox/git/BroadbandGame
git rev-parse HEAD > $DIR/BroadbandGame.version
git status >> $DIR/BroadbandGame.version

cd /Core/home/kkoning/Dropbox/git/Agency
git rev-parse HEAD > $DIR/Agency.version
git status >> $DIR/Agency.version

cd $DIR

java ec.Evolve -file $1


