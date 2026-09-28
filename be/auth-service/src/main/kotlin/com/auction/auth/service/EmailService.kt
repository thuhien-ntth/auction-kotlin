package com.auction.auth.service

import org.slf4j.LoggerFactory
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.SimpleMailMessage
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val mailSender: JavaMailSender
) {
    private val logger = LoggerFactory.getLogger(EmailService::class.java)

    fun sendVerificationEmail(toEmail: String, token: String) {
        val subject = "Xác nhận đăng ký tài khoản Auction App"
        val body = "Mã xác nhận của bạn là: $token\n\nVui lòng nhập mã này trên ứng dụng để hoàn tất đăng ký."
        
        // Luôn in ra console để dễ test trong môi trường dev
        logger.info("=========================================")
        logger.info("EMAIL MOCK/LOG:")
        logger.info("To: $toEmail")
        logger.info("Subject: $subject")
        logger.info("Body:\n$body")
        logger.info("=========================================")

        try {
            val message = SimpleMailMessage()
            message.setTo(toEmail)
            message.subject = subject
            message.text = body
            mailSender.send(message)
            logger.info("Email sent successfully to $toEmail")
        } catch (e: Exception) {
            logger.error("Failed to send email to $toEmail (Check SMTP configuration): ${e.message}")
        }
    }
}
