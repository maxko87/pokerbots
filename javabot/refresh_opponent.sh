#!/bin/bash
rm -rf ./opponent
cp -R . ../opponent
mv ../opponent/ .
open ./opponent/src/pokerbots/player/BrainSwitchingPlayer_5.java