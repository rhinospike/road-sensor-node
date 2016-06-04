/*
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * This file is part of the Contiki operating system.
 *
 */

#include "contiki.h"
#include "contiki-lib.h"
#include "contiki-net.h"

#include "sys/ctimer.h"
#include "dev/leds.h"
#include <string.h>

#include "sensortag/board-peripherals.h"
#include "sensortag/cc2650/board.h"
#include "lib/cc26xxware/driverlib/gpio.h"

#include "ti-lib.h"

#define SENSORTAG_GROVE2_DP2	1 << BOARD_IOID_DP2
#define SENSORTAG_GROVE2_DP3	1 << BOARD_IOID_DP3

#define DEBUG DEBUG_PRINT
#include "net/ip/uip-debug.h"

#define UIP_IP_BUF ((struct uip_ip_hdr *)&uip_buf[UIP_LLH_LEN])

#define MAX_PAYLOAD_LEN 120

static void RGB_write(uint8_t, uint8_t, uint8_t);

static struct uip_udp_conn *server_conn;

static struct ctimer utc_timer;
static struct ctimer status_timer;
static uint32_t localtimeutc;

static void
updateUtcTime(uint32_t utctime)
{
	localtimeutc = utctime;
}

static uint32_t
getUtcTimeFromLocalTime()
{
	return localtimeutc;
}

static void
utccallback(void *ptr)
{
	ctimer_reset(&utc_timer);
	localtimeutc++;

	switch (localtimeutc % 9)
	{
		case 0:
			RGB_write(255,0,0);
			break;
		case 3:
			RGB_write(0,255,0);
			break;
		case 6:
			RGB_write(0,0,255);
			break;
		default:
			break;
	}
}

static void
status_callback(void *ptr)
{
	leds_toggle(LEDS_RED);
  	ctimer_reset(&status_timer);
	uip_udp_packet_send(server_conn, &localtimeutc, sizeof(uint32_t));
 	PRINTF("UTC: '%lu'\r\n", getUtcTimeFromLocalTime());
	leds_toggle(LEDS_RED);
}

PROCESS(udp_server_process, "UDP server process");
AUTOSTART_PROCESSES(&resolv_process,&udp_server_process);
/*---------------------------------------------------------------------------*/
static void
tcpip_handler(void)
{
  leds_toggle(LEDS_GREEN);
  if(uip_newdata()) {
    ((char *)uip_appdata)[uip_datalen()] = 0;
    updateUtcTime(*((uint32_t *)uip_appdata));
    PRINTF("UTC received: '%lu' from ", getUtcTimeFromLocalTime());
    PRINT6ADDR(&UIP_IP_BUF->srcipaddr);
    PRINTF("\n");

    uip_ipaddr_copy(&server_conn->ripaddr, &UIP_IP_BUF->srcipaddr);
    /* Restore server connection to allow data from any node */
    //memset(&server_conn->ripaddr, 0, sizeof(server_conn->ripaddr));
  }
  leds_toggle(LEDS_GREEN);
}
/*---------------------------------------------------------------------------*/
static void
print_local_addresses(void)
{
  int i;
  uint8_t state;

  PRINTF("Server IPv6 addresses: ");
  for(i = 0; i < UIP_DS6_ADDR_NB; i++) {
    state = uip_ds6_if.addr_list[i].state;
    if(uip_ds6_if.addr_list[i].isused &&
       (state == ADDR_TENTATIVE || state == ADDR_PREFERRED)) {
      PRINT6ADDR(&uip_ds6_if.addr_list[i].ipaddr);
      PRINTF("\n\r");
    }
  }
}

static void
RGB_write(uint8_t red, uint8_t blue, uint8_t green)
{
	static int i;
	GPIOPinWrite(SENSORTAG_GROVE2_DP3, 0);

	uint8_t flag = (red & 0xc0) >> 6;
	flag |= (green & 0xc0) >> 4;
	flag |= (blue & 0xc0) >> 2;
	flag = ~flag;

	uint32_t col_frame = (flag << 24) + (blue << 16) + (green << 8) + red;

	// Zero frame for start
	GPIOPinWrite(SENSORTAG_GROVE2_DP2, 0);
	for (i = 0; i < 64; i++) GPIOPinToggle(SENSORTAG_GROVE2_DP3);

	for (i = 0; i < 32; i++)
	{
		GPIOPinWrite(SENSORTAG_GROVE2_DP2, (col_frame >> (31 - i)) & 1);
		GPIOPinToggle(SENSORTAG_GROVE2_DP3);
		GPIOPinToggle(SENSORTAG_GROVE2_DP3);
	}
	for (i = 0; i < 32; i++)
	{
		GPIOPinWrite(SENSORTAG_GROVE2_DP2, (col_frame >> (31 - i)) & 1);
		GPIOPinToggle(SENSORTAG_GROVE2_DP3);
		GPIOPinToggle(SENSORTAG_GROVE2_DP3);
	}

	// Zero frame for end
	GPIOPinWrite(SENSORTAG_GROVE2_DP2, 0);
	for (i = 0; i < 64 * 2; i++) GPIOPinToggle(SENSORTAG_GROVE2_DP3);
}


/*---------------------------------------------------------------------------*/
PROCESS_THREAD(udp_server_process, ev, data)
{
#if UIP_CONF_ROUTER
  uip_ipaddr_t ipaddr;
#endif /* UIP_CONF_ROUTER */

  PROCESS_BEGIN();
  PRINTF("UDP server started\n\r");

#if RESOLV_CONF_SUPPORTS_MDNS
  resolv_set_hostname("contiki-udp-server");
#endif

#if UIP_CONF_ROUTER
  uip_ip6addr(&ipaddr, 0xaaaa, 0, 0, 0, 0, 0, 0, 0);
  uip_ds6_set_addr_iid(&ipaddr, &uip_lladdr);
  uip_ds6_addr_add(&ipaddr, 0, ADDR_AUTOCONF);
#endif /* UIP_CONF_ROUTER */

  print_local_addresses();

  GPIODirModeSet(SENSORTAG_GROVE2_DP2, GPIO_DIR_MODE_OUT); // Data
  GPIODirModeSet(SENSORTAG_GROVE2_DP3, GPIO_DIR_MODE_OUT); // Clock

  RGB_write(255,0,0);
  clock_wait(CLOCK_SECOND);
  RGB_write(0,255,0);
  clock_wait(CLOCK_SECOND);
  RGB_write(0,0,255);

  //Create UDP socket and bind to port 3000
  server_conn = udp_new(NULL, UIP_HTONS(3001), NULL);
  server_conn->rport = UIP_HTONS(7000);
  udp_bind(server_conn, UIP_HTONS(3000));

  ctimer_set(&utc_timer, CLOCK_SECOND, utccallback, NULL);
  ctimer_set(&status_timer, 5 * CLOCK_SECOND, status_callback, NULL);

  while(1) {
    PROCESS_YIELD();

	//Wait for tcipip event to occur
    if(ev == tcpip_event) {
      tcpip_handler();
    }
  }

  PROCESS_END();
}
/*---------------------------------------------------------------------------*/
