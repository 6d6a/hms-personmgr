package ru.majordomo.hms.personmgr.controller.rest;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.dto.rpc.Contract;
import ru.majordomo.hms.personmgr.dto.rpc.DocumentType;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.AccountOwnerManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.AccountOwner;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
//import ru.majordomo.hms.personmgr.service.JarUtils;
import ru.majordomo.hms.personmgr.service.ResourceHelper;
import ru.majordomo.hms.personmgr.service.Rpc.MajordomoRpcClient;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.majordomo.hms.personmgr.common.Utils.convertFileToByteArrayOutputStream;


@RestController
@RequestMapping("/{accountId}/document")
public class AccountDocumentRestController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final MajordomoRpcClient majordomoRpcClient;
    private final AccountOwnerManager accountOwnerManager;
    private final PersonalAccountManager personalAccountManager;
    private final ApplicationContext applicationContext;

    @Autowired
    public AccountDocumentRestController(
            MajordomoRpcClient majordomoRpcClient,
            AccountOwnerManager accountOwnerManager,
            PersonalAccountManager personalAccountManager,
            ApplicationContext applicationContext
    ){
        this.majordomoRpcClient = majordomoRpcClient;
        this.accountOwnerManager = accountOwnerManager;
        this.personalAccountManager = personalAccountManager;
        this.applicationContext = applicationContext;
    }

    @GetMapping("/{documentType}")
    @ResponseBody
    public void getContract(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @PathVariable(value = "documentType") DocumentType documentType,
            @RequestParam Map<String, String> params,
            HttpServletResponse response,
            HttpServletRequest request
    ) {

        final ServletContext servletContext = request.getSession().getServletContext();
        final File tempDirectory = (File) servletContext.getAttribute("javax.servlet.context.tempdir");
        final String temporaryFilePath = tempDirectory.getAbsolutePath() + "/";

//        logger.info("temporaryFilePath: " + temporaryFilePath);

        AccountOwner owner = accountOwnerManager.findOneByPersonalAccountId(accountId);

        AccountOwner.Type ownerType = owner.getType();

        Contract contract;
        switch (documentType) {
            case VIRTUAL_HOSTING_OFERTA:
                throw new ParameterValidationException("Нельзя заказать оферту");

            case VIRTUAL_HOSTING_CONTRACT:
                throw new ParameterValidationException("Нельзя заказать договор");

            case VIRTUAL_HOSTING_BUDGET_CONTRACT:
                if (!ownerType.equals(AccountOwner.Type.BUDGET_COMPANY)) {
                    throw new ParameterValidationException("Вы не можете заказать такой документ");
                }
                contract = majordomoRpcClient.getActiveContractVirtualHosting();

                break;
            default:
                throw new ParameterValidationException("Неизвестный тип договора");
        }

        String template;

        try {
            String header = getHeader();
            String body = contract.getBody();
            String footer = contract.getFooter();
            List<Integer> noFooterPages = contract.getNoFooterPages();

            if (body == null || footer == null || noFooterPages == null) {
                throw new ParameterValidationException("Один из элементов (body||footer||noFooterPages) равен null");
            }
            template = createTemplate(header, body, footer, noFooterPages);
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
            throw new ParameterValidationException("Не удалось сгенерировать договор.");
        }

//        String WKHTMLTOPDF_FILENAME = "wkhtmltopdf";
        String WKHTMLTOPDF_FILENAME = "wkhtmltox/bin/wkhtmltopdf";
        File wkhtmltopdfFile = new File(temporaryFilePath + WKHTMLTOPDF_FILENAME);
        if (!wkhtmltopdfFile.exists()) {
//        Resource resource = applicationContext.getResource("classpath:pdfconvert/" + WKHTMLTOPDF_FILENAME);
//            try {
//                URL url = getClass().getResource("/pdfconvert/" + WKHTMLTOPDF_FILENAME);
//                InputStream name = url.openStream();
//                URI uri = URI.create(temporaryFilePath + WKHTMLTOPDF_FILENAME);
//
//                Files.copy(name, wkhtmltopdfFile.toPath());
//                logger.info(String.valueOf(wkhtmltopdfFile.setExecutable(true, true)));
////            Process p = Runtime.getRuntime().exec(wkhtmltopdfFile.toPath().toString() + " " + );
////            logger.info(p.getOutputStream().toString());
//            } catch (IOException e) {
//                logger.error("Catch exception with copy wkhtmltopdf to tmpdir, message: " + e.getMessage());
//                e.printStackTrace();
//                return;
//            }

            prepareWkhtmltopdf();
        } /*else if (!wkhtmltopdfFile.canExecute()) {
            wkhtmltopdfFile.setExecutable(true, true);
        }*/
        wkhtmltopdfFile.setExecutable(true, true);



        String document = replaceFields(template, owner, new HashMap<>());
        String pathToFile = temporaryFilePath + owner.getPersonalAccountId() + ".html";
        String pathToPdf = temporaryFilePath + owner.getPersonalAccountId() + ".pdf";

        ResourceHelper.saveFile(pathToFile, document);
        try {
            String converterPath = wkhtmltopdfFile.getPath().toString();
            logger.info(converterPath);
            String command = converterPath + " "
                    + pathToFile + " " + pathToPdf;
//            Process p = Runtime.getRuntime().exec(converterPath + " " + pathToFile + " " + pathToPdf);
            Process p = Runtime.getRuntime().exec(command);
//            logger.info("exitStatus = " + p.exitValue());
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }
            p.waitFor();
            logger.info("exitStatus = " + p.exitValue());
            System.out.println("ok!");

            in.close();
        } catch (Exception e) {
            logger.error("Can't convert to pdf, message: " + e.getMessage());
            e.printStackTrace();
//            return new FileSystemResource(new File(pathToFile));
        }
        try {
//            response.setContentType("text/html; charset=utf-8");
            response.setContentType("application/pdf");
            response.setHeader("Content-disposition", "attachment; filename=" + accountId + "_contract.pdf");
//            ServletOutputStream out = response.getOutputStream();
//            out.println("Dfasdgasdgasdg");
//            out.flush();
//            out.close();
            ByteArrayOutputStream baos;
            baos = convertFileToByteArrayOutputStream(pathToPdf);
            OutputStream os = response.getOutputStream();
            baos.writeTo(os);
            os.flush();
        } catch (IOException e) {
            logger.error("Не удалось отдать договор");
            e.printStackTrace();
        }

