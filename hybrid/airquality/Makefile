CONTIKI_PROJECT = airquality
all: $(CONTIKI_PROJECT)

CONTIKI=/home/user/contiki-ng

# This flag includes the IPv6 libraries
CONTIKI_WITH_IPV6 = 1

# Do not try to build on Sky because of code size limitation
PLATFORMS_EXCLUDE = sky z1

include $(CONTIKI)/Makefile.dir-variables
MODULES += $(CONTIKI_NG_APP_LAYER_DIR)/mqtt

include $(CONTIKI)/Makefile.identify-target

# Include CoAP resources
MODULES_REL+=./resources

MODULES_REL+=arch/platform/$(TARGET)

# Include the CoAP implementation
include $(CONTIKI)/Makefile.dir-variables
MODULES += $(CONTIKI_NG_APP_LAYER_DIR)/coap

include $(CONTIKI)/Makefile.include
