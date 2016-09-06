/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package school.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
/**
 *
 * @author rmale
 */
public  class  HashingPassword {
    /**
     * MessageDigest class provides applications the functionality of a message digest algorithm, such as SHA-1 or SHA-256.
     * Message digests are secure one-way hash functions that take arbitrary-sized data and output a fixed-length hash value.
     * @param password to change into sha-256 bit hash code
     * @return
     * @throws NoSuchAlgorithmException 
     */
    public static String hashingPassword(String password) throws NoSuchAlgorithmException{
        MessageDigest mDigest = MessageDigest.getInstance("SHA-256");
        byte [] result = mDigest.digest(password.getBytes());
        StringBuilder sb = new StringBuilder();
        for(int i=0; i < result.length; i++){
            //& mean logical AND 
            sb.append(Integer.toString((result[i] & 0xff)+ 0x10, 16).substring(1));
        }
        return sb.toString();
    }
}
