/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package school.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.faces.context.FacesContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import school.student.NewStudentBean;

/**
 *
 * @author rmale
 */
public class backupRestoreBean {

    private String dbUser="root";
    private String dbPwd="password";
    private String dbName="school";
    private String backupMsg="";
    private String restoreMsg="";
    private boolean status = false;
    private int STREAM_BUFFER = 512000;
    String webInf_ClassesPath, fullPath, actualPath;
     
    /**
     * Creates a new instance of backupRestoreBean
     * @throws java.io.UnsupportedEncodingException
     */
    public backupRestoreBean() throws UnsupportedEncodingException {
        //get the build/web/WEB-INF/classess directory absolute path
        webInf_ClassesPath=this.getClass().getResource("/").getPath(); 
        fullPath =URLDecoder.decode(webInf_ClassesPath,"UTF-8"); 
        //get build/web directory absolute path
        actualPath = fullPath.split("/WEB-INF/classes")[0].substring(1); 
    }
    
     public void setDbUser(String dbUser){
        this.dbUser = dbUser;
    }
    
    public String getDbUser(){
        return this.dbUser;
    }
    
     public void setDbPwd(String dbPwd){
        this.dbPwd = dbPwd;
    }
    
    public String getDbPwd(){
        return this.dbPwd;
    }
    
     public void setDbName(String dbName){
        this.dbName = dbName;
    }
    
    public String getDbName(){
        return this.dbName;
    }
    
    public String getRestoreMsg(){
         try{
            java.util.Collection str = FacesContext.getCurrentInstance().getExternalContext().getRequestCookieMap().values();//.toString();
           Cookie[] cookies = (Cookie[]) str.toArray(new Cookie[str.size()]);
           boolean isCookieNull = true;
           for(Cookie cookie : cookies){
               if(cookie.getName() == "restoreMsg"){
                   this.restoreMsg = cookie.getValue();
                   isCookieNull = false;
                   break;
               }
           }
           if(isCookieNull){
               this.restoreMsg = "";
           }
        } catch (NullPointerException ex){
            Logger.getLogger(NewStudentBean.class.getName()).log(Level.SEVERE, null, ex);
        }
        return this.restoreMsg;
    }
    
    public String getBackupMsg(){
        try{
            java.util.Collection str = FacesContext.getCurrentInstance().getExternalContext().getRequestCookieMap().values();//.toString();
           Cookie[] cookies = (Cookie[]) str.toArray(new Cookie[str.size()]);
           boolean isCookieNull = true;
           for(Cookie cookie : cookies){
               if(cookie.getName() == "backupMsg"){
                   this.backupMsg = cookie.getValue();
                   isCookieNull = false;
                   break;
               }
           }
           if(isCookieNull){
               this.backupMsg = "";
           }
        } catch (NullPointerException ex){
            Logger.getLogger(NewStudentBean.class.getName()).log(Level.SEVERE, null, ex);
        }
        return this.backupMsg;
    }
    /*
    *list all database in a mysql server
    *currently no use of this method
    *for future reference
    */
    public List<backupRestoreBean> getAllDbName() throws UnsupportedEncodingException{
        List<backupRestoreBean> data = new ArrayList<backupRestoreBean>();
        String sql = "select database() as db;";
        try {
          Statement st = DBConnect.getConnection().createStatement();
          ResultSet rs = st.executeQuery(sql);
          while (rs.next()) {
            backupRestoreBean brBean = new backupRestoreBean();
            brBean.dbName=rs.getString("db");
            data.add(brBean);
          }
        } catch (SQLException ex) {
          Logger.getLogger(NewStudentBean.class.getName()).log(Level.SEVERE, null, ex);
        }
        return data;
    }
    
   /*
    *get information of all backup sql
    *since fiel is replaced at the time of backup database
    *this function list only one list of backup sql database
    */
    public List<DbBackupData> getBackup(){
         List<DbBackupData> data = new ArrayList<DbBackupData>();
          DbBackupData backupData = null;
        String sql="SELECT fileName, filePath, createdDateTime FROM school.sch_backup_db ORDER BY `createdDateTime` DESC LIMIT 1";
        try{
            Statement st = DBConnect.getConnection().createStatement();
            ResultSet rs = st.executeQuery(sql);
            while(rs.next()){
                backupData = new DbBackupData();
                backupData.setFileName(rs.getString("fileName"));
                backupData.setFilePath("/School_Management"+rs.getString("filePath"));
                backupData.setCreatedDateTime(rs.getString("createdDateTime"));
                //check if file exists
                File f = new File(actualPath+rs.getString("filePath"));
                if(f.exists() && !f.isDirectory()){
                    data.add(backupData);
                } else {
                    data.clear();
                }
                //totalBackupData++;
            }
        } catch (SQLException ex){
            
        }
        return data;
    }
    
    /*
    * This method is used for one click backup
    */
    public void oneClickBackup() throws IOException  {
       FacesContext context = FacesContext.getCurrentInstance();
       
       backupDatabase();
        if(status){
            backupMsg="\"Backup created successfully for - database \"+this.dbName";
        } else {
            backupMsg="Could not create backup for - database \"+this.dbName";
        }
        /*
        *Redirect to backupRestore.xhtml
        */
       Map<String, Object> properties =  new HashMap<String, Object>();
       properties.put("maxAge", 30);
       context.getExternalContext().addResponseCookie("backupMsg", backupMsg, properties);
       HttpServletResponse response = (HttpServletResponse)context.getExternalContext().getResponse();
       response.sendRedirect("backupRestore.xhtml");
    }
    
