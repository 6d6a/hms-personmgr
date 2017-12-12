package ru.majordomo.hms.personmgr.service;

import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Service
public class PdfService {


    public FileSystemResource convertHtmlToPdf(String html) {
        //TODO надо использовать бинарник конвертера из ресурсов в JAR

        saveFile("contract.html", html);

        try {
            Runtime rt = Runtime.getRuntime();
            Process ps = rt.exec("./src/main/resources/wkhtmltopdf contract.html contract.pdf");
        } catch (IOException e){
            return null;
        }

        File file = new File("contract.pdf");
        return new FileSystemResource(file);
    }

    @Deprecated
    private void saveFile(String fileName, String content){
        BufferedWriter bw = null;
        FileWriter fw = null;
        try {
            fw = new FileWriter(fileName);
            bw = new BufferedWriter(fw);
            bw.write(content);
            System.out.println("Done");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null)
                    bw.close();
                if (fw != null)
                    fw.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
