package com.okta;

import net.jradius.packet.attribute.VSADictionary;

import java.util.Map;

public class PaloAltoVSADictionary implements VSADictionary {
    public static final int VENDOR_ID = 25461;

    public PaloAltoVSADictionary() {
    }

    @Override
    public String getVendorName() {
        return "PaloAlto";
    }

    @Override
    public void loadAttributes(Map<Long, Class<?>> map) {
        map.put(new Long(Attr_PaloAltoClientSourceIP.VSA_TYPE), Attr_PaloAltoClientSourceIP.class);
        map.put(new Long(Attr_PaloAltoUserDomain.VSA_TYPE), Attr_PaloAltoUserDomain.class);
        map.put(new Long(Attr_PaloClientHostname.VSA_TYPE), Attr_PaloClientHostname.class);
    }

    @Override
    public void loadAttributesNames(Map<String, Class<?>> map) {
        map.put(Attr_PaloAltoClientSourceIP.NAME, Attr_PaloAltoClientSourceIP.class);
        map.put(Attr_PaloAltoUserDomain.NAME, Attr_PaloAltoUserDomain.class);
        map.put(Attr_PaloClientHostname.NAME, Attr_PaloClientHostname.class);
    }
}
