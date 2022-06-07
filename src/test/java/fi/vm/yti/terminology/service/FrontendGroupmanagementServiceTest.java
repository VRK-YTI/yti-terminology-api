package fi.vm.yti.terminology.service;

import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.Role;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.frontend.FrontendGroupManagementService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@Import({
        FrontendGroupManagementService.class,
})
@TestPropertySource(properties = {
        "groupmanagement.url=http://local.invalid"
})
public class FrontendGroupmanagementServiceTest {

    @MockBean
    AuthenticatedUserProvider userProvider;

    @MockBean
    RestTemplate restTemplate;

    @Autowired
    FrontendGroupManagementService service;

    @Captor
    ArgumentCaptor<String> urlCaptor;

    YtiUser mockUser = new YtiUser(
            "test@test.invalid",
            "firstname",
            "lastname",
            UUID.fromString("384c982c-7254-43df-ab7c-5037b6fb71c0"),
            false,
            false,
            LocalDateTime.of(2005, 4, 2, 1, 10),
            LocalDateTime.of(2006, 4, 2, 1, 10),
            Collections.emptyMap(),
            "",
            "");

    @BeforeEach
    public void setUp() {
        when(userProvider.getUser()).thenReturn(mockUser);

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(), any(Class.class)))
            .thenReturn(new ResponseEntity("", HttpStatus.OK));
    }

    @Test
    public void testAddDefaultRole() throws Exception {
        var orgId = UUID.randomUUID();

        service.sendRequest(orgId, null);

        verify(restTemplate).exchange(urlCaptor.capture(), eq(HttpMethod.POST), eq(null), eq(String.class));

        URL url = new URL(urlCaptor.getValue());

        assertTrue(url.getQuery().contains("userId=" + mockUser.getId()));
        assertTrue(url.getQuery().contains("role=" + Role.TERMINOLOGY_EDITOR.toString()));
        assertTrue(url.getQuery().contains("organizationId=" + orgId.toString()));
    }

    @Test
    public void testAddMultipleRoles() throws Exception {
        var orgId = UUID.randomUUID();

        service.sendRequest(orgId, new String[] {
                Role.CODE_LIST_EDITOR.toString(),
                Role.TERMINOLOGY_EDITOR.toString()
        });

        verify(restTemplate).exchange(urlCaptor.capture(), eq(HttpMethod.POST), eq(null), eq(String.class));

        URL url = new URL(urlCaptor.getValue());

        assertTrue(url.getQuery().contains("role=" + Role.CODE_LIST_EDITOR.toString()));
        assertTrue(url.getQuery().contains("role=" + Role.TERMINOLOGY_EDITOR.toString()));
    }
}
