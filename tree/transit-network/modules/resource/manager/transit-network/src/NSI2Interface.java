/*
 * OGF NSI Requester API, ver. 2.0.sc
 * Copyright 2003-2013 National Institute of Advanced Industrial Science and Technology
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Hashtable;
import java.net.URL;

import net.glambda.nsi2.impl.EventListener;
import net.glambda.nsi2.util.NSITextDump;
import net.glambda.nsi2.util.TypesBuilder;
import nsi2.reply.QueryNotificationReply;
import nsi2.reply.QueryReply;
import nsi2.reply.ReserveCommitReply;
import nsi2.reply.ReserveReply;
import nsi2.NSI2Client;
import nsi2.SampleEventListener;

import org.ogf.schemas.nsi._2013._12.connection._interface.ServiceException;
import org.ogf.schemas.nsi._2013._12.connection.types.QueryNotificationConfirmedType;
import org.ogf.schemas.nsi._2013._12.connection.types.QueryNotificationType;
import org.ogf.schemas.nsi._2013._12.connection.types.QueryRecursiveResultType;
import org.ogf.schemas.nsi._2013._12.connection.types.QuerySummaryConfirmedType;
import org.ogf.schemas.nsi._2013._12.connection.types.QuerySummaryResultType;
import org.ogf.schemas.nsi._2013._12.connection.types.QueryType;
import org.ogf.schemas.nsi._2013._12.connection.types.ReservationConfirmCriteriaType;
import org.ogf.schemas.nsi._2013._12.connection.types.ReservationRequestCriteriaType;
import org.ogf.schemas.nsi._2013._12.connection.types.ScheduleType;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.transport.http.HTTPConduit;

public class NSI2Interface {
    static final boolean debug = true;
    static final int DEFAULT_DURATION_HOUR = 24 * 3; // 3days
    static Hashtable<String, ReservationRequestCriteriaType> criterias =
	new Hashtable<String, ReservationRequestCriteriaType>();
    static Hashtable<String, Integer> versions =
	new Hashtable<String, Integer>();
    static Hashtable<String, Long> capacities =
	new Hashtable<String, Long>();

    String providerNSA = null;
    String providerURI = null;
    String requesterNSA = null;
    String requesterURI = null;
    String httpUser = null;
    String httpPassword = null;
    long replyWait = 60 * 1000L; // 60 sec

    EventListener listener = null;
    NSI2Client client = null;
    boolean hasRequester = false;

    private final String DEFAULT_GLOBAL_RESERVATION_ID = "global";
    private final String DEFAULT_DESCRIPTION = "nsi2 test";

    public NSI2Interface
	(String providerNSA, String providerURI, 
	 String requesterNSA, String requesterURI,
	 String httpUser, String httpPassword)
	throws Exception
    {
	this.providerNSA = providerNSA;
	this.providerURI = providerURI;
	this.requesterNSA = requesterNSA;
	this.requesterURI = requesterURI;
	this.httpUser = httpUser;
	this.httpPassword = httpPassword;

	if (requesterURI != null) hasRequester = true;
        listener = new SampleEventListener();
	client = getNSI2Client();
    }

    private NSI2Client getNSI2Client() 
	throws Exception
    {
	NSI2Client c = null;
	String oauth = null;

	if (httpUser != null && httpPassword == null) oauth = httpUser;
        if (oauth == null) {
	    c = new NSI2Client
		(providerNSA, providerURI, requesterNSA, requesterURI,
		 replyWait, httpUser, httpPassword, listener);
	} else {
	    c = new NSI2Client
		(providerNSA, providerURI, requesterNSA, requesterURI,
		 replyWait, oauth, listener);
        }
	return c;
    }

    private int getNewVersion(String reservationId) 
    {
        Integer it = versions.get(reservationId);
	if (it == null) {
	    it = new Integer(0);
	    versions.put(reservationId, it);
	} else {
	    it ++;
	}
	if (debug) System.out.println("******************* value=" + it);
	return it.intValue();
    }


    public String reserveCommit
	(String sSTP, String dSTP, int sVlan, int dVlan, long capacity,
	 int start, int end)
	throws Exception 
    {
	ReservationRequestCriteriaType criteria = makeReservationCriteria
	    (sSTP, dSTP, sVlan, dVlan, capacity, start, end);
	String reservationId = reserve(criteria);
	commit(reservationId);

	criterias.put(reservationId, criteria);
	capacities.put(reservationId, new Long(capacity));
	return reservationId;
    }

    public ReservationRequestCriteriaType makeReservationCriteria
	(String sSTP, String dSTP, int sVlan, int dVlan, long capacity,
	 int startTime, int endTime)
    {
        String srcstp = sSTP;
        String deststp = dSTP;

        Calendar start = Calendar.getInstance();
	long currentTime = start.getTimeInMillis();
	long lstart = ((long) startTime) * 1000;
	long lend = ((long) endTime) * 1000;
	if (lstart >= currentTime) start.setTimeInMillis(lstart);
        Calendar end = (Calendar) start.clone();
	if (lend <= 1000L*3600*3) 
	    end.add(Calendar.HOUR, DEFAULT_DURATION_HOUR);
	else
	    end.setTimeInMillis(lend);
	
        int srcvlan = sVlan;
        int destvlan = dVlan;
        long bandwidth = capacity; // [Mbps]
	
        ScheduleType schedule = TypesBuilder.makeScheduleType(start, end);
        ReservationRequestCriteriaType criteria =
	    TypesBuilder.makeReservationRequestCriteriaType
	    (schedule, srcstp, srcvlan, deststp, destvlan, capacity);

	criteria.setVersion(0);
	return criteria;
    }

    public String reserve(ReservationRequestCriteriaType criteria) 
	throws Exception 
    {
	String reservationId = null;
        String globalReservationId = DEFAULT_GLOBAL_RESERVATION_ID;
        String description = DEFAULT_DESCRIPTION;
        if (debug) showMessage(criteria, NSITextDump.toString(criteria));

        ReserveReply reply = client.reserve
	    (reservationId, globalReservationId, description, criteria);

        if (reply.getConfirm() != null) {
            ReservationConfirmCriteriaType conf = reply.getConfirm();
            if (debug) showMessage("ConnectionID", reply.getConnectionId());
            if (debug) showMessage(conf, NSITextDump.toString(conf));
	    int version = conf.getVersion();
	    versions.put(reply.getConnectionId(), new Integer(version));
        } else if (reply.getServiceException() != null) {
            if (debug) showMessage
			   (reply.getServiceException(),
			    NSITextDump.toString(reply.getServiceException()));
            if (debug) showMessage
			   (reply.getConnectionStates(),
			    NSITextDump.toString(reply.getConnectionStates()));
        }
        return reply.getConnectionId();
    }

    public void commit(String reservationId) 
	throws Exception 
    {
        ReserveCommitReply reply = client.reserveCommit(reservationId);
        if (reply.getServiceException() == null) {
            if (debug) System.out.println("ReserveCommitConfirmed");
        } else if (reply.getServiceException() != null) {
            if (debug) showMessage
			   (reply.getServiceException(),
			    NSITextDump.toString(reply.getServiceException()));
            if (debug) showMessage
			   (reply.getConnectionStates(),
			    NSITextDump.toString(reply.getConnectionStates()));
        }
    }

    public void abort(String reservationId) 
	throws Exception 
    {
        client.reserveAbort(reservationId);
    }

    public void modify (String reservationId, int end_time_sec)
	throws Exception 
    {
        String globalReservationId = DEFAULT_GLOBAL_RESERVATION_ID;
        String description = DEFAULT_DESCRIPTION;

        ReservationRequestCriteriaType old = 
	    (ReservationRequestCriteriaType) criterias.get(reservationId);
	if (old == null) {
	    throw new ServiceException
		("No such reservation, id=" + reservationId);
	}
        ScheduleType oldSchedule = old.getSchedule();
        Calendar oldStart = oldSchedule.getStartTime();

        Calendar start = Calendar.getInstance();
	
	long lend = ((long) end_time_sec) * 1000;
        Calendar end = Calendar.getInstance();
	end.setTimeInMillis(lend);

        // Calendar end = (Calendar) start.clone();
        // end.add(Calendar.HOUR, DEFAULT_DURATION_HOUR);
        ScheduleType schedule = TypesBuilder.makeScheduleType(oldStart, end);

	long capacity = capacities.get(reservationId).longValue();

        ReservationRequestCriteriaType criteria =
	    TypesBuilder.makeReservationRequestCriteriaType(schedule, capacity);
        criteria.setVersion(getNewVersion(reservationId));
	if (debug) showMessage(criteria, NSITextDump.toString(criteria));

        ReserveReply reply = client.reserve
	    (reservationId, globalReservationId, description, criteria);

        if (reply.getConfirm() != null) {
            ReservationConfirmCriteriaType conf = reply.getConfirm();
            if (debug) showMessage(conf, NSITextDump.toString(conf));
	    int version = conf.getVersion();
	    versions.put(reservationId, new Integer(version));
        } else if (reply.getServiceException() != null) {
            if (debug) showMessage
			   (reply.getServiceException(),
			    NSITextDump.toString(reply.getServiceException()));
            if (debug) showMessage
			   (reply.getConnectionStates(),
			    NSITextDump.toString(reply.getConnectionStates()));
        }
    }


    public void modifyCommit (String reservationId, int end_time_sec)
	throws Exception 
    {
	modify(reservationId, end_time_sec);
        commit(reservationId);
    }

    public void query(String reservationId) throws Exception {
        QueryType queryType = new QueryType();
        queryType.getConnectionId().add(reservationId);
        {
            // QuerySummarySync
            QuerySummaryConfirmedType summary = 
		client.querySummarySync(queryType);
            if (debug) showMessage
			   ("QuerySummary (sync)", 
			    NSITextDump.toString(summary));
        }
        if (hasRequester) {
            // QuerySummary (async)
            QueryReply reply = client.querySummary(queryType);
            if (reply.getSummary() != null) {
                List<QuerySummaryResultType> summary = reply.getSummary();
                if (debug) showMessage
			       ("QuerySummary (async)", 
				NSITextDump.toStringQuerySummayList(summary));
            } else {
                if (debug) showMessage
			       ("QuerySummary (async)", 
				NSITextDump.toString(reply.getException()));
            }
        }
        if (hasRequester) {
            // QueryRecursive (async)
            QueryReply reply = client.queryRecursive(queryType);
            if (reply.getRecursive() != null) {
                List<QueryRecursiveResultType> recursive = reply.getRecursive();
                if (debug) showMessage
			       ("QueryRecursive (async)",
				NSITextDump.toStringQueryRecursiveList(recursive));
            } else {
                if (debug) showMessage
			       ("QueryRecursive (async)", 
				NSITextDump.toString(reply.getException()));
            }
        }
    }

    public void queryNotification (String reservationId) 
	throws Exception 
    {
        QueryNotificationType type = new QueryNotificationType();
        type.setConnectionId(reservationId);
        {
            // QueryNotification (sync)
            QueryNotificationConfirmedType conf = client.queryNotificationSync(type);
            if (debug) showMessage
			   ("QueryNotification (sync)", 
			    NSITextDump.toString(conf));
        }
        if (hasRequester) {
            // QueryNotification (async)
            QueryNotificationReply reply = client.queryNotification(type);
            if (reply.getConfirmed() != null) {
                if (debug) showMessage
			       ("QueryNotification (async)", 
				NSITextDump.toString(reply.getConfirmed()));
            } else {
                if (debug) showMessage
			       ("QueryNotification (async)", 
				NSITextDump.toString(reply.getException()));
            }
        }
    }

    public void provision(String reservationId) 
	throws Exception 
    {
	client.provision(reservationId);
    }

    public void release(String reservationId) 
	throws Exception 
    {
	client.release(reservationId);
    }

    public void terminate(String reservationId) 
	throws Exception 
    {
	client.terminate(reservationId);

	criterias.remove(reservationId);
	versions.remove(reservationId);
	capacities.remove(reservationId);
    }

    static private void showMessage(String header, String txt) 
    {
        System.out.println("**** " + header + " ****");
        System.out.println(txt);
        System.out.println();
    }

    static private void showMessage(Object o, String txt) 
    {
        if (o != null) {
            showMessage(o.getClass().getSimpleName(), txt);
        } else {
            showMessage("", txt);
        }
    }

    public static void main(String[] args)
    {
	try {
	    NSI2Interface nsi = new NSI2Interface
		("urn:ogf:network:aist.go.jp:2013:nsa",
		 "https://127.0.0.1:22311/aist_upa/services/ConnectionProvider",
		 "urn:ogf:network:aist.go.jp:2013:nsa",
		 "https://127.0.0.1:29081/nsi2_requester/services/ConnectionRequester",
		 null,
		 null);
	    
	    String id = nsi.reserveCommit
		("urn:ogf:network:aist.go.jp:2013:bi-ps",
		 "urn:ogf:network:aist.go.jp:2013:bi-aist-jgn-x",
		 1783, 1783, 400, 0, 0);
	    nsi.provision(id);

	    Thread.sleep(10 * 1000);
	    nsi.modifyCommit(id, 0);

	    Thread.sleep(10 * 1000);
	    nsi.release(id);

	    Thread.sleep(10 * 1000);
	    nsi.provision(id);

	    Thread.sleep(10 * 1000);
	    nsi.terminate(id);
	} catch (Exception ex) {
	    ex.printStackTrace();
	}
    }
}
