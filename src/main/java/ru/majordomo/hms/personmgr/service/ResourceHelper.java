package ru.majordomo.hms.personmgr.service;

import java.io.*;

public class ResourceHelper {

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
        //System.out.println("File copied from " + src + " to " + dest);

    }

    public static void saveFile(String fileName, String content){
        BufferedWriter bw = null;
        FileWriter fw = null;
        try {
            fw = new FileWriter(fileName);
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
}
