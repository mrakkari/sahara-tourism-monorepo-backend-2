//package com.camping.duneinsolite.service.impl;
//
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.mail.SimpleMailMessage;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.stereotype.Service;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class EmailService {
//
//    private final JavaMailSender mailSender;
//
//    /**
//     * Sends a welcome email with temporary password to a newly created user (admin flow)
//     */
//    public void sendWelcomeEmail(String to, String name, String temporaryPassword) {
//        try {
//            SimpleMailMessage message = new SimpleMailMessage();
//            message.setTo(to);
//            message.setSubject("Bienvenue sur Dune Insolite - Vos identifiants temporaires");
//
//            String emailBody = """
//                Bonjour %s,
//
//                Votre compte a été créé avec succès par l'administrateur.
//
//                Voici vos identifiants de connexion temporaires :
//
//                Email          : %s
//                Mot de passe   : %s
//
//                ⚠️ Pour des raisons de sécurité, nous vous recommandons vivement
//                de changer votre mot de passe dès votre première connexion.
//
//                Vous pouvez vous connecter ici : https://votre-domaine.com/login
//
//                Cordialement,
//                L'équipe Dune Insolite
//                """.formatted(name, to, temporaryPassword);
//
//            message.setText(emailBody);
//
//            mailSender.send(message);
//
//            log.info("✅ Welcome email with temporary password successfully sent to: {}", to);
//
//        } catch (Exception e) {
//            log.error("❌ Failed to send welcome email to: {}", to, e);
//            throw new RuntimeException("Email sending failed", e); // Optional: rethrow if you want
//        }
//    }
//}