    /**
     * This method is used for backup the mysql database
     * and store records(backup file name, created time) into database
     * @return the status of the backup true/false
     */
    
    public boolean backupDatabase() {
        try {
            //directory where mysql backup file is stored
            String backupfile ="/resources/backup/";
            //use getMysqlPath()+"mysql" if mysql is not set in environment variable
            String mysqlDumpExePath = "mysqldump ";
            // get mysql database dump data
            String dump = getServerDumpData(this.dbUser, this.dbPwd, this.dbName, mysqlDumpExePath);
            //check the backup dump
            if (status && dump != "") {
                byte[] data = dump.getBytes();
               
                // Set backup folder
                String backupPath = actualPath + backupfile;

                // See if backup folder exists
                File file = new File(backupPath);
                if (!file.isDirectory()) {
                    // Create backup folder when missing. Write access is needed.
                    file.mkdir();
                }
                // Compose full path, create a filename as you wish
                DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy hh-mm-ss");
                Date date = new Date();
                String fileName = "backup-" + this.dbUser  + ".sql";//"-(" + dateFormat.format(date) + ").sql";
                String filepath = backupPath + fileName;
                String backupFilePath = backupfile + fileName;
                // Write SQL DUMP file
                File filedst = new File(filepath);
                FileOutputStream dest = new FileOutputStream(filedst);
                dest.write(data);
                dest.close();
                
                //record backup file into database
                String query = "INSERT INTO school.sch_backup_db (fileName, createdDateTime, filePath)"
                        + "VALUES (?, ?, ?)";
                try{
                    Connection con = DBConnect.getConnection();
                    PreparedStatement ps = con.prepareStatement(query);
                    ps.setString(1, fileName);
                    java.sql.Timestamp sqlDate;
                    sqlDate = new java.sql.Timestamp(date.getTime());
                    ps.setTimestamp(2,  sqlDate);
                    ps.setString(3, backupFilePath);
                    ps.executeUpdate();
                } catch (SQLException ex){
                    Logger.getLogger(NewStudentBean.class.getName()).log(Level.SEVERE, null, ex);
                } 
            } 
        } catch (Exception ex) {
             Logger.getLogger(NewStudentBean.class.getName()).log(Level.SEVERE, null, ex);
        }

        return status;
    }

    /*
    *used to restore sql database from file in the server
    *
    */
    public void oneClickRestore() throws IOException, InterruptedException{
       List<DbBackupData> data = getBackup();
       FacesContext context = FacesContext.getCurrentInstance();
        if(data.isEmpty()){
            restoreMsg="No backup is found. Please backup your database.";
        } else {
            String relBackup = data.get(0).getFilePath().split("/School_Management")[1];
            String absPath =actualPath+ relBackup;
            
            //use getMysqlPath()+"mysql" if mysql is not set in environment variable
            String[] cmd = new String[]{"mysql", "--user="+dbUser, "--password="+dbPwd, "-e", "\"source", absPath+"\""};
            String command =Arrays.stream(cmd).collect(Collectors.joining(" "));
            Process runtimeProcess;
            try{
                runtimeProcess = Runtime.getRuntime().exec(command);
                int i = runtimeProcess.waitFor();
                
                if(i == 0){
                    restoreMsg="Database backup restored succesfully.";
                } else {
                    restoreMsg = "Could not restore database backup";
                }
            } catch (IOException ex){
                 Logger.getLogger(backupRestoreBean.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
       
       Map<String, Object> sessionMap = context.getExternalContext().getSessionMap();
       sessionMap.put("restoreMsg", restoreMsg);
       HttpServletResponse response = (HttpServletResponse)context.getExternalContext().getResponse();
       response.sendRedirect("backupRestore.xhtml");
    }
    
    /*
    *@dumpdata = used to store result of mysqldump
    *convert @dumpdata into string and return back
    */
    private String getServerDumpData(String user, String password, String db, String mysqlDumpExePath) {
        StringBuilder dumpdata = new StringBuilder();
        try {
            if (user != null /* && password != null*/ && db != null) {
                String command[] = new String[]{mysqlDumpExePath,
                    "--user=" + user,
                    "--password=" + password,
                    "--skip-comments",
                    "--databases",
                    db};

                // Run mysqldump
                ProcessBuilder pb = new ProcessBuilder(command);
                Process process = pb.start();

                InputStream in = process.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(in));

                int count;
                char[] cbuf = new char[STREAM_BUFFER];

                // Read datastream
                while ((count = br.read(cbuf, 0, STREAM_BUFFER)) != -1) {
                    dumpdata.append(cbuf, 0, count);
                }

                //set the status
                int processComplete = process.waitFor();
                if (processComplete == 0) {                   
                    status = true;
                } else {
                    status = false;
                }
                // Close
                br.close();
                in.close();
            }

        } catch (Exception ex) {
            Logger.getLogger(NewStudentBean.class.getName()).log(Level.SEVERE, null, ex);
            return "";
        }
        return dumpdata.toString();
    }
    /*
    *get the mysql.exe path 
    *use this method to retrive mysql.exe path if it is not configured in environment variable
    */
    private String getMySqlPath(){
        Statement st = null;
        ResultSet rs  = null;
        String mySql_BaseDir="";
        try{
            st = DBConnect.getConnection().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
             rs = st.executeQuery("Select @@basedir");
            while(rs.next()){
                mySql_BaseDir=rs.getString(1);
            }
        } catch (SQLException ex){
            Logger.getLogger(backupRestoreBean.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        mySql_BaseDir = mySql_BaseDir +"/bin/";
        return mySql_BaseDir;
    }           
}
