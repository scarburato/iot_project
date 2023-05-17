#include <stdlib.h>
#include <string.h>
#include "contiki.h"
#include "coap-engine.h"
#include "dev/leds.h"

/* Log configuration */
#include "sys/log.h"
#define LOG_MODULE "float-level"
#define LOG_LEVEL LOG_LEVEL_APP

extern bool float_low;

void level_get_handler(
	coap_message_t *request,
	coap_message_t *response,
	uint8_t *buffer,
	uint16_t preferred_size,
	int32_t *offset)
{
	static const int length = 64;
	// @TODO
	snprintf(
		(char*)buffer, length, "{\"node\": %d, \"float\": \"%s\"}",
		/*(unsigned int) node_id*/ 3, float_low ? "low" : "high"
	);

	size_t len = strlen((char*)buffer);

	// invio pacchetto
	coap_set_header_content_format(response, TEXT_PLAIN);
	coap_set_header_etag(response, (uint8_t *)&len, 1);
	coap_set_payload(response, buffer, len);
}

RESOURCE(res_float_level,
         "title=\"Float Level\";rt=\"Control\"",
         level_get_handler,
         NULL,
         NULL,
         NULL);
