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

#define LOG_MODULE "humidity"
#ifdef  MQTT_CLIENT_CONF_LOG_LEVEL
#define LOG_LEVEL MQTT_CLIENT_CONF_LOG_LEVEL
#else
#define LOG_LEVEL LOG_LEVEL_DBG
#endif

/* MQTT broker address. */
#define MQTT_CLIENT_BROKER_IP_ADDR "fd00::1"

static const char *broker_ip = MQTT_CLIENT_BROKER_IP_ADDR;

// Defaukt config values
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

PROCESS_NAME(humidity_analyzer_process);
AUTOSTART_PROCESSES(&humidity_analyzer_process);

/* Maximum TCP segment size for outgoing segments of our socket */
#define MAX_TCP_SEGMENT_SIZE    32
#define CONFIG_IP_ADDR_STR_LEN  64

/*
 * Buffers for Client ID and Topics.
 */
#define BUFFER_SIZE 64

static char client_id[BUFFER_SIZE];
static char pub_topic[BUFFER_SIZE];
//static char sub_topic[BUFFER_SIZE];

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

PROCESS(humidity_analyzer_process, "Humidity analyzer process");

static bool increase_humidity = false;
static bool decrease_humidity = false;
#define MIN_HUMIDITY 0
#define MAX_HUMIDITY 100
static int humidity_percentage = 50; // we cannot use float value in the testbed
static int variation = 0;

// Function called for handling an incoming message
static void pub_handler(const char *topic, uint16_t topic_len, const uint8_t *chunk, uint16_t chunk_len)
{
	LOG_INFO("Message received: topic='%s' (len=%u), chunk_len=%u\n", topic, topic_len, chunk_len);

	if(strcmp(topic, "humidifier") != 0){
		LOG_ERR("Topic not valid!\n");
		return;
	}
	
	LOG_INFO("Received Actuator command\n");
	if (strcmp((const char*) chunk, "INC") == 0){
		LOG_INFO("Switch ON humidifier\n");
		increase_humidity = true;
		decrease_humidity = false;	
	}else if (strcmp((const char*) chunk, "DEC") == 0){
		LOG_INFO("Switch ON dehumidifier\n");	
		increase_humidity = false;
		decrease_humidity = true;
	}else if (strcmp((const char*) chunk, "OFF") == 0){
		LOG_INFO("Turn OFF humidity regulator\n");
		increase_humidity = false;
		decrease_humidity = false;
	}
}

// This function is called each time occurs a MQTT event
static void mqtt_event(struct mqtt_connection *m, mqtt_event_t event, void *data)
{
	switch(event) 
	{
		case MQTT_EVENT_CONNECTED: 
			LOG_INFO("MQTT connection acquired\n");
			state = STATE_CONNECTED;
			break;
		case MQTT_EVENT_DISCONNECTED: 
			printf("MQTT connection disconnected. Reason: %u\n", *((mqtt_event_t *)data));
			state = STATE_DISCONNECTED;
			process_poll(&humidity_analyzer_process);
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

static bool have_connectivity(void)
{
	return !(uip_ds6_get_global(ADDR_PREFERRED) == NULL || uip_ds6_defrt_choose() == NULL);
}

PROCESS_THREAD(humidity_analyzer_process, ev, data)
{

	PROCESS_BEGIN();

	static char broker_address[CONFIG_IP_ADDR_STR_LEN];
	
	LOG_INFO("Avvio...");
	
	// Initialize the ClientID as MAC address
	snprintf(client_id, BUFFER_SIZE, "%02x%02x%02x%02x%02x%02x",
		     linkaddr_node_addr.u8[0], linkaddr_node_addr.u8[1],
		     linkaddr_node_addr.u8[2], linkaddr_node_addr.u8[5],
		     linkaddr_node_addr.u8[6], linkaddr_node_addr.u8[7]);

	// Broker registration					 
	mqtt_register(&conn, &humidity_analyzer_process, client_id, mqtt_event, MAX_TCP_SEGMENT_SIZE);
			
	state=STATE_INIT;
				    
	// Initialize periodic timer to check the status 
	etimer_set(&periodic_timer, PUBLISH_INTERVAL);

	while(true) 
	{
		PROCESS_YIELD();

		if(!((ev == PROCESS_EVENT_TIMER && data == &periodic_timer) || ev == PROCESS_EVENT_POLL))
			continue;
		
		switch(state){
			case STATE_INIT:
				LOG_INFO("state init..");
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
			case STATE_SUBSCRIBED:	
			sprintf(pub_topic, "%s", "humidity");

			if (humidity_percentage > 58+3) {
				increase_humidity = false;
				decrease_humidity = true;
			} else if (humidity_percentage < 45) {
				increase_humidity = true;
				decrease_humidity = false;
			}
			
			// simulate the behavior of the sensor				
			if (increase_humidity || decrease_humidity)
			{
				variation = (rand()%5)+1; 	// a value in [1,5]
				if((rand()%10) < 8) {
					if (increase_humidity)
						humidity_percentage = humidity_percentage + variation;
					else
						humidity_percentage = humidity_percentage - variation;
				}
			}
			else // humidity regulator OFF
			{
				// compute a probability to have a change
				if ((rand()%10) < 3) // 30% chance that the humidity will change
				{
					variation = (rand()%7)-4; // a value in
					humidity_percentage = humidity_percentage + variation;
				}
			}	

			if (humidity_percentage > MAX_HUMIDITY) // impossible behavior in a real environment
				humidity_percentage = MAX_HUMIDITY; 
			else if (humidity_percentage < MIN_HUMIDITY) // impossible behavior in a real environment
				humidity_percentage = MIN_HUMIDITY;

			LOG_INFO("New value of humidity: %d%%\n", humidity_percentage);
			
			sprintf(app_buffer, "{\"node\": %d, \"humidity\": %d}", 50, humidity_percentage);
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
