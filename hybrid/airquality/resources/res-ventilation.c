#include <stdlib.h>
#include <string.h>
#include "contiki.h"
#include "coap-engine.h"
#include "dev/leds.h"

/* Log configuration */
#include "sys/log.h"
#define LOG_MODULE "ventilation-system"
#define LOG_LEVEL LOG_LEVEL_DBG

static void co2_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

RESOURCE(res_ventilation_system,
         "title=\"Ventilation System\";rt=\"Control\"",
         NULL,
         NULL,
         co2_put_handler,
         NULL);

extern bool ventilation_on;

static void co2_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
	size_t len = 0;
	const char *text = NULL;
	char mode[4];
	memset(mode, 0, 3);

	// @FIXME
	//len = coap_get_post_variable(request, "mode", &text);
	len = coap_get_payload(request, (const uint8_t**)(&text));

	if(len <= 0 || len >= 4)
		goto exit_fail;

	memcpy(mode, text, len);
	if(strncmp(mode, "ON", len) == 0) {
		ventilation_on = true;
		//leds_set(LEDS_NUM_TO_MASK(LEDS_GREEN));
		LOG_INFO("Ventilation System ON\n");
	} else if(strncmp(mode, "OFF", len) == 0) {
		ventilation_on = false;
		//leds_set(LEDS_NUM_TO_MASK(LEDS_RED));
		LOG_INFO("Ventilation System OFF\n");
	} else 
		goto exit_fail;

	return;
	
exit_fail:
    coap_set_status_code(response, BAD_REQUEST_4_00);
}
