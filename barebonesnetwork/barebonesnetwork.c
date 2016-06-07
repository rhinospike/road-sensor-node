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
#include "sys/etimer.h"

#define DEBUG DEBUG_PRINT
#include "net/ip/uip-debug.h"

#include "simple-udp.h"

#include <stdbool.h>
#include <stdio.h>
#include <string.h>

#include "dev/leds.h"


#define UDP_PORT 1234

#define SEND_INTERVAL		(5 * CLOCK_SECOND)
#define SEND_TIME		(random_rand() % (SEND_INTERVAL))

static struct simple_udp_connection broadcast_connection;

typedef struct _compact_addr {
	uint32_t upper;
	uint32_t lower;
} compact_addr_t;

typedef struct _message_id {
	compact_addr_t addr;
	uint32_t id;
} message_id_t;

typedef struct _message {
	message_id_t messageId;
	uint32_t contents;
	uint32_t hops;
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

/*---------------------------------------------------------------------------*/
PROCESS(broadcast_process, "UDP broadcast example process");
AUTOSTART_PROCESSES(&resolv_process, &broadcast_process);
/*---------------------------------------------------------------------------*/
static void
receiver(message_t *message) {
	/* Process the message */
	printf("[Received] ");
	print_compact_address(&message->messageId.addr);
	printf(" ID: %lu, M-Index: %lu Hops: %lu\r\n",
			message->messageId.id, message->contents, message->hops);
}

static message_t*
sender(void) {
	static 	message_t message;

	message.messageId.id = messagesSent;
	copy_compact_address(&message.messageId.addr, &tagAddress);
	message.contents = packetRegisterHead;
	message.hops = 0; // Hops incremented every time the message is sent.

	return &message;
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
	static struct etimer periodic_timer;
	static struct etimer send_timer;

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


	etimer_set(&periodic_timer, SEND_INTERVAL);
	while(1) {
		PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&periodic_timer));
		etimer_reset(&periodic_timer);
		etimer_set(&send_timer, SEND_TIME);

		PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&send_timer));

		// printf("[Broadcasting]\r\n");
		send_message(sender());

		messagesSent++;
	}

	PROCESS_END();
}
/*---------------------------------------------------------------------------*/
