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
    
       public void setFileName(String fileName){
        this.fileName = fileName;
    }
    
    public String getFileName(){
        return this.fileName;
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
   
}
