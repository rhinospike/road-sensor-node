#!/usr/bin/env python

import sys
import signal
import spidev
import opc
import threading
import time
from time import sleep

class StoppableThread(threading.Thread):
    def __init__(self):
        super().__init__()
        self._stopevent = threading.Event()

    def stop(self):
        self._stopevent.set()

class SensorTag_Thread(StoppableThread):
    def __init__(self):
        super().__init__()

    def run(self):
        while(1):
            threading.sleep(1)

            if self._stopevent.isSet():
                # Turn the opc OFF
                self.alphasense.off()
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
        sleep(1)


    def run(self):
        i = 0
        while(1):

            if i == self.period:
                # Read the histogram
                print (self.alphasense.histogram())
                i = 0

            i = i + 1

            # Sleep for a second
            time.sleep(1)

            if self._stopevent.isSet():
                # Turn the opc OFF
                self.alphasense.off()
                return

if __name__ == "__main__":
    opcthread = OPC_Thread(5)

    try:
        opcthread.start()
        signal.pause()
    except KeyboardInterrupt:
        print("\nExiting...")
        opcthread.stop()
        opcthread.join()
        sys.exit(0)
