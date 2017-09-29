package com.okta;

import net.jradius.client.RadiusClient;
import net.jradius.client.auth.RadiusAuthenticator;
import net.jradius.dictionary.Attr_CallingStationId;
import net.jradius.dictionary.Attr_NASPort;
import net.jradius.dictionary.Attr_NASPortType;
import net.jradius.dictionary.Attr_UserName;
import net.jradius.dictionary.Attr_UserPassword;
import net.jradius.dictionary.Attr_ServiceType;
import net.jradius.packet.AccessRequest;
import net.jradius.packet.RadiusRequest;
import net.jradius.packet.RadiusResponse;
import net.jradius.packet.attribute.AttributeFactory;
import net.jradius.packet.attribute.AttributeList;
import net.jradius.packet.attribute.RadiusAttribute;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.io.PrintStream;

import static java.net.InetAddress.getLocalHost;

/*
To make a jar file.
1. Go to out dir
2. ln -s ../lib lib
3. jar cvfm jradius.jar ../src/META-INF/MANIFEST.MF com lib
 */

public class ConsoleClient {

    public enum Params {
        server("-r", "Server to connect to"),
        secret("-s", "Shared secret to use"),
        username("-u", "Username to use"),
        password("-p", "Password to use"),
        answer("-a", "Use to answer question as mfa"),
        clientIP("-cip", "Client IP address"),
        standardCode("-sc", "Standard code to use for client IP"),
        vendorCode("-vc", "Vendor code to use for client IP"),
        vendorID("-vid", "Vendor ID code"),
        noPassword("-np", "Do not send password attribute", true),
        unknown("-k", "Unknown");

        private String shortName;
        private String meaning;
        private boolean switchOnly;

        Params(String s, String mean) {
            shortName = s;
            meaning = mean;
            switchOnly = false;
        }

        Params(String s, String mean, boolean switchOnly) {
            shortName = s;
            meaning = mean;
            this.switchOnly = switchOnly;
        }
    }

    static Params getParamFromString(String s) {
        if (s == null || s.isEmpty()) {
            return Params.unknown;
        }

        String longPrefix = "--";

        if (s.startsWith(longPrefix) && s.length() > longPrefix.length()) {
            s = s.substring(longPrefix.length());
        }

        for (Params params : Params.values()) {
            if (params.shortName.equals(s) || params.name().equals(s)) {
                return params;
            }
        }
        return Params.unknown;
    }

    public static Map<Params, String> parseArgs(String[] args) {
        Map<Params, String> r = new HashMap<>();
        Params lastParam = Params.unknown;

        for(int i = 0; i < args.length; ++i) {
            if (lastParam == Params.unknown) {
                lastParam = getParamFromString(args[i]);
                if (lastParam.switchOnly) {
                    r.put(lastParam, "");
                    lastParam = Params.unknown;
                }
            } else {
                r.put(lastParam, args[i]);
                lastParam = Params.unknown;
            }
        }

        System.out.println("Args length = " + args.length);
        System.out.println(r);
        return r;
    }

    public static boolean hasReqArgs(Map<Params, String> params) {
        return   params.containsKey(Params.server) && params.get(Params.server) != null
              && params.containsKey(Params.secret)
              && params.containsKey(Params.username)
              && (params.containsKey(Params.password) || params.containsKey(Params.noPassword));
    }

    public static void usage() {
        System.out.println("Insufficient arguments");
        System.out.println("Usage: ConsoleClient <parameters>");
        for (Params params : Params.values()) {
            if (params != Params.unknown) {
                System.out.println(params.shortName + " " + params.meaning);
            }
        }
    }


    public static AttributeList getBaseAttrList(Map<Params, String> argMap) {

        AttributeList attrs = new AttributeList();
        attrs.add(new Attr_UserName(argMap.get(Params.username)));
        attrs.add(new Attr_NASPortType(Attr_NASPortType.Wireless80211));
        attrs.add(new Attr_NASPort(1L));

        String clientIp = argMap.get(Params.clientIP);
        if (clientIp != null && clientIp.length() > 0) {
            addVendorSpecificAttributes(argMap, attrs, clientIp);

            addStandardAttributes(argMap, attrs, clientIp);
        }

        return attrs;
    }

