from soaplib.wsgi_soap import SimpleWSGISoapApp
from soaplib.service import soapmethod
from soaplib.serializers.primitive import *
from soaplib.serializers.clazz import *
#import cElementTree as et
from geniLight_types import *
import socket
import struct
from ctypes import create_string_buffer

AGGREGATE_MANAGER_PORT = 2603

# Message IDs for all the GENI light calls
# This will be used by the aggrMgr controller
SFA_START_SLICE = 101
SFA_STOP_SLICE = 102
SFA_DELETE_SLICE = 103
SFA_CREATE_SLICE = 104
SFA_LIST_SLICES = 105
SFA_LIST_COMPONENTS = 106
SFA_REGISTER = 107
SFA_REBOOT_COMPONENT = 108

def print_buffer(self, buf):
    for i in range(0,len(buf)):
        print('%x' % buf[i])

##
# The GeniServer class provides stubs for executing Geni operations at
# the Aggregate Manager.

class GeniResult(ClassSerializer):
    class types:
        code = Integer
        error_msg = String

    def __init__(self, c, e):
        self.code = c
        self.error_msg = e

class GeniLightServer(SimpleWSGISoapApp):
    def __init__(self):
        '''Connect to the Resource Manager component'''
        self.aggrMgr_sock = socket.socket ( socket.AF_INET, socket.SOCK_STREAM )
        self.aggrMgr_sock.connect ( ( 'localhost', AGGREGATE_MANAGER_PORT ) )
        self.aggrMgr_sock.settimeout(.1)
        print 'connected to resource manager'

    # implicitly no __init__()
    @soapmethod(UserSliceInfo,_returns=GeniResult)
    def start_slice(self, slice):
        slice_id = slice.save_to_string()
        buf = create_string_buffer(2 + 1 + len(slice_id)) 
        format = 'hB%ds' % len(slice_id)
        struct.pack_into(format, buf, 0, socket.htons(len(buf)),
                SFA_START_SLICE, slice_id)
        self.aggrMgr_sock.send(buf)

        return GeniResult(1, 'Success')

    @soapmethod(UserSliceInfo,_returns=GeniResult)
    def stop_slice(self, slice):
        slice_id = slice.save_to_string()
        buf = create_string_buffer(2 + 1 + len(slice_id)) 
        format = 'hB%ds' % len(slice_id)
        struct.pack_into(format, buf, 0, socket.htons(len(buf)),
                SFA_STOP_SLICE, slice_id)
        self.aggrMgr_sock.send(buf)

        return GeniResult(1, 'Success')

    @soapmethod(UserSliceInfo,String,_returns=GeniResult)
    def create_slice(self, slice, rspec_str):
        slice_id = slice.save_to_string()
        buf = create_string_buffer(2 + 1 + len(slice_id) + len(rspec_str)) 
        format = 'hB%ds%ds' % (len(slice_id), len(rspec_str))
        struct.pack_into(format, buf, 0, socket.htons(len(buf)),
                SFA_CREATE_SLICE, slice_id, rspec_str)
        self.aggrMgr_sock.send(buf)

        return GeniResult(1, 'Success')

    @soapmethod(UserSliceInfo,_returns=GeniResult)
    def delete_slice(self, slice):
        slice_id = slice.save_to_string()
        buf = create_string_buffer(2 + 1 + len(slice_id)) 
        format = 'hB%ds' % len(slice_id)
        struct.pack_into(format, buf, 0, socket.htons(len(buf)),
                SFA_DELETE_SLICE, slice_id)
        self.aggrMgr_sock.send(buf)

        return GeniResult(1, 'Success')

    @soapmethod(_returns=String)
    def list_slices(self):
        buf = create_string_buffer(2 + 1) 
        struct.pack_into('hB', buf, 0, socket.htons(len(buf)),
                SFA_LIST_SLICES)
        self.aggrMgr_sock.send(buf)

        return 'list of slices'

    @soapmethod(_returns=String)
    def list_components(self):
        buf = create_string_buffer(2 + 1) 
        struct.pack_into('hB', buf, 0, socket.htons(len(buf)), SFA_LIST_COMPONENTS)
        self.aggrMgr_sock.send(buf)

        return 'list of components'

    @soapmethod(GeniRecordEntry,_returns=GeniResult)
    def register(self, record):
        # Unclear what this will be used for 
        return GeniResult(1, 'Success')

    @soapmethod(String,_returns=GeniResult)
    def reboot_component(self,name):
        buf = create_string_buffer(2 + 1 + len(name)) 
        struct.pack_into(format, buf, 0, socket.htons(len(buf)),
                SFA_REBOOT_COMPONENT, name)
        self.aggrMgr_sock.send(buf)

        return GeniResult(1, 'Success')

if __name__=='__main__':
    try:from cherrypy.wsgiserver import CherryPyWSGIServer
    except:from cherrypy._cpwsgiserver import CherryPyWSGIServer
    gls = GeniLightServer()
    server = CherryPyWSGIServer(('localhost',7889),gls)
    gls.list_components()
    server.start()

