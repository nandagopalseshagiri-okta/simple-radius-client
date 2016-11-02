package com.okta;

import net.jradius.dictionary.vsa_adsl.forum.Attr_ADSLAgentCircuitId;
import net.jradius.dictionary.vsa_adsl.forum.VSADictionaryImpl;
import net.jradius.packet.attribute.AttributeDictionary;

import java.util.Map;

public class CustomAttributeDictionary implements AttributeDictionary {

    @Override
    public void loadVendorCodes(Map<Long, Class<?>> map) {
        map.put(new Long(PaloAltoVSADictionary.VENDOR_ID), PaloAltoVSADictionary.class);
    }

    @Override
    public void loadAttributes(Map<Long, Class<?>> map) {
    }

    @Override
    public void loadAttributesNames(Map<String, Class<?>> map) {
    }
}
