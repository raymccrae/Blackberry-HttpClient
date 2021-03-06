//#preprocess

package com.inlinemarmalade.util.http;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

import net.rim.device.api.servicebook.ServiceBook;
import net.rim.device.api.servicebook.ServiceRecord;
import net.rim.device.api.system.CoverageInfo;
import net.rim.device.api.system.DeviceInfo;
import net.rim.device.api.system.WLANInfo;

//#ifndef BlackBerrySDK4.5.0 || BlackBerrySDK4.6.0 || BlackBerrySDK4.7.0
import net.rim.device.api.io.transport.ConnectionDescriptor;
import net.rim.device.api.io.transport.TransportInfo;
//#endif

public class ConnectionFactory {
	public static final int TRANSPORT_WIFI = 1;
	public static final int TRANSPORT_BES = 2;
	public static final int TRANSPORT_BIS = 4;
	public static final int TRANSPORT_DIRECT_TCP = 8;
	public static final int TRANSPORT_WAP2 = 16;
	public static final int TRANSPORT_SIM = 32;

	public static final int TRANSPORTS_ANY = TRANSPORT_WIFI | TRANSPORT_BES | TRANSPORT_BIS | TRANSPORT_DIRECT_TCP | TRANSPORT_WAP2 | TRANSPORT_SIM;
	public static final int TRANSPORTS_AVOID_CARRIER = TRANSPORT_WIFI | TRANSPORT_BES | TRANSPORT_BIS | TRANSPORT_SIM;
	public static final int TRANSPORTS_CARRIER_ONLY = TRANSPORT_DIRECT_TCP | TRANSPORT_WAP2 | TRANSPORT_SIM;

	public static final int DEFAULT_TRANSPORT_ORDER[] = { TRANSPORT_SIM, TRANSPORT_WIFI, TRANSPORT_BIS, TRANSPORT_BES, TRANSPORT_WAP2, TRANSPORT_DIRECT_TCP };

	private static final int TRANSPORT_COUNT = DEFAULT_TRANSPORT_ORDER.length;

	// private static ServiceRecord srMDS[], srBIS[], srWAP2[], srWiFi[];
	private static ServiceRecord srWAP2[];
	private static boolean serviceRecordsLoaded = false;
	
	private HttpConnection con = null;
	private OutputStream os = null;
	
	private int transports[];
	private int lastTransport = -1;
	
	//#ifndef BlackBerrySDK4.5.0 || BlackBerrySDK4.6.0 || BlackBerrySDK4.7.0
	private static int[] TRANSPORT_ORDER = { TransportInfo.TRANSPORT_BIS_B, TransportInfo.TRANSPORT_TCP_WIFI, TransportInfo.TRANSPORT_TCP_CELLULAR, TransportInfo.TRANSPORT_MDS, TransportInfo.TRANSPORT_WAP2, TransportInfo.TRANSPORT_WAP };
	private net.rim.device.api.io.transport.ConnectionFactory _proxyConnectionFactory = new net.rim.device.api.io.transport.ConnectionFactory();
	//#endif
	
	public ConnectionFactory() {
		this(0);
	}

	public ConnectionFactory(int allowedTransports) {
		this(transportMaskToArray(allowedTransports));
	}

	public ConnectionFactory(int transportPriority[]) {
		if (!serviceRecordsLoaded) {
			loadServiceBooks(false);
		}
		transports = transportPriority;
		//#ifndef BlackBerrySDK4.5.0 || BlackBerrySDK4.6.0 || BlackBerrySDK4.7.0
		_proxyConnectionFactory.setPreferredTransportTypes(TRANSPORT_ORDER);
		//#endif
	}
	
	public HttpConnection getConnection(String pURL) {
		int curIndex = 0;
		HttpConnection con = null;

		while ((con = tryHttpConnection(pURL, curIndex)) == null) {
			try {
				curIndex = nextTransport(curIndex);
			} catch (IOException e) {
				e.printStackTrace();
				break;
			} finally {
			}
		}

		if (con != null) {
			setLastTransport(transports[curIndex]);
		}
		
		//#ifndef BlackBerrySDK4.5.0 || BlackBerrySDK4.6.0 || BlackBerrySDK4.7.0
		if(con == null) {
			ConnectionDescriptor desc = _proxyConnectionFactory.getConnection(pURL);
			con = (HttpConnection) desc.getConnection();
		}
		//#endif

		return con;
	}
	
	private int nextTransport(int curIndex) throws IOException {
		if ((curIndex >= 0) && (curIndex < transports.length - 1)) {
			return curIndex + 1;
		} else {
			throw new IOException("No more transport available.");
		}
	}
	
	private HttpConnection tryHttpConnection(String pURL, int tIndex) {
		HttpConnection con = null;
		OutputStream os = null;

		switch (transports[tIndex]) {
		case TRANSPORT_SIM:
			try {
				con = getSimConnection(pURL, false);
			} catch (IOException e) {
			} finally {
			}
			break;
		case TRANSPORT_WIFI:
			try {
				con = getWifiConnection(pURL);
			} catch (IOException e) {
			} finally {
			}
			break;
		case TRANSPORT_BES:
			try {
				con = getBesConnection(pURL);
			} catch (IOException e) {
			} finally {
			}
			break;
		case TRANSPORT_BIS:
			try {
				con = getBisConnection(pURL);
			} catch (IOException e) {
			} finally {
			}
			break;
		case TRANSPORT_DIRECT_TCP:
			try {
				con = getTcpConnection(pURL);
			} catch (IOException e) {
			} finally {
			}
			break;
		case TRANSPORT_WAP2:
			try {
				con = getWap2Connection(pURL);
			} catch (IOException e) {
			} finally {
			}
			break;
		}
		
		return con;
	}
	
	
	public static void reloadServiceBooks() {
		loadServiceBooks(true);
	}
	
