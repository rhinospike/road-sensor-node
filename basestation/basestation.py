#!/usr/bin/env python

import sys
import serial
import json
import signal
import spidev
import opc
import threading
import time
import datetime
import requests
import fcntl, socket, struct

def getMAC(interface):
    try:
        mac = open('/sys/class/net/'+interface+'/address').readline()
    except:
        mac = "00:00:00:00:00:00"
    return mac[0:17]

DB_URL = 'https://road-sensor-db.herokuapp.com'
#DB_URL = 'http://xps13-arch:5000'
SENSORS_URL = DB_URL + '/sensors'
READINGS_URL = DB_URL + '/readings'
MAC_ADDRESS = getMAC('wlan0')

class StoppableThread(threading.Thread):
    def __init__(self):
        super().__init__()
        self._stopevent = threading.Event()

    def stop(self):
        self._stopevent.set()

class SensorTag_Thread(StoppableThread):
    def __init__(self):
        super().__init__()
        self.ser = serial.Serial(port = '/dev/ttyACM0', baudrate = 115200, timeout = 0)

    def run(self):
        data = '';
        while(1):
            c = self.ser.read()
            if len(c) > 0:
                data += c.decode()
            elif len(data) > 0 and data[-1] == '\n':# and data.count("{") == data.count("}"):
                print(data)
                data = ''

            if self._stopevent.isSet():
                return

class OPC_Thread(StoppableThread):
    def __init__(self, period):
        super().__init__()
        self.period = period;

        spi = spidev.SpiDev()
        spi.open(0, 0)
        spi.mode = 1
        spi.max_speed_hz = 500000

        self.alphasense = opc.OPCN2(spi)

        # Turn the opc ON
        self.alphasense.on()

    def OPCRead(self):
        # Read the histogram
        hist = self.alphasense.histogram()

        # Calculate the total
        total = 0.0
        samplePeriod = self.period
        for name, value in hist.items():
            if name.startswith('Bin '):
                total += value
            elif name == 'Sampling Period':
                samplePeriod = value

        total /= samplePeriod
        print('Total: {0:.3f}'.format(total))
        return total

    def sendReading(self, reading):
        data = {
            'sensorid' : MAC_ADDRESS,
            'timestamp' : str(datetime.datetime.now()),
            'sensors' : {
                'dust' : reading
            }
        }
        try:
            print(requests.post(READINGS_URL, json=[data]))
        except requests.ConnectionError:
            print('Failed to post OPC reading')

    def run(self):
        i = 0
        while(1):

            if i == self.period:
                # Reset counter
                i = 0
                self.sendReading(self.OPCRead())

            i = i + 1

            # Sleep for a second
            time.sleep(1)

            if self._stopevent.isSet():
                # Turn the opc OFF
                self.alphasense.off()
                return

if __name__ == "__main__":
    opcthread = OPC_Thread(20)
    stthread = SensorTag_Thread();

    try:
        opcthread.start()
        stthread.start()
        signal.pause()
    except KeyboardInterrupt:
        print("\nExiting...")
        opcthread.stop()
        stthread.stop()
        opcthread.join()
        stthread.join()
        sys.exit(0)
