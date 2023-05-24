#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

#include "contiki.h"
#include "coap-engine.h"
#include "sys/etimer.h"
#include "dev/leds.h"
#include "coap-blocking-api.h"

#include "node-id.h"
#include "net/ipv6/simple-udp.h"
#include "net/ipv6/uip.h"
#include "net/ipv6/uip-ds6.h"
#include "net/ipv6/uip-debug.h"
#include "routing/routing.h"

#define SERVER_EP "coap://[fd00::1]:5683"
#define CONNECTION_TRY_INTERVAL 1
#define REGISTRATION_TRY_INTERVAL 1
#define SIMULATION_INTERVAL 1
#define SENSOR_TYPE "air_quality"

#define DO_REGISTER 1

/* Log configuration */
#include "sys/log.h"
#define LOG_MODULE "air-quality"
#define LOG_LEVEL LOG_LEVEL_APP

#define INTERVAL_BETWEEN_SIMULATIONS 3
#define INTERVAL_BETWEEN_CONNECTION_TESTS 1

extern coap_resource_t res_ventilation_system;
extern coap_resource_t res_co2_sensor;

char *service_url = "/registration";
static bool registered = false;

static struct etimer simulation_timer;
static struct etimer connectivity_timer;
static struct etimer wait_registration;



/*---------------------------------------------------------------------------*/
static bool is_connected() {
	if(NETSTACK_ROUTING.node_is_reachable()) {
		LOG_INFO("The Border Router is reachable\n");
		return true;
	}
	
	LOG_INFO("Waiting for connection with the Border Router\n");
	return false;
}

void client_chunk_handler(coap_message_t *response) {
	const uint8_t *chunk;
	if(response == NULL) {
		LOG_INFO("Request timed out\n");
		etimer_set(&wait_registration, CLOCK_SECOND* REGISTRATION_TRY_INTERVAL);
		return;
	}

	int len = coap_get_payload(response, &chunk);

	if(strncmp((char*)chunk, "Success", len) == 0)
		registered = true;
	else
		etimer_set(&wait_registration, CLOCK_SECOND* REGISTRATION_TRY_INTERVAL);
}

/* Declare and auto-start this file's process */
PROCESS(air_quality_server, "Air Quality Server");
AUTOSTART_PROCESSES(&air_quality_server);

PROCESS_THREAD(air_quality_server, ev, data) {
PROCESS_BEGIN();
#ifdef DO_REGISTER
	static coap_endpoint_t server_ep;
	static coap_message_t request; // This way the packet can be treated as pointer as usual
#endif

	// Init seed for stuff
	srand(time(0));

	leds_set(LEDS_NUM_TO_MASK(LEDS_RED));
	PROCESS_PAUSE();

	LOG_INFO("Starting Air Quality CoAP-Server\n");
	coap_activate_resource(&res_ventilation_system, "air_quality/ventilation"); 
	coap_activate_resource(&res_co2_sensor, "air_quality/co2");

	// try to connect to the border router
	etimer_set(&connectivity_timer, CLOCK_SECOND * INTERVAL_BETWEEN_CONNECTION_TESTS);
	PROCESS_WAIT_UNTIL(etimer_expired(&connectivity_timer));
	while(!is_connected()) {
		etimer_reset(&connectivity_timer);
		PROCESS_WAIT_UNTIL(etimer_expired(&connectivity_timer));
	}

#ifdef DO_REGISTER
	while(!registered) {
		LOG_INFO("Sending registration message\n");
		coap_endpoint_parse(SERVER_EP, strlen(SERVER_EP), &server_ep);
		
		// Prepare the message
		coap_init_message(&request, COAP_TYPE_CON, COAP_POST, 0);
		coap_set_header_uri_path(&request, service_url);
		coap_set_payload(&request, (uint8_t *)SENSOR_TYPE, sizeof(SENSOR_TYPE) - 1);

		// Prob. invio pacchetto (?)
		COAP_BLOCKING_REQUEST(&server_ep, &request, client_chunk_handler);

		PROCESS_WAIT_UNTIL(etimer_expired(&wait_registration));
	}
#endif

	etimer_set(&simulation_timer, CLOCK_SECOND * INTERVAL_BETWEEN_SIMULATIONS); // every three seconds
	while(1) {
		PROCESS_WAIT_EVENT();
		if(ev == PROCESS_EVENT_TIMER && data == &simulation_timer) {
			res_co2_sensor.trigger();	
			etimer_set(&simulation_timer, CLOCK_SECOND * INTERVAL_BETWEEN_SIMULATIONS);
		}
	}

	PROCESS_END();
}
