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
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.GeneralSecurityException;
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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import school.student.NewStudentBean;
import org.apache.commons.io.FilenameUtils;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

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
    private String uploadMsg="";
    private boolean status = false;
    private int STREAM_BUFFER = 512000;
    private String actualPath;
    private String adminUser, adminPassword;
    private UploadedFile uploadSql = null; 
    private  boolean uploadFileSuccess = false;
	
	private ServletContext servletContext;
    
     
    /**
     * Creates a new instance of backupRestoreBean
     * @throws java.io.UnsupportedEncodingException
     */
    public backupRestoreBean() throws UnsupportedEncodingException {
        //get the build/web/WEB-INF/classess directory absolute path
       /* webInf_ClassesPath=this.getClass().getResource("/").getPath(); 
        fullPath =URLDecoder.decode(webInf_ClassesPath,"UTF-8"); 
        //get build/web directory absolute path
        actualPath = fullPath.split("/WEB-INF/classes")[0].substring(1); */
		//get the real path to the server ie. build/web/
        ServletContext servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
        actualPath = servletContext.getRealPath("");
    }
    
    public void setAdminUser(String adminUser){
        this.adminUser=adminUser;
    }
    
     public void setAdminPassword(String adminPassword){
        this.adminPassword=adminPassword;
    }
     
    public String getAdminUser(){
        return this.adminUser;
    } 
    
    public String getAdminPassword(){
        return this.adminPassword;
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
    
    public boolean getUploadFileSuccess(){
        return this.uploadFileSuccess;
    }
    
    public void setUploadMsg(String uploadMsg){
        this.uploadMsg = uploadMsg;
    }
    
    public String getUploadMsg(){
        return this.uploadMsg;
    }
    
    public String getRestoreMsg(){
        return this.restoreMsg;
    }
    
    public String getBackupMsg(){
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
        String sql="SELECT fileName, filePath, createdDateTime, storage FROM school.sch_backup_db  WHERE `storage`='server' ORDER BY `createdDateTime` DESC LIMIT 1";
        try{
            Statement st = DBConnect.getConnection().createStatement();
            ResultSet rs = st.executeQuery(sql);
            while(rs.next()){
                backupData = new DbBackupData();
                backupData.setFileName(rs.getString("fileName"));
                backupData.setFilePath("/School_Management"+rs.getString("filePath"));
                backupData.setCreatedDateTime(rs.getString("createdDateTime"));
                backupData.setStorage(rs.getString("storage"));
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
    public String oneClickBackup() throws IOException  {
       FacesContext context = FacesContext.getCurrentInstance();
       
       backupDatabase("backup-root","server", "");
        if(status){
            backupMsg="Backup created successfully for - database "+this.dbName;
        } else {
            backupMsg="Could not create backup for - database "+this.dbName;
        }
        /*
        *Redirect to backupRestore.xhtml
        */
        FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", backupMsg);
        FacesContext.getCurrentInstance().addMessage("warn", msg);
       return "backupRestore";
    }
    
	/*
	*used to backup sql file into google drive 
	*
	*/
    public String backupToGoogleDrive() throws GeneralSecurityException, Exception {
       String query = "SELECT u.user_id, r.role_name, u.isactive "
            + "FROM sch_user u "
            + "JOIN sch_user_role r "
            + "WHERE uname=? AND password=? AND u.role_id = r.role_id;";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            /*
            if connection to mysql driver is failed error message is thrown;
            */
          if(DBConnect.getConnection() == null){
              this.backupMsg="Failed driver connection";
          }
          ps = DBConnect.getConnection().prepareStatement(query);
          ps.setString(1, this.adminUser);
          ps.setString(2, this.adminPassword);
          rs = ps.executeQuery();
            if(rs.next()){
                DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy hh-mm-ss");
                Date date = new Date();
                dateFormat.format(date);
                String fileName = "backup-root-"+date.getTime()+".sql";
                boolean status = backupDatabase("backup-root-gdrive","Google Drive", fileName);
                if(status){
                    GDrive gDrive = new GDrive();
                    status = gDrive.uploadToDrive(actualPath+"/resources/backup/backup-root-gdrive.sql", fileName);
                    if(status){
                        backupMsg = "succesfully backup into google drive";
                    } else {
                        backupMsg = "backup to google drive failed";
                    }
                }
            } else{
                FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_WARN, "Error", "Invalid Username or password");
                FacesContext.getCurrentInstance().addMessage("warn", msg);
                return null;
            }
        }catch (Exception ex) {
                System.out.println(ex);
        } finally {
            this.adminPassword = null;
            this.adminUser = null;
           
        }
        FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", backupMsg);
        FacesContext.getCurrentInstance().addMessage("warn", msg);
        return "backupRestore";
    }
    
    /**
     * This method is used for backup the mysql database
     * and store records(backup file name, created time) into database
     * @param fileName
     * @param filename file name for backup sql
     * @param gDriveFilename
     * @param storage storage name like google drive, server 
     * @return the status of the backup true/false
     */
    
    public boolean backupDatabase(String fileName,String storage, String gDriveFilename) {
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
                DateFormat dateFormat = new SimpleDateFormat("dd_MM_yyyy hh_mm_ss");
                Date date = new Date();
                fileName = fileName+"_" + dateFormat.format(date) + ".sql";
                fileName = (gDriveFilename.isEmpty())?fileName:gDriveFilename;
                String filepath = backupPath + fileName;
                String backupFilePath = backupfile + fileName;
                // Write SQL DUMP file
                File filedst = new File(filepath);
                FileOutputStream dest = new FileOutputStream(filedst);
                dest.write(data);
                dest.close();
                
                //record backup file into database
                String query = "INSERT INTO school.sch_backup_db (fileName, createdDateTime, filePath, storage)"
                        + "VALUES (?, ?, ?, ?)";
                try{
                    Connection con = DBConnect.getConnection();
                    PreparedStatement ps = con.prepareStatement(query);
                    ps.setString(1, fileName);
                    java.sql.Timestamp sqlDate;
                    sqlDate = new java.sql.Timestamp(date.getTime());
                    ps.setTimestamp(2,  sqlDate);
                    ps.setString(3, backupFilePath);
                    ps.setString(4, storage);
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
    public String oneClickRestore() throws IOException, InterruptedException{
            String absPath =actualPath+ "/resources/backup/backup-root.sql";
            File dir = new File(actualPath+"/resources/backup/");
            File[] files = dir.listFiles();
            Arrays.sort(files);
            File backupSqlFile = files[files.length - 1];
/*
            System.out.println("\n\n\n++++++++++++++++list of files+++++++++\n\n\n");
            for(File f : files){
                if(f.isFile()){
                    String file_ext=FilenameUtils.getExtension(f.getAbsolutePath());
                    if(file_ext.equals("sql"))
                        System.err.println(f.getName());
                }
            }*/
            
            return mysqlRestore(backupSqlFile, backupSqlFile.getAbsolutePath());
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
    
    /**
     * 
     * @param backupSqlFile is mysqldump sql file 
     * @param absPath is path to mysql dump sql file
     * @throws InterruptedException
     * @throws IOException 
     */
    private String mysqlRestore(File backupSqlFile, String absPath) throws InterruptedException, IOException{
        if(backupSqlFile.exists()){
                //use getMysqlPath()+"mysql" if mysql is not set in environment variable
                String[] cmd = new String[]{"mysql", "--user="+dbUser, "--password="+dbPwd, "-e", "\"source", absPath+"\""};
                String command =Arrays.stream(cmd).collect(Collectors.joining(" "));
                Process runtimeProcess;
                try{
					//execute mysql.exe process with specified arguements to restora database from sql file
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
                FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", restoreMsg);
                FacesContext.getCurrentInstance().addMessage("warn", msg);
            } else {
               FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Sorry we could not find any backup file.");
               FacesContext.getCurrentInstance().addMessage("error", msg);
            }
       
        return "backupRestore";
    }
    
   /**
    * 
    * @param event event occured when upload button is pressed
    * @throws IOException 
     * @throws java.lang.InterruptedException 
    */
    public void uploadSqlFile(FileUploadEvent event) throws IOException, InterruptedException {
		//FileUploadEvent is triggered when user upload the file
		//UploadedFile uploadSql holds the uploaded file 
        this.uploadSql = event.getFile();
        String ct = uploadSql.getContentType();
		//Check if content type of uploaded file is "application/octet-stream", which is content type of sql 
		//but sometime sql file is of text/plain so checking for both
        if(ct.equals("application/octet-stream") || ct.equals("text/plain")){
            if(UploadFile()){
                setUploadMsg("Uploaded File name is :: "+uploadSql.getFileName()+" Upload is succesful.");
            } else {
                setUploadMsg("Uploading file failed. Please try again later.");
            }
        } else {
            setUploadMsg("Uploaded File should be SQL file.");
        }
        FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", uploadMsg);
        FacesContext.getCurrentInstance().addMessage("warn", msg);
    }
    
    /**
     * upload sql file
     * @return
     * @throws IOException 
     */
    private boolean UploadFile() throws IOException, InterruptedException{
        InputStream inputStream = null;
        OutputStream outputStream = null;
        FacesContext context=FacesContext.getCurrentInstance();
               
        if(uploadSql.getSize()> 0){        
            /*
            * Destination  where the file will be uploaded
            */
            DateFormat dateFormat = new SimpleDateFormat("dd_MM_yyyy hh_mm_ss");
            Date date = new Date();
	    String fileName= "uploaded_backup_"+dateFormat.format(date)+".sql";
            String fileUploadDestination = actualPath+File.separator+"resources"+File.separator+"upload_restore"+File.separator;
            File outputDir = new File(fileUploadDestination);
            if(!outputDir.isDirectory()){
                outputDir.mkdir();
            }
            
            fileUploadDestination=fileUploadDestination +fileName;
            File outputFile = new File(fileUploadDestination);
            
			//read the input stream from uploaded File
            inputStream = uploadSql.getInputstream();
			//instantiate outputStream  for outputFile
            outputStream = new FileOutputStream(outputFile);
            byte[] buffer = new byte[1024];
            int bytesRead = 0;
			
			//reads the next byte of the data from the the input stream and store into speficied array i.e. "buffer"
            while((bytesRead = inputStream.read(buffer)) != -1){
				//writes len bytes i.e. "bytesRead" from the specified byte array i.e. "buffer" starting at offset i.e. "0" to this file output stream.
                outputStream.write(buffer, 0, bytesRead);
            }
            if(outputStream != null){
                outputStream.flush();
                outputStream.close();
            }
            
            if(inputStream !=null ){
                inputStream.close();
            }
            uploadFileSuccess = true;
			//now restore uploaded sql file;
            mysqlRestore(outputFile, fileUploadDestination);
        }
        return uploadFileSuccess;
    }
    
    /**
     * list all backup of database in a server where website is hosted
     * @return list of database file; DbBackupData class
     * @throws IOException 
     */
    public List<DbBackupData> ListFiles() throws IOException{
        String filePath = actualPath+"/resources/backup";
        File directory = new File(filePath);
        //list all files in the directory backup;
        File[] files = directory.listFiles();
        List<DbBackupData> backupList = new ArrayList<DbBackupData>();
        //loop each and every file inside directory and list them
        for(File f : files){
            //check if the object f is a file or not
            if(f.isFile()){
                //get the extention of file
                String file_ext=FilenameUtils.getExtension(f.getAbsolutePath());
                //check if file extention is sql or not 
                if(file_ext.equals("sql")){
                    //use to get the file attribute detail like attribute name, file created date time and so on.
                    BasicFileAttributes attr = Files.readAttributes(f.toPath(), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                    backupList.add(new DbBackupData(f.getName(), "/School_Management",f.getAbsolutePath().substring(actualPath.length()-1),attr.creationTime().toString()));
                }
            }
        }
        return backupList;
    }
    
    /**
     * restore  selected database from list of databases;
     */
    public void RestoreFromSelected(String filePath) throws InterruptedException, IOException{
       String absolutePath = actualPath + filePath;
        mysqlRestore(new File(absolutePath), absolutePath);
    }
}
