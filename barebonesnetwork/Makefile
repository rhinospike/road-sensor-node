CONTIKI_PROJECT=barebonesnetwork

TARGET=srf06-cc26xx
BOARD=sensortag/cc2650

all: $(CONTIKI_PROJECT)

#CONTIKI = $(HOME)/Documents/CSSE4011/contiki-git
CONTIKI_WITH_IPV6 = 1
#UIP_CONF_ROUTER = 0
CFLAGS += -DUIP_CONF_ND6_SEND_NA=1

include $(CONTIKI)/Makefile.include

prog:
	DSLite load -c ../sensortag.ccxml -f $(CONTIKI_PROJECT).elf

.PHONY: tags
tags:
	ctags -R . $(CONTIKI)
	$(MAKE) $(CONTIKI_PROJECT) -n | awk '/$(CONTIKI_PROJECT)/{i++}i==2' | sed -e 's/ -/\n-/g' | tail -n +2 > .syntastic_c_config
	~/.vim/plugged/YCM-Generator/config_gen.py . -M="-i -j4 CC='clang -Werror -Wextra' LD=clang" -e
