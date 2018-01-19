package ru.majordomo.hms.personmgr.common;

import java.io.*;

public class FileUtils {

    public static void copyFolder(File src, File dest) throws IOException {
        if(src.isDirectory()){
            if(!dest.exists()){
                dest.mkdir();
            }
            copyFilesFromSrcToDest(src, dest);
        }else{
            copyFileTo(src, dest);
        }
    }

    public static void copyFilesFromSrcToDest(File src, File dest) throws IOException {
        //list all the directory contents
        String files[] = src.list();

        if (files == null) { return; }

        for (String file : files) {
            //construct the src and dest file structure
            File srcFile = new File(src, file);
            File destFile = new File(dest, file);
            //recursive copy
            copyFolder(srcFile,destFile);
        }
    }

    public static void copyFileTo(File src, File dest) throws  IOException{
        //if file, then copy it
        //Use bytes stream to support all file types
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dest);
        byte[] buffer = new byte[1024];
        int length;
        //copy the file content in bytes
        while ((length = in.read(buffer)) > 0){
            out.write(buffer, 0, length);
        }
        in.close();
        out.close();
    }

    public static void saveFile(String fileName, String content){

        File destFile = new File(fileName);
        saveFile(destFile, content);
    }

    public static void saveFile(File destFile, String content){
        BufferedWriter bw = null;
        FileWriter fw = null;
        try {
            fw = new FileWriter(destFile);
            bw = new BufferedWriter(fw);
            bw.write(content);
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

    public static void saveInputStreamToFile(InputStream inputStream, String filePath) throws IOException{
        FileOutputStream fos = new FileOutputStream(new File(filePath));
        int inByte;
        while((inByte = inputStream.read()) != -1)
            fos.write(inByte);
        inputStream.close();
        fos.close();
    }

    public static String getStringFromInputStream(InputStream inputStream) throws IOException {
        return getStringFromInputStream(inputStream, "UTF-8");
    }

    public static String getStringFromInputStream(InputStream inputStream, String charsetName) throws IOException {
        /*https://stackoverflow.com/a/35446009/5584853*/
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString(charsetName);//"UTF-8"
    }
}
