package ru.majordomo.hms.personmgr.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.feign.RcUserFeignClient;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.rc.user.resources.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class ResourceHelper {
    private final RcUserFeignClient rcUserFeignClient;
    private final BusinessHelper businessHelper;
    private final AccountHistoryManager history;

    public List<Domain> getDomains(PersonalAccount account) {
        try {
            return rcUserFeignClient.getDomains(account.getId());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("with account {} on getDomains catch e {} message {}", account.getId(), e.getClass(), e.getMessage());
            return new ArrayList<>();
        }
    }

    public void switchAntiSpamForMailboxes(PersonalAccount account, Boolean state) {

        Collection<Mailbox> mailboxes = rcUserFeignClient.getMailboxes(account.getId());

        for (Mailbox mailbox : mailboxes) {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setParams(new HashMap<>());
            message.setAccountId(account.getId());
            message.addParam("resourceId", mailbox.getId());
            message.addParam("antiSpamEnabled", state);

            businessHelper.buildAction(BusinessActionType.MAILBOX_UPDATE_RC, message);

            String historyMessage = "Отправлена заявка на" + (state ? "включение" : "отключение") + "анти-спама у почтового ящика '"
                    + mailbox.getFullName() + "' в связи с " + (state ? "включением" : "отключением") + " услуги";
            history.save(account, historyMessage);
        }
    }

    public void switchAccountResources(PersonalAccount account, Boolean state) {
        switchWebsites(account, state);
        switchDatabaseUsers(account, state);
        switchMailboxes(account, state);
        switchDomains(account, state);
        switchFtpUsers(account, state);
        switchUnixAccounts(account, state);
        switchRedirects(account, state);
    }

    private void switchWebsites(PersonalAccount account, Boolean state) {
        try {

            List<WebSite> webSites = rcUserFeignClient.getWebSites(account.getId());

            for (WebSite webSite : webSites) {
                SimpleServiceMessage message = messageForSwitchOn(webSite, state);

                businessHelper.buildAction(BusinessActionType.WEB_SITE_UPDATE_RC, message);

                String historyMessage = "Отправлена заявка на " + (state ? "включение" : "выключение") + " сайта '" + webSite.getName() + "'";
                history.save(account, historyMessage);
            }

        } catch (Exception e) {
            log.error("account WebSite switch failed for accountId: " + account.getId());
            e.printStackTrace();
        }
    }

    private void switchDatabaseUsers(PersonalAccount account, Boolean state) {
        try {

            List<DatabaseUser> databaseUsers = rcUserFeignClient.getDatabaseUsers(account.getId());

            for (DatabaseUser databaseUser : databaseUsers) {
                SimpleServiceMessage message = messageForSwitchOn(databaseUser, state);

                businessHelper.buildAction(BusinessActionType.DATABASE_USER_UPDATE_RC, message);

                String historyMessage = "Отправлена заявка на " + (state ? "включение" : "выключение") + " пользователя базы данных '" + databaseUser.getName() + "'";
                history.save(account, historyMessage);
            }

        } catch (Exception e) {
            log.error("account DatabaseUsers switch failed for accountId: " + account.getId());
            e.printStackTrace();
        }
    }

    private void switchMailboxes(PersonalAccount account, Boolean state) {
        try {

            Collection<Mailbox> mailboxes = rcUserFeignClient.getMailboxes(account.getId());

            for (Mailbox mailbox : mailboxes) {
                SimpleServiceMessage message = messageForSwitchOn(mailbox, state);

                businessHelper.buildAction(BusinessActionType.MAILBOX_UPDATE_RC, message);

                String historyMessage = "Отправлена заявка на " + (state ? "включение" : "выключение") + " почтового ящика '" + mailbox.getFullName() + "'";
                history.save(account, historyMessage);
            }

        } catch (Exception e) {
            log.error("account Mailboxes switch failed for accountId: " + account.getId());
            e.printStackTrace();
        }
    }

    private void switchDomains(PersonalAccount account, Boolean state) {
        try {

            List<Domain> domains = rcUserFeignClient.getDomains(account.getId());

            for (Domain domain : domains) {
                SimpleServiceMessage message = messageForSwitchOn(domain, state);

                businessHelper.buildAction(BusinessActionType.DOMAIN_UPDATE_RC, message);

                String historyMessage = "Отправлена заявка на " + (state ? "включение" : "выключение") + " домена '" + domain.getName() + "'";
                history.save(account, historyMessage);
            }

        } catch (Exception e) {
            log.error("account Domains switch failed for accountId: " + account.getId());
            e.printStackTrace();
        }
    }

    private void switchFtpUsers(PersonalAccount account, Boolean state) {
        try {

            List<FTPUser> ftpUsers = rcUserFeignClient.getFTPUsers(account.getId());

            for (FTPUser ftpUser : ftpUsers) {
                SimpleServiceMessage message = messageForSwitchOn(ftpUser, state);

                businessHelper.buildAction(BusinessActionType.FTP_USER_UPDATE_RC, message);

                String historyMessage = "Отправлена заявка на " + (state ? "включение" : "выключение") + " FTP-пользователя '" + ftpUser.getName() + "'";
                history.save(account, historyMessage);
            }

        } catch (Exception e) {
            log.error("account FTPUsers switch failed for accountId: " + account.getId());
            e.printStackTrace();
        }
    }

    private void switchUnixAccounts(PersonalAccount account, Boolean state) {
        try {

            Collection<UnixAccount> unixAccounts = rcUserFeignClient.getUnixAccounts(account.getId());

            for (UnixAccount unixAccount : unixAccounts) {
                SimpleServiceMessage message = messageForSwitchOn(unixAccount, state);

                businessHelper.buildAction(BusinessActionType.UNIX_ACCOUNT_UPDATE_RC, message);

                String historyMessage = "Отправлена заявка на " + (state ? "включение" : "выключение") + " UNIX-аккаунта '" + unixAccount.getName() + "'";
                history.save(account, historyMessage);
            }

        } catch (Exception e) {
            log.error("account UnixAccounts switch failed for accountId: " + account.getId());
            e.printStackTrace();
        }
    }

    private void switchRedirects(PersonalAccount account, Boolean state) {
        try {
            List<Redirect> redirects = rcUserFeignClient.getRedirects(account.getId());

            for (Redirect redirect : redirects) {
                SimpleServiceMessage message = messageForSwitchOn(redirect, state);

                businessHelper.buildAction(BusinessActionType.REDIRECT_UPDATE_RC, message);

                String historyMessage = "Отправлена заявка на " + (state ? "включение" : "выключение") + " переадресации '" + redirect.getName() + "'";
                history.save(account, historyMessage);
            }

        } catch (Exception e) {
            log.error("account Redirect switch failed for accountId: " + account.getId());
            e.printStackTrace();
        }
    }

    private SimpleServiceMessage messageForSwitchOn(Resource resource, Boolean state) {
        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setParams(new HashMap<>());
        message.setAccountId(resource.getAccountId());
        message.addParam("resourceId", resource.getId());
        message.addParam("switchedOn", state);
        return message;
    }

    public void updateUnixAccountQuota(PersonalAccount account, Long quotaInBytes) {
        try {

            Collection<UnixAccount> unixAccounts = rcUserFeignClient.getUnixAccounts(account.getId());

            for (UnixAccount unixAccount : unixAccounts) {
                if (!unixAccount.getQuota().equals(quotaInBytes)) {
                    SimpleServiceMessage message = new SimpleServiceMessage();
                    message.setParams(new HashMap<>());
                    message.setAccountId(account.getId());
                    message.addParam("resourceId", unixAccount.getId());
                    message.addParam("quota", quotaInBytes);

                    businessHelper.buildAction(BusinessActionType.UNIX_ACCOUNT_UPDATE_RC, message);

                    String historyMessage = "Отправлена заявка на установку новой квоты в значение '" + quotaInBytes +
                            " байт' для UNIX-аккаунта '" + unixAccount.getName() + "'";
                    history.save(account, historyMessage);
                }
            }

        } catch (Exception e) {
            log.error("account UnixAccounts set quota failed for accountId: " + account.getId());
            e.printStackTrace();
        }
    }

    public void switchCertificates(PersonalAccount account, boolean state) {
        Collection<SSLCertificate> sslCertificates = rcUserFeignClient.getSSLCertificates(account.getId());

        for (SSLCertificate sslCertificate : sslCertificates) {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setAccountId(account.getId());
            message.addParam("resourceId", sslCertificate.getId());
            message.addParam("switchedOn", state);

            businessHelper.buildAction(BusinessActionType.SSL_CERTIFICATE_UPDATE_RC, message);

            String historyMessage = "Отправлена заявка на выключение SSL сертификата '" + sslCertificate.getName() + "'";
            history.save(account, historyMessage);
        }
    }

    public void deleteRedirects(PersonalAccount account, String domainName) {
        rcUserFeignClient
                .getRedirects(account.getId())
                .stream()
                .filter(r -> r.getName().equals(domainName))
                .forEach(r -> {
                    SimpleServiceMessage message = new SimpleServiceMessage();
                    message.setParams(new HashMap<>());
                    message.setAccountId(account.getId());
                    message.addParam("resourceId", r.getId());

                    businessHelper.buildAction(BusinessActionType.REDIRECT_DELETE_RC, message);

                    String historyMessage = "Отправлена заявка на удаление переадресации '" + r.getName() + "'";
                    history.save(account, historyMessage);
                });
    }

    public void disableAndScheduleDeleteForAllMailboxes(PersonalAccount account) {
        Collection<Mailbox> mailboxes = rcUserFeignClient.getMailboxes(account.getId());

        for (Mailbox mailbox : mailboxes) {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setParams(new HashMap<>());
            message.setAccountId(account.getId());
            message.addParam("resourceId", mailbox.getId());
            message.addParam("switchedOn", false);
            message.addParam("willBeDeletedAfter", LocalDateTime.now().plusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            businessHelper.buildAction(BusinessActionType.MAILBOX_UPDATE_RC, message);

            String historyMessage = "Отправлена заявка на выключение и отложенное удаление почтового ящика '" + mailbox.getName() + "'";
            history.save(account, historyMessage);
        }
    }

    public void disableAndScheduleDeleteForAllDatabases(PersonalAccount account) {
        Collection<Database> databases = rcUserFeignClient.getDatabases(account.getId());

        for (Database database : databases) {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setParams(new HashMap<>());
            message.setAccountId(account.getId());
            message.addParam("resourceId", database.getId());
            message.addParam("switchedOn", false);
            message.addParam("willBeDeletedAfter", LocalDateTime.now().plusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            businessHelper.buildAction(BusinessActionType.DATABASE_UPDATE_RC, message);

            String historyMessage = "Отправлена заявка на выключение и отложенное удаление базы данных '" + database.getName() + "'";
            history.save(account, historyMessage);
        }
    }

    public void disableAndScheduleDeleteForAllDatabaseUsers(PersonalAccount account) {
        Collection<DatabaseUser> databaseUsers = rcUserFeignClient.getDatabaseUsers(account.getId());

        for (DatabaseUser databaseUser : databaseUsers) {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setParams(new HashMap<>());
            message.setAccountId(account.getId());
            message.addParam("resourceId", databaseUser.getId());
            message.addParam("switchedOn", false);
            message.addParam("willBeDeletedAfter", LocalDateTime.now().plusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            businessHelper.buildAction(BusinessActionType.DATABASE_USER_UPDATE_RC, message);

            String historyMessage = "Отправлена заявка на выключение и отложенное удаление пользователя баз данных '" + databaseUser.getName() + "'";
            history.save(account, historyMessage);
        }
    }

    public void unScheduleDeleteForAllMailboxes(PersonalAccount account) {
        Collection<Mailbox> mailboxes = rcUserFeignClient.getMailboxes(account.getId());

        for (Mailbox mailbox : mailboxes) {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setParams(new HashMap<>());
            message.setAccountId(account.getId());
            message.addParam("resourceId", mailbox.getId());
            message.addParam("willBeDeletedAfter", null);

            businessHelper.buildAction(BusinessActionType.MAILBOX_UPDATE_RC, message);

            String historyMessage = "Отправлена заявка на отмену отложенного удаления почтового ящика '" + mailbox.getName() + "'";
            history.save(account, historyMessage);
        }
    }

    public void unScheduleDeleteForAllDatabases(PersonalAccount account) {
        Collection<Database> databases = rcUserFeignClient.getDatabases(account.getId());

        for (Database database : databases) {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setParams(new HashMap<>());
            message.setAccountId(account.getId());
            message.addParam("resourceId", database.getId());
            message.addParam("willBeDeletedAfter", null);

            businessHelper.buildAction(BusinessActionType.DATABASE_UPDATE_RC, message);

            String historyMessage = "Отправлена заявка на отмену отложенного удаления базы данных '" + database.getName() + "'";
            history.save(account, historyMessage);
        }
    }

    public void unScheduleDeleteForAllDatabaseUsers(PersonalAccount account) {
        Collection<DatabaseUser> databaseUsers = rcUserFeignClient.getDatabaseUsers(account.getId());

        for (DatabaseUser databaseUser : databaseUsers) {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setParams(new HashMap<>());
            message.setAccountId(account.getId());
            message.addParam("resourceId", databaseUser.getId());
            message.addParam("willBeDeletedAfter", null);

            businessHelper.buildAction(BusinessActionType.DATABASE_USER_UPDATE_RC, message);

            String historyMessage = "Отправлена заявка на отмену отложенного удаления пользователя баз данных '" + databaseUser.getName() + "'";
            history.save(account, historyMessage);
        }
    }

    public List<Quotable> getQuotableResources(PersonalAccount account) {
        List<Quotable> quotableResources = new ArrayList<>();

        try {
            Collection<UnixAccount> unixAccounts = rcUserFeignClient.getUnixAccounts(account.getId());
            quotableResources.addAll(unixAccounts);
        } catch (Exception e) {
            log.error("get unixAccounts failed for accountId: " + account.getId());
            e.printStackTrace();
        }

        try {
            Collection<Mailbox> mailboxes = rcUserFeignClient.getMailboxes(account.getId());
            quotableResources.addAll(mailboxes);
        } catch (Exception e) {
            log.error("get Mailbox failed for accountId: " + account.getId());
            e.printStackTrace();
        }

        try {
            Collection<Database> databases = rcUserFeignClient.getDatabases(account.getId());
            quotableResources.addAll(databases);
        } catch (Exception e) {
            log.error("get Database failed for accountId: " + account.getId());
            e.printStackTrace();
        }
        return quotableResources;
    }

    public List<Quotable> filterQuotableResoursesByWritableState(List<Quotable> quotableResources, boolean state) {
        return quotableResources.stream().filter(
                quotableResource -> (quotableResource.getWritable() == state)
        ).collect(Collectors.toList());
    }

    public void setWritableForAccountQuotaServicesByList(PersonalAccount account, Boolean state, List<Quotable> resourses) {

        for (Quotable resource: resourses) {
            try {
                if (resource instanceof UnixAccount) {

                    setWritableForUnixAccount(account, (UnixAccount) resource, state);

                } else if (resource instanceof Mailbox) {

                    setWritableForMailbox(account, (Mailbox) resource, state);

                } else if (resource instanceof Database) {

                    setWritableForDatabase(account, (Database) resource, state);

                } else {

                    log.error("can't cast resource [" + resource + "] for accountId: " + account.getId());
                }
            } catch (Exception e) {
                log.error("account resource [" + resource + "] writable switch failed for accountId: " + account.getId());
                e.printStackTrace();
            }
        }
    }

    private void setWritableForUnixAccount(PersonalAccount account, UnixAccount unixAccount, boolean state) {

        try {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setParams(new HashMap<>());
            message.setAccountId(account.getId());
            message.addParam("resourceId", unixAccount.getId());
            message.addParam("writable", state);

            businessHelper.buildAction(BusinessActionType.UNIX_ACCOUNT_UPDATE_RC, message);

            String historyMessage = "Отправлена заявка на " + (state ? "включение" : "выключение") +
                    " возможности записывать данные (writable) для UNIX-аккаунта '" + unixAccount.getName() + "'";
            history.save(account, historyMessage);

        } catch (Exception e) {
            log.error("account unixAccount [" + unixAccount.getId() + "] writable switch failed for accountId: " + account.getId());
            e.printStackTrace();
        }
    }

    private void setWritableForMailbox(PersonalAccount account, Mailbox mailbox, boolean state) {

        try {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setParams(new HashMap<>());
            message.setAccountId(account.getId());
            message.addParam("resourceId", mailbox.getId());
            message.addParam("writable", state);

            businessHelper.buildAction(BusinessActionType.MAILBOX_UPDATE_RC, message);

            String historyMessage = "Отправлена заявка на " + (state ? "включение" : "выключение") +
                    " возможности сохранять письма (writable) для почтового ящика '" + mailbox.getFullName() + "'";
            history.save(account, historyMessage);


        } catch (Exception e) {
            log.error("account Mailbox [" + mailbox.getId() + "] writable switch failed for accountId: " + account.getId());
            e.printStackTrace();
        }
    }

    private void setWritableForDatabase(PersonalAccount account, Database database, Boolean state) {

        try {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setParams(new HashMap<>());
            message.setAccountId(account.getId());
            message.addParam("resourceId", database.getId());
            message.addParam("writable", state);

            businessHelper.buildAction(BusinessActionType.DATABASE_UPDATE_RC, message);

            String historyMessage = "Отправлена заявка на " + (state ? "включение" : "выключение") +
                    " возможности записывать данные (writable) для базы данных '" + database.getName() + "'";
            history.save(account, historyMessage);

        } catch (Exception e) {
            log.error("account Database [" + database.getName() + "] writable switch failed for accountId: " + account.getId());
            e.printStackTrace();
        }
    }
}
