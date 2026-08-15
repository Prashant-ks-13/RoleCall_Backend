package com.rolecall.payment.razorpay;

import com.razorpay.PaymentLink;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RazorpayGatewayImpl implements RazorpayGateway {

    private final RazorpayClient razorpayClient;

    public RazorpayGatewayImpl(RazorpayClient razorpayClient) {
        this.razorpayClient = razorpayClient;
    }

    @Override
    public RazorpayPaymentLink createPaymentLink(long amountPaise, String currency, String description,
                                                  String callbackUrl, Map<String, String> notes) throws RazorpayException {
        JSONObject request = new JSONObject();
        request.put("amount", amountPaise);
        request.put("currency", currency);
        request.put("description", description);
        request.put("callback_url", callbackUrl);
        request.put("callback_method", "get");
        request.put("notes", new JSONObject(notes));

        PaymentLink paymentLink = razorpayClient.paymentLink.create(request);
        String id = paymentLink.get("id");
        String shortUrl = paymentLink.get("short_url");
        return new RazorpayPaymentLink(id, shortUrl);
    }

    @Override
    public void verifyWebhookSignature(String payload, String signature, String webhookSecret) throws RazorpayException {
        boolean valid = Utils.verifyWebhookSignature(payload, signature, webhookSecret);
        if (!valid) {
            throw new RazorpayException("Invalid Razorpay webhook signature");
        }
    }
}
