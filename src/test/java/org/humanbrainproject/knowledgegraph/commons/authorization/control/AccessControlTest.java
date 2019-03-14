package org.humanbrainproject.knowledgegraph.commons.authorization.control;

import org.humanbrainproject.knowledgegraph.commons.authorization.entity.AccessRight;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.OidcAccessToken;
import org.humanbrainproject.knowledgegraph.testFactory.TestObjectFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class AccessControlTest {

    AccessControl controller;

    @Before
    public void setup(){
        controller = new AccessControl();
    }

    @Test
    public void getAccessRights(){
        //given
        OidcAccessToken token = new OidcAccessToken();
        AccessRight right = new AccessRight("foo", AccessRight.Permission.READ);
        controller.tokenToAccessRights.put(token, new HashSet<>(Arrays.asList(right)));

        //when
        Set<AccessRight> accessRights = controller.getAccessRights(token);

        //then
        assertEquals(1, accessRights.size());
        AccessRight firstRight = accessRights.iterator().next();
        assertEquals("foo", firstRight.getPath());
        assertTrue(firstRight.isReadOnly());
        assertFalse(firstRight.canWrite());
    }


    @Test
    public void getReadableOrganizationsNullWhitelist(){
        //given
        OidcAccessToken token = new OidcAccessToken();
        AccessRight right = new AccessRight("foo", AccessRight.Permission.READ);
        controller.tokenToAccessRights.put(token, new HashSet<>(Arrays.asList(right)));

        //when
        Set<String> readableOrganizations = controller.getReadableOrganizations(token, null);

        //then
        assertEquals(1, readableOrganizations.size());
        assertEquals("foo", readableOrganizations.iterator().next());
    }

    @Test
    public void getReadableOrganizationsWithWhitelistMatch(){
        //given
        OidcAccessToken token = new OidcAccessToken();
        AccessRight right = new AccessRight("foo", AccessRight.Permission.READ);
        controller.tokenToAccessRights.put(token, new HashSet<>(Arrays.asList(right)));

        //when
        Set<String> readableOrganizations = controller.getReadableOrganizations(token, Arrays.asList("foo", "bar"));

        //then
        assertEquals(1, readableOrganizations.size());
        assertEquals("foo", readableOrganizations.iterator().next());
    }

    @Test
    public void getReadableOrganizationsWithWhitelistNoMatch(){
        //given
        OidcAccessToken token = new OidcAccessToken();
        AccessRight right = new AccessRight("foo", AccessRight.Permission.READ);
        controller.tokenToAccessRights.put(token, new HashSet<>(Arrays.asList(right)));

        //when
        Set<String> readableOrganizations = controller.getReadableOrganizations(token, Arrays.asList("bar"));

        //then
        assertEquals(0, readableOrganizations.size());
    }


    @Test
    public void isReadableTrue(){
        //given
        Map<String, Object> arangoInstance = TestObjectFactory.createArangoInstanceSkeleton("fooinstance", "foopermission");
        OidcAccessToken token = new OidcAccessToken();
        AccessRight right = new AccessRight("foopermission", AccessRight.Permission.READ);
        controller.tokenToAccessRights.put(token, new HashSet<>(Arrays.asList(right)));

        //when
        boolean readable = controller.isReadable(arangoInstance, token);

        //then
        assertTrue(readable);
    }

    @Test
    public void isReadableFalse(){
        //given
        Map<String, Object> arangoInstance = TestObjectFactory.createArangoInstanceSkeleton("fooinstance", "foopermission");
        OidcAccessToken token = new OidcAccessToken();
        AccessRight right = new AccessRight("barpermission", AccessRight.Permission.READ);
        controller.tokenToAccessRights.put(token, new HashSet<>(Arrays.asList(right)));

        //when
        boolean readable = controller.isReadable(arangoInstance, token);

        //then
        assertFalse(readable);
    }
}