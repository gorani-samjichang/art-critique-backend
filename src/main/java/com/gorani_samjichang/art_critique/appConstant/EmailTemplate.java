package com.gorani_samjichang.art_critique.appConstant;

public enum EmailTemplate {
    WELCOME("Verifying Code Email For Art Critique","<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
            "    <meta charset=\"UTF-8\">" +
            "    <title>Welcome!</title>" +
            "</head>" +
            "<body>" +
            "    <h1>Welcome to Our Service</h1>" +
            "    <p style=\"margin-bottom:12px\">Thank you for signing up. Please copy the code below to verify your email address:</p>" +
            "    <p style=\"margin-bottom:12px\">%s</p>" +
            "    <p style=\"margin-bottom:12px\">This expires in 30 minutes. </p>" +
            "    <p style=\"margin-bottom:12px\">If you did not sign up for this account, please ignore this email.</p>" +
            "    <p style=\"margin-bottom:12px\">Best regards,<br>Your Company</p>" +
            "</body>" +
            "</html>");

    private final String subject;
    private final String template;


    EmailTemplate(String subject, String template) {
        this.subject=subject;
        this.template = template;
    }

    public String getTemplate() {
        return this.template;
    }
    public String getSubject(){
        return this.subject;
    }
}
