/*
 * Copyright 2020 EPFL/Human Brain Project PCO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.humanbrainproject.knowledgegraph.commons.logging;

import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LoggingUtils {
    private static MessageDigest md = null;

    public static final String hashUserId(String userId){
        String userHashedId = "";
        try{
            md = MessageDigest.getInstance("MD5");
        }catch(NoSuchAlgorithmException e){}finally {
            if(md != null){
                md.update(userId.getBytes());
                byte[] digest = md.digest();
                userHashedId = DatatypeConverter.printHexBinary(digest).toUpperCase();
            }
        }
        return userHashedId;
    }
}