	public int getLastTransport() {
		return lastTransport;
	}

	public String getLastTransportName() {
		return getTransportName(getLastTransport());
	}

	private void setLastTransport(int pLastTransport) {
		lastTransport = pLastTransport;
	}

	private HttpConnection getSimConnection(String pURL, boolean mdsSimulatorRunning) throws IOException {
		if (DeviceInfo.isSimulator()) {
			if (mdsSimulatorRunning) {
				return getConnection(pURL, ";deviceside=false", null);
			} else {
				return getConnection(pURL, ";deviceside=true", null);
			}
		}
		return null;
	}

	private HttpConnection getBisConnection(String pURL) throws IOException {
		if (CoverageInfo.isCoverageSufficient(4 /* CoverageInfo.COVERAGE_BIS_B */)) {
			return getConnection(pURL, ";deviceside=false;ConnectionType=mds-public", null);
		}
		return null;
	}

	private HttpConnection getBesConnection(String pURL) throws IOException {
		if (CoverageInfo.isCoverageSufficient(2 /* CoverageInfo.COVERAGE_MDS */)) {
			return getConnection(pURL, ";deviceside=false", null);
		}
		return null;
	}

	private HttpConnection getWifiConnection(String pURL) throws IOException {
		if (WLANInfo.getWLANState() == WLANInfo.WLAN_STATE_CONNECTED) {
			return getConnection(pURL, ";interface=wifi", null);
		}
		return null;
	}

	private HttpConnection getWap2Connection(String pURL) throws IOException {
		if (CoverageInfo.isCoverageSufficient(1 /* CoverageInfo.COVERAGE_DIRECT */) && (srWAP2 != null) && (srWAP2.length != 0)) {
			return getConnection(pURL, ";deviceside=true;ConnectionUID=", srWAP2[0].getUid());
		}
		return null;
	}

	private HttpConnection getTcpConnection(String pURL) throws IOException {
		if (CoverageInfo.isCoverageSufficient(1 /* CoverageInfo.COVERAGE_DIRECT */)) {
			return getConnection(pURL, ";deviceside=true", null);
		}
		return null;
	}

	private HttpConnection getConnection(String pURL, String transportExtras1, String transportExtras2) throws IOException {
		StringBuffer fullUrl = new StringBuffer();
		fullUrl.append(pURL);
		if (transportExtras1 != null) {
			fullUrl.append(transportExtras1);
		}
		if (transportExtras2 != null) {
			fullUrl.append(transportExtras2);
		}
		return (HttpConnection) Connector.open(fullUrl.toString());
	}
	
	private static synchronized void loadServiceBooks(boolean reload) {
		if (serviceRecordsLoaded && !reload) {
			return;
		}
		ServiceBook sb = ServiceBook.getSB();
		ServiceRecord[] records = sb.getRecords();
		Vector mdsVec = new Vector();
		Vector bisVec = new Vector();
		Vector wap2Vec = new Vector();
		Vector wifiVec = new Vector();

		if (!serviceRecordsLoaded) {
			for (int i = 0; i < records.length; i++) {
				ServiceRecord myRecord = records[i];
				String cid, uid;

				if (myRecord.isValid() && !myRecord.isDisabled()) {
					cid = myRecord.getCid().toLowerCase();
					uid = myRecord.getUid().toLowerCase();
					if ((cid.indexOf("wptcp") != -1) && (uid.indexOf("wap2") != -1) && (uid.indexOf("wifi") == -1) && (uid.indexOf("mms") == -1)) {
						wap2Vec.addElement(myRecord);
					}
				}
			}

			srWAP2 = new ServiceRecord[wap2Vec.size()];
			wap2Vec.copyInto(srWAP2);
			wap2Vec.removeAllElements();
			wap2Vec = null;

			serviceRecordsLoaded = true;
		}
	}

	public static int[] transportMaskToArray(int mask) {
		if (mask == 0) {
			mask = TRANSPORTS_ANY;
		}
		int numTransports = 0;
		for (int i = 0; i < TRANSPORT_COUNT; i++) {
			if ((DEFAULT_TRANSPORT_ORDER[i] & mask) != 0) {
				numTransports++;
			}
		}
		int transports[] = new int[numTransports];
		int index = 0;
		for (int i = 0; i < TRANSPORT_COUNT; i++) {
			if ((DEFAULT_TRANSPORT_ORDER[i] & mask) != 0) {
				transports[index++] = DEFAULT_TRANSPORT_ORDER[i];
			}
		}
		return transports;
	}

	private static String getTransportName(int transport) {
		String tName;
		switch (transport) {
		case TRANSPORT_WIFI:
			tName = "WIFI";
			break;
		case TRANSPORT_BES:
			tName = "BES";
			break;
		case TRANSPORT_BIS:
			tName = "BIS";
			break;
		case TRANSPORT_DIRECT_TCP:
			tName = "TCP";
			break;
		case TRANSPORT_WAP2:
			tName = "WAP2";
			break;
		case TRANSPORT_SIM:
			tName = "SIM";
			break;
		default:
			tName = "UNKNOWN";
			break;
		}
		return tName;
	}
	
}
