#!/bin/bash
rm -rf ./opponent
cp -R . ../opponent
mv ../opponent/ .
open ./opponent/src/pokerbots/player/Main.java
open ./opponent/src/pokerbots/utils/BettingBrain_old_v2.java