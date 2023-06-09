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
#include "log.h"
#include "coap-engine.h"
#include "coap-blocking-api.h"
#include <string.h>
#include <strings.h>

#include <sys/node-id.h>
#include <time.h>

#define LOG_MODULE "airquality"
#ifdef MQTT_CLIENT_CONF_LOG_LEVEL
#define LOG_LEVEL MQTT_CLIENT_CONF_LOG_LEVEL
#else
#define LOG_LEVEL LOG_LEVEL_DBG
#endif

#define SERVER_EP "coap://[fd00::1]:5683"

/* MQTT broker address. */
#define MQTT_CLIENT_BROKER_IP_ADDR "fd00::1"
// #define MQTT_CLIENT_BROKER_IP_ADDR "fd00::f6ce:36b3:3f0b:956"

static const char *broker_ip = MQTT_CLIENT_BROKER_IP_ADDR;

// Defaukt config values
#define DEFAULT_BROKER_PORT 1883
#define DEFAULT_PUBLISH_INTERVAL (30 * CLOCK_SECOND)
#define PUBLISH_INTERVAL (5 * CLOCK_SECOND)

// We assume that the broker does not require authentication

/* Various states */
static uint8_t state;

#define STATE_INIT 0         // initial state
#define STATE_NET_OK 1       // Network is initialized
#define STATE_CONNECTING 2   // Connecting to MQTT broker
#define STATE_CONNECTED 3    // Connection successful
#define STATE_SUBSCRIBED 4   // Topics subscription done
#define STATE_DISCONNECTED 5 // Disconnected from MQTT broker

PROCESS_NAME(co2_process);
AUTOSTART_PROCESSES(&co2_process);

/* Maximum TCP segment size for outgoing segments of our socket */
#define MAX_TCP_SEGMENT_SIZE 32
#define CONFIG_IP_ADDR_STR_LEN 64

/*
 * Buffers for Client ID and Topics.
 */
#define BUFFER_SIZE 64

static char client_id[BUFFER_SIZE];
static char pub_topic[BUFFER_SIZE];
//static char sub_topic[BUFFER_SIZE];

// Periodic timer to check the state of the MQTT client
#define STATE_MACHINE_PERIODIC (CLOCK_SECOND >> 1)
static struct etimer periodic_timer;

/*
 * The main MQTT buffers.
 * We will need to increase if we start publishing more data.
 */
#define APP_BUFFER_SIZE 512
static char app_buffer[APP_BUFFER_SIZE];

static struct mqtt_message *msg_ptr = 0;

static struct mqtt_connection conn;

// CoaP stuff
extern coap_resource_t res_ventilation_system;

#define DO_REGISTER 1

#ifdef DO_REGISTER
#define CONNECTION_TRY_INTERVAL 1
#define REGISTRATION_TRY_INTERVAL 1
#define SIMULATION_INTERVAL 1
#define SENSOR_TYPE "{\"deviceType\": \"fan\", \"sensorId\": %u}"

static struct etimer wait_registration;

char *service_url = "/registration";

static bool registered = false;

void client_chunk_handler(coap_message_t *response)
{
    const uint8_t *chunk;
    if (response == NULL)
    {
        LOG_INFO("Request timed out\n");
        etimer_set(&wait_registration, CLOCK_SECOND * REGISTRATION_TRY_INTERVAL);
        return;
    }

    int len = coap_get_payload(response, &chunk);

    if (strncmp((char *)chunk, "Success", len) == 0)
        registered = true;
    else
        etimer_set(&wait_registration, CLOCK_SECOND * REGISTRATION_TRY_INTERVAL);
}
#endif

PROCESS(co2_process, "CO2 analyzer process");

static unsigned int co2_level = 300;
bool ventilation_on = false;

static bool update_co2()
{ // simulate the behavior of the real sensor
    unsigned int old_co2_level = co2_level;
    int value = 0;

    if (ventilation_on)
    {                           // If the ventilation system is turned on, air quality improves
        value = rand() % 7 + 6; // a random number in [6;12]
        co2_level = (int)(co2_level - 1.8 * value);
    }
    else
    {
        value = rand() % 16;                         // a random number between 0 and 15
        co2_level = (int)(co2_level + 0.6 * value); // In any case, the CO2 level can only increase more or less rapidly
    }

    if (co2_level < 350)
        leds_set(LEDS_GREEN);
    else if (co2_level >= 350 && co2_level < 500)
        leds_set(LEDS_BLUE);
    else
        leds_set(LEDS_RED);

    return old_co2_level != co2_level;
}

