# run ./refresh_opponent first
import os, random, csv, re

NUM_RUNS = 1000

def replaceVal(valNum, newValue):
    with open("src/pokerbots/utils/BettingBrain.java", "r+") as f:
        text = f.read()

	with open("src/pokerbots/utils/BettingBrain.java", "w") as fw:

		start_index = text.index("val"+str(valNum)+" = ")
		end_index = text.index(";", start_index)
		text = text[0:start_index] + "val"+str(valNum)+" = " + str(newValue)+"f" + text[end_index:]
		fw.write(text)
		fw.close()

def bound(num, min, max):
	if num < min:
		return min
	if num > max:
		return max
	return num

def runEngine():

	os.system("./socket_pokerbot_helper.sh")
	os.system("java -jar engine_1.3.jar")
	#tommy!

def updateLog(r):
	
	#grab final and intermediate chip counts from dump
	with open("BOT_V4.dump", "r+") as f:
		dump = f.read()
		chip_count1000 = re.findall("HANDOVER (-?\d+) ", dump)[-1]
		chip_count333 = re.findall("HANDOVER (-?\d+) ", dump)[333]
		chip_count666 = re.findall("HANDOVER (-?\d+) ", dump)[666]
		print chip_count333, chip_count666, chip_count1000 
	
	#write score and params to csv
	res = [chip_count1000, chip_count666, chip_count333]
	for i in range(1,len(r)):
		res += [r[i]]
	csvout = csv.writer(open("data.csv", "a"))
	csvout.writerow(tuple(res))

def initalizeCsv():
	csvout = csv.writer(open("data.csv", "a"))
	csvout.writerow(("score 1000","score 666","score 333","val1","val2","val3","val4","val5","val6","val7","val8","val9","val10","val11","val12"))


initalizeCsv()

for run in range(1,NUM_RUNS+1):
	r0 = None

	r1 = random.random()
	r2 = r1 + random.random()*.5
	r3 = random.random()
	r4 = r3 + random.random()*.5
	r5 = random.random()
	r6 = r5 + random.random()*.5
	r7 = random.random()
	r8 = r7 + random.random()*.5

	r9 = random.uniform(0, .5)
	r10 = random.uniform(0, .5)
	r11 = random.uniform(0, .5)
	r12 = random.uniform(0, .5)

	r = [r0, r1, r2, r3, r4, r5, r6, r7, r8, r9, r10, r11, r12]
	
	for i in range(1,len(r)):
		replaceVal(i, r[i])
	
	runEngine()
	updateLog(r)	






















