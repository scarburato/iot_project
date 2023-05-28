#include <stdlib.h>
#include <string.h>
#include "contiki.h"
#include "coap-engine.h"
#include "dev/leds.h"

/* Log configuration */
#include "sys/log.h"
#define LOG_MODULE "light-switch"
#define LOG_LEVEL LOG_LEVEL_DBG


static void pump_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

RESOURCE(res_light_switch,
         "title=\"Pump's motor\";rt=\"Control\"",
         NULL,
         NULL,
         pump_put_handler,
         NULL);

bool pump_on = false;

static void pump_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
	size_t len = 0;
	const char *text = NULL;
		
	len = coap_get_post_variable(request, "motor", &text);
	if(len <= 0 || len >= 4)
		goto error;
	
	if(strncmp(text, "ON", len) == 0) {
		pump_on = true;
		leds_set(LEDS_NUM_TO_MASK(LEDS_BLUE));
		LOG_INFO("PUMP ON\n");
	} else if(strncmp(text, "OFF", len) == 0) {
		pump_on = false;
		leds_set(LEDS_NUM_TO_MASK(LEDS_RED));
		LOG_INFO("PUMP OFF\n");
	}
	else
		goto error;

	return;
error:
	coap_set_status_code(response, BAD_REQUEST_4_00);
}
