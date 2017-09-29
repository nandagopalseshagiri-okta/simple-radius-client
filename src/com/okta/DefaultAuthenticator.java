package com.okta;

import net.jradius.client.RadiusClient;
import net.jradius.client.auth.PAPAuthenticator;
import net.jradius.dictionary.Attr_ReplyMessage;
import net.jradius.dictionary.Attr_UserPassword;
import net.jradius.exception.RadiusException;
import net.jradius.packet.RadiusPacket;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultAuthenticator extends PAPAuthenticator {
    private final static String SECURITY_QUESTION = "Security Question";
    private RadiusClient radiusClient;
    private String answer;
    private boolean needPasswordProcessing = true;

    public DefaultAuthenticator(RadiusClient rc, String answer) {
        radiusClient = rc;
        this.answer = answer;
    }

    public void setPasswordProcessing(boolean needed) {
        needPasswordProcessing = needed;
    }

    @Override
    public void processChallenge(RadiusPacket request, RadiusPacket challenge) throws RadiusException {
        try {
            super.processChallenge(request, challenge);

            this.username = null;
            this.password = null;

            System.out.println("received challenge");
            System.out.println(challenge.toString());

            if (!answerMfaSelection(request, challenge) && !answerQuestionChallenge(request)) {
                // ask the user to respond to the challenge
                String passCode = (new BufferedReader(new InputStreamReader(System.in))).readLine();
                request.overwriteAttribute(new Attr_UserPassword(passCode));
            }

            this.setupRequest(radiusClient, request);
            this.processRequest(request);
        } catch (Exception e) {
            throw new RadiusException("passcode input failed", e);
        }
    }

    @Override
    public void processRequest(RadiusPacket p) throws RadiusException {
        if (needPasswordProcessing) {
            super.processRequest(p);
        }
    }

    private boolean answerQuestionChallenge(RadiusPacket request) {
        boolean answered = false;
        if (answer != null && answer.length() > 0) {
            request.overwriteAttribute(new Attr_UserPassword(answer));
            answered = true;
        }

        return answered;
    }

    private boolean answerMfaSelection(RadiusPacket request, RadiusPacket challenge) {
        Pattern pattern = Pattern.compile("(?<id>\\d*)\\s*-\\s*" + SECURITY_QUESTION + "\\.");

        boolean answered = false;
        Attr_ReplyMessage replyMessage = (Attr_ReplyMessage)challenge.findAttribute(Attr_ReplyMessage.TYPE);
        if (replyMessage != null) {
            String msg = replyMessage.getValue().toString();

            Matcher matcher = pattern.matcher(msg);
            if (matcher.find()) {
                String mfaSelection = matcher.group("id");
                if (mfaSelection != null && mfaSelection.length() > 0) {
                    request.overwriteAttribute(new Attr_UserPassword(mfaSelection));
                    answered = true;
                }
            }
        }

        return answered;
    }
}