#include <stdlib.h>
#include <string.h>
#include "contiki.h"
#include "coap-engine.h"
#include "dev/leds.h"
#include "../controls.h"

/* Log configuration */
#include "sys/log.h"
#define LOG_MODULE "dehumidifier"
#define LOG_LEVEL LOG_LEVEL_DBG

static int dehumidifier_status = DEHUMIDIFIER_OFF;

static void dehumidifier_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

RESOURCE(res_de,
         "title=\"Dehumidifier's controls\";rt=\"Control\"",
         NULL,
         NULL,
         dehumidifier_put_handler,
         NULL);

static void dehumidifier_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset)
{
    size_t len = 0;
	const char *text = NULL;
	//char mode[4] = {0};
	//memset(mode, 0, 3);

	//int mode_success = 1;

	//len = coap_get_post_variable(request, "mode", &text);
	len = coap_get_payload(request, (const uint8_t**)&text);

	if(len <= 0 || len >= 4)
		goto exit_fail;
    
    if (strcmp(text, "INC") == 0){
		LOG_INFO("Switch ON humidifier\n");
		dehumidifier_status = DEHUMIDIFIER_INC;	
        leds_set(LEDS_BLUE);
	}else if (strcmp(text, "DEC") == 0){
		LOG_INFO("Switch ON dehumidifier\n");	
		dehumidifier_status = DEHUMIDIFIER_DEC;
        leds_set(LEDS_GREEN);
	}else if (strcmp(text, "OFF") == 0){
		LOG_INFO("Turn OFF humidity regulator\n");
		dehumidifier_status = DEHUMIDIFIER_OFF;
        leds_set(LEDS_RED);
	} else
        goto exit_fail;

    return;
    
exit_fail:
    coap_set_status_code(response, BAD_REQUEST_4_00);
}
