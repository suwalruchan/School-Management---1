/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package school.util;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import java.io.IOException;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import com.google.api.services.drive.DriveScopes;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.spi.FileTypeDetector;
import java.security.GeneralSecurityException;
import java.util.Collections;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import org.apache.jasper.tagplugins.jstl.core.Url;

/**
 *
 * @author rmale
 */
public class GDrive {
   
      /** Global Drive API client. */
  private static Drive drive;
   private static final java.io.File DATA_STORE_DIR =
      new java.io.File(System.getProperty("user.home"), ".credentials/drive-java-quickstart.json");

  /**
   * Global instance of the {@link DataStoreFactory}. The best practice is to make it a single
   * globally shared instance across your application.
   */
   private static final String APPLICATION_NAME = "SMS/1.0";
  private static FileDataStoreFactory dataStoreFactory;

  /** Global instance of the HTTP transport. */
  private static HttpTransport httpTransport;

  /** Global instance of the JSON factory. */
  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    /**
     * @param args the command line arguments
     */
  
   private static Credential authorize() throws Exception {
        ServletContext servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
        String basePath =  servletContext.getRealPath("");
        basePath = URLDecoder.decode(basePath, "UTF-8");
        System.out.println(basePath);
        File clientSecret = new File(basePath+"/resources/client_secret.json");
        if(clientSecret.exists()){
               // load client secrets
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
            new InputStreamReader( servletContext.getResourceAsStream("/resources/client_secret.json")));
            //servletContext.getResourceAsStream(clientSecret.getAbsolutePath());
            if (clientSecrets.getDetails().getClientId().startsWith("Enter")
                || clientSecrets.getDetails().getClientSecret().startsWith("Enter ")) {
              System.out.println(
                  "Enter Client ID and Secret from https://code.google.com/apis/console/?api=drive "
                  + "into drive-cmdline-sample/src/main/resources/client_secrets.json");
              System.exit(1);
            }
             GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets,
                Collections.singleton(DriveScopes.DRIVE_FILE)).setDataStoreFactory(dataStoreFactory)
                .build();
               AuthorizationCodeInstalledApp auth;
               LocalServerReceiver local = new LocalServerReceiver();
              auth = new AuthorizationCodeInstalledApp(flow,local );
               // authorize
              return auth.authorize("user");
        }
        return null;
   }
    
    public  boolean uploadToDrive(String filePathUrl, String fileName) throws FileNotFoundException, IOException, GeneralSecurityException, Exception {
        boolean status = false;
        // TODO code application logic here
        File mediaFile = new File(filePathUrl);
        com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
        fileMetadata.setName(fileName);
        InputStreamContent mediaContent =new InputStreamContent("application/octet-stream", new BufferedInputStream(new FileInputStream(mediaFile)));
	mediaContent.setLength(mediaFile.length());
                
        httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
        Credential credential = authorize();
        if(credential != null){
            drive = new Drive.Builder(httpTransport, JSON_FACTORY, credential).setApplicationName(
            APPLICATION_NAME).build();
                  Drive.Files.Create request = drive.files().create(fileMetadata, mediaContent);
                  request.getMediaHttpUploader().setProgressListener(new CustomProgressListener());
                  request.execute();
                  status = true;
        }
        return status;
    }
    
}

class CustomProgressListener implements MediaHttpUploaderProgressListener {
  public void progressChanged(MediaHttpUploader uploader) throws IOException {
    switch (uploader.getUploadState()) {
      case INITIATION_STARTED:
        System.out.println("Initiation has started!");
        break;
      case INITIATION_COMPLETE:
        System.out.println("Initiation is complete!");
        break;
      case MEDIA_IN_PROGRESS:
        System.out.println(uploader.getProgress());
        break;
      case MEDIA_COMPLETE:
        System.out.println("Upload is complete!");
    }
  }
}
