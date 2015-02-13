Monitoring GUI Prototype
====================

Requirements
-------------

You need to install Expedient (v0.8.3) before MGUI installation.
This README assumes Expedient is installed in the `/opt/ofelia` directory.

Installation
----------

1. Install libraries
   * $ sudo easy_install pip
   * $ sudo pip install xmltodict
   * $ sudo pip install pytz

2. Install module (*1)
   * $ cp -r /opt/felix/ms-gui/modules/ui/gui/src/expedient/src /opt/ofelia/expedient/src/
   (e.g. GIT repository for MGUI is `/opt/felix/ms-gui/` here)

(*1) MGUI is installed as Expedient plugin (UI_PLUGINS) by this procedure.
     (please see also note section at the bottom.)

Configuration
-----------

Configure `setting.conf` file in the `/opt/felix/ms-gui/modules/ui/gui/src//expedient/src/python/plugins/m_gui/` directory as below:

```
#
# Other plugin, to be enabled in Expedient
#
# This is a N-set of 3-tuples:
#
#    1. The first element is the absolute path to the Other class.
#       
#    2. The second element is the prefix that is prepended to all urls for
#       accessing the plugin. This should be unique across all plugins and
#       applications.
#      
#    3. The third element is the absolute path to the module that should be
#       included in URLConf and that contains all the plugin's URLs.
#
UI_PLUGINS = ('m_gui.models.MGUI', 'm_gui', 'm_gui.urls')

#
# List of the APPs within the plugin to be activated in Django
#
INSTALLED_APPS = ['m_gui']

[mgui]
#
# MS_REST_URI = URI
#   URI      : URL of MS REST API
#
MS_REST_URI = 'http://127.0.0.1:8080/monitoring-system/'

#
# MONITORING_TIMEZONE = timezone, display, offset
#   timezone : Time zone of value
#   display  : Time zone display value
#   offset   : Time difference from GMT
# ex.
#   MONITORING_TIMEZONE = (('WET', 'GMT+0000 (WET)', 0),)
#
MONITORING_TIMEZONE = (('WET', 'GMT+0000 (WET)', 0),
                       ('CET', 'GMT+0100 (CET)', 1),
                       ('EET', 'GMT+0200 (EET)', 2),
                       ('Asia/Tokyo', 'GMT+0900 (JST)', 9))

#
# MONITORING_XXX_METRIC = metric, display, scale, definision, MS's item
#   XXX        : SDN or CP
#   metric     : metric of value
#   display    : metric display value
#   scale      : scale of metric
#                1:linear, 2:ordinal
#   definision : definition of metric
#                case scale equal 1
#                  the value of the metric and the display
#                  (metric, display)
#                case scale equal 2
#                  Number of decimal places
#   MS's item  : item name of MS corresponding to the metric
#                (item, )
# ex.
#   MONITORING_SDN_METRIC = (('Status', 'Port Status', 2, (('1', 'UP'), ('2', 'DOWN')), ('status', )),
#                            ('Traffic', 'Traffic', 1, (0, ), ('in_bps', 'out_bps')))
#
MONITORING_SDN_METRIC = (('Status', 'Port Status', 2, (('1', 'UP'), ('2', 'DOWN')), ('status', )),
                         ('Traffic', 'Traffic', 1, (0, ), ('in_bps', 'out_bps')))

MONITORING_CP_METRIC = (('Status', 'Status', 2, (('1', 'UP'), ('2', 'DOWN')), ('status', )), 
                        ('CPU Load', 'CPU Load', 1, (2, ), ('cpu_load', )))

#
# MONITORING_SHOW = number
#   number   : display number
# ex.
#   MONITORING_SHOW = (10, 100, 1000)
#
MONITORING_SHOW = (10, 100, 1000)
```


Operation
-------

MGUI automatically and concurrently starts or stops when its Expedient starts or stops, respectively. 

You can find log file like:
`$ tail -f /var/log/apache2/expedient/clearinghouse/error_log`

To access the monitoring GUI, click `Monitoring` link at top_menu_options in the Expedient.


Note
----------

MGUI code is tested only with Expedient v0.8.3 on Debian 7.0. 
It is not tested with the later version Expedient (if any).

Currently KDDI/NAL develops and operates MGUI as Expedient plugin.
(please see also (*1) above)

Status of this Monitoring GUI code:

  SDN Monitoring
  ... fully supported (development completed)

  CP Monitoring
  ... supported (only unit test. some bugs might be found...)

  SE/TN Monitoring:
  ... not supported (now under design and coding)

  * You may find some test and temporal codes for SE/TN topology view.
    Please ignore them.

