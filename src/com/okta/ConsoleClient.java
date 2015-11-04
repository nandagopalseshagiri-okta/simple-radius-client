package com.okta;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.Exception;
import java.lang.System;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import net.jradius.client.RadiusClient;
import net.jradius.client.auth.MSCHAPv2Authenticator;
import net.jradius.client.auth.PAPAuthenticator;
import net.jradius.dictionary.Attr_AcctInputOctets;
import net.jradius.dictionary.Attr_AcctOutputOctets;
import net.jradius.dictionary.Attr_AcctSessionId;
import net.jradius.dictionary.Attr_AcctSessionTime;
import net.jradius.dictionary.Attr_AcctStatusType;
import net.jradius.dictionary.Attr_AcctTerminateCause;
import net.jradius.dictionary.Attr_NASPort;
import net.jradius.dictionary.Attr_NASPortType;
import net.jradius.dictionary.Attr_ReplyMessage;
import net.jradius.dictionary.Attr_UserName;
import net.jradius.dictionary.Attr_UserPassword;
import net.jradius.exception.RadiusException;
import net.jradius.packet.*;
import net.jradius.packet.attribute.AttributeFactory;
import net.jradius.packet.attribute.AttributeList;
import net.jradius.packet.attribute.value.StringValue;
import net.jradius.util.RadiusRandom;

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
        unknown("-k", "Unknown");

        private String shortName;
        private String meaning;

        Params(String s, String mean) {
            shortName = s;
            meaning = mean;
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
              && params.containsKey(Params.password);
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

    public static class SmsAuthenticator extends PAPAuthenticator {
        private RadiusClient radiusClient;

        public SmsAuthenticator(RadiusClient rc) {
            radiusClient = rc;
        }

        @Override
        public void processChallenge(RadiusPacket request, RadiusPacket challenge) throws RadiusException {
            try {
                System.out.println("received challenge");
                super.processChallenge(request, challenge);
                System.out.println(challenge.toString());
                String passCode = (new BufferedReader(new InputStreamReader(System.in))).readLine();
                request.overwriteAttribute(new Attr_UserPassword(passCode));

                this.username = null;
                this.password = null;

                this.setupRequest(radiusClient, request);
                this.processRequest(request);
            } catch (Exception e) {
                throw new RadiusException("passcode input failed", e);
            }
        }
    }

    public static AttributeList getBaseAttrList(Map<Params, String> argMap) {
        AttributeList attrs = new AttributeList();
        attrs.add(new Attr_UserName(argMap.get(Params.username)));
        attrs.add(new Attr_NASPortType(Attr_NASPortType.Wireless80211));
        attrs.add(new Attr_NASPort(new Long(1)));
        return attrs;
    }

    public static void main(String[] args) {
        try {
            Map<Params, String> argMap = parseArgs(args);
            if (!hasReqArgs(argMap)) {
                usage();
                return;
            }

            AttributeFactory.loadAttributeDictionary("net.jradius.dictionary.AttributeDictionaryImpl");

            InetAddress host = InetAddress.getByName(argMap.get(Params.server));
            RadiusClient rc = new RadiusClient(host, argMap.get(Params.secret), 1812, 1813, 10);

            RadiusRequest request = new AccessRequest(rc, getBaseAttrList(argMap));
            request.addAttribute(new Attr_UserPassword(argMap.get(Params.password)));

            System.out.println("Sending:\n" + request.toString());

            RadiusResponse reply = rc.authenticate((AccessRequest) request, new SmsAuthenticator(rc), 5);

            System.out.println("Received:\n" + reply.toString());
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }
}
