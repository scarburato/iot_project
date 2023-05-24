#include "contiki.h"
#include "net/routing/routing.h"
#include "mqtt.h"
#include "net/ipv6/uip.h"
#include "net/ipv6/uip-icmp6.h"
#include "net/ipv6/sicslowpan.h"
#include "sys/etimer.h"
#include "sys/ctimer.h"
#include "lib/sensors.h"
#include "dev/button-hal.h"
#include "dev/leds.h"
#include "os/sys/log.h"

#include <string.h>
#include <strings.h>

#include <sys/node-id.h>
#include <time.h>

#define LOG_MODULE "temperature"
#ifdef  MQTT_CLIENT_CONF_LOG_LEVEL
#define LOG_LEVEL MQTT_CLIENT_CONF_LOG_LEVEL
#else
#define LOG_LEVEL LOG_LEVEL_DBG
#endif

/* MQTT broker address. */
#define MQTT_CLIENT_BROKER_IP_ADDR "fd00::1"

static const char *broker_ip = MQTT_CLIENT_BROKER_IP_ADDR;

// Default config values
#define DEFAULT_BROKER_PORT         1883
#define DEFAULT_PUBLISH_INTERVAL    (30 * CLOCK_SECOND)
#define PUBLISH_INTERVAL	    (5 * CLOCK_SECOND)

// We assume that the broker does not require authentication

/* Various states */
static uint8_t state;

#define STATE_INIT    		0	// initial state
#define STATE_NET_OK    	1	// Network is initialized
#define STATE_CONNECTING      	2	// Connecting to MQTT broker
#define STATE_CONNECTED       	3	// Connection successful
#define STATE_SUBSCRIBED      	4	// Topics subscription done
#define STATE_DISCONNECTED    	5	// Disconnected from MQTT broker

PROCESS_NAME(process_for_temperature_sensor);
AUTOSTART_PROCESSES(&process_for_temperature_sensor);

/* Maximum TCP segment size for outgoing segments of our socket */
#define MAX_TCP_SEGMENT_SIZE    32
#define CONFIG_IP_ADDR_STR_LEN  64

/*
 * Buffers for Client ID and Topics.
 */
#define BUFFER_SIZE 64

static char client_id[BUFFER_SIZE];
static char pub_topic[BUFFER_SIZE];
static char sub_topic[BUFFER_SIZE];

// Periodic timer to check the state of the MQTT client
#define STATE_MACHINE_PERIODIC     (CLOCK_SECOND >> 1)
static struct etimer periodic_timer;

/*
 * The main MQTT buffers.
 * We will need to increase if we start publishing more data.
 */
#define APP_BUFFER_SIZE 512
static char app_buffer[APP_BUFFER_SIZE];

static struct mqtt_message *msg_ptr = 0;

static struct mqtt_connection conn;

PROCESS(process_for_temperature_sensor, "Temperature sensor process");

static bool increase_temperature = false;
static bool decrease_temperature = false;
static int temperature = 50; // !we cannot use float value in the testbed!
static int variation = 0;

// Function called for handling an incoming message
static void pub_handler(const char *topic, uint16_t topic_len, const uint8_t *chunk, uint16_t chunk_len) {
	LOG_INFO("Message received: topic='%s' (len=%u), chunk_len=%u\n", topic, topic_len, chunk_len);

	if(strcmp(topic, "AC") != 0) {
		LOG_ERR("Topic not valid!\n");
		return;
	}
	
	LOG_INFO("Received Actuator command\n");
	if(strcmp((const char*) chunk, "INC") == 0) {
		LOG_INFO("Turn ON the heating\n");
		increase_temperature = true;
		decrease_temperature = false;	
	} else if(strcmp((const char*) chunk, "DEC") == 0) {
		LOG_INFO("Turn ON the cooler\n");	
		increase_temperature = false;
		decrease_temperature = true;
	} else if(strcmp((const char*) chunk, "OFF") == 0)  {
		LOG_INFO("Turn OFF the AC\n");	
		increase_temperature = false;
		decrease_temperature = false;
	}	
}

