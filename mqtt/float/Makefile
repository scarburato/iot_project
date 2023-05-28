CONTIKI_PROJECT = float
all: $(CONTIKI_PROJECT)

CONTIKI=/home/user/contiki-ng

include $(CONTIKI)/Makefile.dir-variables
MODULES += $(CONTIKI_NG_APP_LAYER_DIR)/mqtt

-include $(CONTIKI)/Makefile.identify-target

MODULES_REL += arch/platform/$(TARGET)

include $(CONTIKI)/Makefile.include
