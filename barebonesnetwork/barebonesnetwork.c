/*
 * Copyright (c) 2011, Swedish Institute of Computer Science.
 * All rights reserved.
 *
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
#include "net/ip/resolv.h"

#include "lib/random.h"
#include "sys/ctimer.h"

#define DEBUG DEBUG_PRINT
#include "net/ip/uip-debug.h"

#include "net/ip/simple-udp.h"

#include <stdbool.h>
#include <stdio.h>
#include <string.h>

#include "dev/leds.h"
#include "board-peripherals.h"
#include "ti-lib.h"


#include "contiki.h"
#include <stdio.h>
#include "dev/leds.h"
#include "sensortag/board-peripherals.h"
#include "sensortag/cc2650/board.h"
#include "lib/cc26xxware/driverlib/gpio.h"
#include "ti-lib.h"
#include "driverlib/aux_adc.h"
#include "driverlib/aux_wuc.h"
#include "sys/etimer.h"
#include "sys/ctimer.h"
#include "dev/watchdog.h"
#include "random.h"
#include "board-peripherals.h"
#include <stdint.h>

#define UDP_PORT 1234

#define SEND_INTERVAL		(5 * CLOCK_SECOND)
#define SEND_TIME		(random_rand() % (SEND_INTERVAL))

static struct simple_udp_connection broadcast_connection;

char* reading_type_names[] =
{
	"luminance",
	"irtempambient",
	"irtempobject",
	"humidity",
	"humiditytemp",
	"gas",
	"-"
};

typedef enum {
	READING_TYPE_LUMINANCE,
	READING_TYPE_IR_TEMP_AMBIENT,
	READING_TYPE_IR_TEMP_OBJECT,
	READING_TYPE_HUMIDITY,
	READING_TYPE_HUMIDITY_TEMP,
	READING_TYPE_GAS,
	READING_TYPE_LAST
} reading_type;

typedef struct _compact_addr {
	uint32_t upper;
	uint32_t lower;
} compact_addr_t;

typedef struct _message_id {
	compact_addr_t addr;
	uint32_t id;
} message_id_t;

typedef struct _reading {
	int32_t whole;
	uint16_t frac;
} reading_t;

typedef struct _message {
	message_id_t messageId;
	int32_t hops;
	reading_t readings[READING_TYPE_LAST];
} message_t;


static bool
message_in_register(message_id_t *messageId);

static void
copy_to_compact_address(compact_addr_t *caddr, uip_ipaddr_t *uipaddr);

static void
copy_compact_address(compact_addr_t *dest, compact_addr_t *src);

static bool
compact_addresses_match(compact_addr_t *caddr1, compact_addr_t *caddr2);

static void
print_compact_address(compact_addr_t *addr);

static void
add_message_to_register(message_id_t *messageId);

static bool
message_in_register(message_id_t *messageId);

static void
send_message(message_t *message);

static void
raw_receiver(struct simple_udp_connection *c,
		const uip_ipaddr_t *sender_addr,
		uint16_t sender_port,
		const uip_ipaddr_t *receiver_addr,
		uint16_t receiver_port,
		const uint8_t *data,
		uint16_t datalen);

/* Array for storing all previously seen ip addresses. Incoming addresses
 * are compared against this array to generate the address alias, since
 * there will be many more packets sent than IP addresses. */
#define MESSAGE_REGISTER_SIZE 64
static message_id_t messageRegister[MESSAGE_REGISTER_SIZE];

/* Index of current leading packet */
static uint16_t packetRegisterHead = 0;
static bool didWrap = false;

static compact_addr_t tagAddress;
static uint32_t messagesSent = 0;

static struct ctimer sensor_read_timer;

static void
activate_sensors(void *not_used)
{
	SENSORS_ACTIVATE(opt_3001_sensor);
	SENSORS_ACTIVATE(tmp_007_sensor);
	SENSORS_ACTIVATE(hdc_1000_sensor);
}

// Formats with 3 decimal places
static void
shift_decimal(int val, int places, reading_t* result)
{
	int i, divisor = 1;

	for (i = 0; i < places; i++)
	{
		divisor *= 10;
	}

	result->whole = val / divisor;
	result->frac = val % divisor;

	for (i = 0; i < 3 - places; i++)
	{
		result->frac *= 10;
	}
}

static void
print_message(message_t* m)
{
	printf("{"
		"\"sensorid\": %04lx%08lx,"
		"\"messageid\": %d,"
		"\"hops\": %d",
		m->messageId.addr.upper & 0xFFFF, m->messageId.addr.lower,
		m->messageId.id,
		m->hops);

	int i;
	for (i = 0; i < READING_TYPE_LAST; i++)
	{
		if (i == READING_TYPE_GAS &&
			m->readings[i].whole == 0 &&
		       	m->readings[i].frac < 10)
		{
			continue;
		}

		printf(",\"%s\": %ld.%03d",
			reading_type_names[i],
			m->readings[i].whole,
			m->readings[i].frac);
	}
	printf("}\r\n");
}

