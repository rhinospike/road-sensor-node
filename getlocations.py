#!/usr/bin/env python

import os
import exifread
import requests

url = 'https://road-sensor-db.herokuapp.com'

def degreesToFloat(deg):
    res = 0.0
    div = 1
    for i in range(0,3):
        res += (float(deg.values[i].num) / deg.values[i].den) / div
        div *= 60
    return res

if __name__ == "__main__":
    for d in os.listdir('TagLocations'):
        if os.path.isdir('TagLocations/' + d):
            print(d)
            totLongitude = 0.0
            totLatitude = 0.0
            n = 0
            for f in os.listdir('TagLocations/' + d):
                if f.endswith('.jpg'):
                    image = open('TagLocations/' + d + '/' + f, 'rb')
                    tags = exifread.process_file(image)
                    totLongitude += degreesToFloat(tags['GPS GPSLongitude'])
                    totLatitude -= degreesToFloat(tags['GPS GPSLatitude'])
                    n += 1
            if n > 0:
                latitude = totLatitude/n
                longitude = totLongitude/n
                newsensor = {"sensorid": d,"latitude": latitude,"longitude": longitude}
                print(newsensor)
                print(requests.post(url + '/sensors',json=newsensor))
