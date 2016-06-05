CONTIKI_PROJECT=$(PROJ)

TARGET=srf06-cc26xx
BOARD=sensortag/cc2650

all: $(CONTIKI_PROJECT)

CONTIKI = $(HOME)/Documents/CSSE4011/contiki-git
CONTIKI_WITH_IPV6 = 1
#UIP_CONF_ROUTER = 0
CFLAGS += -DUIP_CONF_ND6_SEND_NA=1

include $(CONTIKI)/Makefile.include

prog:
	$(HOME)/Documents/CSSE4011/DSLite/DebugServer/bin/DSLite load -c ~/Documents/CSSE4011/contiki-examples/tools/sensortag.ccxml -f $(CONTIKI_PROJECT).elf