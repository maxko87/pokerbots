#!/bin/bash
rm -rf ./opponent
cp -R . ../opponent
mv ../opponent/ .
open ./opponent/src/pokerbots/player/Main.java