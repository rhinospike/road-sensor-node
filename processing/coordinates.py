#!/usr/bin/env python2
import requests
import gmplot


r = requests.get('https://road-sensor-db.herokuapp.com/sensors')

latitudes = []
longitudes = []

for sensor in r.json():
    if sensor['latitude'] != 0  and sensor['longitude'] != 0:
        latitudes.append(sensor['latitude'])
        longitudes.append(sensor['longitude'])

print(longitudes)
print(latitudes)

gmap = gmplot.GoogleMapPlotter(latitudes[0], longitudes[0], 18)
gmap.scatter(latitudes, longitudes, 'b', size=10, marker=False)

gmap.draw("taglocations.html")
