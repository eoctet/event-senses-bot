/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.core.handler;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.flink.util.concurrent.ExecutorThreadFactory;
import pro.octet.senses.bot.core.entity.ActionParam;
import pro.octet.senses.bot.core.model.EmailConfig;
import pro.octet.senses.bot.exception.ServerException;
import pro.octet.senses.bot.utils.CommonUtils;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Email manager
 *
 * @author William
 * @since 22.0816.2.6
 */
@Slf4j
public final class EmailManager implements AutoCloseable {

    private static volatile EmailManager manager;
    private static final String CHARSET_NAME = "UTF-8";
    private final static int SOCKET_TIMEOUT = 1000 * 3;
    private final static int SOCKET_CONNECTION_TIMEOUT = 1000 * 3;
    private final BlockingQueue<HtmlEmail> operationQueue;
    private final AtomicReference<Throwable> failureThrowable = new AtomicReference<>();
    private transient ScheduledThreadPoolExecutor executor;
    private transient ScheduledFuture<?> scheduledFuture;
    private volatile transient boolean closed = false;
    private volatile transient boolean init = false;

    private EmailManager() {
        this.operationQueue = new LinkedBlockingQueue<>();
    }

    /**
     * Get an instance of email
     *
     * @return EmailManager
     */
    public static EmailManager getInstance() {
        if (manager == null) {
            synchronized (EmailManager.class) {
                if (manager == null) {
                    manager = new EmailManager();
                }
            }
        }
        return manager;
    }

    /**
     * Init email worker task
     */
    private synchronized void initScheduledWorker() {
        this.executor = new ScheduledThreadPoolExecutor(1, new ExecutorThreadFactory("Email-worker-task"));
        this.scheduledFuture = this.executor.scheduleWithFixedDelay(() -> {
            if (closed) {
                return;
            }
            try {
                if (!operationQueue.isEmpty()) {
                    HtmlEmail email = operationQueue.poll(5, TimeUnit.MILLISECONDS);
                    if (email != null) {
                        String result = email.send();
                        log.info("Send email success, Message ID: {}.", result);
                    }
                }
            } catch (Exception e) {
                failureThrowable.compareAndSet(null, e);
            }
        }, 200, 100, TimeUnit.MILLISECONDS);
        init = true;
    }

    /**
     * Setting email server config
     *
     * @param emailConfig Email config
     * @return HtmlEmail
     */
    private HtmlEmail createEmail(EmailConfig emailConfig, String contentId) {
        // setting email server config
        HtmlEmail email = new HtmlEmail();
        email.setSocketTimeout(SOCKET_TIMEOUT);
        email.setSocketConnectionTimeout(SOCKET_CONNECTION_TIMEOUT);
        email.setDebug(false);
        Map<String, String> headers = Maps.newHashMap();
        headers.put("Content-ID", contentId);
        email.setHeaders(headers);
        Preconditions.checkArgument(StringUtils.isNotBlank(emailConfig.getServer()), "Email SMTP server address cannot be empty");
        email.setHostName(emailConfig.getServer());
        Preconditions.checkArgument(StringUtils.isNotBlank(emailConfig.getSmtp()), "Email SMTP server port cannot be empty");
        email.setSmtpPort(Integer.parseInt(emailConfig.getSmtp()));
        email.setSslSmtpPort(emailConfig.getSmtp());
        email.setSSLOnConnect(emailConfig.isSsl());
        email.setStartTLSEnabled(emailConfig.isTls());
        if (StringUtils.isNotBlank(emailConfig.getUsername()) || StringUtils.isNotBlank(emailConfig.getPassword())) {
            email.setAuthenticator(new DefaultAuthenticator(emailConfig.getUsername(), emailConfig.getPassword()));
        }
        return email;
    }

    /**
     * Check error and rethrow
     */
    private void checkErrorAndRethrow() {
        Throwable cause = failureThrowable.get();
        if (cause != null) {
            throw new ServerException("Error sending email", cause);
        }
    }

    /**
     * Send email
     *
     * @param actionParam Action parameters
     * @param emailConfig Email config
     * @throws EmailException
     */
    public void sendEmail(ActionParam actionParam, EmailConfig emailConfig) throws EmailException {
        if (!init) {
            initScheduledWorker();
        }
        checkErrorAndRethrow();
        // setting email server config
        String contentId = CommonUtils.createEmailContentId();
        HtmlEmail email = createEmail(emailConfig, contentId);
        // setting email header config
        Preconditions.checkArgument(StringUtils.isNotBlank(emailConfig.getFrom()), "Email sender cannot be empty");
        email.setFrom(emailConfig.getFrom(), StringUtils.substringBefore(emailConfig.getFrom(), "@"));
        email.setCharset(CHARSET_NAME);
        Preconditions.checkArgument(StringUtils.isNotBlank(emailConfig.getSubject()), "Email subject cannot be empty");
        email.setSubject(emailConfig.getSubject());
        Preconditions.checkArgument(emailConfig.getRecipients() != null, "Email recipient cannot be empty");
        email.addTo(emailConfig.getRecipients());
        if (emailConfig.getCarbonCopies() != null) {
            email.addCc(emailConfig.getCarbonCopies());
        }
        // setting email content config
        Preconditions.checkArgument(StringUtils.isNotBlank(emailConfig.getContent()), "Email content cannot be empty");
        String finalContent = CommonUtils.parameterBindingFormat(actionParam, emailConfig.getContent());
        email.setHtmlMsg(finalContent);
        log.debug("Create a new email, Content ID: {}", contentId);
        // put email to the operation queue
        try {
            boolean status = operationQueue.offer(email, 5, TimeUnit.MILLISECONDS);
            log.info("Put email to the operation queue, Queue size: {}, Status: {}.", operationQueue.size(), status);
        } catch (InterruptedException e) {
            log.warn("", e);
        }
    }

    @Override
    public void close() {
        closed = true;
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            if (executor != null) {
                executor.shutdownNow();
                log.debug("Closed email worker thread pool.");
            }
        }
    }

}