//        return new FileSystemResource(new File(pathToPdf));
    }

    private boolean prepareWkhtmltopdf(){
//        Resource resource = applicationContext.getResource("classpath:wkhtmltox");
//        try {
//
//            resource.getInputStream();
//
//            File src = resource.getFile();
//            ResourceHelper.copyFolder(src, new File("/tmp/wkhtmltox"));
//
//        } catch (IOException e) {
//            logger.error("catch exception in prepareWkhtmltopdf, message: " + e.getMessage());
//            e.printStackTrace();
//            return false;
//        }
//        return true;
        throw new ParameterValidationException("wkhtmltopdf");
    }

//    public static void copyFolder(File src, File dest)
//            throws IOException{
//
//        if(src.isDirectory()){
//
//            //if directory not exists, create it
//            if(!dest.exists()){
//                dest.mkdir();
//                System.out.println("Directory copied from "
//                        + src + "  to " + dest);
//            }
//
//            //list all the directory contents
//            String files[] = src.list();
//
//            for (String file : files) {
//                //construct the src and dest file structure
//                File srcFile = new File(src, file);
//                File destFile = new File(dest, file);
//                //recursive copy
//                copyFolder(srcFile,destFile);
//            }
//
//        }else{
//            //if file, then copy it
//            //Use bytes stream to support all file types
//            InputStream in = new FileInputStream(src);
//            OutputStream out = new FileOutputStream(dest);
//
//            byte[] buffer = new byte[1024];
//
//            int length;
//            //copy the file content in bytes
//            while ((length = in.read(buffer)) > 0){
//                out.write(buffer, 0, length);
//            }
//
//            in.close();
//            out.close();
//            System.out.println("File copied from " + src + " to " + dest);
//        }
//    }
//
//    private void saveFile(String fileName, String content){
//        BufferedWriter bw = null;
//        FileWriter fw = null;
//        try {
//            fw = new FileWriter(fileName);
//            bw = new BufferedWriter(fw);
//            bw.write(content);
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                if (bw != null)
//                    bw.close();
//                if (fw != null)
//                    fw.close();
//            } catch (IOException ex) {
//                ex.printStackTrace();
//            }
//        }
//    }

    private String createTemplate(String header, String body, String footer, List<Integer> noFooterPages) {
                //TODO генерация шаблона
                return header + body + footer;
    }

    private String replaceFields(String template, AccountOwner owner, Map<String, String> params){
        Map<String, String> replaceMap = buildReplaceParameters(owner, params);

        for (Map.Entry<String, String> entry : replaceMap.entrySet()) {
            try {
                template = template.replaceAll(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                throw new ParameterValidationException("Не удалось заполнить договор");
            }
        }
        return template;
    }

    private String getHeader() throws IOException {
        InputStream inputStream = this.getClass()
                .getResourceAsStream("/contract/budget_contract_header.html");

       return CharStreams.toString(new InputStreamReader(inputStream, Charsets.UTF_8));
    }

    private Map<String, String> buildReplaceParameters(AccountOwner owner, Map<String, String> params){

        PersonalAccount account = personalAccountManager.findOne(owner.getPersonalAccountId());

        Map<String, String> replaceMap = new HashMap<>();

        /*Заказчик: #URNAME#                    обязательно
        Юридический адрес: #URADR#              обязательно
        Почтовый адрес: #PADR#                  обязательно
        ИНН: #INN# КПП: #KPP#                   обязательно
        ОГРН: #OGRN#                            обязательно
        ОКПО: #OKPO#                            необязательно
        ОКВЭД: #OKVED#                          необязательно
        Телефон: #TEL#                          обязательно
        Факс: #FAX#                             необязательно
        Наименование банка: #BANKNAME#          необязательно
        Расчетный счет: #RASSCHET#              необязательно
        БИК: #BIK#                              необязательно
        Корреспондентский счет: #KORSCHET#      необязательно*/
        replaceMap.put("#NUMER#", account.getAccountId());
        replaceMap.put("#DEN#", String.valueOf(LocalDate.now().getDayOfMonth()));
        replaceMap.put("#MES#", String.valueOf(LocalDate.now().getMonthValue())); //месяц надо в виде слова
        replaceMap.put("#YAR#", String.valueOf(LocalDate.now().getYear()));
        replaceMap.put("#URNAME#", owner.getName());
        replaceMap.put("#URFIO#", "ИМЯ ВЛАДЕЛЬЦА В РОДИТЕЛЬНОМ ПАДЕЖЕ"); //названия кого-то там, кого передал юзер
        replaceMap.put("#USTAVA#", "УСТАВ ИЛИ ЧЕ ТАМ У НИХ В РОДИТЕЛЬНОМ ПАДЕЖЕ"); // на основании устава
        replaceMap.put("#URADR#", owner.getContactInfo().getPostalAddress());
        replaceMap.put("#PADR#", owner.getPersonalInfo().getAddress());
        replaceMap.put("#TEL#", owner.getPersonalInfo().getNumber() != null && !owner.getPersonalInfo().getNumber().equals("") ? owner.getPersonalInfo().getNumber() : "НОМЕР ТЕЛЕФОНА");
        replaceMap.put("#FAX#", owner.getPersonalInfo().getNumber() != null && !owner.getPersonalInfo().getNumber().equals("") ? owner.getPersonalInfo().getNumber() : "НОМЕР ТЕЛЕФОНА");
        replaceMap.put("#BANKNAME#", owner.getContactInfo().getBankName());
        replaceMap.put("#RASSCHET#", owner.getContactInfo().getBankAccount());
        replaceMap.put("#KORSCHET#", owner.getContactInfo().getCorrespondentAccount());
        replaceMap.put("#BIK#", owner.getContactInfo().getBik());
        replaceMap.put("#INN#", owner.getPersonalInfo().getInn());
        replaceMap.put("#KPP#", owner.getPersonalInfo().getKpp());
        replaceMap.put("#OKPO#", owner.getPersonalInfo().getOkpo() != null ? owner.getPersonalInfo().getOkpo() : "ОКПО");
        replaceMap.put("#OKVED#", owner.getPersonalInfo().getOkvedCodes() != null ? owner.getPersonalInfo().getOkvedCodes() : "ОКВЕД");
        replaceMap.put("#OGRN#", owner.getPersonalInfo().getOgrn());
        replaceMap.put("#PAGE#", "\n<pdf:pagenumber>\n"); //надо определять page при подстановке футера

        //это надо делать отдельно от подстановки юзерских параметров
        replaceMap.put("#FONT_PATH#", "/home/val/arial.ttf");
        return replaceMap;
    }

}