// This function is called each time occurs a MQTT event
static void mqtt_event(struct mqtt_connection *m, mqtt_event_t event, void *data)
{
    switch (event)
    {
    case MQTT_EVENT_CONNECTED:
        LOG_INFO("MQTT connection acquired\n");
        state = STATE_CONNECTED;
        break;
    case MQTT_EVENT_DISCONNECTED:
        printf("MQTT connection disconnected. Reason: %u\n", *((mqtt_event_t *)data));
        state = STATE_NET_OK;
        //process_poll(&co2_process);
        break;
    case MQTT_EVENT_PUBLISH:
        msg_ptr = data;
        LOG_INFO(
            "Message received: topic='%s' (len=%u), chunk_len=%u\n",
            msg_ptr->topic, (unsigned int)strlen(msg_ptr->topic), msg_ptr->payload_length);
        break;
    case MQTT_EVENT_SUBACK:
#if MQTT_311
        mqtt_suback_event_t *suback_event = (mqtt_suback_event_t *)data;
        if (suback_event->success)
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
    // return true;
    return !(uip_ds6_get_global(ADDR_PREFERRED) == NULL || uip_ds6_defrt_choose() == NULL);
}

PROCESS_THREAD(co2_process, ev, data)
{
    PROCESS_BEGIN();

    //static mqtt_status_t status;
    static char broker_address[CONFIG_IP_ADDR_STR_LEN] = {0};
    static button_hal_button_t *btn;
    static coap_endpoint_t server_ep;
    static coap_message_t request;
    static char registrationString[100] = {0};
    static int registrationStringSize = 0;

    LOG_INFO("Avvio...");

    btn = button_hal_get_by_index(0);
    if (btn == NULL)
    {
        LOG_ERR("Unable to find button 0... exit");
        goto exit;
    }

    // Initialize the ClientID as MAC address
    snprintf(client_id, BUFFER_SIZE, "%02x%02x%02x%02x%02x%02x",
             linkaddr_node_addr.u8[0], linkaddr_node_addr.u8[1],
             linkaddr_node_addr.u8[2], linkaddr_node_addr.u8[5],
             linkaddr_node_addr.u8[6], linkaddr_node_addr.u8[7]);

    // Broker registration
    mqtt_register(&conn, &co2_process, client_id, mqtt_event, MAX_TCP_SEGMENT_SIZE);

    state = STATE_INIT;

    // Init seed for stuff
    srand(time(0));

    PROCESS_PAUSE();

    // Initialize periodic timer to check the status
    etimer_set(&periodic_timer, PUBLISH_INTERVAL);

    // AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
#ifdef DO_REGISTER
    goto balzo;
registra:
    while (!registered)
    {
        LOG_INFO("Sending CoAP registration message\n");
        coap_endpoint_parse(SERVER_EP, strlen(SERVER_EP), &server_ep);

        // Prepare the message
        coap_init_message(&request, COAP_TYPE_CON, COAP_POST, 0);
        coap_set_header_uri_path(&request, service_url);
        memset(registrationString, 0x00, 100);
        registrationStringSize = snprintf(registrationString, 100, SENSOR_TYPE, node_id);
        coap_set_payload(&request, (uint8_t *)registrationString, registrationStringSize);

        // Prob. invio pacchetto (?)
        COAP_BLOCKING_REQUEST(&server_ep, &request, client_chunk_handler);

        PROCESS_WAIT_UNTIL(etimer_expired(&wait_registration));
    }

    LOG_INFO("CoAP DONE!!!\n");
    goto fine_registra;

balzo:
#endif
    // AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA

    while (true)
    {
        PROCESS_YIELD();

        if (ev == button_hal_press_event)
        {
            co2_level += 300;
            LOG_INFO("Manually increased co2 to %dppm\n", co2_level);
            update_co2();
            continue;
        }

        update_co2();

        if ((ev == PROCESS_EVENT_TIMER && data == &periodic_timer) || ev == PROCESS_EVENT_POLL)
        {
            switch (state)
            {
            case STATE_INIT:
                LOG_INFO("state init..\n");
                if (!have_connectivity())
                    break;

                state = STATE_NET_OK;
            // no break
            case STATE_NET_OK:
                LOG_INFO("Connecting to MQTT server\n");
                memcpy(broker_address, broker_ip, strlen(broker_ip));

                mqtt_connect(&conn, broker_address, DEFAULT_BROKER_PORT,
                             (DEFAULT_PUBLISH_INTERVAL * 3) / CLOCK_SECOND,
                             MQTT_CLEAN_SESSION_ON);

                LOG_INFO("Starting Air Quality CoAP-Server\n");
                coap_activate_resource(&res_ventilation_system, "air_quality/ventilation");

#ifdef DO_REGISTER
                goto registra;
fine_registra:
#endif

                //state = STATE_CONNECTING;
                state = STATE_SUBSCRIBED;
                break;
            case STATE_CONNECTED:
            /*    // Subscribe to a topic
                strcpy(sub_topic, "co2");
                status = mqtt_subscribe(&conn, NULL, sub_topic, MQTT_QOS_LEVEL_0);
                if (status == MQTT_STATUS_OUT_QUEUE_FULL)
                {
                    LOG_ERR("Tried to subscribe but command queue was full!\n");
                    //PROCESS_EXIT();
                }
                state = STATE_SUBSCRIBED;*/
            // no break
            case STATE_SUBSCRIBED:
                sprintf(pub_topic, "%s", "co2");

                // simulate the behavior of the sensor
                // @TODO

                LOG_INFO("New value of co2: %dppm\n", co2_level);

                sprintf(app_buffer, "{\"node\": %d, \"co2\": %d}", node_id, co2_level);
                mqtt_publish(&conn, NULL, pub_topic, (uint8_t *)app_buffer,
                             strlen(app_buffer), MQTT_QOS_LEVEL_0, MQTT_RETAIN_OFF);

                break;

            case STATE_DISCONNECTED:
                LOG_ERR("Disconnected from MQTT broker\n");
                state = STATE_NET_OK;
                break;
            }
            etimer_set(&periodic_timer, PUBLISH_INTERVAL);
        }
    }

exit:
    update_co2(); // fake call to make GCC happy
    PROCESS_END();
}
