#!/bin/sh
osascript <<END
tell app "Terminal" to do script "sleep 5; cd \"`pwd`\"; ./tommy_pokerbot.sh 3001; exit" 
END