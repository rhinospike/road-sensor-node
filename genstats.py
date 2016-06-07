#!/usr/bin/env python
import requests
import datetime

url = 'https://road-sensor-db.herokuapp.com'

r = requests.get(url + '/readings')

data = r.json()

sensorTotals = {}
avgHops = {}
maxHops = 0
messageids = {}
firstTransmission = {}
lastTransmission = {}

for reading in data:

    sensorid = reading['sensorid']
    hops = reading['hops']
    messageid = reading['messageid']
    timestamp = reading['timestamp']

    if hops > maxHops:
        maxHops = hops

    if not sensorid in firstTransmission or timestamp < firstTransmission[sensorid]:
        firstTransmission[sensorid] = timestamp;

    if not sensorid in lastTransmission or timestamp > lastTransmission[sensorid]:
        lastTransmission[sensorid] = timestamp;

    if sensorid in sensorTotals:
        sensorTotals[sensorid] += 1
        avgHops[sensorid] += hops
        messageids[sensorid].append(messageid)
    else:
        sensorTotals[sensorid] = 1
        avgHops[sensorid] = hops
        messageids[sensorid] = [messageid]

for sensor in avgHops.keys():
    avgHops[sensor] /= sensorTotals[sensor]

expectedpackets = {}
receivedpackets = {}
successrates = {}

for sensor in messageids.keys():
    ids = messageids[sensor]
    splitids = []
    expectedpackets[sensor] = 0
    receivedpackets[sensor] = 0

    lastsplit = 0
    for i in range(1, len(ids)):
        if ids[i] >= ids[i-1]:
            splitids.append(ids[lastsplit:i])
            lastsplit = i
    splitids.append(ids[lastsplit:])

    for split in splitids:
        expectedpackets[sensor] += max(split) - min(split) + 1
        receivedpackets[sensor] += len(split)
    successrates[sensor] = receivedpackets[sensor] / expectedpackets[sensor]

    if successrates[sensor] > 1:
        print(splitids)

print(sensorTotals)
print(avgHops)
print(maxHops)
print('Sensor\tExpected\tReceived\tSuccess\tAvg. Hops\tFirst\tLast')
for sensor in expectedpackets.keys():
    print('{0}\t{1}\t{2}\t{3:.2f}%\t{4:.2f}\t{5}\t{6}'.format(
        sensor,
        expectedpackets[sensor],
        receivedpackets[sensor],
        successrates[sensor] * 100,
        avgHops[sensor],
        str(firstTransmission[sensor]),
        str(lastTransmission[sensor])))