// This function is called each time occurs a MQTT event
static void mqtt_event(struct mqtt_connection *m, mqtt_event_t event, void *data) {
	switch(event) {
		case MQTT_EVENT_CONNECTED: 
			LOG_INFO("MQTT connection acquired\n");
			state = STATE_CONNECTED;
			break;
		case MQTT_EVENT_DISCONNECTED: 
			printf("MQTT connection disconnected. Reason: %u\n", *((mqtt_event_t *)data));
			state = STATE_DISCONNECTED;
			process_poll(&process_for_temperature_sensor);
			break;
		case MQTT_EVENT_PUBLISH: 
			msg_ptr = data;
			pub_handler(msg_ptr->topic, strlen(msg_ptr->topic), msg_ptr->payload_chunk, msg_ptr->payload_length);
			break;
		case MQTT_EVENT_SUBACK: 
			#if MQTT_311
			mqtt_suback_event_t *suback_event = (mqtt_suback_event_t *)data;
			if(suback_event->success) 
				LOG_INFO("Application has subscribed to the topic\n");
			else
				LOG_ERR("Application failed to subscribe to topic (ret code %x)\n", suback_event->return_code);
			#else
			LOG_INFO("Application has subscribed to the topic\n");
			#endif
			break;
		case MQTT_EVENT_UNSUBACK: 
			LOG_INFO("Application is unsubscribed to topic successfully\n");
			break;
		case MQTT_EVENT_PUBACK: 
			LOG_INFO("Publishing complete.\n");
			break;
		default:
			LOG_INFO("Application got a unhandled MQTT event: %i\n", event);
			break;
	}
}

static bool have_connectivity(void) {
	return !(uip_ds6_get_global(ADDR_PREFERRED) == NULL || uip_ds6_defrt_choose() == NULL);
}

PROCESS_THREAD(process_for_temperature_sensor, ev, data) {

	PROCESS_BEGIN();

	static mqtt_status_t status;
	static char broker_address[CONFIG_IP_ADDR_STR_LEN];
	
	LOG_INFO("Avvio...");
	
	// Initialize the ClientID as MAC address
	snprintf(client_id, BUFFER_SIZE, "%02x%02x%02x%02x%02x%02x",
		     linkaddr_node_addr.u8[0], linkaddr_node_addr.u8[1],
		     linkaddr_node_addr.u8[2], linkaddr_node_addr.u8[5],
		     linkaddr_node_addr.u8[6], linkaddr_node_addr.u8[7]);

	// Broker registration					 
	mqtt_register(&conn, &process_for_temperature_sensor, client_id, mqtt_event, MAX_TCP_SEGMENT_SIZE);
			
	state=STATE_INIT;
				    
	// Initialize periodic timer to check the status 
	etimer_set(&periodic_timer, PUBLISH_INTERVAL);

	while(true) {
		PROCESS_YIELD();

		if(!((ev == PROCESS_EVENT_TIMER && data == &periodic_timer) || ev == PROCESS_EVENT_POLL))
			continue;
		
		switch(state) {		  			  
		case STATE_INIT:
			LOG_INFO("Init....");
			if(have_connectivity())
				state = STATE_NET_OK;
			else
				break;
		// no break
		case STATE_NET_OK:
			LOG_INFO("Connecting to MQTT server\n"); 
		  	memcpy(broker_address, broker_ip, strlen(broker_ip));
		  
		  	mqtt_connect(&conn, broker_address, DEFAULT_BROKER_PORT,
					   (DEFAULT_PUBLISH_INTERVAL * 3) / CLOCK_SECOND,
					   MQTT_CLEAN_SESSION_ON);
		  	state = STATE_CONNECTING;
		break;
		case STATE_CONNECTED:
			// Subscribe to a topic
			strcpy(sub_topic,"AC");
			status = mqtt_subscribe(&conn, NULL, sub_topic, MQTT_QOS_LEVEL_0);
			if(status == MQTT_STATUS_OUT_QUEUE_FULL) {
				LOG_ERR("Tried to subscribe but command queue was full!\n");
				PROCESS_EXIT();
			}
			state = STATE_SUBSCRIBED;
		// no break
		case STATE_SUBSCRIBED:	
			sprintf(pub_topic, "%s", "temperature");
			
			// simulate the behavior of the sensor				
			if (increase_temperature || decrease_temperature) {
				variation = rand()%3; 	// a value in [0,2]
				temperature = (increase_temperature) ? (temperature + variation) : (temperature - variation);
			} else {
				if((rand()%10) < 6) { // 60% chance that the temperature will change
					variation = (rand()%5)-2; // a value in [-2, 2]
					temperature = temperature + variation;
				}
			}

			LOG_INFO("New value of temperature: %d\n", temperature);
			
			sprintf(app_buffer, "{\"node\": %d, \"temperature\": %d}", node_id, temperature);
			mqtt_publish(&conn, NULL, pub_topic, (uint8_t *)app_buffer,
			strlen(app_buffer), MQTT_QOS_LEVEL_0, MQTT_RETAIN_OFF);

		break; 
		case STATE_DISCONNECTED:
			LOG_ERR("Disconnected from MQTT broker\n");	
			state = STATE_INIT;
		break;
		}
		etimer_set(&periodic_timer, PUBLISH_INTERVAL);
	}
	PROCESS_END();
}
