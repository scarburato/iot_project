#include <stdlib.h>
#include <string.h>
#include "contiki.h"
#include "coap-engine.h"
#include "dev/leds.h"

/* Log configuration */
#include "sys/log.h"
#define LOG_MODULE "ventilation-system"
#define LOG_LEVEL LOG_LEVEL_APP

static void co2_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

RESOURCE(res_ventilation_system,
         "title=\"Ventilation System\";rt=\"Control\"",
         NULL,
         NULL,
         co2_put_handler,
         NULL);

bool ventilation_on = false;

static void co2_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
	size_t len = 0;
	const char *text = NULL;
	char mode[4];
	memset(mode, 0, 3);

	int mode_success = 1;

	len = coap_get_post_variable(request, "mode", &text);
	if(len > 0 && len < 4) {
		memcpy(mode, text, len);
		if(strncmp(mode, "ON", len) == 0) {
			ventilation_on = true;
			leds_set(LEDS_NUM_TO_MASK(LEDS_GREEN));
			LOG_INFO("Ventilation System ON\n");
		} else if(strncmp(mode, "OFF", len) == 0) {
			ventilation_on = false;
			leds_set(LEDS_NUM_TO_MASK(LEDS_RED));
			LOG_INFO("Ventilation System OFF\n");
		} else {
			mode_success = 0;
		}
	} else {
		mode_success = 0;
	}
	
	if(!mode_success) {
    		coap_set_status_code(response, BAD_REQUEST_4_00);
 	}
}