    private static void addStandardAttributes(Map<Params, String> argMap, AttributeList attrs, String clientIp) {
        String standardCode = argMap.get(Params.standardCode);
        if (standardCode != null) {
            long code = Integer.parseInt(standardCode);
            if (code < 1  || code > 64) {
                throw new RuntimeException("The standard code values is outside [1..64] range.");
            }

            try {
                RadiusAttribute attribute = AttributeFactory.newAttribute(code);
                attribute.setValue(clientIp);
                attrs.add(attribute);
            } catch (Exception ignored) {
                System.out.printf("Failed to add standard attribute. Exception: %s", ignored.toString());
            }
        } else {
            attrs.add(new Attr_CallingStationId(clientIp));
        }
    }

    private static void addVendorSpecificAttributes(Map<Params, String> argMap, AttributeList attrs, String clientIp) {
        String vendorId = argMap.get(Params.vendorID);

        if (vendorId != null && vendorId.equals(new Integer(PaloAltoVSADictionary.VENDOR_ID).toString())) {
            attrs.add(new Attr_PaloAltoClientSourceIP(clientIp));
            try {
                attrs.add(new Attr_PaloClientHostname(getLocalHost().getHostName()));
                attrs.add(new Attr_PaloAltoUserDomain(getLocalHost().getCanonicalHostName()));
            } catch (UnknownHostException e) {
                attrs.add(new Attr_PaloClientHostname("HOSTNAME"));
                attrs.add(new Attr_PaloClientHostname("DOMAIN"));
            }
            return;
        }

        String vendorCode = argMap.get(Params.vendorCode);
        if (vendorId != null && vendorId.length() > 0 && vendorCode != null && vendorCode.length() > 0) {
            try {
                long vendor = Integer.valueOf(vendorId);
                long code = Integer.valueOf(vendorCode);
                RadiusAttribute attr = AttributeFactory.newAttribute(vendor, code, null, -1);
                attr.setValue(clientIp);
                attrs.add(attr);
            } catch (Exception e) {
                System.out.println(e.toString());
                System.out.println("Ignoring vendor specific attribute settings... Continue");
            }
        }
    }

    private static class Pair<K, V> {
        public K k;
        public V v;

        public Pair(K k, V v) {
            this.k = k;
            this.v = v;
        }
    } 

    private static Pair<String, Integer> hostAndPort(String s) {
        int i = s.lastIndexOf(':');
        int port = 1812;
        if (i <= 0) {
            return new Pair(s, port);
        }

        String[] hp = new String[] {s.substring(0, i), s.substring(i+1)};
        try {
            port = Integer.parseInt(hp[1]);
        } catch (NumberFormatException e) {
        }

        return new Pair(hp[0], port);
    }

    public static void main(String[] args) {
        int radiusAccountServerPort = 1813;
        int timeOut = 10;
        int numRetries = 5;

        try {
            Map<Params, String> argMap = parseArgs(args);
            if (!hasReqArgs(argMap)) {
                usage();
                return;
            }

            AttributeFactory.loadAttributeDictionary("net.jradius.dictionary.AttributeDictionaryImpl");
            AttributeFactory.loadAttributeDictionary(new CustomAttributeDictionary());

            Pair<String, Integer> hp = hostAndPort(argMap.get(Params.server));
            InetAddress host = InetAddress.getByName(hp.k);
            RadiusClient rc = new RadiusClient(host, argMap.get(Params.secret), hp.v, radiusAccountServerPort, timeOut);

            RadiusRequest request = new AccessRequest(rc, getBaseAttrList(argMap));

            if (!argMap.containsKey(Params.noPassword)) {
                request.addAttribute(new Attr_UserPassword(argMap.get(Params.password)));
            } else {
                int value = 12;
                System.out.println("not sending password attribute. Sending Attr_ServiceType with value " + value);
                request.addAttribute(new Attr_ServiceType(value));
            }

            System.out.println("Sending:\n" + request.toString());

            DefaultAuthenticator authenticator = new DefaultAuthenticator(rc, argMap.get(Params.answer));
            authenticator.setPasswordProcessing(!argMap.containsKey(Params.noPassword));
            
            RadiusResponse reply = rc.authenticate((AccessRequest) request, authenticator, numRetries);

            System.out.println("Received:\n" + reply.toString());
        } catch (Exception e) {
            e.printStackTrace(new PrintStream(System.out));
        }
    }
}