uint16_t get_gas_value(void) {
	uint16_t singleSampleADC;
	//intialisation of ADC
	ti_lib_aon_wuc_aux_wakeup_event(AONWUC_AUX_WAKEUP);
	while(!(ti_lib_aon_wuc_power_status_get() & AONWUC_AUX_POWER_ON)) { }
	// Enable clock for ADC digital and analog interface (not currently enabled in driver)
	// Enable clocks
	ti_lib_aux_wuc_clock_enable(AUX_WUC_ADI_CLOCK | AUX_WUC_ANAIF_CLOCK | AUX_WUC_SMPH_CLOCK);
	while(ti_lib_aux_wuc_clock_status(AUX_WUC_ADI_CLOCK | AUX_WUC_ANAIF_CLOCK | AUX_WUC_SMPH_CLOCK) != AUX_WUC_CLOCK_READY) { }
	// Connect AUX IO7 (DIO23, but also DP2 on XDS110) as analog input.
	AUXADCSelectInput(ADC_COMPB_IN_AUXIO6); 
	// Set up ADC range
	// AUXADC_REF_FIXED = nominally 4.3 V
	AUXADCEnableSync(AUXADC_REF_FIXED,  AUXADC_SAMPLE_TIME_2P7_US, AUXADC_TRIGGER_MANUAL);
	//Trigger ADC converting
	AUXADCGenManualTrigger();
	//reading adc value
	singleSampleADC = AUXADCReadFifo();
	//shut the adc down
	AUXADCDisable();
	return singleSampleADC;
}

static void
sensors_handler(process_data_t data)
{
	static message_t message;
	static bool ir_set = false, hdc_set = false, lum_set = false;

	if (data == &opt_3001_sensor) {
		shift_decimal(opt_3001_sensor.value(0), 2, &message.readings[READING_TYPE_LUMINANCE]);
		lum_set = true;
	} else if (data == &tmp_007_sensor) {
		tmp_007_sensor.value(TMP_007_SENSOR_TYPE_ALL);
		shift_decimal(tmp_007_sensor.value(TMP_007_SENSOR_TYPE_AMBIENT), 3, &message.readings[READING_TYPE_IR_TEMP_AMBIENT]);
		shift_decimal(tmp_007_sensor.value(TMP_007_SENSOR_TYPE_OBJECT), 3, &message.readings[READING_TYPE_IR_TEMP_OBJECT]);
		ir_set = true;
	} else if (data == &hdc_1000_sensor) {
		shift_decimal(hdc_1000_sensor.value(HDC_1000_SENSOR_TYPE_HUMIDITY), 2, &message.readings[READING_TYPE_HUMIDITY]);
		shift_decimal(hdc_1000_sensor.value(HDC_1000_SENSOR_TYPE_TEMP), 2, &message.readings[READING_TYPE_HUMIDITY_TEMP]);
		hdc_set = true;
	}

	if (ir_set && lum_set && hdc_set)
	{
		lum_set = false;
		ir_set = false;
		hdc_set = false;

		shift_decimal(get_gas_value(), 3, &message.readings[READING_TYPE_GAS]);

		message.hops = 0;
		message.messageId.id = messagesSent;
		copy_compact_address(&message.messageId.addr, &tagAddress);

		// Print message so basestation can process it
		print_message(&message);

		// printf("[Broadcasting]\r\n");
		send_message(&message);

		messagesSent++;

		//Callback timer for temp sensor
		ctimer_set(&sensor_read_timer, SEND_INTERVAL + SEND_TIME, activate_sensors, NULL);
	}
}

/*---------------------------------------------------------------------------*/
PROCESS(broadcast_process, "UDP broadcast example process");
AUTOSTART_PROCESSES(&resolv_process, &broadcast_process);
/*---------------------------------------------------------------------------*/
static void
receiver(message_t *message) {
	/* Process the message */
	printf("[Received] ");
	print_compact_address(&message->messageId.addr);
	printf(":\r\n");
	//printf(" ID: %lu, Hops: %lu:\r\n",
	//	message->messageId.id, message->hops);
	print_message(message);
	printf("\r\n");
}

