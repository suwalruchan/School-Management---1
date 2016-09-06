/*
 * Store information about backup sql data
 */
package school.util;

/**
 *
 * @author rmale
 */
public class DbBackupData {
    private String fileName="";
    private String filePath="";
    private String createdDateTime="";
    private String storage="";
    private String projectName;
    
    public DbBackupData(){}
    
    public DbBackupData(String fileName,String projectName, String filePath, String createdDateTime){
        this.fileName = fileName;
        this.filePath = filePath;
        this.createdDateTime = createdDateTime;
        this.projectName = projectName;
    }
    
    public void setFileName(String fileName){
        this.fileName = fileName;
    }
    
    public String getFileName(){
        return this.fileName;
    }
    
    public String getProjectName(){
        return this.projectName + this.filePath;
    }
    

    public void setFilePath(String filePath){
        this.filePath = filePath;
    }
    
    public String getFilePath(){
        return this.filePath;
    }
    
    public void setCreatedDateTime(String dateTime){
        this.createdDateTime=dateTime;
    }
    
    public String getCreatedDateTime(){
        return this.createdDateTime;
    }
   
        public void setStorage(String storage){
        this.storage = storage;
    }
    
    public String getStorage(){
        return this.storage;
    }
}
