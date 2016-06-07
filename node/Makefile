CONTIKI_PROJECT=node

all: $(CONTIKI_PROJECT)

TARGET = srf06-cc26xx
BOARD = sensortag/cc2650
CONTIKI = $(HOME)/repos/contiki
CONTIKI_WITH_IPV6 = 1
#UIP_CONF_ROUTER = 0
CFLAGS += -DUIP_CONF_ND6_SEND_NA=1
CFLAGS += -DRF_CORE_CONF_CHANNEL=13
include $(CONTIKI)/Makefile.include

erase:
	$(HOME)/uniflash_3.4/uniflash.sh -ccxml ~/Downloads/sensortag.ccxml -targetOp reset -operation Erase

prog:
	DSLite load -c ~/repos/csse4011-s4321490/sensortag.ccxml -f $(CONTIKI_PROJECT).elf

progu:
	$(HOME)/uniflash_3.4/uniflash.sh -ccxml ~/Downloads/sensortag.ccxml -targetOp reset -operation Erase
	$(HOME)/uniflash_3.4/uniflash.sh -ccxml ~/Downloads/sensortag.ccxml -program $(CONTIKI_PROJECT).hex

.PHONY: tags
tags:
	ctags -R . $(CONTIKI)
	$(MAKE) $(CONTIKI_PROJECT) -n | awk '/$(CONTIKI_PROJECT)/{i++}i==2' | sed -e 's/ -/\n-/g' | tail -n +2 > .syntastic_c_config
	~/.vim/plugged/YCM-Generator/config_gen.py . -M="-i -j4 CC='clang -Werror -Wextra' LD=clang"
