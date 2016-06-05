#include "contiki.h"
#include <stdio.h>
#include "dev/leds.h"
#include "sensortag/board-peripherals.h"
#include "sensortag/cc2650/board.h"
#include "lib/cc26xxware/driverlib/gpio.h"
#include "ti-lib.h"
#include "driverlib/aux_adc.h"
#include "driverlib/aux_wuc.h"

//#include /* For printf() */

//#define CLOCK_SECOND 128

char buf[32];
static struct etimer et;

/*---------------------------------------------------------------------------*/
PROCESS(hello_world_process, "Hello world process");
AUTOSTART_PROCESSES(&hello_world_process);
/*---------------------------------------------------------------------------*/
PROCESS_THREAD(hello_world_process, ev, data)
{
  PROCESS_BEGIN();
    //unsigned short i;
    uint16_t singleSample;

    etimer_set(&et, CLOCK_SECOND);
    printf("Contiki CC26xx ADC demo using DIO23 as ADC input\r\n");
    while(1)
    {
        if(ev == PROCESS_EVENT_TIMER && etimer_expired(&et)) {
        //intialisation of ADC
        ti_lib_aon_wuc_aux_wakeup_event(AONWUC_AUX_WAKEUP);
        while(!(ti_lib_aon_wuc_power_status_get() & AONWUC_AUX_POWER_ON))
        { }

        // Enable clock for ADC digital and analog interface (not currently enabled in driver)
        // Enable clocks
        ti_lib_aux_wuc_clock_enable(AUX_WUC_ADI_CLOCK | AUX_WUC_ANAIF_CLOCK | AUX_WUC_SMPH_CLOCK);
        while(ti_lib_aux_wuc_clock_status(AUX_WUC_ADI_CLOCK | AUX_WUC_ANAIF_CLOCK | AUX_WUC_SMPH_CLOCK) != AUX_WUC_CLOCK_READY)
        { }

        printf("clock selected\r\n");
       
        // Connect AUX IO7 (DIO23, but also DP2 on XDS110) as analog input.
        AUXADCSelectInput(ADC_COMPB_IN_AUXIO6); 
        printf("input selected\r\n");
       
        // Set up ADC range
        // AUXADC_REF_FIXED = nominally 4.3 V
        AUXADCEnableSync(AUXADC_REF_FIXED,  AUXADC_SAMPLE_TIME_2P7_US, AUXADC_TRIGGER_MANUAL);
        printf("init adc --- OK\r\n");

        //Trigger ADC converting
        AUXADCGenManualTrigger();
        printf("trigger --- OK\r\n");
       
        //reading adc value
        singleSample = AUXADCReadFifo();

        printf("%d mv on ADC\r\n",singleSample);
       
        //shut the adc down
        AUXADCDisable();
        printf("disable --- OK\r\n");

        }
        etimer_set(&et, CLOCK_SECOND);
        PROCESS_YIELD();
    }

  PROCESS_END();
}