/*---------------------------------------------------------------------------*/
static void
raw_receiver(struct simple_udp_connection *c,
		const uip_ipaddr_t *sender_addr,
		uint16_t sender_port,
		const uip_ipaddr_t *receiver_addr,
		uint16_t receiver_port,
		const uint8_t *data,
		uint16_t datalen)
{
	message_t *received;

	if (datalen != sizeof(message_t)) {
		printf("Incorrect incoming data size\r\n");
		return;
	}

	leds_on(LEDS_GREEN);

	received = (message_t *)data;

	/* Prints all incoming messages including messages already seen that
	 * should be ignored */
	// printf("[Recieved] ");
	// print_compact_address(&received->messageId.addr);
	// printf(" ID: %lu, M-Index: %lu Hops: %lu\r\n",
	// 	received->messageId.id, received->contents, received->hops);

	if (!message_in_register(&received->messageId)) {
		// printf("*>> Rebroadcasting\r\n");
		receiver(received);
		send_message(received);
	}
	leds_off(LEDS_GREEN);
}
/*---------------------------------------------------------------------------*/
static void
set_global_address(void)
{
	uip_ipaddr_t ipaddr;

	uip_ip6addr(&ipaddr, 0xaaaa, 0, 0, 0, 0, 0, 0, 0);
	uip_ds6_set_addr_iid(&ipaddr, &uip_lladdr);
	uip_ds6_addr_add(&ipaddr, 0, ADDR_AUTOCONF);
}
/*---------------------------------------------------------------------------*/
static void
add_message_to_register(message_id_t *messageId) {
	message_id_t *targetMessage;

	packetRegisterHead++;
	if (packetRegisterHead == MESSAGE_REGISTER_SIZE) {
		packetRegisterHead = 0;
		didWrap = true;
	}

	targetMessage = &messageRegister[packetRegisterHead];

	copy_compact_address(&targetMessage->addr, &messageId->addr);
	targetMessage->id = messageId->id;
}

static bool
message_in_register(message_id_t *messageId) {
	message_id_t *targetMessage;
	int searchTarget = packetRegisterHead;
	bool searchWrapped = false;

	// print_compact_address(&messageId->addr);
	// printf(" message id: %lu \r\n", messageId->id);

	while ((searchTarget >= 0 && !searchWrapped) ||
			(searchTarget > packetRegisterHead && searchWrapped)) {

		targetMessage = &messageRegister[searchTarget];
		// print_compact_address(&targetMessage->addr);
		// printf(" message id: %lu \r\n", targetMessage->id);
		if (targetMessage->id == messageId->id &&
				compact_addresses_match(&targetMessage->addr,
					&messageId->addr)) {
			return true;
		}

		searchTarget--;

		if (searchTarget == 0) {
			searchTarget = MESSAGE_REGISTER_SIZE - 1;
			searchWrapped = true;
		}
	}
	return false;
}
/*---------------------------------------------------------------------------*/
static void
copy_to_compact_address(compact_addr_t *caddr, uip_ipaddr_t *uipaddr) {
	caddr->upper = (uipaddr->u16[4] << 16) | uipaddr->u16[5];
	caddr->lower = (uipaddr->u16[6] << 16) | uipaddr->u16[7];
}

static void
copy_compact_address(compact_addr_t *dest, compact_addr_t *src) {
	dest->upper = src->upper;
	dest->lower = src->lower;
}

static bool
compact_addresses_match(compact_addr_t *caddr1, compact_addr_t *caddr2) {
	return (caddr1->upper == caddr2->upper) && (caddr1->lower == caddr2->lower);
}

static void
print_compact_address(compact_addr_t *addr) {
	printf("%04x:%04x:%04x:%04x", (int)(addr->upper >> 16),
			(int)(addr->lower & 0xffff), (int)(addr->lower >> 16),
			(int)(addr->lower & 0xffff));
}
/*---------------------------------------------------------------------------*/
static void
send_message(message_t *message) {
	uip_ipaddr_t addr; /* The BROADCAST address.
			      (Not the address of the ST) */

	leds_on(LEDS_RED);
	add_message_to_register(&message->messageId);

	message->hops++;
	uip_create_linklocal_allnodes_mcast(&addr);
	simple_udp_sendto(&broadcast_connection, message, sizeof(message_t), &addr);
	leds_off(LEDS_RED);
}
/*---------------------------------------------------------------------------*/
PROCESS_THREAD(broadcast_process, ev, data)
{
	PROCESS_BEGIN();

	/* Configure radio */
	int maxpower;
	NETSTACK_RADIO.get_value(RADIO_CONST_TXPOWER_MAX, &maxpower);
	NETSTACK_RADIO.set_value(RADIO_PARAM_TXPOWER, maxpower);
	NETSTACK_RADIO.set_value(RADIO_PARAM_CHANNEL, 18);

	set_global_address();

	uint32_t i, state;

	for(i = 0; i < UIP_DS6_ADDR_NB; i++) {
		state = uip_ds6_if.addr_list[i].state;
		if(uip_ds6_if.addr_list[i].isused &&
				(state == ADDR_TENTATIVE || state == ADDR_PREFERRED)) {
			copy_to_compact_address(&tagAddress, &uip_ds6_if.addr_list[i].ipaddr);
			break;
		}
	}

	printf("Tag address: ");
	print_compact_address(&tagAddress);
	printf("\r\n");

	simple_udp_register(&broadcast_connection, UDP_PORT,
			NULL, UDP_PORT,
			raw_receiver);

	activate_sensors(NULL);

	while(1) {
		PROCESS_YIELD(); // Let other threads run

		// Wait for sensor event
		if (ev == sensors_event)
		{
			sensors_handler(data);
		}

	}

	PROCESS_END();
}
/*---------------------------------------------------------------------------*/
