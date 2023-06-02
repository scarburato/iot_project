#include <stdlib.h>
#include <string.h>
#include "contiki.h"
#include "coap-engine.h"
#include "dev/leds.h"

/* Log configuration */
#include "sys/log.h"
#define LOG_MODULE "light-color"
#define LOG_LEVEL LOG_LEVEL_APP

extern bool light_on;

static void light_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

RESOURCE(res_light_color,
         "title=\"Light Intensity\";rt=\"Control\"",
         NULL,
         NULL,
         light_put_handler,
         NULL);

uint8_t led = LEDS_GREEN;

static void light_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
	size_t len = 0;
	const char *text = NULL;
	
	len = coap_get_post_variable(request, "color", &text);
	if(len <= 0 || len >= 7)
        goto error;
    
    if(strncmp(text, "YELLOW", len) == 0 || strncmp(text, "ORANGE", len) == 0) {
        led = LEDS_BLUE;
    } else if(strncmp(text, "GREEN", len) == 0) {
        led = LEDS_GREEN;
    } else if(strncmp(text, "RED", len) == 0) {
            led = LEDS_RED;
    } else
        goto error;
    
    LOG_INFO("Color = %s\n", text);
    if(light_on) {
        leds_set(led);
    }

    return;

error:
    coap_set_status_code(response, BAD_REQUEST_4_00);
}