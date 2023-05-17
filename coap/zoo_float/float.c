#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "contiki.h"
#include "coap-engine.h"
#include "dev/leds.h"
#include "coap-blocking-api.h"

#include "node-id.h"
#include "net/ipv6/simple-udp.h"
#include "net/ipv6/uip.h"
#include "net/ipv6/uip-ds6.h"
#include "net/ipv6/uip-debug.h"
#include "routing/routing.h"

#define SERVER_EP "coap://[fd00::1]:5683" // da capire che metterci
#define CONNECTION_TRY_INTERVAL 1
#define REGISTRATION_TRY_INTERVAL 1
#define SIMULATION_INTERVAL 1
#define SENSOR_TYPE "light"

/* Log configuration */
#include "sys/log.h"
#define LOG_MODULE "light"
#define LOG_LEVEL LOG_LEVEL_APP

#define INTERVAL_BETWEEN_CONNECTION_TESTS 1

extern coap_resource_t res_float_level;

bool float_low = true;

//char *service_url = "/registration";
//static bool registered = false;

static struct etimer connectivity_timer;
//static struct etimer wait_registration;

static struct etimer simulator_timer;

static bool is_connected() {
	if(NETSTACK_ROUTING.node_is_reachable()) {
		LOG_INFO("The Border Router is reachable\n");
		return true;
  	}

	LOG_INFO("Waiting for connection with the Border Router\n");
	return false;
}

/* Declare and auto-start this file's process */
PROCESS(float_server, "Float Server");
AUTOSTART_PROCESSES(&float_server);

PROCESS_THREAD(float_server, ev, data){
	PROCESS_BEGIN();
	PROCESS_PAUSE();

	LOG_INFO("Starting Float CoAP-Server\n");
	coap_activate_resource(&res_float_level, "float/level");

    // Start simulator
    srand(42); // Most causal number

	// try to connect to the border router
	etimer_set(&connectivity_timer, CLOCK_SECOND * INTERVAL_BETWEEN_CONNECTION_TESTS);
	PROCESS_WAIT_UNTIL(etimer_expired(&connectivity_timer));
	while(!is_connected()) {
		etimer_reset(&connectivity_timer);
		PROCESS_WAIT_UNTIL(etimer_expired(&connectivity_timer));
	}

    // Simulator
    etimer_set(&simulator_timer, CLOCK_SECOND * 20);
    while(true) {
        PROCESS_WAIT_UNTIL(etimer_expired(&connectivity_timer));

        float_low = rand() >= RAND_MAX * 3.0/4.0;
        LOG_INFO("Float status = %u", float_low);

        etimer_reset(&connectivity_timer);
    }

    PROCESS_END();
}