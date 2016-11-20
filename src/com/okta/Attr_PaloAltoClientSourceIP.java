package com.okta;

import net.jradius.packet.attribute.VSAttribute;
import net.jradius.packet.attribute.value.StringValue;

import java.io.Serializable;

public final class Attr_PaloAltoClientSourceIP extends VSAttribute {
    public static final String NAME = "PaloAlto-Client-Source-IP";
    public static final int VENDOR_ID = 25461;
    public static final int VSA_TYPE = 7;
    public static final long TYPE = 26L;
    public static final long serialVersionUID = 7L;

    public void setup() {
        this.attributeName = NAME;
        this.attributeType = TYPE;
        this.vendorId = VENDOR_ID;
        this.vsaAttributeType = VSA_TYPE;
        this.attributeValue = new StringValue();
    }

    public Attr_PaloAltoClientSourceIP() {
        this.setup();
    }

    public Attr_PaloAltoClientSourceIP(Serializable o) {
        this.setup(o);
    }

    @Override
    public String toString() {
        return String.format("%s := %s", NAME,
                attributeValue == null || attributeValue.toString() == null || attributeValue.toString().length() == 0 ?
                        "<null>" : attributeValue.toString());
    }
}
