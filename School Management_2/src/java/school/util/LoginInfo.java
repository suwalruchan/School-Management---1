/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package school.util;

import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;
import school.teacher.TeacherBean;

/**
 *
 * @author rmale
 */
public class LoginInfo {
  private String oldPassword;
  private String newPassword;
  private String confirmPassword;
  private String userName;
  private int userId;
  
   public String getUserName() {
       HttpSession session =  LoginUtil.getSession();   
    this.userName =(String) session.getAttribute("username");
    return userName;
  }

  public String getOldPassword() {
    return oldPassword;
  }

  public void setOldPassword(String oldPassword) {
      try {
          //hashingPassword(param) method is used to change text into sha-256 bit hash code, one way encryption;
          this.oldPassword = HashingPassword.hashingPassword(oldPassword);
      } catch (NoSuchAlgorithmException ex) {
          Logger.getLogger(TeacherBean.class.getName()).log(Level.SEVERE, null, ex);
      }
  }

  public String getNewPassword() {
    return newPassword;
  }

  public void setNewPassword(String newPassword) {
     try {
         //hashingPassword(param) method is used to change text into sha-256 bit hash code, one way encryption;
          this.newPassword = HashingPassword.hashingPassword(newPassword);
      } catch (NoSuchAlgorithmException ex) {
          Logger.getLogger(TeacherBean.class.getName()).log(Level.SEVERE, null, ex);
      }
  }

  public String getConfirmPassword() {
    return confirmPassword;
  }

  public void setConfirmPassword(String confirmPassword) {
     try {
         //hashingPassword(param) method is used to change text into sha-256 bit hash code, one way encryption;
          this.confirmPassword = HashingPassword.hashingPassword(confirmPassword);
      } catch (NoSuchAlgorithmException ex) {
          Logger.getLogger(TeacherBean.class.getName()).log(Level.SEVERE, null, ex);
      }
  }

  public int getUserId() {
    HttpSession session =  LoginUtil.getSession();   
    this.userId= (Integer) (session.getAttribute("userId"));
    return userId;
  }

  /*
   *update user password 
   * @return 
   */
  public String savePassword(){
      if(verifyOldPassword()){
          if(this.newPassword.equals(this.confirmPassword)){
              try {
                String sql = "UPDATE sch_user SET password = ? WHERE user_id = ? and uname = ? ";
                PreparedStatement ps= DBConnect.getConnection().prepareStatement(sql);
                ps.setString(1, this.getNewPassword());
                ps.setInt(2, this.getUserId());
                ps.setString(3, this.userName);
                ps.executeUpdate();
                FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO,"Hip!! Hip!! Hurray!!!","Password has been successfully updated");
                FacesContext.getCurrentInstance().addMessage(null,msg);
            } catch (SQLException ex) {
                Logger.getLogger(TeacherBean.class.getName()).log(Level.SEVERE, null, ex);
                 FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,"Oops!!" ,"Something went wrong please try again later.");
                FacesContext.getCurrentInstance().addMessage(null,msg);
            } 
          } else {
               FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,"Oops!!" ,"Confirm password does not match with new password");
                FacesContext.getCurrentInstance().addMessage(null,msg);
          }
      } else {
          FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR,"Oops!!", "You have entered wrong old password");
          FacesContext.getCurrentInstance().addMessage(null,msg);
      }
     return "changePassword";
  }
  
  public boolean verifyOldPassword(){
                boolean verification = false; // true mean verified that is old password and new password are correct

      try {
          String sql = "SELECT 1 FROM sch_user WHERE user_id= ? and uname = ? and password = ?";
          PreparedStatement ps = DBConnect.getConnection().prepareStatement(sql);
          ps.setInt(1, this.getUserId());
          ps.setString(2, this.getUserName());
          ps.setString(3, this.getOldPassword());
          ResultSet rs  = ps.executeQuery();
          while(rs.next()){
              verification = true;
          }
      } catch (SQLException ex) {
          Logger.getLogger(TeacherBean.class.getName()).log(Level.SEVERE, null, ex);
      }
      return verification;
  }

    
}
